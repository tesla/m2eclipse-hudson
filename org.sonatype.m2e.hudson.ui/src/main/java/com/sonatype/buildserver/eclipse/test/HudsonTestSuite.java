/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.test;

import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRoot;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

import org.hudsonci.rest.model.build.TestCaseDTO;
import org.hudsonci.rest.model.build.TestSuiteDTO;


@SuppressWarnings( "restriction" )
public class HudsonTestSuite extends TestSuiteElement
{
    private TestSuiteDTO suite;
    private TestElement children[] = null;
    public HudsonTestSuite( TestSuiteDTO suite, TestRoot root )
    {
        super( root, suite.getName(), suite.getName() , suite.getCases().size() );
        this.suite = suite;
    }

    @Override
    public Status getStatus()
    {
        return getCumulatedStatus();
    }
    
    private Status getCumulatedStatus() {
        TestElement[] children= getChildren();
        if (children.length == 0)
            return getSuiteStatus();

        Status cumulated= children[0].getStatus();

        for (int i= 1; i < children.length; i++) {
            Status childStatus= children[i].getStatus();
            cumulated= Status.combineStatus(cumulated, childStatus);
        }
        return cumulated;
    }
    
    @Override
    public TestElement[] getChildren()
    {
        if (children != null )
            return children;
        
        HudsonTestCase[] results = new HudsonTestCase[suite.getCases().size()];
        int i = 0;
        for ( TestCaseDTO testCase : suite.getCases() )
        {
            results[i++] = new HudsonTestCase(testCase, this);
        }
        children = results;   
        return children;
    }
    
    @Override
    public FailureTrace getFailureTrace()
    {
        return null;
    }
}
