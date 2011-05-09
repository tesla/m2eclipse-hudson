/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.spi;

import java.util.List;

import javax.swing.event.ChangeListener;

/**
 * this extension provides jobs to the hudson integration. These are dynamically retrieved from 
 * external sources, are expected to be externally persisted across IDE sessions. When a change occurs during
 * IDE uptime a ChangeEvent is to be fired to interested parties and the getHudsonServers() method 
 * shall have a new list available by that time.
 * @author mkleint
 *
 */
public interface HudsonJobProvider
{

    /**
     * return a list of <code>HudsonServer</code> instances denoting what servers and jobs are to be listed.
     * @return unmodifiable list
     */
    List<HudsonServer> getHudsonServers();
    
    void addChangeListener(ChangeListener list);
    
    void removeChangeListener(ChangeListener list);
    
}
