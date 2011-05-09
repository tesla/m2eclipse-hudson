/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.maven.ide.eclipse.authentication.AuthFacade;
import org.maven.ide.eclipse.authentication.IAuthData;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.matrixclient.MatrixFactory;
import com.sonatype.matrix.rest.client.MatrixClient;
import com.sonatype.matrix.rest.client.OpenOptions;

public class HudsonManager
{
    private static final Map<URI, MatrixClient> serverConnections =
        Collections.synchronizedMap( new HashMap<URI, MatrixClient>() );

    private static final Map<String, HudsonMonitor> monitors = Collections.synchronizedMap( new HashMap<String, HudsonMonitor>() );
    private static final Map<String, CompositeMonitor> composites = Collections.synchronizedMap( new HashMap<String, CompositeMonitor>() );
    private static Logger log = LoggerFactory.getLogger( HudsonManager.class );

    static MatrixClient getServer( URI server )
    {
        synchronized ( serverConnections )
        {
        	MatrixClient client = serverConnections.get( server );
            if ( client == null )
            {
                client = createClient(server);
            } else {
                // Ensure that authentication data hasn't changed
                IAuthData data = AuthFacade.getAuthService().select( server.toString() );
                OpenOptions op = client.getOptions();
                boolean changed;
                if (data != null) {
                    String persistedUser = data.getUsername() == null || data.getUsername().length() == 0 ? null : data.getUsername();
                    String persistedPass = data.getPassword() == null || data.getPassword().length() == 0 ? null : data.getPassword();
                    changed = changed(persistedUser, op.getUsername()) || changed(persistedPass, op.getPassword());
                } else {
                    changed = op.getPassword() != null || op.getUsername() != null;
                }
                if (changed) {
                    client.close();
                    resetServer(server);
                    client = createClient(server);
                }
            }
            return client;
        }
    }

	private static MatrixClient createClient(URI server) {
		MatrixClient client = MatrixFactory.getClient();
		IAuthData data = AuthFacade.getAuthService().select( server.toString() );
        // Deal with other things like disable self-signed, etc.
		OpenOptions options = new OpenOptions();
		options.setDisableCertificateValidation( true );
		if ( data != null )
		{
		    setAuthentication(data, options);
		}
		IProxyData proxyInfo = selectProxy(server, HudsonUIActivator.getDefault().getProxyService());
		if ( proxyInfo != null && proxyInfo.getHost() != null )
		{
			options.setProxyHost( proxyInfo.getHost() );
			options.setProxyPort( proxyInfo.getPort() );
			options.setProxyProtocol( proxyInfo.getType() );
			if ( proxyInfo.isRequiresAuthentication() )
		    {
				options.setProxyUsername( proxyInfo.getUserId() );
				options.setProxyPassword( proxyInfo.getPassword() );
				log.debug( "Connecting to {} via proxy {} and authentication", server, options.getProxyHost() );
		    }
		    else
		    {
		        log.debug( "Connecting to {} via proxy {} and no authentication", server, options.getProxyHost() );
		    }
		}
		else
		{
		    log.debug( "Connecting to {} without proxy", server );
		}
		client.open( server, options );
		serverConnections.put( server, client );
		return client;
	}

    private static boolean changed(String newStr, String oldStr) {
    	return newStr == null && oldStr != null || (newStr != null && !newStr.equals(oldStr));
    }

	private static void setAuthentication(IAuthData data, OpenOptions options) {
		String login = data.getUsername();
		String pwd = data.getPassword();
		if ( login != null && login.length() > 0 )
		{
		    options.setUsername( login );
		    options.setPassword( pwd );
		} else {
			options.setUsername(null);
			options.setPassword(null);
		}
	}
    
    static void resetServer( URI serverURI )
    {
        synchronized ( serverConnections )
        {
            serverConnections.remove( serverURI );
        }
    }

    
    public static HudsonMonitor addHudsonMonitor( URI uri, UUID uuid) {
        if (uuid != null) {
            synchronized ( monitors )
            {
                 HudsonMonitor bm = monitors.get( uuid );
                 if (bm != null) {
                     return bm;
                 }
            }
        } 
        HudsonMonitorSesame open = new HudsonMonitorSesame( uri, uuid );
        HudsonMonitor bm = new HudsonMonitor( open );
        monitors.put( bm.getId().toString(), bm );
        return bm;
    }

    //TODO this pattern is fairly error prone, we shall have just one place creating hudsonMonitors
    public static CompositeMonitor addCompositeMonitor( CompositeMonitor m )
    {
        if ( m == null )
            return null;
        synchronized ( composites )
        {
            if ( !composites.containsKey( m.getId().toString() ) )
            {
                composites.put(m.getId().toString(), m );
                return m;
            }
            else
            {
                return composites.get( m.getId().toString() );
            }
        }
    }
    

    public static CompositeMonitor getCompositeMonitor( String monitorId )
    {
        synchronized ( composites )
        {
            CompositeMonitor monitor = composites.get( monitorId );
            if ( monitor != null )
                return monitor;

            Object loaded = MonitorPersistence.load( monitorId );
            if (! (loaded instanceof CompositeMonitor)) {
                throw new IllegalStateException("Wrong type of monitor loaded for " + monitorId);
            }
            monitor = (CompositeMonitor)loaded;
            monitor.populateProvidedJobs( false );
            monitor.refreshAll();
            return addCompositeMonitor( monitor );
        }
    }
    
    public static void setClient( URI uri, MatrixClient client ) {
        synchronized ( serverConnections )
        {
            serverConnections.put( uri, client );
        }
    }
    
    public static synchronized CompositeMonitor getDefaultMonitor()
    {
        IEclipsePreferences pref = new InstanceScope().getNode( HudsonUIActivator.PLUGIN_ID );
        String id = pref.get( "defaultMonitor", null ); //$NON-NLS-1$

        // Something was in there from previous runs.
        if ( id != null )
        {
            CompositeMonitor o = HudsonManager.getCompositeMonitor( id );
            if ( o != null )
                return o;
        }

        // Nothing was previously persisted
        // Persist the new object we have created
        CompositeMonitor createdMonitor = new CompositeMonitor();
        pref.put( "defaultMonitor", createdMonitor.getId().toString() ); //$NON-NLS-1$
        try
        {
            pref.flush();
        }
        catch ( BackingStoreException e )
        {
            log.debug( "Error while storing default monitor UUID.", e );
            return createdMonitor;
        }
        createdMonitor.save();
        return HudsonManager.getCompositeMonitor( createdMonitor.getId().toString() );
    }
    
    private static IProxyData selectProxy( URI url, IProxyService proxyService )
    {
        if ( proxyService != null && proxyService.isProxiesEnabled() )
        {
            IProxyData[] proxies = proxyService.select( url );

            if ( proxies.length > 0 )
            {
                if ( proxies.length == 1 )
                {
                    return proxies[0];
                }
                else
                {
                    String protocol = url.getScheme();
                    for ( IProxyData proxy : proxies )
                    {
                        if ( protocol.equalsIgnoreCase( proxy.getType() ) )
                        {
                            return proxy;
                        }
                    }
                    return proxies[0];
                }
            }
        }

        return null;
    }

    /**
     * only hudsonmanager shall create new instances of hudsonmonitor
     * @author mkleint
     *
     */
    static final class HudsonMonitorSesame {
        final URI uri;
        final UUID uuid;
        
        private HudsonMonitorSesame(URI uri, UUID uuid) {
            this.uri = uri;
            this.uuid = uuid;
        }
        
    }
    

        }
