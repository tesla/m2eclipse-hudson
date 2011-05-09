/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.eclipse.jface.dialogs.DialogSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;

class MonitorPersistence
{
    private static final Logger log = LoggerFactory.getLogger( MonitorPersistence.class );

    private static final String JOBS = "hudson.jobIds"; //$NON-NLS-1$

    private static final String ADDRESS = "hudson.uri";//$NON-NLS-1$

    private static final String MONITORS = "hudson.monitors";//$NON-NLS-1$

    private static final String SUFFIX = ".prefs";//$NON-NLS-1$

    /**
     * Persist a {@code HudsonMonitor}, it can be reloaded later using the monitor's ID.
     * 
     * @param monitor the monitor to persist
     */
    static void store( HudsonMonitor monitor )
    {
        synchronized ( monitor )
        {
            DialogSettings settings = new DialogSettings( monitor.getId().toString() );
            settings.put( ADDRESS, monitor.getServerURI().toString() );

            Collection<String> jobIds = monitor.getMonitoredJobIds();
            if ( jobIds != null && jobIds.size() > 0 )
                settings.put( JOBS, jobIds.toArray( new String[jobIds.size()] ) );

            try
            {
                settings.save( getLocation( monitor.getId().toString() ) );
            }
            catch ( IOException e )
            {
                log.error( "Error while saving Hudson preferences: " + e.getMessage() );
            }
        }
    }

    /**
     * Persist a {@code CompositeMonitor}, it can be reloaded later using the monitor's ID.
     * 
     * @param compositeMonitor the monitor to persist
     */
    static void store( CompositeMonitor compositeMonitor )
    {
        synchronized ( compositeMonitor )
        {
            DialogSettings settings = new DialogSettings( compositeMonitor.getId().toString() );
            Collection<HudsonMonitor> monitors = compositeMonitor.getAllMonitors();

            Collection<String> monitorIDs = new ArrayList<String>();
            for ( HudsonMonitor m : monitors )
            {
                store( m );
                monitorIDs.add( m.getId().toString() );
            }
            settings.put( MONITORS, monitorIDs.toArray( new String[monitorIDs.size()] ) );

            try
            {
                settings.save( getLocation( compositeMonitor.getId().toString() ) );
            }
            catch ( IOException e )
            {
                log.error( "Error while saving Hudson preferences: " + e.getMessage() );
            }
        }
    }

    /**
     * Retrieve a monitor given its ID.
     * 
     * @param id the monitor's id
     * @return the loaded monitor, or null if it does not exist
     */
    static synchronized Object load( String id )
    {
        DialogSettings settings = new DialogSettings( id.toString() );
        try
        {
            settings.load( getLocation( id ) );
        }
        catch ( IOException e )
        {
            return new CompositeMonitor( UUID.fromString( id ) );
        }

        String[] monitors = settings.getArray( MONITORS );
        if ( monitors != null )
        {
            return loadComposite( id, monitors );
        }
        return loadMonitor( id, settings );
    }

    private static CompositeMonitor loadComposite( String id, String[] monitorIDs )
    {
        CompositeMonitor compositeMonitor = new CompositeMonitor( UUID.fromString( id ) );
        for ( String monitorID : monitorIDs )
        {
            try
            {
                compositeMonitor.addMonitor( loadMonitor( monitorID ) );
            }
            catch ( IOException e )
            {
                log.error( "Failed to load dialog: " + monitorID );
            }
        }
        
        return compositeMonitor;
    }

    private static HudsonMonitor loadMonitor( String id, DialogSettings settings )
    {
        String[] jobIDs = settings.getArray( JOBS );
        HudsonMonitor monitor = HudsonManager.addHudsonMonitor( URI.create( settings.get( ADDRESS ) ), UUID.fromString( id ) );
        if ( jobIDs != null && jobIDs.length > 0 )
            monitor.addMonitoredJobs( Arrays.asList( jobIDs ) );
        return monitor;
    }

    private static HudsonMonitor loadMonitor( String id )
        throws IOException
    {
        DialogSettings settings = new DialogSettings( id );
        settings.load( getLocation( id ) );
        return loadMonitor( id, settings );
    }

    private static String getLocation( String id )
    {
        return HudsonUIActivator.getDefault().getStateLocation().append( id + SUFFIX ).toString();
    }
}
