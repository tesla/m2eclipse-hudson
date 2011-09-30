/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.Messages;
import org.hudsonci.rest.model.build.BuildDTO;
import org.hudsonci.rest.model.build.ChangesDTO;

class RetrieveChangesJob
    extends Job
{
    private static Logger log = LoggerFactory.getLogger( RetrieveChangesJob.class );
    
    final private HudsonMonitor hudsonMonitor;

    final private HudsonJob job;

    final private BuildDTO build;

    public RetrieveChangesJob( HudsonMonitor monitor, HudsonJob job, BuildDTO bld )
    {
        super( NLS.bind( Messages.background_retrieve_changes, job.getJobName(), bld.getId() ) );
        this.hudsonMonitor = monitor;
        this.job = job;
        this.build = bld;
    }

    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        log.debug( "Retrieve list of changes for job {} / build {} on server {}", new Object[] {job.getJobName(), build.getId(), job.getServerName()} );//$NON-NLS-1$
        ChangesDTO changes = hudsonMonitor.retrieveChanges( job, build );
        hudsonMonitor.notifyListeners( new HudsonJobChangesEvent( job, changes, build ) );
        return Status.OK_STATUS;
    }

}
