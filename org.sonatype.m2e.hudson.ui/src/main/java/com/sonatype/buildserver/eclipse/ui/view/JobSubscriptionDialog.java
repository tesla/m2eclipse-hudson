/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.view;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.maven.ide.eclipse.ui.common.dialogs.RemoteResourceLookupDialog;

import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.HudsonManager;
import com.sonatype.buildserver.monitor.HudsonMonitor;
import com.sonatype.buildserver.monitor.CompositeMonitor;
import com.sonatype.matrix.rest.model.project.ProjectDTO;

public class JobSubscriptionDialog
    extends RemoteResourceLookupDialog
{
    private CheckboxTableViewer viewer;

    private Text filterText;
    
    private String filter = ""; //$NON-NLS-1$

    private CompositeMonitor monitor;

    private Set<Object> selectedJobs = new HashSet<Object>();
    
    private Set<ProjectDTO> allJobs = new HashSet<ProjectDTO>();

    private String baseServerUrl;

    private HudsonMonitor serverMonitor;

    private Button selectAllButton;

    private Button deselectAllButton;

	private IStructuredContentProvider contentProvider;

    public JobSubscriptionDialog( Shell parentShell, CompositeMonitor monitor )
    {
        super( parentShell, null );
        setServerName( Messages.jobSubscriptionDialog_buildServer );
        setLoadButtonText( Messages.jobSubscriptionDialog_loadJobs );
        setReadyToLoadMessage( Messages.jobSubscriptionDialog_clickLoadNow );
        setResourceLabelText( Messages.jobSubscriptionDialog_availableJobs );
        setSelectMessage( Messages.jobSubscriptionDialog_selectJobs );
        setTitle( Messages.jobSubscriptionDialog_title );

        this.monitor = monitor;
    }

    private String shortenDescription( String description )
    {
        StringTokenizer tok = new StringTokenizer( description, "\n\r\f" ); //$NON-NLS-1$
        if ( tok.countTokens() > 1 )
            return tok.nextToken() + ELLIPSIS;
        return description;
    }

    @Override
    protected Composite createResourcePanel( Composite parent )
    {
        filterText = new Text( parent, SWT.BORDER | SWT.SEARCH );
        final ModifyListener mlist = new ModifyListener()
        {
            public void modifyText( ModifyEvent e )
            {
                filter = filterText.getText().trim();
                viewer.refresh();
                viewer.setCheckedElements( selectedJobs.toArray() );
            }
        };
        
        filterText.setText( Messages.JobSubscriptionDialog_filter_hint );
        filterText.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_GRAY ) );
        
        filterText.addFocusListener( new FocusListener()
        {
            
            public void focusLost( FocusEvent e )
            {
                filterText.removeModifyListener( mlist );
                if (filter.length() == 0) {
                    filterText.setText( Messages.JobSubscriptionDialog_filter_hint );
                    filterText.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_GRAY ) );
                }
            }
            
            public void focusGained( FocusEvent e )
            {
                if (filter.length() == 0) {
                    filterText.setText( "" ); //$NON-NLS-1$
                    filterText.setForeground( null );
                }
                filterText.addModifyListener( mlist );
            }
        });
        filterText.setData( "name", "filterText" ); //$NON-NLS-1$ //$NON-NLS-2$
        filterText.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false ) );
        filterText.setEnabled( false );
        
        viewer = CheckboxTableViewer.newCheckList( parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION );
        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, true );
        gd.heightHint = 400;
        gd.widthHint = 400;
        viewer.getControl().setLayoutData( gd );
        contentProvider = new JobContentProvider();
        viewer.setContentProvider(contentProvider);
        
        viewer.setLabelProvider( new LabelProvider()
        {
            @Override
            public Image getImage( Object element )
            {
                if ( element instanceof ProjectDTO )
                {
                	ProjectDTO job = (ProjectDTO) element;
                    if (!job.isEnabled()) {
                        return HudsonImages.NOT_BUILT_IMAGE;
                    }
//                    if (job.getLastBuild() != null) {
//                        return HudsonImages.getResultImage( job.getLastBuild().getResult() );
//                    }
                    return HudsonImages.getImage( HudsonImages.EMPTY );
                }
                return super.getImage( element );
            }

            @Override
            public String getText( Object element )
            {
                if ( element instanceof ProjectDTO )
                {
                	ProjectDTO job = (ProjectDTO) element;
                    String description = job.getDescription();
                    String name = job.getName();
                    return ( description == null || description.length() == 0 ) ? name
                                    : NLS.bind( Messages.jobSubscriptionDialog_labelTemplate, name,
                                                shortenDescription( HudsonUtils.stripHtml( description ) ) );
                }
                return super.getText( element );
            }
        } );

        viewer.addCheckStateListener( new ICheckStateListener()
        {
            public void checkStateChanged( CheckStateChangedEvent event )
            {
                int loc = 0;
                Object element = viewer.getElementAt( loc );
                while (element != null) {
                    if (viewer.getChecked( element )) {
                        selectedJobs.add( element );
                    } else {
                        selectedJobs.remove( element );
                    }
                    loc++;
                    element = viewer.getElementAt( loc );
                }
            }
        } );

        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayout( new GridLayout( 2, true ) );
        composite.setLayoutData( new GridData( SWT.RIGHT, SWT.BOTTOM, false, false ) );

        selectAllButton = new Button( composite, SWT.PUSH );
        selectAllButton.setData( "name", "selectAllButton" ); //$NON-NLS-1$ //$NON-NLS-2$
        selectAllButton.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, false, false ) );
        selectAllButton.setText( Messages.jobSubscriptionDialog_selectAll );
        selectAllButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                int index = 0;
                Object o = viewer.getElementAt( index );
                while (o != null) {
                	selectedJobs.add( o );
                	viewer.setChecked(o, true);
                	index = index + 1;
                	o = viewer.getElementAt( index );
                }
            }
        } );

        deselectAllButton = new Button( composite, SWT.PUSH );
        deselectAllButton.setData( "name", "deselectAllButton" ); //$NON-NLS-1$ //$NON-NLS-2$
        deselectAllButton.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, false, false ) );
        deselectAllButton.setText( Messages.jobSubscriptionDialog_deselectAll );
        deselectAllButton.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                int index = 0;
                Object o = viewer.getElementAt( index );
                while (o != null) {
                	selectedJobs.remove( o );
                	viewer.setChecked(o, false);
                	index = index + 1;
                	o = viewer.getElementAt( index );
                }
            }
        } );

        return viewer.getTable();
    }

    @Override
    protected Object loadResources( String url, IProgressMonitor monitor )
        throws Exception
    {
        baseServerUrl = url;
        serverMonitor = this.monitor.getMonitor( url );
        if ( serverMonitor == null )
            serverMonitor = HudsonManager.addHudsonMonitor( URI.create( url ), null);

        Collection<ProjectDTO> jobs = serverMonitor.getAllJobs();
        Collection<ProjectDTO> filter = new ArrayList<ProjectDTO>();
        for (ProjectDTO j : jobs) {
            if (!HudsonUtils.isMatrixConfig( j )) {
                filter.add(j);
            }
        }
        return filter.toArray( new ProjectDTO[filter.size()] );
    }

    @Override
    protected void setInput( Object input )
    {
        viewer.setInput( input );

        if ( input == null || baseServerUrl == null )
        {
            viewer.getTable().setEnabled( false );
            selectAllButton.setEnabled( false );
            deselectAllButton.setEnabled( false );
//            selectedJobs.clear();
            return;
        }

        assert input instanceof ProjectDTO[];

        Collection<String> monitoredJobs = serverMonitor.getMonitoredJobIds();
        List<ProjectDTO> jobsToSelect = new ArrayList<ProjectDTO>();
        allJobs.clear();
        for ( ProjectDTO job : (ProjectDTO[]) input )
        {
            allJobs.add( job );
            if ( monitoredJobs.contains( job.getName() ) )
            {
                jobsToSelect.add( job );
            }
        }
        selectedJobs.clear();
        selectedJobs.addAll( jobsToSelect );
        viewer.setCheckedElements( selectedJobs.toArray() );
        filterText.setEnabled( true );
        selectAllButton.setEnabled( true );
        deselectAllButton.setEnabled( true );
    }

    public String getBaseServerUrl()
    {
        return baseServerUrl;
    }

    @Override
    protected String exceptionToUIText( Exception e )
    {
        return HudsonUtils.restExceptionToString( e, null );
    }

    public HudsonMonitor getSelectedJobs()
    {
        List<String> list = new ArrayList<String>();
        for ( Object o : selectedJobs )
        {
            if ( o instanceof ProjectDTO )
            {
                list.add( ( (ProjectDTO) o ).getName() );
            }
        }
        serverMonitor.setMonitoredJobs( list );
        return serverMonitor;
    }
    
    private class JobContentProvider implements IStructuredContentProvider
    {
        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        	updateOkState(newInput != null);
        }

        public void dispose()
        {
        }

        public Object[] getElements( Object inputElement )
        {
            Object[] elements = new Object[0];
            boolean enable = true;

            if ( inputElement == null )
            {
                enable = false;
                elements = new Object[] { Messages.jobSubscriptionDialog_empty_noServerSelected };
            }
            else if ( inputElement instanceof ProjectDTO[] )
            {
                String filterExpression = filter.toLowerCase();
                if ( filterExpression.length() == 0 )
                {
                    elements = (Object[]) inputElement;
                    if ( elements.length == 0 )
                    {
                        elements = new Object[] { Messages.jobSubscriptionDialog_empty_noJobsAvailable };
                        enable = false;
                    }
                }
                else
                {
                    List<ProjectDTO> filteredProjectDTOs = new ArrayList<ProjectDTO>();
                    for ( ProjectDTO job : (ProjectDTO[]) inputElement )
                    {
                        if ( job.getName().toLowerCase().startsWith( filterExpression ) )
                        {
                            filteredProjectDTOs.add( job );
                        }
                    }
                    elements = filteredProjectDTOs.toArray();
                    if ( elements.length == 0 )
                    {
                        elements = new Object[] { Messages.jobSubscriptionDialog_empty_noMatchingJobs };
                        enable = false;
                    }
                }
            }

            viewer.getTable().setEnabled( enable );
            return elements;
        }
    }

}
