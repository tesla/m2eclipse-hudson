/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.job.view;

import java.beans.Beans;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.AbstractHudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.HudsonJobBuildsEvent;
import com.sonatype.buildserver.monitor.HudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJobListener;
import com.sonatype.buildserver.monitor.ErrorJob;
import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.BuildResultDTO;
import com.sonatype.matrix.rest.model.build.BuildStateDTO;
import com.sonatype.matrix.rest.model.build.CauseDTO;
import com.sonatype.matrix.rest.model.project.ProjectDTO;

public class SummaryPage
    extends FormPage
{

    private static final String LABEL = "label=";

    private HudsonJob job;

    private Link lnkJobName;

    private Link lnkServerLocation;

    private Text txtDescription;

    private Link lnkBuild;

    private Table table;

    private TableViewer tableViewer;

    private Label lblStartedOn;

    private Label lblDuration;

    private Label lblBuildStatus;

    private boolean initialized = false;

    private ScrolledForm form;

    private Label lblTrigger;

    private Label lblEnable;

    private Label lblType;

    private Label lblContributions;

    private SingleJobView parent;

    private Section sctnJobProperties;

    private Composite compJob;

    private Section sctnBuildProperties;

    private Composite compBuild;

    private Label lblBlocked;

    private ImageHyperlink mghprlnkTestResults;

    private ImageHyperlink mghprlnkConsoleOutput;

    private Composite composite_1;

    private Link linkTempWorkspace;
    private Section sctnConfigurations;
    private Composite compConfigs;

    private FormToolkit toolkit;
    private ImageHyperlink mghprlnkParent;
    private Composite composite;
    private Label lblKeptForever;

    /**
     * Create the composite.
     */
    public SummaryPage( FormEditor parent )
    {
        super( parent, getPageId( "summary" ), Messages.SummaryPage_title ); //$NON-NLS-1$
        if ( !Beans.isDesignTime() )
        {
            this.parent = (SingleJobView) parent;
        }
    }

    public static String getPageId( String suffix )
    {
        return HudsonUIActivator.PLUGIN_ID + ".codebaseeditor." + suffix; //$NON-NLS-1$
    }

    /**
     * Implements the required method by refreshing the form when set active. Subclasses must call super when overriding
     * this method.
     */
    public void setActive( boolean active )
    {
        super.setActive( active );
        if ( active )
        {
            if ( job != null )
            {
                setJob( job );
            }
        }
    }

    @Override
    protected void createFormContent( IManagedForm managedForm )
    {
        toolkit = managedForm.getToolkit();
        form = managedForm.getForm();

        toolkit.decorateFormHeading( form.getForm() );

        Composite body = form.getBody();

        tableViewer = new TableViewer( body, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER );
        table = tableViewer.getTable();
        table.setHeaderVisible( false );
        tableViewer.setLabelProvider( new BuildLabelProvider() );
        tableViewer.setContentProvider( new BuildContentProvider() );

        toolkit.paintBordersFor( table );

        sctnJobProperties = managedForm.getToolkit().createSection( managedForm.getForm().getBody(), Section.TITLE_BAR );
        managedForm.getToolkit().paintBordersFor( sctnJobProperties );
        sctnJobProperties.setText( Messages.SummaryPage_section_job );

        sctnBuildProperties =
            managedForm.getToolkit().createSection( managedForm.getForm().getBody(), Section.TITLE_BAR );
        managedForm.getToolkit().paintBordersFor( sctnBuildProperties );
        sctnBuildProperties.setText( Messages.SummaryPage_section_build );

        GroupLayout layout = new GroupLayout( body );
        layout.setHorizontalGroup( layout.createParallelGroup( GroupLayout.LEADING ).add( layout.createSequentialGroup().addContainerGap().add( layout.createParallelGroup( GroupLayout.TRAILING ).add( sctnJobProperties,
                                                                                                                                                                                                        GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                        508,
                                                                                                                                                                                                        Short.MAX_VALUE ).add( layout.createSequentialGroup().add( sctnBuildProperties,
                                                                                                                                                                                                                                                                   GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                   377,
                                                                                                                                                                                                                                                                   Short.MAX_VALUE ).addPreferredGap( LayoutStyle.RELATED ).add( table,
                                                                                                                                                                                                                                                                                                                                 GroupLayout.PREFERRED_SIZE,
                                                                                                                                                                                                                                                                                                                                 125,
                                                                                                                                                                                                                                                                                                                                 GroupLayout.PREFERRED_SIZE ) ) ).addContainerGap() ) );
        layout.setVerticalGroup( layout.createParallelGroup( GroupLayout.LEADING ).add( layout.createSequentialGroup().addContainerGap().add( sctnJobProperties,
                                                                                                                                              GroupLayout.DEFAULT_SIZE,
                                                                                                                                              163,
                                                                                                                                              Short.MAX_VALUE ).addPreferredGap( LayoutStyle.UNRELATED ).add( layout.createParallelGroup( GroupLayout.LEADING ).add( table,
                                                                                                                                                                                                                                                                     GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                     193,
                                                                                                                                                                                                                                                                     Short.MAX_VALUE ).add( sctnBuildProperties,
                                                                                                                                                                                                                                                                                            GroupLayout.DEFAULT_SIZE,
                                                                                                                                                                                                                                                                                            193,
                                                                                                                                                                                                                                                                                            Short.MAX_VALUE ) ).addContainerGap() ) );

        compBuild = managedForm.getToolkit().createComposite( sctnBuildProperties, SWT.NONE );
        managedForm.getToolkit().paintBordersFor( compBuild );
        sctnBuildProperties.setClient( compBuild );
        compBuild.setLayout( new GridLayout( 4, false ) );

        lblBuildStatus = new Label( compBuild, SWT.NONE );
        toolkit.adapt( lblBuildStatus, true, true );

        lnkBuild = new Link( compBuild, SWT.NONE );
        lnkBuild.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 2, 1 ) );
        toolkit.adapt( lnkBuild, true, true );

        composite_1 = new Composite( compBuild, SWT.NONE );
        composite_1.setLayoutData( new GridData( SWT.RIGHT, SWT.FILL, false, false, 1, 4 ) );
        managedForm.getToolkit().adapt( composite_1 );
        managedForm.getToolkit().paintBordersFor( composite_1 );
        composite_1.setLayout( new GridLayout( 1, false ) );

        mghprlnkTestResults = managedForm.getToolkit().createImageHyperlink( composite_1, SWT.NONE );
        managedForm.getToolkit().paintBordersFor( mghprlnkTestResults );

        mghprlnkTestResults.setText( Messages.SummaryPage_test_results );
        mghprlnkTestResults.setImage( HudsonImages.JUNIT_IMAGE );
        mghprlnkTestResults.setToolTipText( Messages.SummaryPage_test_results_tooltip );

        mghprlnkConsoleOutput = managedForm.getToolkit().createImageHyperlink( composite_1, SWT.NONE );
        managedForm.getToolkit().paintBordersFor( mghprlnkConsoleOutput );
        mghprlnkConsoleOutput.setText( Messages.SummaryPage_console );
        mghprlnkConsoleOutput.setImage( HudsonImages.getImage( HudsonImages.CONSOLE ) );
        mghprlnkConsoleOutput.setToolTipText( Messages.SummaryPage_console_tooktip );

        new Label( compBuild, SWT.NONE );
        new Label( compBuild, SWT.NONE );

        lblTrigger = new Label( compBuild, SWT.NONE );
        lblTrigger.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 1, 1 ) );
        managedForm.getToolkit().adapt( lblTrigger, true, true );
        lblTrigger.setText( "trigger" ); //$NON-NLS-1$

        new Label( compBuild, SWT.NONE );
        new Label( compBuild, SWT.NONE );

        lblStartedOn = new Label( compBuild, SWT.NONE );
        lblStartedOn.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 1, 1 ) );
        toolkit.adapt( lblStartedOn, true, true );
        lblStartedOn.setText( "started on ${0}" ); //$NON-NLS-1$
        new Label( compBuild, SWT.NONE );
        new Label( compBuild, SWT.NONE );

        lblDuration = new Label( compBuild, SWT.NONE );
        lblDuration.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 1, 1 ) );
        toolkit.adapt( lblDuration, true, true );
        lblDuration.setText( "took ${0} to finish" ); //$NON-NLS-1$
        new Label(compBuild, SWT.NONE);
        new Label(compBuild, SWT.NONE);
        
        lblKeptForever = new Label(compBuild, SWT.NONE);
        lblKeptForever.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        managedForm.getToolkit().adapt(lblKeptForever, true, true);
        lblKeptForever.setText("is kept around forever");
        new Label(compBuild, SWT.NONE);
        new Label( compBuild, SWT.NONE );
        new Label( compBuild, SWT.NONE );
        lblContributions = new Label( compBuild, SWT.WRAP );
        lblContributions.setLayoutData( new GridData( SWT.LEFT, SWT.TOP, true, true, 2, 1 ) );
        toolkit.adapt( lblContributions, true, true );
        lblContributions.setText( "contributions" ); //$NON-NLS-1$

        compJob = managedForm.getToolkit().createComposite( sctnJobProperties, SWT.NONE );
        managedForm.getToolkit().paintBordersFor( compJob );
        sctnJobProperties.setClient( compJob );
        compJob.setLayout( new GridLayout( 3, false ) );
        
                lnkJobName = new Link( compJob, SWT.NONE );
                toolkit.adapt( lnkJobName, true, true );
                lnkJobName.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 1, 1 ) );
                
        
                linkTempWorkspace = new Link( compJob, SWT.NONE );
                linkTempWorkspace.setToolTipText( Messages.SummaryPage_workspace_tooltip );
                linkTempWorkspace.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 2, 1 ) );
                managedForm.getToolkit().adapt( linkTempWorkspace, true, true );
                linkTempWorkspace.setText( Messages.SummaryPage_workspace );
        
        composite = new Composite(compJob, SWT.NONE);
        GridLayout gl_composite = new GridLayout(2, false);
        gl_composite.marginWidth = 0;
        gl_composite.marginHeight = 0;
        composite.setLayout(gl_composite);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        managedForm.getToolkit().adapt(composite);
        managedForm.getToolkit().paintBordersFor(composite);
        
        lnkServerLocation = new Link( composite, SWT.NONE );
        toolkit.adapt( lnkServerLocation, true, true );
        GridData gd_lnkServerLocation = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gd_lnkServerLocation.horizontalIndent = 12;
        lnkServerLocation.setLayoutData(gd_lnkServerLocation);

                
                mghprlnkParent = managedForm.getToolkit().createImageHyperlink(composite, SWT.NONE);
                mghprlnkParent.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
                managedForm.getToolkit().paintBordersFor(mghprlnkParent);
                mghprlnkParent.setText("Open Parent Job");
                mghprlnkParent.setImage( HudsonImages.getImage( HudsonImages.MATRIX ) );
        
        sctnConfigurations = managedForm.getToolkit().createSection(compJob, Section.TITLE_BAR);
        sctnConfigurations.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 1, 5));
        managedForm.getToolkit().paintBordersFor(sctnConfigurations);
        sctnConfigurations.setText("Configurations");
        
        compConfigs = managedForm.getToolkit().createComposite(sctnConfigurations, SWT.NONE);
        managedForm.getToolkit().paintBordersFor(compConfigs);
        sctnConfigurations.setClient(compConfigs);
        compConfigs.setLayout(new GridLayout(1, false));
        
                lblType = new Label( compJob, SWT.NONE );
                GridData gd_lblType = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
                gd_lblType.horizontalIndent = 12;
                lblType.setLayoutData(gd_lblType);
                managedForm.getToolkit().adapt( lblType, true, true );
                lblType.setText( "type" );
        
                lblEnable = new Label( compJob, SWT.NONE );
                GridData gd_lblEnable = new GridData( SWT.LEFT, SWT.CENTER, false, false, 2, 1 );
                gd_lblEnable.horizontalIndent = 12;
                lblEnable.setLayoutData( gd_lblEnable );
                managedForm.getToolkit().adapt( lblEnable, true, true );
                lblEnable.setText( "disabled state" );
        
                lblBlocked = new Label( compJob, SWT.NONE );
                GridData gd_lblBlocked = new GridData( SWT.LEFT, SWT.CENTER, false, false, 2, 1 );
                gd_lblBlocked.horizontalIndent = 12;
                lblBlocked.setLayoutData( gd_lblBlocked );
                managedForm.getToolkit().adapt( lblBlocked, true, true );
                lblBlocked.setText( "blocked" );
        
                txtDescription = new Text( compJob, SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI );
                txtDescription.setLayoutData( createTxtDescriptionData( 1 ) );
                toolkit.adapt( txtDescription, true, true );
        body.setLayout( layout );

        parent.constructToolbar( form );

        if ( Beans.isDesignTime() )
        {
            // this is scary.. if the link texts get set in 3.5.2, the link gets wrong visuals.
            // it seems that keeping those here solves the problem.
            lnkBuild.setText( "<a>build</a>" ); //$NON-NLS-1$
            lblBuildStatus.setText( "status" );
            lnkJobName.setText( "<a>job</a>" );
            lnkServerLocation.setText( "<a>server</a>" );
            
        }

        initialize();
    }
    
    private GridData createTxtDescriptionData(int span) {
        GridData gd_txtDescription = new GridData( SWT.FILL, SWT.FILL, true, true, 2, 1 );
        gd_txtDescription.widthHint = 100;
        gd_txtDescription.horizontalIndent = 12;
        return gd_txtDescription;
    }

    private void initialize()
    {
        lnkJobName.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                HudsonUtils.openUrl( job.getJobDetails().getUrl() );
            }
        } );
        lnkServerLocation.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                HudsonUtils.openUrl( job.getServerName() );
            }
        } );
        lnkBuild.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                IStructuredSelection sel = (IStructuredSelection) tableViewer.getSelection();
                if ( sel.isEmpty() )
                {
                    HudsonUtils.openUrl( job.getJobDetails().getLastBuild().getUrl() );
                }
                else
                {
                    Object selected = sel.getFirstElement();
                    if ( selected != null && selected instanceof BuildDTO )
                    {
                        HudsonUtils.openUrl( ((BuildDTO)selected).getUrl() );
                    }
                }
            }
        } );
        tableViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                IStructuredSelection sel = (IStructuredSelection) tableViewer.getSelection();
                if ( sel.isEmpty() )
                {
                    setBuild( job.getJobDetails().getLastBuild() );
//                    parent.setPartName( HudsonUtils.getJobTitle( job, null ) );
                }
                else
                {
                    Object selected = sel.getFirstElement();
                    if ( selected != null && selected instanceof BuildDTO )
                    {
                        setBuild( (BuildDTO) selected );
// not setting the build number in the view tab. users shall not assume actions are related to build
//                        parent.setPartName( HudsonUtils.getJobTitle( job, (Build) selected ) );
                    }
                }
            }
        } );
        mghprlnkTestResults.addHyperlinkListener( new HyperlinkAdapter()
        {
            public void linkActivated( HyperlinkEvent e )
            {
                BuildDTO build;
                if ( tableViewer.getSelection().isEmpty() )
                {
                    build = job.getJobDetails().getLastBuild();
                }
                else
                {
                    build = (BuildDTO) ( (StructuredSelection) tableViewer.getSelection() ).getFirstElement();
                }
                HudsonUtils.openTests( job, build );
            }

        } );

        mghprlnkConsoleOutput.addHyperlinkListener( new HyperlinkAdapter()
        {
            public void linkActivated( HyperlinkEvent e )
            {
                BuildDTO build;
                if ( tableViewer.getSelection().isEmpty() )
                {
                    build = job.getJobDetails().getLastBuild();
                }
                else
                {
                    build = (BuildDTO) ( (StructuredSelection) tableViewer.getSelection() ).getFirstElement();
                }

                HudsonUtils.openConsole( job, build );
            }
        } );
        mghprlnkParent.addHyperlinkListener( new HyperlinkAdapter()
        {
            public void linkActivated( HyperlinkEvent e )
            {
                openParentJob();
            }
        } );
        
        linkTempWorkspace.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                String url = job.getJobDetails().getUrl();
                if ( !url.endsWith( "/" ) ) //$NON-NLS-1$
                {
                    url = url + "/"; //$NON-NLS-1$
                }
                url = url + "ws"; //$NON-NLS-1$
                HudsonUtils.openUrl( url );
            }
        } );

        initialized = true;
    }


    public void setJob( HudsonJob job )
    {
        this.job = job;
        if ( !initialized )
        {
            return;
        }
        form.setText( job.getJobName() );
        form.getForm().setMessage( null, IMessageProvider.NONE);
        form.setImage( HudsonImages.getHealthImage( job ) );
        

        if ( tableViewer.getInput() == null )
        {
            tableViewer.setInput( job );
        }

        lnkJobName.setText( NLS.bind(Messages.SummaryPage_job_link, job.getJobName()));

        lnkServerLocation.setText( NLS.bind( Messages.SummaryPage_build_on, job.getServerName()));
        lnkServerLocation.setToolTipText( NLS.bind( Messages.SummaryPage_build_on_tooltip, job.getServerName()));
        ProjectDTO details = job.getJobDetails();
        if ( details != null && !( details instanceof ErrorJob ) )
        {
            if (details.isQueued()) {
                form.setText( job.getJobName()  + " [New build queued]");
            }
            
            markAsVisible( lblEnable, true );
            lblEnable.setText( details.isEnabled() ? Messages.SummaryPage_enabled : Messages.SummaryPage_disabled );
            if ( details.isBlocked() )
            {
                lblBlocked.setText( Messages.SummaryPage_blocked + details.getBlockedReason() + "\" " ); //$NON-NLS-2$
                markAsVisible( lblBlocked, true );
            }
            else
            {
                markAsVisible( lblBlocked, false );
            }
            markAsVisible( lblType, true );
            lblType.setText( getTypeStrings( details ) );
            form.setToolTipText( details.getHealth().getDescription() );
            lnkJobName.setToolTipText( NLS.bind( Messages.SummaryPage_job_tooltip, details.getUrl()));
            String desc = details.getDescription();
            if ( desc == null || desc.trim().length() == 0 )
            {
                desc = Messages.SummaryPage_no_description;
            }
            markAsVisible( txtDescription, true );
            txtDescription.setText( HudsonUtils.stripHtml( desc ) );
            // only force last build if no build is selected in table,
            // useful on refreshes/updates when user has some context selected
            if ( tableViewer.getSelection().isEmpty() )
            {
                setBuild( details.getLastBuild() );
            }
            if (HudsonUtils.isMatrixType( details )) {
                markAsVisible( sctnConfigurations, true );
                loadConfigurations();
                composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
                txtDescription.setLayoutData( createTxtDescriptionData( 2 ) );
            } else {
                markAsVisible( sctnConfigurations, false );
                composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
                txtDescription.setLayoutData( createTxtDescriptionData( 3 ) );
            }
            if (HudsonUtils.isMatrixConfig( details )) {
                markAsVisible( mghprlnkParent, true );
            } else {
                markAsVisible( mghprlnkParent, false );
            }

            // System.out.println("type=" + details.getType());
            // System.out.println("job name=" + details.getLastBuild().getJobName());
            // System.out.println("id=" + details.getLastBuild().getId());
            // System.out.println("url=" + details.getLastBuild().getUrl());
            //
            //
        } else
        {
            if (details instanceof ErrorJob)
            {
                ErrorJob ej = (ErrorJob)details;
                form.getForm().setMessage( ej.getErrorMessage(), IMessageProvider.ERROR);
            }
            markAsVisible( lblType, false );
            markAsVisible( lblBlocked, false );
            markAsVisible( lblEnable, false );
            markAsVisible( txtDescription, false );
            markAsVisible( sctnConfigurations, false);
            markAsVisible( mghprlnkParent, false );
            setBuild( null );
        } 
        compJob.layout();
    }

    private void loadConfigurations()
    {
        for (Control c : compConfigs.getChildren()) {
            c.dispose();
        }
        Label lblLoading = new Label( compConfigs, SWT.NONE );
//        lblLoading.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 1, 1 ) );
        toolkit.adapt( lblLoading, true, true );
        lblLoading.setText( "Loading..." );
        compConfigs.layout();
        compJob.layout();
        org.eclipse.core.runtime.jobs.Job jb = new org.eclipse.core.runtime.jobs.Job("Load Hudson Job Configurations")
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                final List<HudsonJob> configs = job.loadDescendants();
                Display.getDefault().asyncExec( new Runnable()
                {
                    public void run()
                    {
                        for (Control c : compConfigs.getChildren()) {
                            c.dispose();
                        }
                        for (final HudsonJob bj : configs) {
                            final ImageHyperlink link = toolkit.createImageHyperlink( compConfigs, SWT.NONE );
                            toolkit.paintBordersFor( link );

                            link.setText( stripConfigName(bj.getJobName()) );
                            link.setImage( HudsonImages.getResultImage( bj ));
                            link.setToolTipText( bj.getJobName() );
                            link.addHyperlinkListener( new HyperlinkAdapter() {
                                @Override
                                public void linkActivated( HyperlinkEvent e )
                                {
                                    HudsonUtils.openJob( bj );
                                }
                                
                            });
                            final HudsonJobListener listener = new HudsonJobListener()
                            {
                                public void getModified( AbstractHudsonJobEvent event )
                                {
                                    if (event instanceof HudsonJobEvent) {
                                        Display.getDefault().asyncExec( new Runnable()
                                        {
                                            public void run()
                                            {
                                                link.setImage( HudsonImages.getResultImage( bj ));
                                            }
                                        });
                                    }
                                }

                                public boolean isUIUp()
                                {
                                    return true;
                                }
                            };
                            bj.addListener( listener );
                            link.addDisposeListener( new DisposeListener()
                            {
                                public void widgetDisposed( DisposeEvent e )
                                {
                                    bj.removeListener( listener );
                                }
                            });

                        }
                        compConfigs.layout();
                        compJob.layout();
                    }
                });
                return Status.OK_STATUS;
            }
        };
        jb.schedule();
    }
    
    protected void openParentJob()
    {
        org.eclipse.core.runtime.jobs.Job jb = new org.eclipse.core.runtime.jobs.Job("Load Hudson Job Parent")
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                final HudsonJob parent = job.loadParent();
                if (parent != null) {
                    Display.getDefault().asyncExec( new Runnable()
                    {
                        public void run()
                        {
                            HudsonUtils.openJob( parent );
                        }
                    });
                }
                return Status.OK_STATUS;
            }
        };
        jb.schedule();
    }
    
    
    private String stripConfigName(String name) {
        if (name.startsWith( job.getJobName() + "/" )) {
            name = name.substring( job.getJobName().length() + 1 );
        }
        if (name.contains( LABEL )) {
            name = name.substring( name.indexOf( LABEL ) + LABEL.length() );
            int comma = name.indexOf( "," );
            if (comma > -1) {
                name = name.substring( 0, comma  - 1 );
            }
        }
        if (name.length() > 20) {
            name = name.substring( 0, 17 ) + Dialog.ELLIPSIS;
        }
        return name;
    }

    private String getTypeStrings( ProjectDTO job )
    {
        String type = job.getType();
        if ( "hudson.model.FreeStyleProject".equals( type ) ) //$NON-NLS-1$
        {
            return Messages.SummaryPage_freestyle;
        }
        if ( "hudson.XXX".equals( type ) ) //$NON-NLS-1$
        { // TODO
            return Messages.SummaryPage_external;
        }
        if ( HudsonUtils.isMatrixType( job ) )
        {
            return Messages.SummaryPage_matrix;
        }
        if ( HudsonUtils.isMatrixConfig( job ) )
        {
            return Messages.SummaryPage_configuration;
        }
        return Messages.SummaryPage_unknown + type;
    }
    
    public void setBuild( BuildDTO build )
    {
        if ( build == null )
        {
            lnkBuild.setText( Messages.SummaryPage_no_builds_yet );
            lblBuildStatus.setImage( HudsonImages.NOT_BUILT_IMAGE );
            markAsVisible( lblStartedOn, false );
            markAsVisible( lblDuration, false );
            markAsVisible( lblTrigger, false );
            mghprlnkTestResults.setEnabled( false );
            mghprlnkConsoleOutput.setEnabled( false );
            markAsVisible( lblKeptForever, false);
            markAsVisible( lblContributions, false );
            compBuild.layout();
            return;
        }
        lnkBuild.setText( NLS.bind( Messages.SummaryPage_build_link, build.getNumber(), getBuildStatusString( build ) ));
        lnkBuild.setToolTipText( NLS.bind(Messages.SummaryPage_build_link_tooptip, build.getUrl()));
        lblBuildStatus.setImage( getBuildStatusImage( build ) );
        markAsVisible(lblStartedOn, true);
        lblStartedOn.setText( NLS.bind( Messages.SummaryPage_started, new Date( build.getTimeStamp() ).toString() ) );
        // TODO if running or queued, say so, not yet finished..
        if ( build.getDuration() == 0 )
        {
            markAsVisible( lblDuration, false );
        }
        else
        {
            markAsVisible( lblDuration, true );
            lblDuration.setText( NLS.bind( Messages.SummaryPage_duration, HudsonUtils.getFormattedDuration( build ) ) );
        }

        //only enable test link when tests available.
        mghprlnkTestResults.setEnabled( build.isTestsAvailable() ); //|| (job.getJobDetails() != null && isMatrixType( job.getJobDetails().getType() ) ) );
        
        mghprlnkConsoleOutput.setEnabled( true );

        if ( build.getCauses() != null && build.getCauses().size() > 0 )
        {
            for ( CauseDTO cause : build.getCauses() )
            {
                if ( cause.getDescription() != null && cause.getDescription().trim().length() > 0 )
                {
                    String c = cause.getDescription();
                    // will mess up on turkish locale, but well
                    // we don't have a clue anyway what locale have we been sent the message in anyway
                    c = c.substring( 0, 1 ).toLowerCase() + c.substring( 1 );
                    lblTrigger.setText( c );
                }
                else
                {
                    lblTrigger.setText( cause.getType() );
                }
            }
            markAsVisible( lblTrigger, true );
        }
        else
        {
            markAsVisible( lblTrigger, false );
        }
        if ( build.getCulprits() != null && build.getCulprits().size() > 0 )
        {
            String tit = ""; //$NON-NLS-1$
            for ( String part : build.getCulprits() )
            {
                tit = tit + ", " + part; //$NON-NLS-1$
            }
            lblContributions.setText( NLS.bind( Messages.SummaryPage_changes_by, tit.substring( ", ".length() ) )); //$NON-NLS-2$
            markAsVisible( lblContributions, true );
        }
        else
        {
            markAsVisible( lblContributions, false );
        }
        markAsVisible( lblKeptForever, build.isKept() );
        
        // compBuild.pack(true);
        compBuild.layout();
    }

    private void markAsVisible( Control label, boolean visible )
    {
        GridData data = (GridData) label.getLayoutData();
        data.exclude = !visible;
        label.setVisible( visible );
    }

    private String getBuildStatusString( BuildDTO bld )
    {
        BuildStateDTO state = bld.getState();
        BuildResultDTO result = bld.getResult();
        if ( state == BuildStateDTO.BUILDING )
        {
            return Messages.SummaryPage_running;
        }
        if ( state == BuildStateDTO.NOT_STARTED )
        {
            return Messages.SummaryPage_queued;
        }
        if ( state == null || state == BuildStateDTO.COMPLETED )
        {
            if ( result == BuildResultDTO.SUCCESS )
            {
                return Messages.SummaryPage_completed;
            }
            if ( result == BuildResultDTO.ABORTED )
            {
                return Messages.SummaryPage_aborted;
            }
            if ( result == BuildResultDTO.FAILURE )
            {
                return Messages.SummaryPage_failed;
            }
            if ( result == BuildResultDTO.UNSTABLE )
            {
                return Messages.SummaryPage_unstable;
            }
        }
        return Messages.SummaryPage_build_state_unknown;
    }

    static Image getBuildStatusImage( BuildDTO bld )
    {
        BuildStateDTO state = bld.getState();
        BuildResultDTO result = bld.getResult();
        if ( state == BuildStateDTO.BUILDING )
        {
            return HudsonImages.getImage( HudsonImages.RUNNING );
        }
        if ( state == BuildStateDTO.NOT_STARTED )
        {
            return HudsonImages.getImage( HudsonImages.NOT_BUILT );
        }
        // HUDSON-38
        if ( state == null || state == BuildStateDTO.COMPLETED )
        {
            String basePath = HudsonImages.getResultImagePath( result );
            if (bld.isKept()) {
                return HudsonImages.getOverlayImage( basePath, null, HudsonImages.LOCKED_BADGE );
            }
            return HudsonImages.getImage( basePath ); 
        }
        return HudsonImages.getImage( HudsonImages.NOT_BUILT );
    }

    private class BuildLabelProvider
        extends LabelProvider
        implements ITableLabelProvider
    {

        public Image getColumnImage( Object element, int columnIndex )
        {
            if ( element instanceof BuildDTO )
            {
                BuildDTO bld = (BuildDTO) element;
                return getBuildStatusImage( bld );
            }
            return null;
        }

        public String getColumnText( Object element, int columnIndex )
        {
            if ( element instanceof BuildDTO )
            {
                BuildDTO bld = (BuildDTO) element;
                return "#" + bld.getNumber();
            }
            return null;
        }

    }

    private class BuildContentProvider
        implements IStructuredContentProvider, HudsonJobListener
    {
        private boolean initialized = false;

        private BuildDTO[] current;

        public Object[] getElements( Object inputElement )
        {
            BuildDTO[] toRet = new BuildDTO[0];
            if ( !initialized )
            {
                initialized = true;
                job.addListener( this );
            }

            if ( inputElement instanceof HudsonJob )
            {
                HudsonJob job = (HudsonJob) inputElement;
                List<BuildDTO> builds = job.getBuilds();
                // either missing or dirty
                if ( !job.hasBuildsLoaded() )
                {
                    job.loadBuilds();
                }
                if ( builds == null )
                {
                    if ( job.getJobDetails() != null )
                    {
                        BuildDTO last = job.getJobDetails().getLastBuild();
                        if ( last != null )
                        {
                            toRet = new BuildDTO[] { last };
                        }
                    }
                }
                else
                {
                    toRet = builds.toArray( new BuildDTO[0] );
                }
            }
            current = toRet;

            return toRet;
        }

        public void dispose()
        {
        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        }

        public void getModified( final AbstractHudsonJobEvent event )
        {
            if ( event instanceof HudsonJobEvent )
            {
                if ( !event.getModifiedJob().hasBuildsLoaded() )
                {
                    job.loadBuilds();
                }
            }
            else if ( event instanceof HudsonJobBuildsEvent )
            {
                Display.getDefault().asyncExec( new Runnable()
                {
                    public void run()
                    {
                        // when hidden, no more refreshes..
                        if ( form != null && form.getBody() != null && form.getBody().isDisposed() )
                        {
                            return;
                        }

                        // only add/remove build instances that changed..
                        final List<BuildDTO> newList = ( (HudsonJobBuildsEvent) event ).getBuilds();
                        if (newList == null) {
                            //some error on the line..
                            return;
                        }
                        final List<BuildDTO> oldList = Arrays.asList( current );
                        Iterator<BuildDTO> newIt = newList.iterator();
                        Iterator<BuildDTO> oldIt = oldList.iterator();
                        BuildDTO n = newIt.hasNext() ? newIt.next() : null;
                        BuildDTO o = oldIt.hasNext() ? oldIt.next() : null;
                        int index = 0;
                        while ( n != null || o != null )
                        {
                            if ( n != null )
                            {
                                if ( o != null && n.getNumber() == o.getNumber() )
                                {
                                    // for some reason build state is null
                                    // HUDSON-38
                                    if ( o.getState() != null && !BuildStateDTO.COMPLETED.equals( o.getState() ) )
                                    {
                                        tableViewer.insert( n, index );
                                        tableViewer.remove( o );
                                    }
                                    n = newIt.hasNext() ? newIt.next() : null;
                                    o = oldIt.hasNext() ? oldIt.next() : null;
                                }
                                else
                                {
                                    tableViewer.insert( n, index );
                                    n = newIt.hasNext() ? newIt.next() : null;
                                }
                            }
                            else if ( o != null )
                            {
                                tableViewer.remove( o );
                                o = oldIt.hasNext() ? oldIt.next() : null;

                            }
                            index++;
                        }
                        current = newList.toArray( new BuildDTO[0] );
                    }
                } );

            }

        }

        public boolean isUIUp()
        {
            return true;
        }
    }
}
