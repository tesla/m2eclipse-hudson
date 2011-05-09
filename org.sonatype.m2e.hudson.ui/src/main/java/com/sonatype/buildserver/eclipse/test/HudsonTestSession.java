/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.test;

import org.eclipse.jdt.internal.junit.model.TestRunSession;

import com.sonatype.matrix.rest.model.build.TestsDTO;


@SuppressWarnings( "restriction" )
public class HudsonTestSession extends TestRunSession 
{
    private HudsonTestRoot root;
    
    public HudsonTestSession(String sessionName, TestsDTO tests)
    {
           super( sessionName, null );
           root = new HudsonTestRoot( this, tests );
    }
    
    public synchronized HudsonTestRoot getTestRoot()
    {
        return root;
    }
    
    @Override
    public int getFailureCount()
    {
        return root.getFailureCount();
    }

    @Override
    public int getIgnoredCount()
    {
        return root.getIgnoredCount();
    }
    
    @Override
    public int getErrorCount()
    {
        return root.getErrorCount();
    }
    
    @Override
    public int getTotalCount()
    {
        return root.getTotalCount();
    }
    
    @Override
    public int getStartedCount()
    {
        return root.getTotalCount();
    }
    
    @Override
    public double getElapsedTimeInSeconds()
    {
        return root.getElapsedTimeInSeconds();
    }
}

