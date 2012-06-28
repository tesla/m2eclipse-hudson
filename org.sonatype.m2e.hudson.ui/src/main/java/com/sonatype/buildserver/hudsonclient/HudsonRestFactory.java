/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.hudsonclient;

import org.sonatype.guice.bean.binders.WireModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hudsonci.rest.client.HudsonClient;
import org.hudsonci.rest.client.internal.HudsonClientModule;

public class HudsonRestFactory {
	public static HudsonClient getClient() {
		Injector injector = Guice.createInjector(new WireModule(new HudsonClientModule()));
		return (HudsonClient) injector.getInstance(HudsonClient.class);
	}
}
