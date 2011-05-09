/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class HudsonUIActivator
    extends AbstractUIPlugin
{
    public final static String PLUGIN_ID = "org.sonatype.m2e.hudson.ui"; //$NON-NLS-1$

    // The shared instance
    private static HudsonUIActivator plugin;

    private static BundleContext context = null;

    private ServiceTracker proxyServiceTracker;
    
    /**
     * The constructor
     */
    public HudsonUIActivator()
    {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start( BundleContext context )
        throws Exception
    {
        super.start( context );
        plugin = this;
        HudsonUIActivator.context = context;
        
        proxyServiceTracker = new ServiceTracker( context, IProxyService.class.getName(), null );
        proxyServiceTracker.open();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext context )
        throws Exception
    {
        HudsonUIActivator.context = null;
        try
        {
            proxyServiceTracker.close();
            proxyServiceTracker = null;
        }
        finally
        {
	        plugin = null;
	        super.stop( context );
        }
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static HudsonUIActivator getDefault()
    {
        return plugin;
    }

    /**
     * returns the proxy service or null..
     * @return
     */
    public IProxyService getProxyService()
    {
        return (IProxyService) proxyServiceTracker.getService();
    }
    
    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     * 
     * @param path the path
     * @return the image descriptor
     */
    public ImageDescriptor getImageDescriptor( String path )
    {
        ImageRegistry imageRegistry = getImageRegistry();
        if ( imageRegistry != null )
        {
            ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( path );
            if ( imageDescriptor == null )
            {
                imageDescriptor = imageDescriptorFromPlugin( PLUGIN_ID, path );
                imageRegistry.put( path, imageDescriptor );
            }
            return imageDescriptor;
        }

        return null;
    }

    public Image getImage( String path )
    {
        ImageRegistry imageRegistry = getImageRegistry();
        if ( imageRegistry != null )
        {
            getImageDescriptor( path );
            return imageRegistry.get( path );
        }
        return null;
    }

    public static BundleContext getContext()
    {
        return context;
    }
}
