/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;

import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.BuildStateDTO;
import com.sonatype.matrix.rest.model.build.ChangesDTO;
import com.sonatype.matrix.rest.model.build.ConsoleDTO;
import com.sonatype.matrix.rest.model.build.TestsDTO;
import com.sonatype.matrix.rest.model.project.ProjectDTO;
import com.sonatype.matrix.rest.model.project.ProjectReferenceDTO;

public class HudsonJob
{
    final HudsonMonitor monitor;

    final String jobId;

    private List<BuildDTO> builds;

    private ProjectDTO details;

    private State detailsState = State.STATE_NOT_LOADED;

    private State buildState = State.STATE_NOT_LOADED;

    enum State
    {
        STATE_OK, STATE_LOADING, STATE_NOT_LOADED, STATE_DIRTY, STATE_FAILED
    }

    private Map<Integer, ChangesDTO> changes = new HashMap<Integer, ChangesDTO>();

    private List<HudsonJobListener> listeners = new ArrayList<HudsonJobListener>();

    private HudsonJobListener monitorListener = new HudsonJobListener()
    {

        public void getModified( AbstractHudsonJobEvent event )
        {
            if ( event.getModifiedJob() == HudsonJob.this )
            {
                refireEvent( event );
            }
        }

        public boolean isUIUp()
        {
            boolean hasOne = false;
            synchronized (listeners) {
                for (HudsonJobListener list : listeners) {
                    if (list.isUIUp()) {
                        hasOne = true;
                        break;
                    }
                }
            }
            return hasOne;
        }
    };

    /**
     * BE VERY CAREFUL WHEN CREATING NEW INSTANCES, THESE ARE HOLDERS OF PREVISOUSLY RETRIEVED VALUES..
     */
    HudsonJob( String job, HudsonMonitor hudsonMonitor )
    {
        monitor = hudsonMonitor;
        jobId = job;
    }
    
    /**
     * BE VERY CAREFUL WHEN CREATING NEW INSTANCES, THESE ARE HOLDERS OF PREVISOUSLY RETRIEVED VALUES..
     * the constructor immediately retrieves the job details synchronously.
     *  
     */
    HudsonJob( ProjectDTO details, HudsonMonitor hudsonMonitor )
    {
        assert Display.getCurrent() == null;
        monitor = hudsonMonitor;
        jobId = details.getName();
        this.details = details;
        detailsState = State.STATE_OK;
    }
    
    
    public String getServerName()
    {
        return monitor.getServerURI().toString();
    }

    public String getJobName()
    {
        if (jobId == null) {
            if (details != null) {
                return details.getName();
            }
            assert false : "no jobid, no details, we are doomed.";
        }
        return jobId;
    }

    public ProjectDTO getJobDetails()
    {
        return details;
    }
    
    boolean isDetailsLoaded() {
        return detailsState == State.STATE_OK;
    }

    void setLoadedJobDetails( ProjectDTO details, State result )
    {
        this.details = details;
        detailsState = result;
        if ( buildState == State.STATE_OK )
        {
            buildState = State.STATE_DIRTY;
        }
    }
    
    /**
     * tells if the job is a monitored one (thus manually defined by used) or not.
     * a job can be both isMonitored and isDeclarativelyProvided
     * @return
     */
    public boolean isMonitored() {
        return monitor.isMonitoredJob( this );
    }
    /**
     * tells if the job is declared by a provider or not.
     * a job can be both isMonitored and isDeclarativelyProvided
     * @return
     */
    public boolean isDeclarativelyProvided() {
        return monitor.isProvidedJob( this );
    }

    public IStatus build()
    {
        assert Display.getCurrent() == null;
        return monitor.build( this );
    }

    public void enable( boolean enable ) throws Exception
    {
        assert Display.getCurrent() == null;
        monitor.enable( this, enable );
    }

    public void dispose()
    {
        monitor.removeJob( this );
    }

    public void refresh()
    {
        refresh (true, false);
    }
    
    private void refresh(boolean async, boolean force) {
        assert async || Display.getCurrent() == null;
        if ( !force  && detailsState == State.STATE_LOADING )
        {
            return;
        }
        detailsState = State.STATE_LOADING;
        RetrieveJob jb = new RetrieveJob( monitor, this );
        jb.schedule();
        if (!async) {
            try
            {
                jb.join();
            }
            catch ( InterruptedException e )
            {
                
            }
        }
    }
    /**
     * 
     * called from hudsonMonitor.addMonitoredJobs to allow batch loading
     */
    void markAsLoading() {
        detailsState = State.STATE_LOADING;
    }

    public void loadBuilds()
    {
        if ( buildState == State.STATE_LOADING )
        {
            return;
        }
        buildState = State.STATE_LOADING;
        monitor.scheduleBuildsRetrieval( this );
    }

    public void keepBuild( BuildDTO build, boolean keep ) throws Exception
    {
        assert Display.getCurrent() == null;
        monitor.keepBuild( this, build, keep );
    }

    public void deleteBuild( BuildDTO build ) throws Exception
    {
        assert Display.getCurrent() == null;
        monitor.deleteBuild( this, build );
    }

    public void loadChanges( BuildDTO build )
    {
        monitor.scheduleChangesRetrieval( this, build );
    }
    
    public List<HudsonJob> loadDescendants() {
        assert Display.getCurrent() == null;
        
        List<HudsonJob> toRet = new ArrayList<HudsonJob>();
        if (detailsState == State.STATE_NOT_LOADED || detailsState == State.STATE_DIRTY) {
            refresh( false, true );
        }
        ProjectDTO details = getJobDetails();
        if (details != null) {
            List<ProjectReferenceDTO> refs = details.getDescendants();
            for (ProjectReferenceDTO ref : refs) {
                HudsonJob bj = monitor.hudsonJobForReference( ref ); 
                if (bj != null) {
                    toRet.add( bj );
                }
            }
        }
        return toRet;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + jobId.hashCode();
        result = prime * result + monitor.hashCode();
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
        HudsonJob other = (HudsonJob) obj;
        if ( !jobId.equals( other.jobId ) )
            return false;
        if ( !monitor.equals( other.monitor ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        //toString used in copy operation.. expects a proper job url.
        return getServerName() + "/job/" + getJobName();
    }

    /**
     * returns null when builds were not yet retrieved and empty list if no builds are available on server to retrieve
     * the list of build instances use loadBuilds() method
     * 
     * @return
     */
    public List<BuildDTO> getBuilds()
    {
        return builds;
    }
    
    public BuildDTO loadBuild( int number ) {
        assert Display.getCurrent() == null;
        if (details != null && details.getLastBuild().getNumber() == number) {
            return details.getLastBuild();
        }
        List<BuildDTO> blds = builds;
        if (blds != null) {
            for (BuildDTO b : blds) {
                if (b.getNumber() == number) {
                    return b;
                }
            }
        }
        BuildDTO b = monitor.retrieveBuild( this, number );
        if (b != null) {
            return b;
        }
        return null;
    }

    void setLoadedBuilds( List<BuildDTO> newBuilds, State result )
    {
        buildState = result;
        if (result == State.STATE_FAILED) {
            return;
        }
        if ( this.builds != null )
        {
            Map<Integer, BuildDTO> numbers = new HashMap<Integer, BuildDTO>();
            for ( BuildDTO b : this.builds )
                numbers.put( new Integer( b.getNumber() ), b );
            List<BuildDTO> reversed = new ArrayList<BuildDTO>( newBuilds );
            Collections.reverse( reversed );
            for ( BuildDTO b : reversed )
            {
                BuildDTO old = numbers.remove( new Integer( b.getNumber() ) );
                boolean exists = old != null;
                if ( !exists )
                {
                    this.builds.add( 0, b );
                }
                if ( exists && old.getState() != BuildStateDTO.COMPLETED )
                {
                    // this assumes Build instances are not equals by number..
                    int index = builds.indexOf( old );
                    builds.add( index, b );
                    builds.remove( old );
                }
            }
            for ( BuildDTO b : numbers.values() )
            {
                this.builds.remove( b );
            }
        }
        else
        {
            this.builds = new ArrayList<BuildDTO>( newBuilds != null ? newBuilds : Collections.<BuildDTO>emptyList());
        }
    }

    public boolean hasBuildsLoaded()
    {
        return buildState == State.STATE_OK;
    }

    /**
     * returns scm changes for the given build or null if not yet loaded or not present.. check hasChangesLoaded() for
     * loaded state..
     * 
     * @param build
     * @return
     */
    public ChangesDTO getChanges( BuildDTO build )
    {
        return changes.get( new Integer( build.getNumber() ) );
    }

    public boolean hasChangesLoaded( BuildDTO build )
    {
        return changes.containsKey( new Integer( build.getNumber() ) );
    }

    void setChanges( ChangesDTO changes, int number )
    {
        this.changes.put( new Integer( number ), changes );
    }

    public TestsDTO getTests( BuildDTO build ) throws Exception
    {
        assert Display.getCurrent() == null;
        return monitor.getTests( this, build );
    }

    public ConsoleDTO getConsoleInfo( BuildDTO build ) throws Exception
    {
        assert Display.getCurrent() == null;
        return monitor.getConsoleInfo( this, build );
    }

    public InputStream getConsoleContent( BuildDTO build, Long beginning, Long end ) throws Exception
    {
        assert Display.getCurrent() == null;
        return monitor.getConsoleContent( this, build, beginning, end );
    }

    /**
     * only events related to this object are to be returned..
     * 
     * @param listener
     */
    public void addListener( HudsonJobListener listener )
    {
        synchronized ( listeners )
        {
            if ( listeners.size() == 0 )
            {
                monitor.addHudsonJobListener( monitorListener );
            }
            listeners.add( listener );
        }
    }

    /**
     * only events related to this object are to be returned..
     * 
     * @param listener
     */
    public void removeListener( HudsonJobListener listener )
    {
        synchronized ( listeners )
        {
            listeners.remove( listener );
            if ( listeners.size() == 0 )
            {
                monitor.removeHudsonJobListener( monitorListener );
            }
        }
    }

    private void refireEvent( AbstractHudsonJobEvent event )
    {
        List<HudsonJobListener> lists = new ArrayList<HudsonJobListener>();
        synchronized ( listeners )
        {
            lists.addAll( listeners );
        }
        for ( HudsonJobListener list : lists )
        {
            list.getModified( event );
        }
    }

    /**
     * optional unique id of the job, can be null
     * @return
     */
    public ProjectReferenceDTO getJobReference()
    {
        if (details != null) {
            return details.getRef();
        }
        return null;
    }

    public HudsonJob loadParent()
    {
        assert Display.getCurrent() == null;
        HudsonJob toRet = null;
        if (detailsState == State.STATE_NOT_LOADED || detailsState == State.STATE_DIRTY) {
            refresh( false, true );
        }
        ProjectDTO details = getJobDetails();
        if (details != null) {
        	ProjectReferenceDTO ref = details.getParent();
            if (ref != null) {
                toRet = monitor.hudsonJobForReference( ref ); 
            }
        }
        return toRet;
    }

}
