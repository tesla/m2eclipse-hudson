/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.view;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.spi.BuildDetails;
import com.sonatype.buildserver.spi.BuildNotification;

public class BumpAndFlashBuildNotification implements BuildNotification {
    public void notifyFailure(BuildDetails res) {
        notifyStatus();
    }

    public void notifySuccess(BuildDetails res) {
        notifyStatus();
    }
    
    private void notifyStatus() {
        // paranoid backup parachute.. just in case apple messes with
        // the stuff in a way that would render our plugin
        // unusable..
        if ( !Boolean.getBoolean( "hudson.skip.app.fronting" ) ) //$NON-NLS-1$
        {
            Display.getDefault().asyncExec( new Runnable()
            {
                public void run()
                {

                    if ( Platform.WS_WIN32.equals( SWT.getPlatform() )
                        && ( "Windows 7".equals( System.getProperty( "os.name" ) ) || "Windows XP".equals( System.getProperty( "os.name" ) ) ) ) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                    {
                        // only do this when the content of the status bar actually changes..
                        // and on platforms that don't actually front the application but flash the taskbar
                    	IWorkbenchWindow window = HudsonUIActivator.getDefault().getWorkbench().getActiveWorkbenchWindow();
                    	if (window != null) {
                    		window.getShell().forceActive();
                    	}
//3.6                        HudsonUIActivator.getDefault().getWorkbench().getModalDialogShellProvider().getShell().forceActive();

                        // linux distros behaving dramatically different so not including them
                    }
                    else if ( Boolean.getBoolean( "hudson.force.app.fronting" ) ) //$NON-NLS-1$
                    {
                    	IWorkbenchWindow window = HudsonUIActivator.getDefault().getWorkbench().getActiveWorkbenchWindow();
                    	if (window != null) {
                    		window.getShell().forceActive();
                    	}
//3.6                        HudsonUIActivator.getDefault().getWorkbench().getModalDialogShellProvider().getShell().forceActive();
                    }

                }

            } );
        }
        
    }
}
