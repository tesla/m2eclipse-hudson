/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.spi;

import com.sonatype.buildserver.eclipse.ui.StatusNotification;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.matrix.rest.model.build.BuildDTO;

public final class BuildDetails {
	//trampoline magic to avoid public constructor in public class..
    static {
        StatusNotification.TRAMPOLINE = new StatusNotification.BuildResultTrampoline() {
			@Override
        	public BuildDetails createResult(HudsonJob job, BuildDTO build) {
        		return new BuildDetails(job, build);
        	}
        };
    }
    
    private BuildDTO build;
	private HudsonJob job;

	private BuildDetails(HudsonJob job, BuildDTO bld) {
		this.job = job;
		this.build = bld;
	}
	
	public String getServerName() {
		return getServerName();
	}
	
	public String getJobName() {
		return job.getJobName();
	}
	
	public int getBuildNumber() {
		return build.getNumber();
	}
	
}
