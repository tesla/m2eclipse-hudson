/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

public interface HudsonJobListener {
	
	void getModified( AbstractHudsonJobEvent event );
	
	/**
	 * this ill-named method shall mark if the listener is associated with ui
	 * it's a marker for the HudsonMonitor to be able to decide whether it's ok to show 
	 * popup dialog or not.
	 * @return
	 */
	boolean isUIUp();
}
