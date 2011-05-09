/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.test;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;

import com.sonatype.matrix.rest.model.build.TestCaseDTO;

@SuppressWarnings( "restriction" )
public class HudsonTestCase
    extends TestCaseElement
{
    private TestCaseDTO testCase;

    private HudsonTestSuite parent;

    public HudsonTestCase( TestCaseDTO testCase, HudsonTestSuite parent )
    {
        super( parent, testCase.getName(), testCase.getName() );
        this.testCase = testCase;
        this.parent = parent;
    }

    @Override
    public ProgressState getProgressState()
    {
        return ProgressState.COMPLETED;
    }

    @Override
    public Result getTestResult( boolean includeChildren )
    {
        switch ( testCase.getStatus() )
        {
            case PASSED:
                return Result.OK;
            case REGRESSION:
                return Result.FAILURE;
            case SKIPPED:
                return Result.UNDEFINED;
            case FAILED:
                return Result.FAILURE;
            case FIXED:
                return Result.OK;
        }
        return Result.UNDEFINED;
    }

    @Override
    public FailureTrace getFailureTrace()
    {
        Result testResult= getTestResult(false);
        if (testResult == Result.ERROR || testResult == Result.FAILURE) {
            return new FailureTrace( testCase.getErrorStackTrace(), testCase.getErrorDetails(), null );
        }
        return null;
    }

    @Override
    public ITestElementContainer getParentContainer()
    {
        return parent;
    }

    @Override
    public ITestRunSession getTestRunSession()
    {
        return null;
    }

    @Override
    public double getElapsedTimeInSeconds()
    {
        return testCase.getDuration();
    }

    public String getTestName()
    {
        //Mimic the way the junit plugin encode methods
        return testCase.getName() + "(" + testCase.getClassName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public Status getStatus()
    {
        switch ( testCase.getStatus() )
        {
            case PASSED:
                return Status.OK;
            case REGRESSION:
                return Status.FAILURE;
            case SKIPPED:
                return Status.NOT_RUN;
            case FAILED:
                return Status.FAILURE;
            case FIXED:
                return Status.OK;
        }
        return Status.NOT_RUN;
    }

    @Override
    public String getTestClassName()
    {
        return testCase.getClassName();
    }

    @Override
    public String getClassName()
    {
        return testCase.getClassName();
    }

    @Override
    public String getTestMethodName()
    {
        return testCase.getName();
    }
    
    @Override
    public String getTrace()
    {
        if (testCase.getErrorDetails() == null && testCase.getErrorStackTrace() == null)
            return ""; //$NON-NLS-1$
        return testCase.getErrorStackTrace().toString();
    }
}
