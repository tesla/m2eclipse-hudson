/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.monitor;

import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.ChangesDTO;

public class HudsonJobChangesEvent
    extends AbstractHudsonJobEvent
{

    private ChangesDTO changes;
    private BuildDTO build;

    public HudsonJobChangesEvent( HudsonJob modified, ChangesDTO changes, BuildDTO build)
    {
        super( modified );
        this.changes = changes;
        this.build = build;
    }
    
    public ChangesDTO getChanges() {
        return changes;
    }
    
    public BuildDTO getBuild() {
        return build;
    }

}
