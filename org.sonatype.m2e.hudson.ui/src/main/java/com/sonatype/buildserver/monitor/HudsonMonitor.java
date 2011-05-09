/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.HudsonJob.State;
import com.sonatype.buildserver.monitor.HudsonJobEvent.EventType;
import com.sonatype.matrix.rest.client.HandshakeFailedException;
import com.sonatype.matrix.rest.client.MatrixClient;
import com.sonatype.matrix.rest.client.ext.BuildClient;
import com.sonatype.matrix.rest.client.ext.BuildClient.BuildListener;
import com.sonatype.matrix.rest.client.ext.ProjectClient;
import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.BuildEventDTO;
import com.sonatype.matrix.rest.model.build.ChangesDTO;
import com.sonatype.matrix.rest.model.build.ConsoleDTO;
import com.sonatype.matrix.rest.model.build.TestsDTO;
import com.sonatype.matrix.rest.model.project.ProjectDTO;
import com.sonatype.matrix.rest.model.project.ProjectReferenceDTO;

public class HudsonMonitor
{
    private static Logger log = LoggerFactory.getLogger( HudsonMonitor.class );
    
    private final URI serverURI;

    private MatrixClient server;
    
    // TODO ReentrantReadWriteLock might have better performance for concurrent server access requests
    private final Object SERVER_LOCK = new Object();

    private final List<HudsonJob> monitoredJobs = Collections.synchronizedList( new ArrayList<HudsonJob>() );
    
    private final List<HudsonJob> cachedJobs = Collections.synchronizedList( new ArrayList<HudsonJob>() );
    
    private final List<HudsonJob> providedJobs = Collections.synchronizedList( new ArrayList<HudsonJob>() );

    private final ListenerList listeners = new ListenerList();

    private final UUID id;

    private final BuildListener buildListener;

    HudsonMonitor(HudsonManager.HudsonMonitorSesame open)
    {
        this(open.uri, open.uuid);
    }
    

    private HudsonMonitor( URI server, UUID id )
    {
        this.id = id != null ? id : UUID.randomUUID();
        this.serverURI = server;
        buildListener = new BuildListener()
        {
            
            private void refreshBuild( BuildEventDTO event) {
                HudsonJob searchedJob = new HudsonJob( event.getProjectName(), HudsonMonitor.this );
                for (HudsonJob jb : getJobs()) {
                    // now we shall schedule the instance that is actually in the monitored list..
                    if ( jb.equals( searchedJob ) )
                    {
                        jb.refresh();
                    }
                }
            }
            
            public void buildStarted( BuildEventDTO event )
            {
                refreshBuild( event );
            }

            public void buildStopped( BuildEventDTO event )
            {
                refreshBuild( event );
            }
        };
    }
    /**
     * jobs provided declaratively by HudsonJobProvider instances
     * @param jobs
     */
    void setProvidedJobs(Collection<String> jobs, boolean scheduleLoading) 
    {
        Collection<HudsonJob> all = getJobs();
        Collection<HudsonJob> old;
        synchronized (providedJobs) {
            old = new ArrayList<HudsonJob>(providedJobs);
            providedJobs.clear();
        }
        addJobs( jobs, providedJobs, all );
        //now notify all the jobs that were removed..
        all = getJobs();
        for (HudsonJob jb : old) {
            if (!all.contains( jb )) {
                notifyListeners( new HudsonJobEvent( jb, EventType.REMOVAL ) );
            }
        }
        if (scheduleLoading) {
            List<HudsonJob> schedule = new ArrayList<HudsonJob>();
            synchronized (providedJobs) {
                for (HudsonJob addedJob : providedJobs) {
                    if (!addedJob.isDetailsLoaded()) {
                        addedJob.markAsLoading();
                        schedule.add( addedJob );
                    }
                }
            }
            scheduleRetrieval( schedule );
        }
    }

    /**
     * returns both the manually monitored and provided jobs
     * @return
     */
    public Collection<HudsonJob> getJobs()
    {
        Collection<HudsonJob> jobs;
        synchronized ( monitoredJobs )
        {
            jobs =  new HashSet<HudsonJob>( monitoredJobs );
        }
        synchronized ( providedJobs )
        {
            jobs.addAll( providedJobs );
        }
        return jobs;
    }

    public void addHudsonJobListener( HudsonJobListener listener )
    {
        listeners.add( listener );
    }

    public void removeHudsonJobListener( HudsonJobListener listener )
    {
        listeners.remove( listener );
    }

    /**
     * jobs added through this method are not refreshed/loaded upon addition
     * @param monJobs
     */
    public Collection<HudsonJob> addMonitoredJobs( Collection<String> monJobs )
    {
        return addJobs(monJobs, this.monitoredJobs, getJobs());
    }
    
    private Collection<HudsonJob> addJobs( Collection<String> monJobs, Collection<HudsonJob> list, Collection<HudsonJob> all )
    {
        Collection<HudsonJob> toRet = new HashSet<HudsonJob>();
        for ( String job : monJobs )
        {
            HudsonJob addedJob = hudsonJobForId( job );
            toRet.add(addedJob);
            synchronized (list) {
                if (!list.contains( addedJob )) {
                    list.add( addedJob );
                }
            }
            if (!all.contains(addedJob)) {
                notifyListeners( new HudsonJobEvent( addedJob, EventType.ADDED ) );
            }
        }
        return toRet;
    }

    public void setMonitoredJobs( Collection<String> toAdd )
    {
        List<HudsonJob> toRemove = new ArrayList<HudsonJob>();
        List<HudsonJob> jobs = new ArrayList<HudsonJob>();
        synchronized ( monitoredJobs )
        {
            for ( HudsonJob job : monitoredJobs ) {
                if ( !toAdd.contains( job.getJobName() ) ) {
                    toRemove.add( job );
                }
            }
            monitoredJobs.removeAll( toRemove );
            addMonitoredJobs( toAdd );
            for (HudsonJob addedJob : monitoredJobs) {
                if (!addedJob.isDetailsLoaded()) {
                    addedJob.markAsLoading();
                    jobs.add( addedJob );
                }
            }
        }
        for (HudsonJob job : toRemove) {
            notifyListeners( new HudsonJobEvent( job, EventType.REMOVAL ) );
        }
        scheduleRetrieval( jobs );
    }

    /**
     * load all jobs in one batch..
     * @param jobs
     */
    private void scheduleRetrieval( List<HudsonJob> jobs )
    {
        new RetrieveJob( this, jobs ).schedule();
    }

    ProjectDTO retrieveDetailForReference( ProjectReferenceDTO ref ) 
    {
        assert Display.getCurrent() == null;
        ProjectDTO job = null;
        try
        {
        	synchronized (SERVER_LOCK)
        	{
	            if (ensureConnection()) 
	            {
	            	ProjectClient client = server.ext( ProjectClient.class );
	            	job = client.getProject( ref );
	            }
        	}
        }
        catch ( Exception e )
        {
            // Exceptions likely caused in transport
            log.error( "Error retrieving job details for reference:" + ref + " " + getServerURI() , e);
        }
        return job;
    }
    
    void retrieveJob( HudsonJob toRetrieve )
    {
        assert Display.getCurrent() == null;
        
        try
        {
        	synchronized (SERVER_LOCK) {
        		if (ensureConnection())
        		{
        			ProjectClient client = server.ext( ProjectClient.class );
        			ProjectDTO job = toRetrieve.getJobReference() != null ? client.getProject( toRetrieve.getJobReference() ) : client.getProject( toRetrieve.getJobName() );
                	toRetrieve.setLoadedJobDetails( job, State.STATE_OK );
        		}
        	}
        }
        catch ( Exception e )
        {
            // Exceptions likely caused in transport
            log.error( "Error retrieving job details for job:" + toRetrieve.getJobName() + " " + toRetrieve.getServerName() , e);
            String errorMessage = HudsonUtils.restExceptionToString( e, "Job '" + toRetrieve.getJobName() + "' is not found on the server." );
            if ( !( toRetrieve.getJobDetails() instanceof ErrorJob ) )
            {
                toRetrieve.setLoadedJobDetails( new ErrorJob( toRetrieve.getJobDetails(), errorMessage, e ), State.STATE_FAILED );
            } else {
                ((ErrorJob)toRetrieve.getJobDetails()).setErrorMessage( errorMessage );
                ((ErrorJob)toRetrieve.getJobDetails()).setException( e );
            }
        }
    }

    /**
     * not to be used directly, call HudsonJob.loadBuilds()
     * 
     * @param job
     */
    void scheduleBuildsRetrieval( HudsonJob job )
    {
        new RetrieveBuildsJob( this, job ).schedule();
    }

    List<BuildDTO> retrieveBuilds( HudsonJob toRetrieve )
    {
        assert Display.getCurrent() == null;
        try
        {
        	synchronized (SERVER_LOCK)
        	{
	            if (ensureConnection()) 
	            {
		            BuildClient client = server.ext( BuildClient.class );
		            List<BuildDTO> builds = client.getBuilds( toRetrieve.getJobName() );
		            toRetrieve.setLoadedBuilds( builds, HudsonJob.State.STATE_OK );
		            return builds;
	            }
        	}
        }
        catch ( Throwable e )
        {
            // Exceptions likely caused in transport
            log.error( "Error retrieving builds for job:" + toRetrieve.getJobName() + " " + toRetrieve.getServerName() , e);
            
            toRetrieve.setLoadedBuilds( null, HudsonJob.State.STATE_FAILED );
        }
        return null;
    }
    
    BuildDTO retrieveBuild( HudsonJob toRetrieve, int number ) {
        assert Display.getCurrent() == null;
        try
        {
        	synchronized (SERVER_LOCK) {
        		if (ensureConnection()) {
        			BuildClient client = server.ext( BuildClient.class );
        			return client.getBuild( toRetrieve.getJobName(), number );
        		}
        	}
        }
        catch ( Throwable e )
        {
            // Exceptions likely caused in transport
            log.error( "Error retrieving build " + number + " for job:" + toRetrieve.getJobName() + " " + toRetrieve.getServerName() , e);
        }
        return null;
        
    }

    public void scheduleChangesRetrieval( HudsonJob hudsonJob, BuildDTO build )
    {
        new RetrieveChangesJob( this, hudsonJob, build ).schedule();
    }

    ChangesDTO retrieveChanges( HudsonJob toRetrieve, BuildDTO bld )
    {
        assert Display.getCurrent() == null;
        try
        {
        	synchronized (SERVER_LOCK)
        	{
	            if (ensureConnection()) 
	            {
	            	BuildClient client = server.ext( BuildClient.class );
	            	ChangesDTO changes = client.getChanges( toRetrieve.getJobName(), bld.getNumber() );
	                toRetrieve.setChanges( changes, bld.getNumber() );
	                return changes;
	            }
        	}
        }
        catch ( Exception e )
        {
            // Exceptions likely caused in transport
            log.error( "Error retrieving changes for job:" + toRetrieve.getJobName() + " " + toRetrieve.getServerName() , e);
            
            toRetrieve.setChanges( null, bld.getNumber() );
        }
        return null;
    }

    void notifyListeners( AbstractHudsonJobEvent event )
    {
        for ( Object listener : listeners.getListeners() )
        {
            ( (HudsonJobListener) listener ).getModified( event );
        }
    }

    public Collection<ProjectDTO> getAllJobs()
        throws Exception
    {
    	synchronized (SERVER_LOCK)
    	{
	        if ( ensureConnection() )
	        {
	        	return server.ext( ProjectClient.class ).getProjects();
	        }
    	}
        return Collections.<ProjectDTO>emptyList();
    }

    void removeJob( HudsonJob job )
    {
        monitoredJobs.remove( job );
        notifyListeners( new HudsonJobEvent( job, EventType.REMOVAL ) );
    }

    /**
     * returns a merge of manually monitored jobs and the ones declared by declarative providers..
     * @return
     */
    public Collection<String> getJobIds()
    {
        Collection<String> ids = getMonitoredJobIds();
        ids.addAll(getJobIds( providedJobs ));
        return ids;
    }
    
    /**
     * return only the manually entered jobids, primarily used by persistence..
     * @return
     */
    public Collection<String> getMonitoredJobIds()
    {
        return getJobIds( monitoredJobs );
    }
    
    boolean isMonitoredJob(HudsonJob job) {
        return monitoredJobs.contains( job );
    }
    
    boolean isProvidedJob(HudsonJob job) {
        return providedJobs.contains( job );
    }
   
    private Collection<String> getJobIds(Collection<HudsonJob> list)
    {
        synchronized ( list )
        {
            Collection<String> ids = new HashSet<String>( list.size() );
            for ( HudsonJob hudsonJob : list )
            {
                ids.add( hudsonJob.getJobName() );
            }
            return ids;
        }
    }    

    public URI getServerURI()
    {
        return serverURI;
    }

    IStatus build( HudsonJob hudsonJob )
    {
        assert Display.getCurrent() == null;
        try
        {
        	synchronized (SERVER_LOCK)
        	{
	            if (ensureConnection()) 
	            {
	            	ProjectClient client = server.ext( ProjectClient.class );
	            	client.scheduleBuild( hudsonJob.getJobName() );
	            }
        	}
            return Status.OK_STATUS;
        }
        catch ( Exception e )
        {
            return new Status( IStatus.ERROR, HudsonUIActivator.PLUGIN_ID,
                               NLS.bind( Messages.hudsonMonitor_build_error, hudsonJob.getJobName() ), e );
        }
    }

    void enable( HudsonJob hudsonJob, boolean enable ) throws Exception
    {
        assert Display.getCurrent() == null;
        synchronized (SERVER_LOCK)
	    {
	        if (ensureConnection()) {
	        	ProjectClient client = server.ext( ProjectClient.class );
	        	client.enableProject( hudsonJob.getJobName(), enable );
	        }
        }
    }

    void keepBuild( HudsonJob hudsonJob, BuildDTO build, boolean keep ) throws Exception
    {
        assert Display.getCurrent() == null;
        synchronized (SERVER_LOCK)
        {
	        if (ensureConnection()) {
	        	BuildClient client = server.ext( BuildClient.class );
	        	client.keepBuild( hudsonJob.getJobName(), build.getNumber(), keep );
	        }
        }
    }

    void deleteBuild( HudsonJob hudsonJob, BuildDTO build ) throws Exception
    {
        assert Display.getCurrent() == null;
        synchronized (SERVER_LOCK)
        {
        	if (ensureConnection())
        	{
        		BuildClient client = server.ext( BuildClient.class );
        		client.deleteBuild( hudsonJob.getJobName(), build.getNumber() );
        	}
        }
    }

    public UUID getId()
    {
        return id;
    }
    
    private boolean ensureConnection() throws Exception 
    {
    	assert Thread.holdsLock(SERVER_LOCK);
        if (server == null) 
        {
            return createConnection();
        }
        return true;
    }

    // a mutex would be probably better solution than sync with
    // creating and using connections as read operations
    // and reseting the connection as write operation.
    void resetConnection() 
    {
    	synchronized (SERVER_LOCK) {
    		if (server != null) {
    			HudsonManager.resetServer( serverURI );
    			BuildClient build = server.ext( BuildClient.class );
    			build.removeBuildListener( buildListener );
    			server.close();
    			server = null;
    		}
        }
    }

    private boolean createConnection()
        throws HandshakeFailedException
    {
    	assert Thread.holdsLock(SERVER_LOCK);
        server = HudsonManager.getServer( serverURI );
        BuildClient build = server.ext( BuildClient.class );
        build.addBuildListener( buildListener );
        return true;
    }

    TestsDTO getTests( HudsonJob job, BuildDTO build ) throws Exception
    {
    	synchronized (SERVER_LOCK) 
    	{
    		if ( ensureConnection() )
	        {
	            BuildClient client = server.ext( BuildClient.class );
	            return client.getTests( job.getJobName(), build.getNumber() );
	        }
	        return null;
    	}
    }
    
    ConsoleDTO getConsoleInfo( HudsonJob job, BuildDTO build ) throws Exception
    {
    	synchronized (SERVER_LOCK) 
    	{
	        if ( ensureConnection() ) 
	        {
	            BuildClient client = server.ext( BuildClient.class );
	            return client.getConsole( job.getJobName(), build.getNumber() );
	        }
	        return null;
    	}
    }
    
    InputStream getConsoleContent( HudsonJob job, BuildDTO build, Long beginning, Long end ) throws Exception
    {
    	synchronized (SERVER_LOCK) 
    	{
	        if ( ensureConnection() )
	        {
	            BuildClient client = server.ext( BuildClient.class );
	            return client.getConsoleContent( job.getJobName(), build.getNumber(), beginning, end );
	        }
	        return null;
    	}
    }

    /**
     * can return null if there is error retrieving details. in that case we don't have the 
     * job name to uniquely identify the hudsonjob
     * @param ref
     * @return
     */
    HudsonJob hudsonJobForReference( ProjectReferenceDTO ref )
    {
        assert ref != null;
        Set<HudsonJob> jobs = new HashSet<HudsonJob>();
        synchronized (cachedJobs) {
            jobs.addAll( cachedJobs );
        }
        for (HudsonJob job : jobs) {
        	ProjectReferenceDTO rf = job.getJobReference();
            if (rf != null && rf.getId().equals( ref.getId() )) {
                return job;
            }
        }
        ProjectDTO job = retrieveDetailForReference( ref );
        if (job != null) {
            HudsonJob newone = new HudsonJob( job, this );
            synchronized (cachedJobs) {
                cachedJobs.add( newone );
            }
            return newone;
        }
        return null;
    }
    
    private HudsonJob hudsonJobForId( String id) {
        assert id != null;
        Set<HudsonJob> jobs = new HashSet<HudsonJob>();
        synchronized (cachedJobs) {
            jobs.addAll( cachedJobs );
        }
        for (HudsonJob job : jobs) {
            String rf = job.getJobName();
            if (rf != null && rf.equals( id )) {
                return job;
            }
        }
        HudsonJob newone = new HudsonJob( id, this );
        synchronized (cachedJobs) {
            cachedJobs.add( newone );
        }
        return newone;
        
    }

    /**
     * return true if there is at least one HudsonJobListener attached that returns true from isUIUp()
     * @return
     */
    boolean hasUIUp()
    {
        for (Object list : listeners.getListeners()) {
            HudsonJobListener listener = (HudsonJobListener)list;
            if (listener.isUIUp()) {
                return true;
            }
        }
        return false;
    }

}
