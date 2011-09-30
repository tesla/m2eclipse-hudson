/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.ErrorJob;
import org.hudsonci.rest.model.build.BuildDTO;
import org.hudsonci.rest.model.build.BuildResultDTO;
import org.hudsonci.rest.model.build.BuildStateDTO;
import org.hudsonci.rest.model.project.HealthDTO;
import org.hudsonci.rest.model.project.ProjectDTO;

public class HudsonImages
{
    private static Logger log = LoggerFactory.getLogger( HudsonImages.class );
    
    private static final String PATH = "icons/";//$NON-NLS-1$
    
    public static final String EMPTY = PATH + "empty.gif";//$NON-NLS-1$

    public static final String JUNIT = PATH + "junit.gif";//$NON-NLS-1$

    public static final String CONSOLE = PATH + "console.gif";//$NON-NLS-1$

    public static final String SHOW_FULL_CONSOLE = PATH + "showFullConsole.gif";//$NON-NLS-1$

    public static final String ABORTED = PATH + "grey.gif";//$NON-NLS-1$
    
    public static final Image ABORTED_IMAGE = getImage( ABORTED );
    
    public static final String LOCKED_BADGE = PATH + "lockedBadge.gif";//$NON-NLS-1$

    public static final String ADD_JOB = PATH + "blue_add.gif";//$NON-NLS-1$

    public static final ImageDescriptor ADD_JOB_DESCRIPTOR = getImageDescriptor( ADD_JOB );

    public static final String BUILD = PATH + "build.png";//$NON-NLS-1$

    public static final ImageDescriptor BUILD_DESCRIPTOR = getImageDescriptor( BUILD );

    public static final String FAILURE = PATH + "red.gif";//$NON-NLS-1$

    public static final Image FAILURE_IMAGE = getImage( FAILURE );
    
    public static final String FAILURE_ANIME = PATH + "red_anime.gif";//$NON-NLS-1$
    
    public static final String NOT_BUILT = PATH + "grey.gif";//$NON-NLS-1$

    public static final Image NOT_BUILT_IMAGE = getImage( NOT_BUILT );

    public static final String REFRESH = PATH + "refresh.gif";//$NON-NLS-1$

    public static final Image REFRESH_IMAGE = getImage( REFRESH );
    
    public static final String RUNNING = PATH + "running.gif";//$NON-NLS-1$

    public static final Image RUNNING_IMAGE = getImage( RUNNING );
    
    public static final String RUNNING_BADGE = PATH + "running_badge.gif";//$NON-NLS-1$

    public static final String SUCCESS = PATH + "blue.gif";//$NON-NLS-1$

    public static final Image SUCCESS_IMAGE = getImage( SUCCESS );
    
    public static final String SUCCESS_ANIME = PATH + "blue_anime.gif";//$NON-NLS-1$

    public static final String UNSTABLE = PATH + "yellow.gif";//$NON-NLS-1$

    public static final Image UNSTABLE_IMAGE = getImage( UNSTABLE );
    
    public static final String UNSTABLE_ANIME = PATH + "yellow_anime.gif";//$NON-NLS-1$

    public static final String WARNING = PATH + "warning.gif";//$NON-NLS-1$

    public static final Image WARNING_IMAGE = getImage( WARNING );

    public static final String HEALTH_00_TO_19 = PATH + "health-00to19.gif";//$NON-NLS-1$

    public static final Image HEALTH_00_TO_20_IMAGE = getImage( HEALTH_00_TO_19 );

    public static final String HEALTH_20_TO_39 = PATH + "health-20to39.gif";//$NON-NLS-1$

    public static final Image HEALTH_21_TO_40_IMAGE = getImage( HEALTH_20_TO_39 );

    public static final String HEALTH_40_TO_59 = PATH + "health-40to59.gif";//$NON-NLS-1$

    public static final Image HEALTH_41_TO_60_IMAGE = getImage( HEALTH_40_TO_59 );

    public static final String HEALTH_60_TO_79 = PATH + "health-60to79.gif";//$NON-NLS-1$

    public static final Image HEALTH_61_TO_80_IMAGE = getImage( HEALTH_60_TO_79 );

    public static final String HEALTH_80_PLUS = PATH + "health-80plus.gif";//$NON-NLS-1$

    public static final Image HEALTH_81_PLUS_IMAGE = getImage( HEALTH_80_PLUS );

    public static final String WEB = PATH + "web.gif";//$NON-NLS-1$

    public static final ImageDescriptor WEB_DESCRIPTOR = getImageDescriptor( WEB );

    public static final String HUDSON = PATH + "hudson.gif";//$NON-NLS-1$

    public static final ImageDescriptor HUDSON_DESCRIPTOR = getImageDescriptor( HUDSON );

    public static final Image ADD_CHANGE = getImage( PATH + "document_add.gif" );//$NON-NLS-1$

    public static final Image DELETE_CHANGE = getImage( PATH + "document_delete.gif" );//$NON-NLS-1$

    public static final Image EDIT_CHANGE = getImage( PATH + "document_edit.gif" );//$NON-NLS-1$

    public static final Image CHANGESET = getImage( PATH + "changeset.gif" );//$NON-NLS-1$

    public static final Image JUNIT_IMAGE = getImage( JUNIT );
    
    public static final String OVERLAY_CHANGED = PATH + "overlay_delta.gif"; //$NON-NLS-1$
    
    public static final String OVERLAY_PENDING = PATH + "overlay_pending.gif"; //$NON-NLS-1$

    public static Image getImage( String image )
    {
        return HudsonUIActivator.getDefault().getImage( image );
    }

    public static ImageDescriptor getImageDescriptor( String image )
    {
        return HudsonUIActivator.getDefault().getImageDescriptor( image );
    }
    
    public static Image getOverlayImage( String base, String overlay1, String overlay2)
    {
        getOverlayImageDescriptor( base, overlay1,overlay2);
        ImageRegistry imageRegistry = getImageRegistry();
        return imageRegistry == null ? null : imageRegistry.get( base + overlay1 + overlay2 );
    }  
    
    private static ImageRegistry getImageRegistry()
    {
        HudsonUIActivator plugin = HudsonUIActivator.getDefault();
        return plugin == null ? null : plugin.getImageRegistry();
    }
    
    private static ImageDescriptor getOverlayImageDescriptor( String basekey, String overlaykey1, String overlaykey2 )
    {
        String key = basekey + overlaykey1 + overlaykey2;
        try
        {
            ImageRegistry imageRegistry = getImageRegistry();
            if ( imageRegistry != null )
            {
                ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( key );
                if ( imageDescriptor == null )
                {
                    ImageDescriptor base = getImageDescriptor( basekey );
                    ImageDescriptor overlay1 = overlaykey1 != null ? getImageDescriptor( overlaykey1 ) : null;
                    ImageDescriptor overlay2 = overlaykey2 != null ? getImageDescriptor( overlaykey2 ) : null;
                    if ( base == null)
                    {
                        log.error( "cannot construct overlay image descriptor for " + basekey + " " + overlaykey1 + " " + overlaykey2 );
                        return null;
                    }
                    imageDescriptor = new DecorationOverlayIcon( base.createImage(), 
                               new ImageDescriptor[] {null, overlay1, null, overlay2, null} );
                    imageRegistry.put( key, imageDescriptor );
                }
                return imageDescriptor;
            }
        }
        catch ( Exception ex )
        {
            log.error( key, ex );
        }
        return null;
    }
    
    public static ImageDescriptor createImageDescriptor( String key, ImageData imageData )
    {
        try
        {
            ImageRegistry imageRegistry = getImageRegistry();
            if ( imageRegistry != null )
            {
                ImageDescriptor imageDescriptor = imageRegistry.getDescriptor( key );
                if ( imageDescriptor != null )
                {
                    imageRegistry.remove( key );
                }
                {
                    imageDescriptor = ImageDescriptor.createFromImageData( imageData );
                    imageRegistry.put( key, imageDescriptor );
                }
                return imageDescriptor;
            }
        }
        catch ( Exception ex )
        {
            log.error( key, ex );
        }
        return null;
    }
    

    public static Image getResultImage( HudsonJob job )
    {
        BuildResultDTO buildResult = null;

        ProjectDTO jobDetails = job.getJobDetails();
        if ( jobDetails != null )
        {
            if ( jobDetails instanceof ErrorJob )
                return HudsonImages.WARNING_IMAGE;
            if ( !jobDetails.isEnabled() )
            {
                return NOT_BUILT_IMAGE;
            }
            //TODO both queued and running shall become just an icon badge when there is a last build..
            String overlay1 = null;
            if (jobDetails.isQueued()) {
                overlay1 =  OVERLAY_PENDING;
            }
            String overlay2 = null;
            BuildDTO build = jobDetails.getLastBuild();
            if ( build != null )
            {
//                if ( build.getState() == BuildState.BUILDING ) {
//                    overlay2 =  RUNNING_BADGE;
//                }

                buildResult = build.getResult();
            }
            String base = getResultImagePath( buildResult );
            if (base == null) {
                //it would be nice to have the running badge attached to the last build result,
                //but at the time we get to query the lastbuild  it's already one with no result.
                if ( build != null && build.getState() == BuildStateDTO.BUILDING ) {
                    base =  RUNNING;
                }
            }
            //just a way to show the badge on top of nothing
            if (base == null && overlay1 != null) {
                return REFRESH_IMAGE;
            }
            if (base == null) {
                return null;
            }
            return getOverlayImage( base, overlay1, overlay2 );
        }
        return null;

    }

    public static String getResultImagePath( BuildResultDTO result )
    {
        if ( result == BuildResultDTO.ABORTED )
        {
            return ABORTED;
        }
        else if ( result == BuildResultDTO.FAILURE )
        {
            return FAILURE;
        }
        else if ( result == BuildResultDTO.NOT_BUILT )
        {
            return NOT_BUILT;
        }
        else if ( result == BuildResultDTO.SUCCESS )
        {
            return SUCCESS;
        }
        else if ( result == BuildResultDTO.UNSTABLE )
        {
            return UNSTABLE;
        }
        return null;
    }
    
    public static Image getResultImage( BuildResultDTO result )
    {
        String path = getResultImagePath( result );
        if (path != null) {
            return getImage( path );
        }
        return null;
    }    

    public static Image getHealthImage( HudsonJob job )
    {
        int score = -1;

        ProjectDTO jobDetails = job.getJobDetails();
        if ( jobDetails != null )
        {
            if ( jobDetails instanceof ErrorJob )
                return null;
            HealthDTO health = jobDetails.getHealth();
            if ( health != null )
            {
                score = health.getScore();
            }
        }

        return getHealthImage( score );
    }

    /**
     * returns a health image for scores 0-100, returns null for score < 0
     * 
     * @param score
     * @return
     */
    public static Image getHealthImage( int score )
    {
        if ( score > 80 )
        {
            return HEALTH_81_PLUS_IMAGE;
        }
        else if ( score > 60 )
        {
            return HEALTH_61_TO_80_IMAGE;
        }
        else if ( score > 40 )
        {
            return HEALTH_41_TO_60_IMAGE;
        }
        else if ( score > 20 )
        {
            return HEALTH_21_TO_40_IMAGE;
        }
        else if ( score >= 0 )
        {
            return HEALTH_00_TO_20_IMAGE;
        }
        else
            return null;
    }
}