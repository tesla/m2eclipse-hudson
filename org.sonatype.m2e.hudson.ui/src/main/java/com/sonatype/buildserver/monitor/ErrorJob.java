/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import org.hudsonci.rest.model.build.BuildDTO;
import org.hudsonci.rest.model.project.HealthDTO;
import org.hudsonci.rest.model.project.ProjectDTO;

public class ErrorJob
    extends ProjectDTO
{
    private ProjectDTO old;
    private String errorMessage;
    private Exception exception;


    public ErrorJob( ProjectDTO old, String errorMessage, Exception e )
    {
        this.old = old;
        this.errorMessage = errorMessage;
        this.exception = e;
    }
    
    public Exception getException()
    {
        return exception;
    }

    public void setException( Exception exception )
    {
        this.exception = exception;
    }
    
    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }

    public ProjectDTO getJob()
    {
        return old;
    }

    public String getBlockedReason()
    {
        if ( old != null )
            return old.getBlockedReason();
        return null;
    }

    public String getDescription()
    {
        if ( old != null )
            return old.getDescription();
        return null;
    }

    public HealthDTO getHealth()
    {
        if ( old != null )
            return old.getHealth();
        return null;
    }

    public BuildDTO getLastBuild()
    {
        if ( old != null )
            return old.getLastBuild();
        return null;
    }

    public String getName()
    {
        if ( old != null )
            return old.getName();
        return null;
    }

    public String getType()
    {
        if ( old != null )
            return old.getType();
        return null;
    }

    public String getUrl()
    {
        if ( old != null )
            return old.getUrl();
        return null;
    }

    public Boolean isBlocked()
    {
        if ( old != null )
            return old.isBlocked();
        return super.isBlocked();
    }

    public boolean isConcurrent()
    {
        if ( old != null )
            return old.isConcurrent();
        return super.isConcurrent();
    }

    public boolean isConfigurabe()
    {
        if ( old != null )
            return old.isConfigurable();
        return super.isConfigurable();
    }

    public boolean isEnabled()
    {
        if ( old != null )
            return old.isEnabled();
        return super.isEnabled();
    }

    public boolean isQueued()
    {
        if ( old != null )
            return old.isQueued();
        return super.isQueued();
    }
}