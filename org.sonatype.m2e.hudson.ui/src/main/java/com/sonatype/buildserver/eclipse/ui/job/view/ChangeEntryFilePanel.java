/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.job.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.matrix.rest.model.build.ChangeFileDTO;

public class ChangeEntryFilePanel
    extends Composite
{
    private final FormToolkit toolkit = new FormToolkit( Display.getCurrent() );
    private ChangeFileDTO entry;
    private HudsonJob job;
    private Label lblDoubleclickToOpen;

    /**
     * Create the composite.
     */
    public ChangeEntryFilePanel( Composite parent, ChangeFileDTO entry, HudsonJob job )
    {
        super( parent, SWT.NONE );

        initComponents();
        
        load(entry, job);
    }

    private void load( ChangeFileDTO entry, HudsonJob job )
    {
        this.entry = entry;
        this.job = job;
    }

    private void initComponents() {
        addDisposeListener( new DisposeListener()
        {
            public void widgetDisposed( DisposeEvent e )
            {
                toolkit.dispose();
            }
        } );
        toolkit.adapt( this );
        toolkit.paintBordersFor( this );
        
        lblDoubleclickToOpen = new Label(this, SWT.NONE);
        toolkit.adapt(lblDoubleclickToOpen, true, true);
        lblDoubleclickToOpen.setText(Messages.ChangeEntryFilePanel_open_file);
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(GroupLayout.LEADING)
                .add(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(lblDoubleclickToOpen)
                    .addContainerGap(379, Short.MAX_VALUE))
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(GroupLayout.LEADING)
                .add(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(lblDoubleclickToOpen)
                    .addContainerGap(274, Short.MAX_VALUE))
        );
        setLayout(groupLayout);
    }
}
