/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.test;

import java.util.List;

import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestRunSession;

import org.hudsonci.rest.model.build.TestSuiteDTO;
import org.hudsonci.rest.model.build.TestsDTO;


@SuppressWarnings( "restriction" )
public class HudsonTestRoot extends TestRoot
{
    private TestsDTO tests;
    private final static ITestElement[] NO_CHILDREN = new ITestElement[0];
    
    public HudsonTestRoot( ITestRunSession session, TestsDTO tests )
    {
        super( session );
        this.tests = tests;
    }
    
    @Override
    public ITestElement[] getChildren()
    {
        if (tests.getSuites().size() == 0)
            return NO_CHILDREN;
        HudsonTestSuite[] children = new HudsonTestSuite[tests.getSuites().size()];
        int i = 0;
        for ( TestSuiteDTO suite : tests.getSuites() )
        {
            children[i++] = new HudsonTestSuite(suite, this);
        }
        return children;
    }
    
    @Override
    public Status getStatus()
    {
        return Status.OK;
    }
    
    @Override
    public ProgressState getProgressState()
    {
        return ProgressState.COMPLETED;
    }
    
    @Override
    public double getElapsedTimeInSeconds()
    {
        float totalTime = 0;
        List<TestSuiteDTO> suites = tests.getSuites();
        for ( TestSuiteDTO suite : suites )
        {
            totalTime += suite.getDuration();
        }
        return totalTime;
    }

    public int getIgnoredCount()
    {
        return tests.getSkipped();
    }

    public int getErrorCount()
    {
        return 0;
    }

    public int getTotalCount()
    {
        return tests.getTotal();
    }

    public int getFailureCount()
    {
        return tests.getFailed();
    }
}
