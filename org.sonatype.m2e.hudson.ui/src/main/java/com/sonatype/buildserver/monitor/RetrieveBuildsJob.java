/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.Messages;
import org.hudsonci.rest.model.build.BuildDTO;

class RetrieveBuildsJob
    extends Job
{
    private static Logger log = LoggerFactory.getLogger( RetrieveBuildsJob.class );
    
    final private HudsonMonitor hudsonMonitor;

    final private HudsonJob job;

    public RetrieveBuildsJob( HudsonMonitor monitor, HudsonJob job )
    {
        super( NLS.bind( Messages.background_retrieve_builds, job.getJobName() ) );
        this.hudsonMonitor = monitor;
        this.job = job;
    }

    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        log.debug( "Retrieve list of builds for job {} on server {}", job.getJobName(), job.getServerName() ); //$NON-NLS-1$
        List<BuildDTO> builds = hudsonMonitor.retrieveBuilds( job );
        hudsonMonitor.notifyListeners( new HudsonJobBuildsEvent( job, builds ) );
        return Status.OK_STATUS;
    }

}
