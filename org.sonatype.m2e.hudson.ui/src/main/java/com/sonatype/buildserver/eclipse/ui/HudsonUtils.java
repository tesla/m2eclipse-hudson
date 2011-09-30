/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui;

import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.console.HudsonConsole;
import com.sonatype.buildserver.eclipse.test.HudsonTestSession;
import com.sonatype.buildserver.eclipse.ui.job.view.SingleJobView;
import com.sonatype.buildserver.hudsonclient.HudsonRestFactory;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.HudsonManager;
import com.sonatype.buildserver.monitor.HudsonMonitor;
import com.sonatype.buildserver.monitor.CompositeMonitor;
import com.sonatype.buildserver.monitor.ErrorJob;
import org.hudsonci.rest.client.HandshakeFailedException;
import org.hudsonci.rest.client.InvalidResponseException;
import org.hudsonci.rest.model.build.BuildDTO;
import org.hudsonci.rest.model.build.BuildResultDTO;
import org.hudsonci.rest.model.build.TestsDTO;
import org.hudsonci.rest.model.project.ProjectDTO;
import com.sun.jersey.api.client.ClientHandlerException;

@SuppressWarnings( "restriction" )
public class HudsonUtils
{
    private static Logger log = LoggerFactory.getLogger( HudsonUtils.class );

    public static final void openJob( HudsonJob o )
    {
        if ( o.getJobDetails() instanceof ErrorJob )
        {
            // do not show the summary page for jobs that are not ok..
            ProjectDTO det = o.getJobDetails();
            if ( det != null && det.getUrl() != null )
            {
                openUrl( det.getUrl() );
            }
            return;
        }
        SingleJobView.openEditor( o );
    }

    public static final void openUrl( String url )
    {
        try
        {
            log.debug( "Opening URL: " + url ); //$NON-NLS-1$
            IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
            IWebBrowser browser =
                support.isInternalWebBrowserAvailable() ? support.createBrowser( IWorkbenchBrowserSupport.AS_EDITOR
                    | IWorkbenchBrowserSupport.LOCATION_BAR | IWorkbenchBrowserSupport.NAVIGATION_BAR
                    | IWorkbenchBrowserSupport.STATUS, null, url, url ) : support.getExternalBrowser();
            browser.openURL( new URL( url ) );
        }
        catch ( PartInitException partInitException )
        {
            StatusManager.getManager().handle( partInitException, HudsonUIActivator.PLUGIN_ID );
        }
        catch ( MalformedURLException malformedURLException )
        {
            StatusManager.getManager().handle( new Status( IStatus.ERROR, HudsonUIActivator.PLUGIN_ID,
                                                           Messages.HudsonUtils_error_open_url + url,
                                                           malformedURLException ) );
        }

    }

    private static JUnitModel getJUnitModel()
    {
        Class pluginClass = null;
        try
        {
            pluginClass = Class.forName( "org.eclipse.jdt.internal.junit.JUnitCorePlugin" ); //$NON-NLS-1$
        }
        catch ( ClassNotFoundException cnf1 )
        {
            try
            {
                pluginClass = Class.forName( "org.eclipse.jdt.internal.junit.ui.JUnitPlugin" ); //$NON-NLS-1$
            }
            catch ( ClassNotFoundException cnf2 )
            {
                log.error( "Could not find JUnit plugin class." ); //$NON-NLS-1$
            }
        }

        Object pluginObject;
        try
        {
            pluginObject = pluginClass.getDeclaredMethod( "getDefault", null ).invoke( pluginClass, null ); //$NON-NLS-1$
            return (JUnitModel) pluginObject.getClass().getDeclaredMethod( "getModel", null ).invoke( pluginObject, //$NON-NLS-1$
                                                                                                      null );
        }
        catch ( Exception e )
        {
            log.error( "Could not find junit related methods.", e ); //$NON-NLS-1$
        }
        return null;
    }

    public static final void openTests( final HudsonJob job, final BuildDTO build )
    {
        assert job != null && build != null;
        Job jb = new Job( NLS.bind( Messages.background_retrieve_tests, job.getJobName(), build.getNumber() ) )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                log.debug( "Getting test results for job {} / build {} from server {}", new Object[] { job.getJobName(), build.getId(), job.getServerName() } ); //$NON-NLS-1$
                try
                {
                    HudsonJob selected = job;
                    BuildDTO bld = build;
                    // now for matrix jobs attempt to find the best suiting configuration for delagation
                    // we delegate to the configuration because the matrix itself has no tests..
                    List<HudsonJob> descs = job.loadDescendants();
                    if ( descs.size() > 0 )
                    {
                        boolean failed =
                            build.getResult() == BuildResultDTO.FAILURE || build.getResult() == BuildResultDTO.UNSTABLE;
                        for ( HudsonJob bj : descs )
                        {
                            BuildDTO b = bj.loadBuild( build.getNumber() );
                            if ( b != null )
                            {
                                if ( failed )
                                {
                                    if ( b.getResult() == BuildResultDTO.FAILURE || b.getResult() == BuildResultDTO.UNSTABLE )
                                    {
                                        selected = bj;
                                        bld = b;
                                        log.debug( "Picked job {} failed configuration {}", new Object[] { job.getJobName(), selected.getJobName() } ); //$NON-NLS-1$
                                        break;
                                    }
                                }
                                else
                                {
                                    log.debug( "Picked job {} configuration {}", new Object[] { job.getJobName(), selected.getJobName() } ); //$NON-NLS-1$
                                    selected = bj;
                                    bld = b;
                                    break;
                                }
                            }
                        }
                    }
                    final TestsDTO tests = selected.getTests( bld );
                    final HudsonJob fSelected = selected;
                    final BuildDTO fBld = bld;
                    Display.getDefault().asyncExec( new Runnable()
                    {

                        public void run()
                        {
                            JUnitModel model = getJUnitModel();
                            // Search for session
                            String sessionName =
                                Messages.HudsonUtils_test_name_prefix + HudsonUtils.getJobTitle( fSelected, fBld );
                            for ( Object session : model.getTestRunSessions() )
                            {
                                if ( ( (ITestRunSession) session ).getTestRunName().equals( sessionName ) )
                                {
                                    model.removeTestRunSession( (TestRunSession) session );
                                    break;
                                }
                            }

                            model.addTestRunSession( new HudsonTestSession( sessionName, tests ) );
                            IWorkbenchPage workbench =
                                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                            try
                            {
                                workbench.showView( TestRunnerViewPart.NAME );
                            }
                            catch ( PartInitException e )
                            {
                                UIJob.errorStatus( e );
                            }

                        }
                    } );
                }
                catch ( Exception e )
                {
                    StatusManager.getManager().handle( new Status( IStatus.ERROR, HudsonUIActivator.PLUGIN_ID,
                                                                   "Failed to retrieve tests for job '"
                                                                       + job.getJobName() + "' build #"
                                                                       + build.getNumber(), e ), StatusManager.LOG );
                }
                return Status.OK_STATUS;
            }
        };
        jb.schedule();

    }

    public static final void openConsole( final HudsonJob job, final BuildDTO build )
    {
        assert job != null && build != null;
        Job jb =
            new Job( NLS.bind( "Retrieve console output for Hodson job {0} #{1}", job.getJobName(), build.getNumber() ) )
            {
                @Override
                protected IStatus run( IProgressMonitor monitor )
                {

                    IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

                    HudsonJob selected = job;
                    BuildDTO bld = build;
                    // now for matrix jobs attempt to find the best suiting configuration for delagation
                    // we delegate to the configuration because the matrix itself has no tests..
                    List<HudsonJob> descs = job.loadDescendants();
                    if ( descs.size() > 0 )
                    {
                        boolean failed =
                            build.getResult() == BuildResultDTO.FAILURE || build.getResult() == BuildResultDTO.UNSTABLE;
                        for ( HudsonJob bj : descs )
                        {
                            BuildDTO b = bj.loadBuild( build.getNumber() );
                            if ( b != null )
                            {
                                if ( failed )
                                {
                                    if ( b.getResult() == BuildResultDTO.FAILURE || b.getResult() == BuildResultDTO.UNSTABLE )
                                    {
                                        selected = bj;
                                        bld = b;
                                        log.debug( "Picked job {} failed configuration {}", new Object[] { job.getJobName(), selected.getJobName() } ); //$NON-NLS-1$
                                        break;
                                    }
                                }
                                else
                                {
                                    log.debug( "Picked job {} configuration {}", new Object[] { job.getJobName(), selected.getJobName() } ); //$NON-NLS-1$
                                    selected = bj;
                                    bld = b;
                                    break;
                                }
                            }
                        }
                    }

                    // Search for console
                    IConsole[] allConsoles = consoleManager.getConsoles();
                    String name = HudsonUtils.getJobTitle( selected, bld );
                    for ( int i = 0; i < allConsoles.length; i++ )
                    {
                        if ( HudsonConsole.HUDSON_TYPE.equals( allConsoles[i].getType() )
                              && allConsoles[i].getName().equals( name ) )
                        {
                            consoleManager.showConsoleView( allConsoles[i] );
                            HudsonConsole c = (HudsonConsole)allConsoles[i];
                            c.checkForUpdates();
                            consoleManager.refresh( allConsoles[i] );
                            return Status.OK_STATUS;
                        }
                    }

                    // Add console
                    HudsonConsole hc = new HudsonConsole( selected, bld );
                    consoleManager.addConsoles( new IConsole[] { hc } );
                    consoleManager.showConsoleView( hc );
                    return Status.OK_STATUS;
                }
            };
        jb.schedule();
    }

    /**
     * @param job
     * @param bld can be null
     * @return
     */
    public static String getJobTitle( HudsonJob job, BuildDTO bld )
    {
        if ( bld == null )
        {
            return job.getJobName();
        }
        return job.getJobName() + Messages.HudsonUtils_build_prefix + bld.getNumber();
    }

    private static long SECOND = 1000;

    private static long MINUTE = 60 * SECOND;

    private static long HOUR = 60 * MINUTE;

    private static long DAY = 24 * HOUR;

    private static long WEEK = 7 * DAY;

    private static long MONTH = 30 * DAY;

    public static String getFormattedLastRun( HudsonJob job )
    {
        if ( job.getJobDetails() == null || job.getJobDetails().getLastBuild() == null )
            return Messages.jobPopup_not_run;
        BuildDTO last = job.getJobDetails().getLastBuild();
        long before = last.getTimeStamp() + ( last.getDuration() != null ? last.getDuration() : 0 );
        long time = System.currentTimeMillis() - before;

        if ( time <= 2 * MINUTE )
        {
            return Messages.jobPopup_a_minute_ago;
        }
        else if ( time <= HOUR )
        {
            return NLS.bind( Messages.jobPopup_minutes_ago, ( time / MINUTE ) );
        }
        else if ( time <= 2 * HOUR )
        {
            return Messages.jobPopup_hour_ago;
        }
        else if ( time <= DAY )
        {
            return NLS.bind( Messages.jobPopup_hours_ago, ( time / HOUR ) );
        }
        else if ( time <= 2 * DAY )
        {
            return Messages.jobPopup_day_ago;
        }
        else if ( time <= WEEK )
        {
            return NLS.bind( Messages.jobPopup_days_ago, ( time / DAY ) );
        }
        else if ( time <= 2 * WEEK )
        {
            return Messages.jobPopup_week_ago;
        }
        else if ( time <= MONTH )
        {
            return NLS.bind( Messages.jobPopup_weeks_ago, ( time / WEEK ) );
        }
        else if ( time <= 2 * MONTH )
        {
            return Messages.jobPopup_month_ago;
        }
        return NLS.bind( Messages.jobPopup_months_ago, ( time / MONTH ) );
    }

    public static void runBuild( final HudsonJob job )
    {
        org.eclipse.core.runtime.jobs.Job jb =
            new org.eclipse.core.runtime.jobs.Job( NLS.bind( Messages.background_build_job, job.getJobName() ) )
            {
                @Override
                protected IStatus run( IProgressMonitor monitor )
                {
                    log.debug( "Triggering build job {} from server {}.", job.getJobName(), job.getServerName() ); //$NON-NLS-1$
                    IStatus status = job.build();
                    if ( !status.isOK() )
                    {
                        StatusManager.getManager().handle( status, StatusManager.SHOW | StatusManager.LOG );
                    } else {
                        // perform a refresh immediately to show the "pending" icon..
                        job.refresh();
                    }
                    return Status.OK_STATUS;
                }

            };
        jb.schedule();
    }

    public static String getFormattedDuration( BuildDTO build )
    {
        long time = build.getDuration();
        long hours = time / HOUR;
        time = time - hours * HOUR;
        long minutes = time / MINUTE;
        time = time - minutes * MINUTE;
        long seconds = time / SECOND;
        if ( hours > 0 )
        {
            return NLS.bind( Messages.HudsonUtils_hour_minute, hours, minutes );
        }
        else if ( minutes > 0 )
        {
            return NLS.bind( Messages.HudsonUtils_minute_second, minutes, seconds );
        }
        else
        {
            if ( seconds < 5 )
            {
                double secs = (double) time / (double) SECOND;
                return NLS.bind( Messages.HudsonUtils_second, MessageFormat.format( "{0,number,#.##}", secs ) ); //$NON-NLS-2$
            }
            return NLS.bind( Messages.HudsonUtils_second, seconds );
        }
    }

    public static String getHealthText( HudsonJob job )
    {
        Image im = HudsonImages.getHealthImage( job );
        if ( im == HudsonImages.HEALTH_81_PLUS_IMAGE )
        {
            return Messages.HudsonUtils_no_recent_fail;
        }
        if ( im == HudsonImages.HEALTH_61_TO_80_IMAGE )
        {
            return Messages.HudsonUtils_minority_fail;
        }
        if ( im == HudsonImages.HEALTH_41_TO_60_IMAGE )
        {
            return Messages.HudsonUtils_some_fail;
        }
        if ( im == HudsonImages.HEALTH_21_TO_40_IMAGE )
        {
            return Messages.HudsonUtils_majority_fail;
        }
        if ( im == HudsonImages.HEALTH_00_TO_20_IMAGE )
        {
            return Messages.HudsonUtils_all_fail;
        }
        return Messages.HudsonUtils_unknown_health;
    }

    /**
     * returns true if the given job is of Matrix job type (multi-configuration)
     * 
     * @param type
     * @return
     */
    public static boolean isMatrixType( ProjectDTO type )
    {
        return "hudson.matrix.MatrixProject".equals( type.getType() ); //$NON-NLS-1$
    }

    /**
     * returns true if the given job is a configuration of a matrix job
     * 
     * @param type
     * @return
     */
    public static boolean isMatrixConfig( ProjectDTO type )
    {
        return "hudson.matrix.MatrixConfiguration".equals( type.getType() ); //$NON-NLS-1$
    }

    /**
     * tries to associate a proper error message with a given exception returns getMessage if not such association was
     * found
     * 
     * @param e
     * @return
     */
    public static String restExceptionToString( Throwable e, String notFound404Message )
    {
        //TODO we currently don't differenciate handshake and non handshake http
        // results..
        boolean isHandshake = false;
        if ( e instanceof HandshakeFailedException )
        {
            isHandshake = true;
            HandshakeFailedException ex = (HandshakeFailedException) e;
            for ( Throwable t : ex.getFailures() )
            {
                e = t;
            }
        }
        if (e instanceof InvalidResponseException) {
            InvalidResponseException ie = (InvalidResponseException)e;
            //404 in handshake means no rest plugin
            if ( isHandshake && ie.getResponse().getStatus() == 404 ) {
                return "Hudson REST plugin not found at given URL.";
            }
            if (ie.getResponse().getStatus() == 404 && notFound404Message != null) {
                return notFound404Message;
            }
            //409 seems to indicate incompatibility.
            if ( isHandshake && ie.getResponse().getStatus() == 409) {
                return "Incompatible REST plugin on Hudson server. Client version is " + HudsonRestFactory.getClient().getVersion();
            }
            //400 bad request, unlikely to be thrown but..
            if ( isHandshake && ie.getResponse().getStatus() == 400) {
                return "Server REST plugin indicated the client sent bad request. Client version is " + HudsonRestFactory.getClient().getVersion();
            }
            //503 service unavailable, hudson server is starting
            if ( isHandshake && ie.getResponse().getStatus() == 503) {
                return "Server REST plugin is not yet fully initiated. Please try again later.";
            }
            // 302 Found - temporary solution until we get automatic redirects.
            if (ie.getResponse().getStatus() == 302) {
               String url = ie.getResponse().getHeaders().getFirst( "Location" );
               if (url.endsWith( "/rest/handshake" )) {
                   url = url.substring( 0, url.lastIndexOf( "/rest/handshake" ) );
               }
               return "Please use following URL instead: " + url;
            }
            // 401 Unauthorized
            if (ie.getResponse().getStatus() == 401) {
            	StatusManager.getManager().handle(new Status(IStatus.ERROR, HudsonUIActivator.PLUGIN_ID, "Unauthorized\n" + ie.getResponse().getEntity(String.class), ie), StatusManager.LOG);
            	return "Unauthorized";
            }
            
            String m = "Http error code " + ie.getResponse().getStatus();
            
            String mes = ie.getResponse().getEntity(String.class);
            if (mes != null && mes.length() > 0) {
                m = m + " : " + mes;
            }
            return m;
        }
        if ( e instanceof ClientHandlerException )
        {
            // this moronic beast wraps the actual exception into a RuntimeException
            // so to get to the root cause we need to do getCause().getCause()
            while ( e.getCause() != null )
            {
                e = e.getCause();
            }
        }

        if ( e instanceof UnknownHostException )
        {
            return Messages.error_unknown_host;
        }
        if ( e instanceof UnresolvedAddressException )
        {
            return Messages.error_unresolved_address;
        }
        if ( e instanceof NoRouteToHostException )
        {
            return Messages.error_no_route_to_host;
        }

        // System.out.println("t=" + t.getClass());
        // System.out.println("t cause=" + t.getCause());
        return e.getMessage();
    }

    // is there a method like this in eclipse sourcebase?
    public static String stripHtml( String source )
    {
        String regex =
            "</?(a|b|big|body|br|center|dd|dl|dt|em|embed|font|form|h1|h2|h3|h4|h5|h6|head|hr|html|i|it|img|input|li|link|menu|meta|ol|option|p|small|strike|strong|table|td|th|title|tr|tt|u|ul).*?>";
        Pattern p = Pattern.compile( regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
        return p.matcher( source ).replaceAll( "" );
    }

    /**
     * Parse the job URLs as returned used in the hudson job UI and return the server URL and the job name
     * 
     * @param jobURL
     * @return array of 2 strings - index 0 is server url and index 1 is the job name
     */
    public static String[] parseHudsonWebURL( String jobURL )
    {
        //!!!! PLEASE note that this code is copied to SourceTreeInfoComposite to avoid interdependencies
        // any change done here has to be duplicated there..
        String serverName = null;
        String jobName = null;

        Pattern p = Pattern.compile( "(.*)/job/([^/]*)/*.*" );
        Matcher m = p.matcher( jobURL );
        if ( !m.find() )
            return null;

        serverName = m.group( 1 );
        jobName = m.group( 2 );

        Pattern viewPattern = Pattern.compile( "(.*)/view.*" );
        Matcher m2 = viewPattern.matcher( m.group( 1 ) );
        if ( m2.find() )
            serverName = m2.group( 1 );
        return new String[] { serverName, jobName };
    }

    public static synchronized CompositeMonitor loadDefaultMonitor()
    {
        return HudsonManager.getDefaultMonitor();
    }

    public static boolean startMonitoringJob( String jobURL, CompositeMonitor monitor ) throws URISyntaxException
    {
        String[] jobInfo = parseHudsonWebURL( jobURL );
        if ( jobInfo == null )
        {
            log.debug( "Could not parse job URL {}", jobURL );
            return false;
        }

        HudsonMonitor serverMonitor = monitor.getMonitor( jobInfo[0] );
        if ( serverMonitor == null )
        {
            serverMonitor = HudsonManager.addHudsonMonitor( new URI( jobInfo[0] ), null);
            monitor.addMonitor( serverMonitor );
        }
        serverMonitor.addMonitoredJobs( Collections.singletonList( jobInfo[1] ) ).iterator().next().refresh();
        monitor.save();
        return true;
    }

}
