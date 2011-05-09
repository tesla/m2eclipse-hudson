/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.Messages;

public class HudsonPageParticipant
    implements IConsolePageParticipant
{
    public IAction getAllAction;

    public HudsonConsole console;

    private Action removeAction;

    private Action removeAllAction;

	private Action refreshAction;

    public void init( IPageBookViewPage page, IConsole console )
    {
        this.console = (HudsonConsole) console;
        IActionBars actionBars = page.getSite().getActionBars();
        configureToolBar( actionBars.getToolBarManager(), console );
        
        //Retrieve some initial content
        new GetConsoleContent( this.console ).getPartial();
    }

    private void configureToolBar( IToolBarManager mgr, final IConsole page )
    {
        getAllAction = new GetConsoleContent( console );
        getAllAction.setToolTipText( Messages.consoleView_getCompleteContent );
        getAllAction.setImageDescriptor( HudsonImages.getImageDescriptor( HudsonImages.SHOW_FULL_CONSOLE ) );
        
        refreshAction = new Action( "Refresh")
        {
            @Override
            public void run()
            {
            	console.checkForUpdates();
            }
        };
        refreshAction.setToolTipText("Check for updates in remote Hudson build output");
        refreshAction.setImageDescriptor(HudsonImages.getImageDescriptor(HudsonImages.REFRESH));
        refreshAction.setEnabled(console.isRunning());
        
        removeAction = new Action( "Remove" )
        {
            @Override
            public void run()
            {
                IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
                consoleManager.removeConsoles( new IConsole[] { console } );

            }
        };
        removeAction.setToolTipText( "Remove remote Hudson build output" );
        removeAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor( ISharedImages.IMG_ELCL_REMOVE ) );
        removeAllAction = new Action( "Remove All" )
        {
            @Override
            public void run()
            {
                IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
                List<IConsole> consoles = new ArrayList<IConsole>();
                for ( IConsole console : consoleManager.getConsoles() )
                {
                    if ( HudsonConsole.HUDSON_TYPE.equals( console.getType() ))
                    {
                        consoles.add( console );
                    }
                }
                if ( !consoles.isEmpty() )
                {
                    consoleManager.removeConsoles( consoles.toArray( new IConsole[0] ) );
                }
            }
        };
        removeAllAction.setToolTipText( "Remove all remote Hudson build outputs" );
        removeAllAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor( ISharedImages.IMG_ELCL_REMOVEALL ) );
        
        mgr.appendToGroup( IConsoleConstants.OUTPUT_GROUP, getAllAction );
        mgr.appendToGroup( IConsoleConstants.OUTPUT_GROUP, refreshAction );
        mgr.appendToGroup(IConsoleConstants.LAUNCH_GROUP,  removeAction );
        mgr.appendToGroup(IConsoleConstants.LAUNCH_GROUP, removeAllAction );
    }

    public void dispose()
    {
        // do not dispose the consoles here, backfires when a new console view is opened.. 
//        console.dispose();
        if (getAllAction != null) getAllAction = null;
        if (removeAllAction != null) removeAllAction = null;
        if (removeAction != null) removeAction = null;
    }

    public void activated()
    {
    }

    public void deactivated()
    {
    }

    public Object getAdapter( Class adapter )
    {
        return null;
    }
}
