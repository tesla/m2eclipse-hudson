/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.matrixclient;

import org.sonatype.guice.bean.binders.WireModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sonatype.matrix.rest.client.MatrixClient;
import com.sonatype.matrix.rest.client.internal.MatrixClientModule;

public class MatrixFactory {
	public static MatrixClient getClient() {
		Injector injector = Guice.createInjector(new WireModule(new MatrixClientModule()));
		return (MatrixClient) injector.getInstance(MatrixClient.class);
	}
}
