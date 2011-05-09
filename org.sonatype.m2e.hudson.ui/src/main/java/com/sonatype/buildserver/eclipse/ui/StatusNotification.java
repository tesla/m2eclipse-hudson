/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.AnimationEngine;
import org.eclipse.ui.internal.ImageCycleFeedbackBase;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.prefs.PreferenceConstants;
import com.sonatype.buildserver.monitor.AbstractHudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.HudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJobEvent.EventType;
import com.sonatype.buildserver.monitor.HudsonJobListener;
import com.sonatype.buildserver.monitor.CompositeMonitor;
import com.sonatype.buildserver.monitor.ErrorJob;
import com.sonatype.buildserver.spi.BuildNotification;
import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.BuildResultDTO;
import com.sonatype.matrix.rest.model.build.BuildStateDTO;
import com.sonatype.matrix.rest.model.build.CauseDTO;
import com.sonatype.matrix.rest.model.build.UserCauseDTO;

@SuppressWarnings( "restriction" )
public class StatusNotification
    implements HudsonJobListener
{
    private static Logger log = LoggerFactory.getLogger( StatusNotification.class );

    private static StatusNotification instance;

    private JobPopup popup;

    private StatusLineCLabelContribution notificationAffordance;

    private static final int MAX_JOBS = 4;
    
    private static final Comparator<? super JobWrapper> hudsonJobComparator = new HudsonJobComparator();

    private final TreeSet<JobWrapper> jobs = new TreeSet<JobWrapper>(hudsonJobComparator);

    private final CompositeMonitor monitor;

    /**
     * only to be used by tests..
     */
    public TestHandle TEST_HANDLE = new TestHandle();

    private AnimationEngine animation;
    
	private final List<BuildNotification> notifications;

    public StatusNotification( CompositeMonitor monitor )
    {
        if ( monitor == null )
            throw new IllegalArgumentException();
        this.monitor = monitor;
        monitor.register( this );
        for ( HudsonJob job : monitor.getJobs() )
        {
            addJob( job );
        }
        notifications = Collections.unmodifiableList(createExternalNotifications());
    }

    private synchronized void createNotificationAffordance()
    {
        if ( notificationAffordance != null )
            return;
        notificationAffordance = new StatusLineCLabelContribution( getClass().getName() );
        notificationAffordance.setTooltip( Messages.jobPopup_view_build_results );
        notificationAffordance.addListener( SWT.MouseDown, new Listener()
        {
            public void handleEvent( Event event )
            {
                // it seems that at start I was getting NPEs here in the log
                Display d = Display.getCurrent();
                if ( d == null )
                    return;
                openPopup();
            }
        } );
        notificationAffordance.addListener( SWT.MouseEnter, new Listener()
        {
            public void handleEvent( Event event )
            {
                // it seems that at start I was getting NPEs here in the log
                Display d = Display.getCurrent();
                if ( d != null )
                {
                    Control cc = d.getCursorControl();
                    if ( cc != null )
                    {
                        cc.setCursor( Display.getCurrent().getSystemCursor( SWT.CURSOR_HAND ) );
                    }
                }
            }
        } );
        notificationAffordance.addListener( SWT.MouseExit, new Listener()
        {
            public void handleEvent( Event event )
            {
                // it seems that at start I was getting NPEs here in the log
                Display d = Display.getCurrent();
                if ( d != null )
                {
                    Control cc = d.getCursorControl();
                    if ( cc != null )
                    {
                        cc.setCursor( null );
                    }
                }
            }
        } );

        Display.getDefault().syncExec( new Runnable()
        {
            public void run()
            {
                IStatusLineManager manager = notificationAffordance.getStatusLineManager();
                if ( manager != null )
                    manager.add( notificationAffordance );
            }
        } );
    }

    private void updateAffordance( final boolean triggerFlash )
    {
        if ( notificationAffordance == null )
            createNotificationAffordance();
        Display.getDefault().syncExec( new Runnable()
        {
            public void run()
            {
                synchronized ( notificationAffordance )
                {
                    synchronized ( jobs )
                    {
                        JobWrapper job = jobs.isEmpty() ? null : jobs.first();
                        notificationAffordance.setVisible( job != null );
                        if ( job != null )
                        {
                            final Image image = HudsonImages.getResultImage( job.getJob() );
                            final String imagePath = HudsonImages.getResultImagePath( job .getBuildResult());
                            notificationAffordance.setImage( image );
                            notificationAffordance.setText( job.getJob().getJobName() );
                            if ( triggerFlash )
                            {
                                if ( animation != null )
                                {
                                    animation.cancel();
                                }
                                CLabel lbl = notificationAffordance.getLabel();
                                if (lbl != null) { // in some weird circumstances
                                	//this can be null, very bad API interactions in swt :(
                                	Flasher flasher = new Flasher( lbl.getShell(), loadAnimatedFor( imagePath ) );
                                	animation = new AnimationEngine(flasher,
                                			8000, 
                                			200 // delay between frames in milliseconds
                                	);
                                	animation.schedule();
                                }
                            }
                        }
                        else
                        {
                            if ( animation != null )
                            {
                                animation.cancel();
                            }
                        }
                    }
                    IStatusLineManager manager = notificationAffordance.getStatusLineManager();
                    if ( manager != null )
                    {
                        manager.update( true );
                    }
                }
            }
        } );
    }

    private void openPopup()
    {
        if ( popup == null )
        {
            popup = new JobPopup( getWorkbenchWindowShell(), monitor, notificationAffordance );
        }
        synchronized ( popup )
        {
            List<HudsonJob> bjs = new ArrayList<HudsonJob>();
            int remaining = 0;
            synchronized ( jobs )
            {
                int count = 0;
                for ( JobWrapper jw : jobs )
                {
                    if ( jw != null )
                    {
                        bjs.add( jw.getJob() );
                        count = count + 1;
                    }
                    if (count >= MAX_JOBS) {
                    	remaining = jobs.size() - MAX_JOBS;
                        break;
                    }
                }
            }
            popup.setJobs( bjs.toArray( new HudsonJob[0] ), remaining );
            popup.open();
        }
    }

    private Shell getWorkbenchWindowShell()
    {
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        return activeWindow != null ? activeWindow.getShell() : null;
    }

    public void getModified( AbstractHudsonJobEvent event )
    {
        if ( !( event instanceof HudsonJobEvent ) )
        {
            // only job refreshes count..
            return;
        }
        HudsonJobEvent ev = (HudsonJobEvent) event;
        boolean triggerFlash = false;
        if ( ev.getType() == EventType.ADDED || ev.getType() == EventType.CHANGED )
            triggerFlash = addJob( ev.getModifiedJob() );
        else if ( ev.getType() == EventType.REMOVAL ) {
            removeJob( ev.getModifiedJob() );
        }

        final boolean finalTrigger = triggerFlash;
        if (triggerFlash) {
        	notifyExtenals( ev.getModifiedJob() );
        }
        Display.getDefault().asyncExec( new Runnable()
        {
            public void run()
            {
                updateAffordance( finalTrigger );
            }
        } );
    }

    private void notifyExtenals(HudsonJob modifiedJob) {
    	BuildDTO lastBuild = modifiedJob.getJobDetails().getLastBuild();
		for (BuildNotification not : notifications) {
			if ( modifiedJob.getJobDetails().getLastBuild().getResult() == BuildResultDTO.SUCCESS ) {
				not.notifySuccess(TRAMPOLINE.createResult(modifiedJob, lastBuild));
			} else {
				not.notifyFailure(TRAMPOLINE.createResult(modifiedJob, lastBuild));
			}
		}
	}
    
    /** The trampoline singleton, defined by {@link BuildResult}. */
    public static BuildResultTrampoline TRAMPOLINE;
    static {
        Class<?> c = com.sonatype.buildserver.spi.BuildDetails.class;
        try {
            Class.forName(c.getName(), true, c.getClassLoader());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public static abstract class BuildResultTrampoline {
    	public abstract com.sonatype.buildserver.spi.BuildDetails createResult(HudsonJob job, BuildDTO build);
    }
    
    
    static List<BuildNotification> createExternalNotifications()
    {
        ArrayList<BuildNotification> providers = new ArrayList<BuildNotification>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint =
            registry.getExtensionPoint( "com.sonatype.buildserver.spi.buildNotifications" );
        if ( extensionPoint != null )
        {
            for ( IExtension extension : extensionPoint.getExtensions() )
            {
                for ( IConfigurationElement element : extension.getConfigurationElements() )
                {
                    if ( "notification".equals( element.getName() ) )
                    {
                        try
                        {
                            BuildNotification prov = (BuildNotification) element.createExecutableExtension( "class" );
                            providers.add( prov );
                        }
                        catch ( CoreException e )
                        {
                            log.error( "Could not create build notification", e );
                        }
                    }
                }
            }
        }
        return providers;
    }  
    

	private void removeJob( HudsonJob modifiedJob )
    {
        synchronized ( jobs )
        {
            JobWrapper wr = new JobWrapper( modifiedJob );
            for (JobWrapper p : jobs) {
                if (p.equals( wr )) {
                    wr = p;
                    break;
                }
            }
            jobs.remove( wr );
        }
    }

    private boolean addJob( HudsonJob job )
    {
        if ( job.getJobDetails() == null || job.getJobDetails().getLastBuild() == null )
            return false;
        JobWrapper wr = new JobWrapper( job );

        if ( wr.getLastState() != null && wr.getLastState() != BuildStateDTO.COMPLETED )
        {
            // do ignore non-finished job states.. here we just display the important jobs..
            // for some reason last state can be null??
            return false;
        }
        if ( !job.getJobDetails().isEnabled() )
        {
            // disabled jobs don't show up in status notification..
            return false;
        }
        boolean triggerFlash = false;

        synchronized ( jobs )
        {
            // is the current job result a failure or test failure?
            boolean isFailure =
                wr.getBuildResult() == BuildResultDTO.FAILURE || wr.getBuildResult() == BuildResultDTO.UNSTABLE;
            JobWrapper prev = null;
            for ( JobWrapper c : jobs )
            {
                if ( c.equals( wr ) )
                {
                    prev = c;
                    break;
                }
            }
            boolean alreadyThere = prev != null;
            
            if ( !alreadyThere && isFailure )
            {
                jobs.add( wr );
                triggerFlash = true;
            }
            else if ( alreadyThere )
            {
                if ( isFailure
                    || ( prev.getBuildResult() != BuildResultDTO.SUCCESS && wr.getBuildResult() == BuildResultDTO.SUCCESS ) )
                {
                    int currentVersion = wr.getLastBuild();
                    int lastVersion = prev.getLastBuild();
                    // this avoids flashing status bar when the update is a mere refresh of the job data,
                    // but not real additional build was triggered.
                    triggerFlash = lastVersion != currentVersion;
                    jobs.remove( prev );
                    jobs.add( wr );
                }
                else
                {
                    // remove the job from list if
                    // 1. not a failure
                    // 2. not a success build following a failed build
                    jobs.remove( prev );
                }
            }

            // flashing only makes sense when the updated job is the first
            // one in line.. any jobs that fall further down the array
            // only appeared on startup or by user manual refresh and in both
            // cases are most probably old information..
            // no need to trigger flashing then
            // Additionally we are only flashing job failures that happened now
            // not the ones that failed 2 weeks ago and only now they were added to monitored jobs..
            BuildDTO last = wr.job.getJobDetails().getLastBuild();
            long before = last.getTimeStamp() + ( last.getDuration() != null ? last.getDuration() : 0 );
            if ( triggerFlash
                && ( ( ( System.currentTimeMillis() - before ) / ( 1000 * 60 ) > 5 ) || !wr.equals( jobs.first() ) ) )
            {
                triggerFlash = false;
            }
        }
        
        // check if user prefers not in be bothered when not directly involved..
        if (triggerFlash
                && HudsonUIActivator
                        .getDefault()
                        .getPreferenceStore()
                        .getBoolean(
                                PreferenceConstants.PREF_TRIGGERED_BY_USER_ONLY)) {
            IAuthData data = AuthFacade.getAuthService().select(
                    job.getServerName());
            String username = data.getUsername();
            List<String> usernames = new ArrayList<String>();
            if (username != null) {
            	usernames.add(username);
            }
            // usernames can come from config as well, in case scm user is different..
            String names = HudsonUIActivator.getDefault().getPreferenceStore().getString(PreferenceConstants.PREF_SCM_USERNAMES);
            if (names != null) {
            	for (String name : names.split(",")) {
            		usernames.add(name.trim());
            	}
            }
            BuildDTO bld = job.getJobDetails().getLastBuild();
            if (!bld.getCauses().isEmpty()) {
                CauseDTO cause = bld.getCauses().iterator().next();
                if (cause instanceof UserCauseDTO) {
                    UserCauseDTO uc = (UserCauseDTO) cause;
                    if (!username.equals(uc.getUserId())) {
                        triggerFlash = false;
                        log.info("Build notification not triggered, user "
                                + username + " didn't start the build, "
                                + uc.getUserId() + " did.");
                    }
                }
            }
            //find at least one match in the usernames
            if (!usernames.removeAll(bld.getCulprits())) {
                triggerFlash = false;

                log.info("Build notification not triggered, user(s) "
                        + Arrays.toString(usernames.toArray()) + " not among the culprits");
            } else {
            	//here override a possible UserCause mismatch.
            	triggerFlash = true;
            }
        }

        return triggerFlash;
    }

    public void remove()
    {
        IStatusLineManager manager = notificationAffordance.getStatusLineManager();
        if ( manager != null )
        {
            manager.remove( notificationAffordance );

        }
    }

    private static class HudsonJobComparator
        implements Comparator<JobWrapper>
    {
        private static final int O2_G = -1;

        private static final int O1_G = 1;

        public int compare( JobWrapper o1, JobWrapper o2 )
        {
            if ( o1 == null && o2 == null )
                return 0;
            else if ( o1 == null )
                return O1_G;
            else if ( o2 == null )
                return O2_G;
            else if ( o1.equals( o2 ) ) {
                //equal here means we get 2 wrappers for a single jobname+servername
                //shall not happen
//                assert false : "Shall not enter 2 wrappers for same job, will result in duplicate entry..";
                return 0;
            }

            long time1 = o1.getLastBuildFinish();
            long time2 = o2.getLastBuildFinish();
            if ( time1 < time2 )
                return 1;
            else if ( time1 > time2 )
                return -1;
            return 0;
        }
    }

    public static void createNotification( CompositeMonitor m )
    {
        synchronized ( hudsonJobComparator )
        {
            if ( instance == null )
                instance = new StatusNotification( m );
        }
    }

    public static class NotificationStartup
        implements IStartup
    {
        public void earlyStartup()
        {
            CompositeMonitor loadedMonitor = HudsonUtils.loadDefaultMonitor( );
            assert loadedMonitor != null;
            createNotification( loadedMonitor );
        }
    }

    private static class JobWrapper
    {

        private HudsonJob job;

        private BuildStateDTO state;

        private BuildResultDTO result;

        private int number = -1;

        private long timestamp = -1;
        
        private final String id;

        public JobWrapper( HudsonJob job )
        {
            this.job = job;
            id = "" + job.getServerName() + "|" + job.getJobName();
            if ( job.getJobDetails() != null && !( job.getJobDetails() instanceof ErrorJob ) )
            {
                // not yet run jobs are null..
                if ( job.getJobDetails().getLastBuild() != null )
                {
                    this.state = job.getJobDetails().getLastBuild().getState();
                    this.result = job.getJobDetails().getLastBuild().getResult();
                    this.number = job.getJobDetails().getLastBuild().getNumber();
                    this.timestamp  = job.getJobDetails().getLastBuild().getTimeStamp();
                    if (job.getJobDetails().getLastBuild().getDuration() != null) {
                        timestamp = timestamp + job.getJobDetails().getLastBuild().getDuration();
                    }
                }
            }
        }

        public BuildStateDTO getLastState()
        {
            return state;
        }

        public BuildResultDTO getBuildResult()
        {
            return result;
        }

        public int getLastBuild()
        {
            return number;
        }
        public long getLastBuildFinish()
        {
            return timestamp;
        }

        public HudsonJob getJob()
        {
            return job;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + id.hashCode();
            return result;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            JobWrapper other = (JobWrapper) obj;
            return id.equals( other.id );
        }

    }

    private class Flasher
        extends ImageCycleFeedbackBase
    {
        
        public Flasher(Shell parentShell, Image[] images) {
         super(parentShell, images);
        }

        public void initialize(AnimationEngine engine) {
            background = notificationAffordance.getLabel().getParent().getBackground();
            display = notificationAffordance.getLabel().getParent().getDisplay();
           }
           
           public void saveStoppedImage() {
            stoppedImage = notificationAffordance.getLabel().getImage();
           }
           
           public void setStoppedImage(Image image) {
               if ( notificationAffordance.isVisible() ) {
                   notificationAffordance.setImage(image);
               }
           }
           
           public void showImage(Image image) {
               if ( notificationAffordance.isVisible() )
               {
                   // setting icon on nonvisible affordance will show it again..
                   notificationAffordance.setImage( image );
               }
           }
    }
    
    private static Image[] loadAnimatedFor( String imagePath )
    {
        Image[] images = null;
        try
        {
            if ( imagePath == HudsonImages.FAILURE )
            {
                images = loadAnimatedGIF( Display.getDefault(), HudsonImages.FAILURE_ANIME );
            }
            else if ( imagePath == HudsonImages.WARNING )
            {
                images = loadAnimatedGIF( Display.getDefault(), HudsonImages.UNSTABLE_ANIME );
            }
            else if ( imagePath == HudsonImages.SUCCESS )
            {
                images = loadAnimatedGIF( Display.getDefault(), HudsonImages.SUCCESS_ANIME );
            }
            else
            {
                assert false : "no correct image for flashing=" + imagePath;
                log.error( "No correct image was found for flashing: " + imagePath
                    + ". Falling back to empty image." );
            }
        }
        catch ( IOException e )
        {
            log.error( "Failure while loading image for flashing: " + imagePath + ". Falling back to empty image.",
                       e );
        }
        finally
        {
            if ( images == null )
            {
                Image image = HudsonImages.getImage( HudsonImages.EMPTY );
                images =
                    new Image[] { new Image( Display.getDefault(), image, SWT.IMAGE_COPY ),
                        new Image( Display.getDefault(), image, SWT.IMAGE_COPY ),
                        new Image( Display.getDefault(), image, SWT.IMAGE_COPY ),
                        new Image( Display.getDefault(), image, SWT.IMAGE_COPY ),
                        new Image( Display.getDefault(), image, SWT.IMAGE_COPY ), };
            }

        }
        return images;

    }
    
    private static Image[] loadAnimatedGIF(Display display, String imagePath) throws IOException {
        URL url = FileLocator.find(HudsonUIActivator.getDefault().getBundle(),
          new Path(imagePath), null);
        ImageLoader imageLoader = new ImageLoader();
        imageLoader.load(url.openStream());
        Image[] images = new Image[imageLoader.data.length];
        for (int i = 0; i < imageLoader.data.length; ++i) {
          ImageData nextFrameData = imageLoader.data[i];
          images[i] = new Image(display, nextFrameData);
        }
        return images;
      }            
    

    /**
     * to be used in tests only.
     * 
     * @author mkleint
     */
    public class TestHandle
    {
        public HudsonJob[] getJobs()
        {
            List<HudsonJob> bj = new ArrayList<HudsonJob>();
            for ( JobWrapper wr : StatusNotification.this.jobs) 
            {
                bj.add(wr.getJob());
            }
            return bj.toArray( new HudsonJob[0] );
        }

        public void resetJobs()
        {
            StatusNotification.this.jobs.clear();
        }

        public boolean addJob( HudsonJob job )
        {
            return StatusNotification.this.addJob( job );
        }

    }

    public boolean isUIUp()
    {
        //we return false here intentionally until deciding otherwise
        return false;
    }
}
