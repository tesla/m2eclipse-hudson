/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

public abstract class AbstractHudsonJobEvent
{
    private HudsonJob modifiedJob;



    public AbstractHudsonJobEvent( HudsonJob modified )
    {
        modifiedJob = modified;
    }

    public HudsonJob getModifiedJob()
    {
        return modifiedJob;
    }

}
