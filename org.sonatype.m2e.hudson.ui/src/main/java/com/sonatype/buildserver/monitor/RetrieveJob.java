/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.statushandlers.StatusManager;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputComposite;
import org.maven.ide.eclipse.ui.common.authentication.UrlInputDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.HudsonJobEvent.EventType;
import org.hudsonci.rest.client.HandshakeFailedException;
import org.hudsonci.rest.client.InvalidResponseException;

class RetrieveJob
    extends Job
{
    private static Logger log = LoggerFactory.getLogger( RetrieveBuildsJob.class );

    final private HudsonMonitor hudsonMonitor;

    final private Collection<HudsonJob> jobs;

    final private CompositeMonitor compositeMonitor;

    RetrieveJob( HudsonMonitor monitor, HudsonJob job )
    {
        super( NLS.bind( Messages.background_retrieve_job, job.getJobName() ) );
        this.hudsonMonitor = monitor;
        this.jobs = Collections.<HudsonJob> singletonList( job );
        compositeMonitor = null;
    }

    RetrieveJob( HudsonMonitor monitor, Collection<HudsonJob> jobs )
    {
        super( Messages.background_retrieve_jobs );
        this.hudsonMonitor = monitor;
        this.jobs = jobs;
        compositeMonitor = null;
    }

    RetrieveJob( CompositeMonitor monitor, Collection<HudsonJob> jobs )
    {
        super( Messages.background_retrieve_jobs );
        this.jobs = jobs;
        hudsonMonitor = null;
        compositeMonitor = monitor;
    }

    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        monitor.beginTask( "Retrieving Hudson jobs", jobs.size() + 1 );
        Set<HudsonMonitor> skipAsking = new HashSet<HudsonMonitor>();
        for ( HudsonJob job : jobs )
        {
            monitor.worked( 1 );
            monitor.subTask( job.getJobName() );
            log.debug( "Retrieve job details for job {} on server {}", job.getJobName(), job.getServerName() );//$NON-NLS-1$
            final HudsonMonitor bmonitor;
            if ( hudsonMonitor == null )
            {
                bmonitor = compositeMonitor.getMonitor( job.getServerName() );
            }
            else
            {
                bmonitor = hudsonMonitor;
            }
            try
            {
                bmonitor.retrieveJob( job );
                if ( bmonitor.hasUIUp() && !skipAsking.contains( bmonitor ) )
                {
                    // TODO remember cancel selection for this hudsonmonitor
                    Boolean askedAndReload = checkForAuthenticationFailed( job, bmonitor );
                    while ( askedAndReload != null && askedAndReload.booleanValue() )
                    {
                        // repeat the retrieval
                        bmonitor.retrieveJob( job );
                        askedAndReload = checkForAuthenticationFailed( job, bmonitor );
                    }
                    if ( askedAndReload != null && askedAndReload.booleanValue() == false )
                    {
                        // user cancelled the dialog, skip for the remainder of the job.
                        skipAsking.add( bmonitor );
                    }
                }
                bmonitor.notifyListeners( new HudsonJobEvent( job, EventType.CHANGED ) );
            }
            catch ( Throwable exc )
            {
                StatusManager.getManager().handle( new Status( IStatus.ERROR, HudsonUIActivator.PLUGIN_ID,
                                                               exc.getMessage(), exc ),
                                                   StatusManager.SHOW | StatusManager.LOG );
            }
        }
        monitor.done();
        return Status.OK_STATUS;
    }

    private Boolean checkForAuthenticationFailed( final HudsonJob job, final HudsonMonitor bmonitor )
    {
        if ( job.getJobDetails() instanceof ErrorJob )
        {
            ErrorJob err = (ErrorJob) job.getJobDetails();
            Throwable e = err.getException();
            if ( e instanceof HandshakeFailedException )
            {
                HandshakeFailedException ex = (HandshakeFailedException) e;
                for ( Throwable t : ex.getFailures() )
                {
                    e = t;
                }
            }

            if ( e != null && e instanceof InvalidResponseException )
            {
                InvalidResponseException http = (InvalidResponseException) e;
                // when unauthorized is returned, it means the user has changed the
                // credentials on the server.
                final Boolean[] ret = new Boolean[1];
                if ( http.getResponse().getStatus() == 401 )
                {
                    Display.getDefault().syncExec( new Runnable()
                    {
                        public void run()
                        {
                            UrlInputDialog dial =
                                new UrlInputDialog(
                                                    HudsonUIActivator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
                                                    "Credentials for Hudson server are not valid anymore. Retype them please.",
                                                    "Hudson server URL:", job.getServerName(),
                                                    UrlInputComposite.READ_ONLY_URL | UrlInputComposite.ALLOW_ANONYMOUS );
                            dial.setBlockOnOpen( true );
                            int res = dial.open();
                            if ( UrlInputDialog.OK == res )
                            {
                                // get url is storing the value!?
                                dial.getUrl();
                                bmonitor.resetConnection();
                                ret[0] = Boolean.TRUE;
                            }
                            else
                            {
                                ret[0] = Boolean.FALSE;
                            }
                        }
                    } );
                }
                return ret[0];
            }
        }
        return null;
    }

}
