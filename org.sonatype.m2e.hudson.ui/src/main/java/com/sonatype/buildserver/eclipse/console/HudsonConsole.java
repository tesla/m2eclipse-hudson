/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.ConsoleDTO;
    
public class HudsonConsole
    extends MessageConsole
{
    /**
     * type identifier of our console instances.
     */
    public static final String HUDSON_TYPE = "com.sonatype.buildserver.eclipse.console.HudsonConsole";

    private final Color ERROR = new Color( Display.getDefault(), 255, 0, 0 );

    private final Color SUCCESS = new Color( Display.getDefault(), 0, 127, 0 );

    private final Color WARN = new Color( Display.getDefault(), 255, 166, 0 );

    private static Logger log = LoggerFactory.getLogger( HudsonConsole.class );

    private static long DOWNLOAD_SIZE = 164000;

    private final BuildDTO build;

    private final HudsonJob job;

    private MessageConsoleStream errorStream;

    private MessageConsoleStream infoStream;

    private MessageConsoleStream successStream;

    private MessageConsoleStream warnStream;
    
    private long lastModified = 0;
    
    private boolean wasCompleteConsoleContent = false;
    
    private boolean runningBuild = false;

    public HudsonConsole( HudsonJob job, BuildDTO build )
    {
        super( HudsonUtils.getJobTitle( job, build ), HUDSON_TYPE, HudsonImages.HUDSON_DESCRIPTOR, true );
        this.job = job;
        this.build = build;
        runningBuild = build.getResult() == null;
    }
    
    private String getFullName()
    {
        return HudsonUtils.getJobTitle( job, build );
    }
    
    public void checkForUpdates() {
    	getContent( wasCompleteConsoleContent );
    }
    
    boolean isRunning() {
    	return runningBuild;
    }
    
    public void getContent( final boolean completeConsoleContent )
    {
        new Job( NLS.bind( Messages.consoleView_getContent_job, getFullName() ) )
        {

            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                log.debug( "Getting console content for job {} from server {}.", job.getJobName(), job.getServerName() ); //$NON-NLS-1$
                try
                {
                    ConsoleDTO consoleData = job.getConsoleInfo( build );
                    if ( !consoleData.isExists() )
                    {
                        // TODO Display something to the user
                    	// shall we clear the console??
                        return Status.OK_STATUS;
                    }
                    if (lastModified == consoleData.getLastModified()) {
                    	if (wasCompleteConsoleContent == completeConsoleContent) {
                    		//we already had a request list that, don't repeat.
                    		log.info("Console last modified flag hasn't changed since last time, skipping..");
                    		return Status.OK_STATUS;
                    	}
                    }
                    //only clear after we checked that the console is not to be refreshed..
                    clearConsole();
                    lastModified = consoleData.getLastModified();
                	wasCompleteConsoleContent = completeConsoleContent;
                    
                    
                    Long consoleOutputSize = consoleData.getLength();
                    Long startPos = new Long( 0 );

                    errorStream = newMessageStream();
                    successStream = newMessageStream();
                    warnStream = newMessageStream();
                    infoStream = newMessageStream();
                    Display.getDefault().syncExec( new Runnable()
                    {
                        public void run()
                        {
                            errorStream.setColor( ERROR );
                            successStream.setColor( SUCCESS );
                            warnStream.setColor( WARN );
                        }
                    });
                    InputStream stream = null;
                    if ( completeConsoleContent )
                    {
                        stream = job.getConsoleContent( build, startPos, consoleOutputSize );
                    }
                    else
                    {
                        if ( consoleOutputSize > DOWNLOAD_SIZE ) {
                            startPos = consoleOutputSize - DOWNLOAD_SIZE;
                            infoStream.println( "Skipping " + (startPos / 1024) + " KB.." );
                            infoStream.println("...");
                        }
                        stream = job.getConsoleContent( build, startPos, consoleOutputSize );
                    }

                    try
                    {
                        transferBytes( stream );
                        errorStream.close();
                        successStream.close();
                        infoStream.close();
                        warnStream.close();
                    }
                    catch ( IOException e )
                    {
                        return new Status( IStatus.INFO, HudsonUIActivator.PLUGIN_ID,
                                           "Problem closing console stream.", e ); //$NON-NLS-1$
                    }
                }
                catch ( Exception ex )
                {
                    return new Status( IStatus.INFO, HudsonUIActivator.PLUGIN_ID,
                                       "Problem retrieving console output for job '" + job.getJobName() + "'", ex ); //$NON-NLS-1$ //$NON-NLS-2$

                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }
    
    public static Pattern createPattern() {
        Pattern patt = Pattern.compile( "\\[8mha.+?\\[0m", Pattern.DOTALL );
        return patt;
    }

    private void transferBytes( InputStream is )
        throws IOException
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
        String line = null;
        Pattern pattern = createPattern();
        while ( ( line = reader.readLine() ) != null )
        {
            Matcher m = pattern.matcher( line );
            if (m.find()) {
                line = m.replaceAll( "" );
            }
            
            if ( line.startsWith( "ERROR" ) || line.contains( "FAILURE" ) || line.contains( "FAILED" )
                || line.startsWith( "Finished: ABORTED" ) )
            {
                errorStream.println( line );
            }
            else if ( line.contains( "SUCCESS" ) || line.contains( ".OK(" ) )
            {
                successStream.println( line );
            }
            else if ( line.contains( "WARN" ) || line.contains( "Finished: UNSTABLE" ) )
            {
                warnStream.println( line );
            }
            else
            {
                infoStream.println( line );
            }
        }
    }

    public void dispose()
    {
        close( errorStream );
        close( infoStream );
        close( successStream );
        close( warnStream );

        ERROR.dispose();
        WARN.dispose();
        SUCCESS.dispose();
    }

    private void close( MessageConsoleStream stream )
    {
        if ( stream == null || stream.isClosed() )
            return;
        try
        {
            synchronized ( stream )
            {
                stream.close();
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }
}
