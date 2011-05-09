/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.job.view;

import org.eclipse.ui.IEditorInput;

import com.sonatype.buildserver.monitor.HudsonJob;

interface IHudsonJobEditorInput extends IEditorInput
{

    /**
     * the current hudson job instance displayed by the editor
     * @return
     */
    HudsonJob getJob();
    
    /**
     * monitor associated with the job.
     * @return
     */
//    HudsonMonitor getMonitor();
    
}
