/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.job.view;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.matrix.rest.model.build.BuildDTO;

public class ChangeBuildPanel
    extends Composite
{

    private final FormToolkit toolkit = new FormToolkit( Display.getCurrent() );
    private Label lblMessage;
    private BuildDTO build;
    private HudsonJob job;
    private Link link;

    /**
     * Create the composite.
     * @param parent
     * @param style
     */
    public ChangeBuildPanel( Composite parent, BuildDTO build, HudsonJob job )
    {
        super( parent, SWT.NONE );

        initComponents();
        
        load(build, job);
        
        link.addSelectionListener( new SelectionAdapter()
        {

            @Override
            public void widgetSelected( SelectionEvent e )
            {
                String url = ChangeBuildPanel.this.job.getJobDetails().getUrl();
                if (! url.endsWith( "/" ) ) { //$NON-NLS-1$
                    url = url + "/"; //$NON-NLS-1$
                }
                url = url + ChangeBuildPanel.this.build.getNumber() + "/changes";  //$NON-NLS-1$
                HudsonUtils.openUrl( url );
            }
            
        });
    }
    
    
    

    private void load( BuildDTO build, HudsonJob job )
    {
        this.build = build;
        this.job = job;
        link.setText( NLS.bind( Messages.ChangeBuildPanel_view_detail_browser, build.getNumber() ));
        String p = Messages.ChangeBuildPanel_participants;
        for (String part : build.getParticipants()) {
            p = p + part + ", "; //$NON-NLS-1$
        }
        if (p.endsWith( ", " )) { //$NON-NLS-1$
            p = p.substring(0, p.length() - ", ".length()); //$NON-NLS-1$
        }
        lblMessage.setText( p );
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
        
        
        link = new Link(this, SWT.NONE);
        toolkit.adapt(link, true, true);
        link.setText("<a>New Link</a>"); //$NON-NLS-1$
        
        lblMessage = new Label(this, SWT.WRAP);
        toolkit.adapt(lblMessage, true, true);
        lblMessage.setText("New Label"); //$NON-NLS-1$
        
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(GroupLayout.LEADING)
                .add(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(groupLayout.createParallelGroup(GroupLayout.LEADING)
                        .add(link)
                        .add(GroupLayout.TRAILING, lblMessage, GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(GroupLayout.LEADING)
                .add(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(link)
                    .addPreferredGap(LayoutStyle.RELATED)
                    .add(lblMessage, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                    .addContainerGap())
        );
        setLayout(groupLayout);
    }
}
