/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.view;

import java.util.Date;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.ErrorJob;
import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.BuildResultDTO;
import com.sonatype.matrix.rest.model.build.BuildStateDTO;
import com.sonatype.matrix.rest.model.project.ProjectDTO;

public class JobLabelProvider
    extends ColumnLabelProvider
    implements ITableLabelProvider, ILabelProvider
{
    public static final int STATUS_COLUMN = 0;

    public static final int WEATHER_COLUMN = 1;

    public static final int NAME_COLUMN = 2;

    public static final int SERVER_COLUMN = 3;

    public static final int LAST_BUILD_COLUMN = 4;

    private int column;
    
    public JobLabelProvider(int column) {
        this.column = column;
    }

    public Image getColumnImage( Object element, int columnIndex )
    {
        if ( element instanceof HudsonJob )
        {
            HudsonJob hudsonJob = (HudsonJob) element;
            switch ( columnIndex )
            {
                case STATUS_COLUMN:
                    return HudsonImages.getResultImage( hudsonJob );
                case WEATHER_COLUMN:
                    return HudsonImages.getHealthImage( hudsonJob );
            }
        }
        return null;
    }

    public String getColumnText( Object element, int columnIndex )
    {
        if ( element instanceof HudsonJob )
        {
            HudsonJob hudsonJob = (HudsonJob) element;
            ProjectDTO job = hudsonJob.getJobDetails();

            switch ( columnIndex )
            {
                case NAME_COLUMN:
                    return hudsonJob.getJobName();
                case SERVER_COLUMN:
                    return hudsonJob.getServerName();
                case LAST_BUILD_COLUMN:
                    if ( job != null )
                    {
                        BuildDTO build = job.getLastBuild();
                        if ( build != null )
                        {
                            return new Date( build.getTimeStamp() ).toString() + " / #" + build.getNumber();
                        }
                    }
                    break;
            }
        }
        return null;
    }
    
    public String getColumnTooltip( Object element, int columnIndex )
    {
        if ( element instanceof HudsonJob )
        {
            HudsonJob hudsonJob = (HudsonJob) element;
            switch ( columnIndex )
            {
                case STATUS_COLUMN : 
                    return getBuildStatusString( hudsonJob );
                case WEATHER_COLUMN:
                    return getBuildWeatherString( hudsonJob );
                case NAME_COLUMN:
                    return appendum(hudsonJob) ;
                case SERVER_COLUMN:
                    return appendum(hudsonJob);
                case LAST_BUILD_COLUMN:
                    return appendum(hudsonJob);
            }
        }
        return null;
    }
    
    private String appendum(HudsonJob job) {
        String toRet = "Name: " + job.getJobName() + "\n" + "Server: " + job.getServerName();
        toRet = toRet + "\nLast build: " + HudsonUtils.getFormattedLastRun( job );
        ProjectDTO det = job.getJobDetails();
        if (det != null) {
            BuildDTO bld = det.getLastBuild();
            if (bld != null) {
                toRet = toRet + "\nLast build number: #" + bld.getNumber();
            }
        }
        
        String errorMessage = errorMessage(job); 
        String appendum = errorMessage != null ? "\n\nError while loading job: " + errorMessage : "";
        if (job.isDeclarativelyProvided()) {
            appendum = appendum + "\n\nThis job has been contributed by external plugins.";
        }
        return toRet + appendum;
    }
    
    private String getBuildWeatherString( HudsonJob hudsonJob )
    {
        return HudsonUtils.getHealthText( hudsonJob );
    }
    
    private String errorMessage(HudsonJob bj) {
        ProjectDTO job = bj.getJobDetails();
        if (job != null) 
        {
            if (job instanceof ErrorJob) {
                String mess = ((ErrorJob)job).getErrorMessage();
                if (mess == null) {
                    mess = "An unknown error appeared while loading the job";
                }
                return mess;
            }
        }
        return null;

    }

    private String getBuildStatusString( HudsonJob bj )
    {
        ProjectDTO job = bj.getJobDetails();
        if (job != null) 
        {
            if (job instanceof ErrorJob) {
                String mess = ((ErrorJob)job).getErrorMessage();
                if (mess == null) {
                    mess = "An error appeared while loading the job";
                }
                return mess;
            }
            if (!job.isEnabled()) {
                return "Job is disabled";
            }
            String main = "";
            BuildDTO bld = job.getLastBuild();
            if (bld != null) {
                BuildStateDTO state = bld.getState();
                BuildResultDTO result = bld.getResult();
                if ( state == BuildStateDTO.BUILDING )
                {
                    main =  "Build is running";
                } 
                else if ( state == BuildStateDTO.NOT_STARTED )
                {
                    main = "Build is queued";
                }
                if ( state == null || state == BuildStateDTO.COMPLETED )
                {
                    if ( result == BuildResultDTO.SUCCESS )
                    {
                        main = "Build completed sucessfully";
                    }
                    if ( result == BuildResultDTO.ABORTED )
                    {
                        main =  "Build was aborted";
                    }
                    if ( result == BuildResultDTO.FAILURE )
                    {
                        main = "Build failed";
                    }
                    if ( result == BuildResultDTO.UNSTABLE )
                    {
                        main = "Build is unstable";
                    }
                }
                
            } else {
                main = "No build was executed yet";
            }
            if (job.isQueued())
            {
                main = main +  "\nNew build is queued.";
            }
            return main;
        }
        return "Job's build state is unknown";
    }
    

    @Override
    public String getToolTipText( Object element )
    {
        return getColumnTooltip( element, column);
    }
    

    public Image getImage( Object element )
    {
        return getColumnImage( element, column );
    }

    public String getText( Object element )
    {
        return getColumnText( element, column );
    }
}
