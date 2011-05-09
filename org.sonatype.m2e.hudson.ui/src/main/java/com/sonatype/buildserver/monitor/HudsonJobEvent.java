/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;


public class HudsonJobEvent extends AbstractHudsonJobEvent
{

    private EventType type;
    
    public static enum EventType
    {
        ADDED, CHANGED, REMOVAL
    }

    public HudsonJobEvent( HudsonJob modified, EventType type )
    {
        super( modified );
        this.type = type;
    }
    
    public EventType getType()
    {
        return type;
    }
    
}
