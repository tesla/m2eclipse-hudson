/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.view;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.eclipse.ui.prefs.PreferenceConstants;
import com.sonatype.buildserver.spi.BuildDetails;
import com.sonatype.buildserver.spi.BuildNotification;

public class MessageBoxBuildNotification implements BuildNotification, IExecutableExtensionFactory {

    private static MessageBoxBuildNotification instance;

    //prevent multiple message boxes to popup, one is enough..
    //too bad one cannot adjust the message for a currently showing box.
    private boolean oneOpened = false;
    
    public MessageBoxBuildNotification() {
        
    }
    
    
	public void notifyFailure(BuildDetails res) {
		notify("Hudson build failed", "A Hudson build failure has occured.");
	}

	public void notifySuccess(BuildDetails res) {
		notify("Hudson build succeeded", "A failing Hudson build got fixed.");
	}
	
	private void notify(final String title, final String message) {
	    synchronized (this) {
	        if (oneOpened) return;
	    }
		Display.getDefault().asyncExec(new Runnable() {
			
			public void run() {
				if (HudsonUIActivator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREF_SHOW_MESSAGE))
				{
					boolean show = Display.getDefault().getActiveShell() == null && !(HudsonUIActivator.getDefault().getWorkbench().isStarting() || HudsonUIActivator.getDefault().getWorkbench().isClosing());
					if (show) {
					    synchronized (MessageBoxBuildNotification.this) {
					        if (oneOpened) return;
					        oneOpened = true;
					    }
					  //3.6                        HudsonUIActivator.getDefault().getWorkbench().getModalDialogShellProvider().getShell()
						MessageBox box = new MessageBox(HudsonUIActivator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(),
								SWT.ICON_INFORMATION | SWT.OK | SWT.APPLICATION_MODAL);
						box.setText(title);
						box.setMessage(message);
                        box.open();
						synchronized (MessageBoxBuildNotification.this) {
						    oneOpened = false;
						}
					}
				}
			}
		});
		
	}
	
	//make 100% sure we always have just once instance around, to be able to keep state across calls.  
    public Object create()
    throws CoreException
{
    synchronized (MessageBoxBuildNotification.class) {
        if (instance == null) {
            instance = new MessageBoxBuildNotification();
        }
        return instance;
    }
}	

}
