/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.console;

import org.eclipse.jface.action.Action;

public class GetConsoleContent
    extends Action
{
    HudsonConsole console;

    public GetConsoleContent( HudsonConsole console )
    {
        this.console = console;
    }

    @Override
    public void run()
    {
        super.run();
        console.getContent(true);
    }
    
    public void getPartial()
    {
        super.run();
        console.getContent( false );
    }
}
