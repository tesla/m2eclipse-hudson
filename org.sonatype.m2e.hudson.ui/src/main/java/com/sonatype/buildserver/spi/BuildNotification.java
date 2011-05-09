/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.spi;

/**
 * gets called on non-UI thread. any UI shall be non blocking to allow other impls to proceed as well.
 * @author mkleint
 *
 */
public interface BuildNotification {

	void notifyFailure(BuildDetails res);
	
	void notifySuccess(BuildDetails res);
	
}
