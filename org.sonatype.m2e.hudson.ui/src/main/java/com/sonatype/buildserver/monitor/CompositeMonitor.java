/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.monitor.HudsonJobEvent.EventType;
import com.sonatype.buildserver.spi.HudsonJobProvider;
import com.sonatype.buildserver.spi.HudsonServer;

public class CompositeMonitor
    implements HudsonJobListener
{
    private static Logger log = LoggerFactory.getLogger( CompositeMonitor.class );

    private final ChangeListener providerListener = new ProviderListener();
    
    final Collection<HudsonMonitor> monitors = new HashSet<HudsonMonitor>();

    private final ListenerList listeners = new ListenerList();

    private final UUID id;

    private List<HudsonJobProvider> providers;

	private IProxyChangeListener proxyChangeListener;

	private Job proxyResetJob;

    public CompositeMonitor( UUID id )
    {
        this.id = id;
        //TODO add listener on changed content of extension point.
        providers = createHudsonJobProviders();
        for (HudsonJobProvider prov : providers) {
            prov.addChangeListener( providerListener  );
        }
        IProxyService service = HudsonUIActivator.getDefault().getProxyService();
        if (service != null) {
			proxyResetJob = new Job("Reset proxy settings for Hudson jobs") {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					synchronized (monitors) {
						for (HudsonMonitor mon : monitors) {
							mon.resetConnection();
						}
						refreshAll();
					}
					return Status.OK_STATUS;
				}
			};
			proxyResetJob.setSystem(true);

        	//the assumption here is that there always will be just one CompositeMonitor in the application
        	//otherwise we get reset cascades..
        	proxyChangeListener = new IProxyChangeListener() {
				
				public void proxyInfoChanged(IProxyChangeEvent event) {
					//often mutliple events get fire our way, using this method to aggregate them
					// and only refresh once.
					//TODO is there a better method?
					if (proxyResetJob.getState() != Job.SLEEPING) {
						proxyResetJob.schedule(4000);
					}
					
				}
			};
        	service.addProxyChangeListener(proxyChangeListener);
        }
    }

    //this method cannot be called from constructor, the persisted hudsonMonitor childs need to be 
    //deserialized with their correct UUIDs first..otherwise creates duplicates..
    void populateProvidedJobs(boolean scheduleLoading)
    {
        Map<String, Collection<String>> jobs = new HashMap<String, Collection<String>>();
        for (HudsonJobProvider provider : providers) {
            //TODO add weak listener..
            List<HudsonServer> servers = provider.getHudsonServers();
            for (HudsonServer s : servers) {
                Collection<String> j = jobs.get( s.getUrl() );
                if (j == null) {
                    j = new HashSet<String>();
                    jobs.put( s.getUrl(), j );
                }
                j.addAll( s.getJobs() );
            }
        }
        //set the provided jobs to the hudson instances
        Set<HudsonMonitor> processed = new HashSet<HudsonMonitor>();
        //addMonitor cannot be enclosed in sync block as the firing shall be out of sync
        for (Map.Entry<String, Collection<String>> ent : jobs.entrySet())
        {
            HudsonMonitor monitor;
            try
            {
                monitor = getMonitor( ent.getKey() ); 
                if (monitor == null) {
                    monitor = HudsonManager.addHudsonMonitor(new URI(ent.getKey()), null);
                    addMonitor( monitor );
                }
                monitor.setProvidedJobs( ent.getValue(), scheduleLoading );
                processed.add( monitor );
            }
            catch ( URISyntaxException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //match the current monitor list against the list we just set to reset the ones not mentioned.
        synchronized (monitors)
        {
            for (HudsonMonitor mon : monitors)
            {
                if (!processed.contains( mon )) 
                {
                    mon.setProvidedJobs( Collections.<String>emptySet(), false );
                }
            }
        }
    }

    public CompositeMonitor()
    {
        this( UUID.randomUUID() );
    }

    public void addMonitor( HudsonMonitor monitor )
    {
        synchronized (monitors) {
            if (monitors.contains( monitor )) {
                return;
            }
            monitors.add( monitor );
        }
        monitor.addHudsonJobListener( this );
    }

    public void removeMonitor( HudsonMonitor monitor )
    {
        synchronized (monitors) {
            monitors.remove( monitor );
        }
        monitor.removeHudsonJobListener( this );
    }

    public Collection<HudsonMonitor> getAllMonitors()
    {
        synchronized (monitors) {
            return new ArrayList<HudsonMonitor>(monitors);
        }
    }

    public Collection<HudsonJob> getJobs()
    {
        Collection<HudsonJob> allJobs = new ArrayList<HudsonJob>();
        for ( HudsonMonitor monitor : getAllMonitors() )
        {
            allJobs.addAll( monitor.getJobs() );
        }
        return allJobs;
    }

    public HudsonMonitor getMonitor( String url )
    {
        URI searchedURI = URI.create( url );
        for ( HudsonMonitor monitor : getAllMonitors() )
        {
            if ( searchedURI.equals( monitor.getServerURI() ) )
                return monitor;
        }
        return null;
    }

    public void register( HudsonJobListener listener )
    {
        listeners.add( listener );
    }

    public void unregister( HudsonJobListener listener )
    {
        listeners.remove( listener );
    }

    public void getModified( AbstractHudsonJobEvent event )
    {
        boolean isRemoval = false;
        if (event instanceof HudsonJobEvent) {
            HudsonJobEvent ev = (HudsonJobEvent)event;
            isRemoval = ev.getType() == EventType.REMOVAL;
        }
        if ( !isRemoval && !getJobs().contains( event.getModifiedJob())) {
            //do not propagate jobs changes that are not in the composite monitor.
            //with exception of removal events.
            return;
        }
        for ( Object listener : listeners.getListeners() )
            ( (HudsonJobListener) listener ).getModified( event );

        // remove empty monitors
        if (event instanceof HudsonJobEvent) {
            HudsonJobEvent ev = (HudsonJobEvent)event;
            if ( ev.getType() == EventType.REMOVAL )
            {
                synchronized ( monitors )
                {
                    Iterator<HudsonMonitor> it = monitors.iterator();
                    while ( it.hasNext() ) 
                    {
                        HudsonMonitor m = it.next();
                        if ( m.getJobIds().isEmpty() ) 
                        {
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    public UUID getId()
    {
        return id;
    }

    public void save()
    {
        MonitorPersistence.store( this );
    }
    
    public void refreshAll() {
        refreshSome( getJobs() );
    }
    
    public void refreshSome(Collection<HudsonJob> jobs) {
        for (HudsonJob bj : jobs) {
            bj.markAsLoading();
        }
        new RetrieveJob( this, jobs ).schedule();
    }
    //TODO we currently ignore the fact that a job provider could be added at runtime..
    // no listening on changes so far..
    static List<HudsonJobProvider> createHudsonJobProviders()
    {
        ArrayList<HudsonJobProvider> providers = new ArrayList<HudsonJobProvider>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint =
            registry.getExtensionPoint( "com.sonatype.buildserver.spi.serverProviders" );
        if ( extensionPoint != null )
        {
            for ( IExtension extension : extensionPoint.getExtensions() )
            {
                for ( IConfigurationElement element : extension.getConfigurationElements() )
                {
                    if ( "provider".equals( element.getName() ) )
                    {
                        try
                        {
                            HudsonJobProvider prov = (HudsonJobProvider) element.createExecutableExtension( "class" );
                            providers.add( prov );
                        }
                        catch ( CoreException e )
                        {
                            log.error( "Could not create server provider", e );
                        }
                    }
                }
            }
        }

        return providers;
    }  
    
    
    private class ProviderListener implements ChangeListener {

        public void stateChanged( ChangeEvent e )
        {
            populateProvidedJobs(true);
        }
        
    }


    public boolean isUIUp()
    {
        boolean hasOne = false;
        synchronized (listeners) {
            for (Object list : listeners.getListeners()) {
                if (((HudsonJobListener)list).isUIUp()) {
                    hasOne = true;
                    break;
                }
            }
        }
        return hasOne;        
    }
    
}
