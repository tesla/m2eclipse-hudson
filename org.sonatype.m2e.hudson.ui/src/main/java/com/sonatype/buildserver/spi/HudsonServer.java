/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.spi;

import java.util.Set;

public final class HudsonServer
{
    private final Set<String> jobs;
    private final String url;

    public HudsonServer(String url, Set<String> jobs) {
        this.url = url;
        this.jobs = jobs;
    }

    public String getUrl()
    {
        return url;
    }

    public Set<String> getJobs()
    {
        return jobs;
    }
}
