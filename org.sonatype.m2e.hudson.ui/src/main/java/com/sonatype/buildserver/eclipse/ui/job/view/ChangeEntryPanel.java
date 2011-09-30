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
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.HudsonJob;
import org.hudsonci.rest.model.build.ChangeEntryDTO;

public class ChangeEntryPanel
    extends Composite
{

    private final FormToolkit toolkit = new FormToolkit( Display.getCurrent() );
    private Label lblAuthor;
    private Hyperlink hyperlink;
    private Label lblMessage;
    private ChangeEntryDTO entry;
    private HudsonJob job;

    /**
     * Create the composite.
     * @param parent
     * @param style
     */
    public ChangeEntryPanel( Composite parent, ChangeEntryDTO entry, HudsonJob job )
    {
        super( parent, SWT.NONE );

        initComponents();
        
        load(entry, job);
    }
    
    
    

    private void load( ChangeEntryDTO entry, HudsonJob job )
    {
        this.entry = entry;
        this.job = job;
        lblMessage.setText( HudsonUtils.stripHtml( entry.getMessage() ) );
        hyperlink.setText( entry.getAuthor().getFullName() );
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
        
        lblAuthor = new Label(this, SWT.NONE);
        toolkit.adapt(lblAuthor, true, true);
        lblAuthor.setText(Messages.ChangeEntryPanel_author);
        
        hyperlink = toolkit.createHyperlink(this, "New Hyperlink", SWT.NONE); //$NON-NLS-1$
        hyperlink.addHyperlinkListener(new IHyperlinkListener() {
            public void linkActivated(HyperlinkEvent e) {
                if (entry != null) {
                    String url = job.getServerName();
                    if (!url.endsWith( "/" )) { //$NON-NLS-1$
                        url = url + "/"; //$NON-NLS-1$
                    }
                    url = url + "user/"; //$NON-NLS-1$
                    url = url + entry.getAuthor().getId();
                    System.out.println("url=" + url);
                    HudsonUtils.openUrl( url );
                }
            }
            public void linkEntered(HyperlinkEvent e) {
            }
            public void linkExited(HyperlinkEvent e) {
            }
        });
        toolkit.paintBordersFor(hyperlink);
        
        lblMessage = new Label(this, SWT.WRAP);
        toolkit.adapt(lblMessage, true, true);
        lblMessage.setText("New Label"); //$NON-NLS-1$
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(GroupLayout.LEADING)
                .add(groupLayout.createSequentialGroup()
                    .add(groupLayout.createParallelGroup(GroupLayout.LEADING)
                        .add(groupLayout.createSequentialGroup()
                            .addContainerGap()
                            .add(lblAuthor)
                            .addPreferredGap(LayoutStyle.RELATED)
                            .add(hyperlink, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .add(groupLayout.createSequentialGroup()
                            .add(18)
                            .add(lblMessage, GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(GroupLayout.LEADING)
                .add(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
                        .add(lblAuthor)
                        .add(hyperlink, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.RELATED)
                    .add(lblMessage, GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                    .addContainerGap())
        );
        setLayout(groupLayout);
    }
}
