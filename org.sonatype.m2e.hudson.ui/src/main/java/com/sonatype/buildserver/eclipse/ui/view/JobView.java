/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.view;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;
import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.AbstractHudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.HudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJobListener;
import com.sonatype.buildserver.monitor.CompositeMonitor;
import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.project.HealthDTO;
import com.sonatype.matrix.rest.model.project.ProjectDTO;

@SuppressWarnings( "restriction" )
public class JobView
    extends ViewPart
    implements HudsonJobListener, IPerspectiveListener2
{
    private static Logger log = LoggerFactory.getLogger( JobView.class );
    
    public static final String ID = "com.sonatype.buildserver.eclipse.ui.view.JobView"; 
    
    public JobView() {
    }
    private static final String HIDE_COLUMN_SERVER = "hide.column.server"; //$NON-NLS-1$

    private static final String HIDE_COLUMN_LAST_BUILD = "hide.column.lastBuild"; //$NON-NLS-1$
    
    private TreeViewer viewer;

    private Tree tree;

    private JobLabelProvider labelProvider = new JobLabelProvider( 0 );

    private TreeViewerColumn statusColumn;

    private TreeViewerColumn weatherColumn;

    private TreeViewerColumn nameColumn;

    private TreeViewerColumn serverColumn;

    private TreeViewerColumn lastBuildColumn;

    private Action addAction;

    private Action buildNowAction;

    private Action openAction;

    private Action removeAction;

    private Action refreshAction;

    private Action showTestResultAction;

    private Action showConsoleAction;

    private Action showWorkspace;

    private CompositeMonitor monitor;

    private SelectionAdapter columnSelectionListener;

    private Action hideServerAction;

    private Action hideLastBuildAction;
    private Composite compNoJobs;
    private Link link;

    private StackLayout stack;

    private Composite stackParent;

    private Action paste;

    private Action copy;

    @Override
    public void createPartControl( Composite parent )
    {
        stack = new StackLayout();
        stackParent = parent;
        parent.setLayout(stack);
        
        createViewer( parent );
        
        compNoJobs = new Composite(parent, SWT.NONE);
        compNoJobs.setLayout(new GridLayout(1, false));
        
        link = new Link(compNoJobs, SWT.NONE);
        GridData gd_link = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
        gd_link.verticalIndent = 12;
        gd_link.horizontalIndent = 12;
        link.setLayoutData(gd_link);
        link.setText(Messages.JobView_no_monitored_job_link);
        link.addSelectionListener( new SelectionAdapter()
        {
            public void widgetSelected( SelectionEvent e )
            {
                addAction.run();
            }
        });
        
        new UrlDropTargetAdapter( compNoJobs );
        new UrlDropTargetAdapter( viewer.getTree() );
        IHandlerService handlerService =  (IHandlerService) getViewSite().getService( IHandlerService.class);
        handlerService.activateHandler( ActionFactory.PASTE.getCommandId(), new PasteFavoritesHandler() );
        handlerService.activateHandler( ActionFactory.COPY.getCommandId(), new CopyFavoritesHandler() );
        createActions();
        createContextMenu();
        populateToolbar();
        
    }

    @Override
    public void setFocus()
    {
        if (viewer.getTree().isVisible()) {
            viewer.getControl().setFocus();
        }
    }

    private void createViewer( Composite parent )
    {
        viewer = new TreeViewer( parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );

        tree = viewer.getTree();

        tree.setHeaderVisible( true );

        columnSelectionListener = new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                TreeColumn oldSortColumn = tree.getSortColumn();
                TreeColumn sortColumn = (TreeColumn) e.widget;
                if ( sortColumn == oldSortColumn )
                {
                    int sortDirection = tree.getSortDirection();
                    tree.setSortDirection( sortDirection == SWT.UP ? SWT.DOWN : SWT.UP );
                }
                else
                {
                    tree.setSortColumn( sortColumn );
                }
                viewer.refresh();
            }
        };

        statusColumn = new TreeViewerColumn( viewer, SWT.CENTER );
        statusColumn.getColumn().setWidth( 40 );
        statusColumn.getColumn().setText( Messages.jobView_columns_status );
        statusColumn.getColumn().addSelectionListener( columnSelectionListener );
        statusColumn.setLabelProvider( new JobLabelProvider( JobLabelProvider.STATUS_COLUMN ) );

        weatherColumn = new TreeViewerColumn( viewer, SWT.CENTER );
        weatherColumn.getColumn().setWidth( 30 );
        weatherColumn.getColumn().setText( Messages.jobView_columns_weather );
        weatherColumn.getColumn().addSelectionListener( columnSelectionListener );
        weatherColumn.setLabelProvider( new JobLabelProvider( JobLabelProvider.WEATHER_COLUMN ) );

        nameColumn = new TreeViewerColumn( viewer, SWT.NONE );
        nameColumn.getColumn().setText( Messages.jobView_columns_name );
        nameColumn.getColumn().addSelectionListener( columnSelectionListener );
        nameColumn.setLabelProvider( new JobLabelProvider( JobLabelProvider.NAME_COLUMN ) );

        if ( !HudsonUIActivator.getDefault().getPreferenceStore().getBoolean( HIDE_COLUMN_SERVER ) )
        {
            serverColumn = createServerColumn();
        }

        if ( !HudsonUIActivator.getDefault().getPreferenceStore().getBoolean( HIDE_COLUMN_LAST_BUILD ) )
        {
            lastBuildColumn = createLastBuildColumn();
        }

        ColumnViewerToolTipSupport.enableFor( viewer, ColumnViewerToolTipSupport.RECREATE );

        tree.addControlListener( new ControlListener()
        {
            public void controlResized( ControlEvent e )
            {
                resizeColumns();
                viewer.getTree().removeControlListener( this );
            }

            public void controlMoved( ControlEvent e )
            {
            }
        } );

        viewer.setContentProvider( new ITreeContentProvider()
        {
            public boolean hasChildren( Object element )
            {
                return false;
            }

            public Object getParent( Object element )
            {
                return null;
            }

            public Object[] getChildren( Object parentElement )
            {
                return null;
            }

            public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
            {
            }

            public void dispose()
            {
            }

            public Object[] getElements( Object inputElement )
            {
                if ( inputElement instanceof Object[] )
                {
                    return (Object[]) inputElement;
                }
                return null;
            }
        } );

        new org.eclipse.core.runtime.jobs.Job( Messages.background_loading_prefs )
        {
            protected IStatus run( IProgressMonitor monitor )
            {
                try
                {
                    CompositeMonitor loadedMonitor = HudsonUtils.loadDefaultMonitor();
                    assert loadedMonitor != null;
                    setMonitor( loadedMonitor );
                    return Status.OK_STATUS;
                }
                catch ( Exception e )
                {
                    setMonitor( new CompositeMonitor() );
                    log.error( "Error loading default composite monitor", e );
                    return new Status( IStatus.ERROR, HudsonUIActivator.PLUGIN_ID, e.getMessage(), e );
                }
            }
        }.schedule();

        tree.setSortColumn( nameColumn.getColumn() );
        tree.setSortDirection( SWT.UP );
        viewer.setComparator( new ViewerComparator()
        {
            @Override
            public int compare( Viewer viewer, Object e1, Object e2 )
            {
                int direction = tree.getSortDirection();
                int directModifier = direction == SWT.UP ? 1 : -1;
                int column = tree.indexOf( tree.getSortColumn() );
                switch ( column )
                {
                    case JobLabelProvider.STATUS_COLUMN:
                        e1 = getBuildResult( e1 );
                        e2 = getBuildResult( e2 );
                        break;
                    case JobLabelProvider.WEATHER_COLUMN:
                        return directModifier * (getBuildHealth( e1 ) - getBuildHealth( e2 ));
                    case JobLabelProvider.LAST_BUILD_COLUMN:
                        return directModifier * (getBuildTime( e1 ) - getBuildTime( e2 ) < 0 ? -1 : 1);
                    default:
                        e1 = labelProvider.getColumnText( e1, column );
                        e2 = labelProvider.getColumnText( e2, column );
                        break;
                }
                int result = super.compare( viewer, e1, e2 );
                return directModifier * result;
            }

            private String getBuildResult( Object o )
            {
                if ( o instanceof HudsonJob )
                {
                    ProjectDTO job = ( (HudsonJob) o ).getJobDetails();
                    if ( job != null )
                    {
                        BuildDTO build = job.getLastBuild();
                        if ( build != null )
                        {
                            return String.valueOf( build.getResult() );
                        }
                    }
                }
                return null;
            }

            private int getBuildHealth( Object o )
            {
                if ( o instanceof HudsonJob )
                {
                    ProjectDTO job = ( (HudsonJob) o ).getJobDetails();
                    if ( job != null )
                    {
                        HealthDTO health = job.getHealth();
                        if ( health != null )
                        {
                            return health.getScore();
                        }
                    }
                }
                return -1;
            }

            private long getBuildTime( Object o )
            {
                if ( o instanceof HudsonJob )
                {
                    ProjectDTO job = ( (HudsonJob) o ).getJobDetails();
                    if ( job != null )
                    {
                        BuildDTO build = job.getLastBuild();
                        if ( build != null )
                        {
                            return build.getTimeStamp();
                        }
                    }
                }
                return -1;
            }
        } );

        viewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                boolean empty = viewer.getSelection().isEmpty();
                StructuredSelection ss = (StructuredSelection)viewer.getSelection();
                boolean hasOnlyDeclared = true;
                for (Object o : ss.toList()) {
                    HudsonJob b = (HudsonJob)o;
                    if (b.isMonitored()) {
                        hasOnlyDeclared = false;
                        break;
                    }
                }
                buildNowAction.setEnabled( !empty );
                removeAction.setEnabled( !empty && !hasOnlyDeclared);
                showTestResultAction.setEnabled( !empty );
            }
        } );
        viewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                openAction.run();
            }
        } );
    }

    private TreeViewerColumn createServerColumn()
    {
        TreeViewerColumn toRet = new TreeViewerColumn( viewer, SWT.NONE );
        toRet.setLabelProvider( new JobLabelProvider( JobLabelProvider.SERVER_COLUMN ) );
        toRet.getColumn().setText( Messages.jobView_columns_server );
        toRet.getColumn().setWidth( 30 );
        toRet.getColumn().addSelectionListener( columnSelectionListener );
        return toRet;
    }

    private TreeViewerColumn createLastBuildColumn()
    {
        TreeViewerColumn toRet = new TreeViewerColumn( viewer, SWT.NONE );
        toRet.getColumn().setText( Messages.jobView_columns_lastBuild );
        toRet.getColumn().addSelectionListener( columnSelectionListener );
        toRet.getColumn().setWidth( 30 );
        toRet.setLabelProvider( new JobLabelProvider( JobLabelProvider.LAST_BUILD_COLUMN ) );
        return toRet;
    }

    private void createActions()
    {
//        paste =  ActionFactory.PASTE.create( getSite().getWorkbenchWindow() );
        paste = new Action(WorkbenchMessages.Workbench_paste) {

            @Override
            public void run()
            {
                IHandlerService handlerService =  (IHandlerService) getViewSite().getService( IHandlerService.class);
                try
                {
                    handlerService.executeCommand( ActionFactory.PASTE.getCommandId(), null );
                }
                catch ( Exception e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        paste.setActionDefinitionId(ActionFactory.PASTE.getCommandId());
        paste.setToolTipText(WorkbenchMessages.Workbench_pasteToolTip);
        ISharedImages sharedImages = getSite().getWorkbenchWindow().getWorkbench()
                .getSharedImages();
        paste.setImageDescriptor(sharedImages
                .getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
        paste.setDisabledImageDescriptor(sharedImages
                .getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
        
//        copy = ActionFactory.COPY.create( getSite().getWorkbenchWindow() );
        copy = new Action(WorkbenchMessages.Workbench_copy) {
                @Override
                public void run()
                {
                    IHandlerService handlerService =  (IHandlerService) getViewSite().getService( IHandlerService.class);
                    try
                    {
                        handlerService.executeCommand( ActionFactory.COPY.getCommandId(), null );
                    }
                    catch ( Exception e )
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
        };
        copy.setToolTipText(WorkbenchMessages.Workbench_copyToolTip);
        copy.setActionDefinitionId(ActionFactory.COPY.getCommandId());
        copy.setImageDescriptor(sharedImages
                .getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        copy.setDisabledImageDescriptor(sharedImages
                .getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));


        addAction = new Action( Messages.jobView_actions_add_title )
        {
            @Override
            public void run()
            {
                JobSubscriptionDialog d = new JobSubscriptionDialog( getSite().getShell(), monitor );
                if ( d.open() == IDialogConstants.OK_ID )
                {
                    monitor.addMonitor( d.getSelectedJobs() );
                    update();
                }
            }
        };
        addAction.setToolTipText( Messages.jobView_actions_add_tooltip );
        addAction.setImageDescriptor( HudsonImages.ADD_JOB_DESCRIPTOR );

        buildNowAction = new Action( Messages.jobView_actions_build_title )
        {
            @Override
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                for ( Iterator i = selection.iterator(); i.hasNext(); )
                {
                    final Object o = i.next();
                    if ( o instanceof HudsonJob )
                    {
                        HudsonUtils.runBuild( (HudsonJob) o );
                    }
                }
            }
        };
        buildNowAction.setToolTipText( Messages.jobView_actions_build_tooltip );
        buildNowAction.setImageDescriptor( HudsonImages.BUILD_DESCRIPTOR );
        buildNowAction.setEnabled( false );

        openAction = new Action( Messages.jobView_actions_open_title )
        {
            @Override
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                for ( Iterator i = selection.iterator(); i.hasNext(); )
                {
                    Object o = i.next();
                    if ( o instanceof HudsonJob )
                    {
                        HudsonJob bj = (HudsonJob) o;
                        HudsonUtils.openJob( bj );
                    }
                }

            }
        };
        openAction.setToolTipText( Messages.jobView_actions_open_tooltip );
        openAction.setImageDescriptor( HudsonImages.HUDSON_DESCRIPTOR );

        removeAction = new Action( Messages.jobView_actions_remove_title )
        {
            @Override
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                for ( Iterator i = selection.iterator(); i.hasNext(); )
                {
                    Object o = i.next();
                    if ( o instanceof HudsonJob )
                    {
                        ( (HudsonJob) o ).dispose();
                    }
                }
                viewer.setSelection(null);
                update();
            }
        };
        removeAction.setToolTipText( Messages.jobView_actions_remove_tooltip );
        removeAction.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages().getImageDescriptor( ISharedImages.IMG_ELCL_REMOVE ) );
        removeAction.setEnabled( false );

        refreshAction = new Action( Messages.jobView_actions_refresh_title )
        {
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                if (selection.isEmpty()) {
                    monitor.refreshAll();
                } else {
                    List<HudsonJob> bjobs = new ArrayList<HudsonJob>();
                    for ( Iterator i = selection.iterator(); i.hasNext(); )
                    {
                        Object o = i.next();
                        if ( o instanceof HudsonJob )
                        {
                            bjobs.add((HudsonJob)o);
                        }
                    }
                    monitor.refreshSome( bjobs );
                }
            }
        };
        refreshAction.setToolTipText( Messages.jobView_actions_refresh_tooltip );
        refreshAction.setImageDescriptor( HudsonImages.getImageDescriptor( HudsonImages.REFRESH ) );
        refreshAction.setEnabled( true );

        showTestResultAction = new Action( Messages.jobView_actions_show_test_results_title )
        {
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                final HudsonJob job = (HudsonJob) selection.getFirstElement();
                if ( job == null )
                    return;
                if ( job.getJobDetails() == null || job.getJobDetails().getLastBuild() == null )
                {
                    return;
                }
                HudsonUtils.openTests( job, job.getJobDetails().getLastBuild() );
            }
        };
        showTestResultAction.setToolTipText( Messages.jobView_actions_show_test_results_tooltip );
        showTestResultAction.setImageDescriptor( HudsonImages.getImageDescriptor( HudsonImages.JUNIT ) );
        showTestResultAction.setEnabled( true );

        showConsoleAction = new Action( Messages.jobView_actions_show_console_title )
        {
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                final HudsonJob job = (HudsonJob) selection.getFirstElement();
                if ( job == null )
                    return;
                if ( job.getJobDetails() == null ) 
                	return;
                if ( job.getJobDetails().getLastBuild() == null ) 
                	return;

                HudsonUtils.openConsole( job, job.getJobDetails().getLastBuild() );
            }
        };
        showConsoleAction.setToolTipText( Messages.jobView_actions_show_console_tooltip );
        showConsoleAction.setImageDescriptor( HudsonImages.getImageDescriptor( HudsonImages.CONSOLE ) );
        showConsoleAction.setEnabled( true );

        showWorkspace = new Action( Messages.JobView_workspace_browser )
        {
            public void run()
            {
                IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                final HudsonJob job = (HudsonJob) selection.getFirstElement();
                if ( job == null )
                    return;
                if ( job.getJobDetails() == null ) 
                	return;
                
                String url = job.getJobDetails().getUrl();
                
                if ( !url.endsWith( "/" ) ) { //$NON-NLS-1$
                    url = url + "/"; //$NON-NLS-1$
                }
                url = url + "ws"; //$NON-NLS-1$

                HudsonUtils.openUrl( url );
            }
        };
        showWorkspace.setToolTipText( Messages.jobView_actions_show_console_tooltip );
        showWorkspace.setImageDescriptor( HudsonImages.getImageDescriptor( HudsonImages.WEB ) );
        showWorkspace.setEnabled( true );

        hideServerAction = new Action()
        {

            @Override
            public String getText()
            {
                boolean hide = HudsonUIActivator.getDefault().getPreferenceStore().getBoolean( HIDE_COLUMN_SERVER );
                return hide ? Messages.JobView_show_server : Messages.JobView_hide_server;
            }

            @Override
            public void run()
            {
                boolean hide = HudsonUIActivator.getDefault().getPreferenceStore().getBoolean( HIDE_COLUMN_SERVER );
                HudsonUIActivator.getDefault().getPreferenceStore().setValue( HIDE_COLUMN_SERVER, !hide );
                this.firePropertyChange( TEXT, null, getText() );
                if ( !hide )
                {
                    serverColumn.getColumn().dispose();
                    serverColumn = null;
                }
                else
                {
                    if ( lastBuildColumn != null )
                    {
                        lastBuildColumn.getColumn().dispose();
                    }
                    serverColumn = createServerColumn();
                    if ( lastBuildColumn != null )
                    {
                        lastBuildColumn = createLastBuildColumn();
                    }
                }
                resizeColumns();
                viewer.refresh();
            }

        };

        hideLastBuildAction = new Action()
        {

            @Override
            public String getText()
            {
                boolean hidden =
                    HudsonUIActivator.getDefault().getPreferenceStore().getBoolean( HIDE_COLUMN_LAST_BUILD );
                return hidden ? Messages.JobView_show_build : Messages.JobView_hide_build;
            }

            @Override
            public void run()
            {
                boolean hidden =
                    HudsonUIActivator.getDefault().getPreferenceStore().getBoolean( HIDE_COLUMN_LAST_BUILD );
                HudsonUIActivator.getDefault().getPreferenceStore().setValue( HIDE_COLUMN_LAST_BUILD, !hidden );
                this.firePropertyChange( TEXT, null, getText() );
                if ( !hidden )
                {
                    lastBuildColumn.getColumn().dispose();
                    lastBuildColumn = null;
                }
                else
                {
                    lastBuildColumn = createLastBuildColumn();
                }
                resizeColumns();
                viewer.refresh();
            }

        };

    }

    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown( true );
        menuMgr.addMenuListener( new IMenuListener()
        {
            public void menuAboutToShow( IMenuManager manager )
            {
                populateContextMenu( manager );
            }
        } );

        Menu menu = menuMgr.createContextMenu( viewer.getControl() );
        viewer.getControl().setMenu( menu );
        getSite().registerContextMenu( menuMgr, viewer );
    }

    private void populateContextMenu( IMenuManager menuManager )
    {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();

        if ( !selection.isEmpty() )
        {
            menuManager.add( openAction );
            menuManager.add( buildNowAction );
            menuManager.add( new Separator() );
            if ( selection.size() == 1 )
            {
                HudsonJob jb = (HudsonJob) selection.getFirstElement();
                ProjectDTO j = jb.getJobDetails();
                showTestResultAction.setEnabled( true );
                if ( j != null && j.getLastBuild() != null )
                {
                    showTestResultAction.setEnabled( j.getLastBuild().isTestsAvailable() );
                }
                menuManager.add( showTestResultAction );
                menuManager.add( showConsoleAction );
                menuManager.add( showWorkspace );
            }
        }
        menuManager.add( new Separator() );

        
//        //brute force enable state..for some reason the actions don't update based on handlers like the global actions do
        Clipboard clip = new Clipboard( getSite().getShell().getDisplay());
        paste.setEnabled( clip.getContents( URLTransfer.getInstance()) != null || clip.getContents( TextTransfer.getInstance() ) != null);
        copy.setEnabled( !selection.isEmpty() );
        
        menuManager.add( copy );
        menuManager.add( paste );
        menuManager.add( new Separator() );
        menuManager.add( addAction );
        if ( !selection.isEmpty() )
        {
            menuManager.add( refreshAction );
            menuManager.add( removeAction );
        }
        // drillDownAdapter.addNavigationActions( menuManager );
        menuManager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }

    private void populateToolbar()
    {
        IActionBars bars = getViewSite().getActionBars();

        IToolBarManager toolbarManager = bars.getToolBarManager();
        toolbarManager.add( buildNowAction );
        toolbarManager.add( new Separator() );
        toolbarManager.add( addAction );
        toolbarManager.add( refreshAction );
        toolbarManager.add( removeAction );
        toolbarManager.add( new Separator() );

        bars.getMenuManager().add( hideServerAction );
        bars.getMenuManager().add( hideLastBuildAction );
        // drillDownAdapter.addNavigationActions( toolbarManager );
    }

    @Override
	public void dispose() {
    	// when disposing, get rid of the listener.
        if ( this.monitor != null )
        {
            this.monitor.unregister( this );
        }
		super.dispose();
		
	}

	private void update()
    {
        assert Display.getCurrent() != null;
        // when the viewer is disposed, we shall not be calling setInput() on it.
        if (viewer.getTree().isDisposed()) {
        	return;
        }
        Collection<HudsonJob> jbs = monitor.getJobs();
        if (jbs.isEmpty()) {
            stack.topControl = compNoJobs;
            viewer.setInput(null);
        } else {
            stack.topControl = viewer.getControl();
            viewer.setInput( jbs.toArray() );
        }
        stackParent.layout();
    }

    private void resizeColumns()
    {
        int devide = serverColumn != null ? 3 : 2;
        devide = devide - ( lastBuildColumn != null ? 0 : 1 );
        int width =
            ( viewer.getTree().getClientArea().width - statusColumn.getColumn().getWidth() - weatherColumn.getColumn().getWidth() )
                / devide;
        nameColumn.getColumn().setWidth( width );
        if ( serverColumn != null )
        {
            serverColumn.getColumn().setWidth( width );
        }
        if ( lastBuildColumn != null )
        {
            lastBuildColumn.getColumn().setWidth( width );
        }
    }

    public void getModified( final AbstractHudsonJobEvent event )
    {
        if ( !( event instanceof HudsonJobEvent ) )
        {
            // only job refreshes count here..
            return;
        }
        Display.getDefault().asyncExec( new Runnable()
        {
            public void run()
            {
                update();
            }
        });
    }

    public void saveState( IMemento memento )
    {
        IEclipsePreferences pref = new InstanceScope().getNode( HudsonUIActivator.PLUGIN_ID );
        pref.put( "defaultMonitor", monitor.getId().toString() ); //$NON-NLS-1$
        try
        {
            pref.flush();
        }
        catch ( BackingStoreException e )
        {

        }
        monitor.save();
    }

    private void setMonitor( CompositeMonitor monitor )
    {
        assert Display.getCurrent() == null;
        if ( this.monitor != null )
        {
            this.monitor.unregister( this );
        }
        this.monitor = monitor;
        this.monitor.register( this );

        getSite().getWorkbenchWindow().addPerspectiveListener( this );
        Display.getDefault().asyncExec( new Runnable()
        {
            public void run()
            {
                update();
            }
        } );
    }

    public void perspectiveActivated( IWorkbenchPage page, IPerspectiveDescriptor perspective )
    {
    }

    public void perspectiveChanged( IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId )
    {
    }

    public void perspectiveChanged( IWorkbenchPage page, IPerspectiveDescriptor perspective,
                                    IWorkbenchPartReference partRef, String changeId )
    {
        // saveState isn't called when the View is simply closed, we need to watch for events & save when this view is
        // closed
        if ( partRef instanceof IViewReference && changeId.equals( IWorkbenchPage.CHANGE_VIEW_HIDE ) )
        {
            String id = ( (IViewReference) partRef ).getId();
            if ( id.equals( getViewSite().getId() ) )
            {
                saveState( null );
            }
        }
    }
    
    private class UrlDropTargetAdapter extends DropTargetAdapter {

        public UrlDropTargetAdapter(Control control)
        {
            DropTarget target =  new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY);
            target.setTransfer(new Transfer[] { URLTransfer.getInstance(), TextTransfer.getInstance() });
            target.addDropListener(this);
        }
        
        public void dragEnter(DropTargetEvent event) 
        { 
            if (event.detail == DND.DROP_MOVE
             || event.detail == DND.DROP_DEFAULT) 
            { 
                if ((event.operations & DND.DROP_COPY) != 0) {
                    event.detail = DND.DROP_COPY; 
                } else {
                    event.detail = DND.DROP_NONE;
                }
            }
        }
            
        @Override
        public void drop( DropTargetEvent event )
        {
            String u = null;
            if (URLTransfer.getInstance().isSupportedType(event.currentDataType)) 
            {
                u = (String) URLTransfer.getInstance().nativeToJava( event.currentDataType );
            } 
            else if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) 
            {
                u = (String)TextTransfer.getInstance().nativeToJava( event.currentDataType );
            }
            if (u != null) {
                try
                {
                    if (HudsonUtils.startMonitoringJob( u, monitor )) {
                        update();
                    }
                }
                catch ( URISyntaxException e )
                {
                    StatusManager.getManager().handle( new Status( IStatus.ERROR, HudsonUIActivator.PLUGIN_ID, "Wrong Hudson server url (" + u + ")", e ), StatusManager.LOG );
                }
            } else {
                event.detail = DND.DROP_NONE;
            }
        }
    }
    
    public class CopyFavoritesHandler
        extends AbstractHandler
    {

        public Object execute( ExecutionEvent event )
            throws ExecutionException
        {
            Clipboard clip = new Clipboard( HandlerUtil.getActiveShell( event ).getDisplay() );
            try
            {
                return execute( event, clip );
            }
            finally
            {
                clip.dispose();
            }
        }

        protected Object execute( ExecutionEvent evt, Clipboard clipboard )
            throws ExecutionException
        {
            if (!viewer.getTree().isVisible() || viewer.getTree().isDisposed()) {
                return null;
            }
                
            ISelection selection = viewer.getSelection();
            if ( selection instanceof IStructuredSelection )
            {
                Object[] objects = ( (IStructuredSelection) selection ).toArray();
                if ( objects.length == 0 )
                {
                    return null;
                }
                if ( objects.length == 1 )
                {
                    try
                    {
                        clipboard.setContents( new Object[] { objects[0].toString(), objects[0].toString() },
                                               new Transfer[] { URLTransfer.getInstance(), TextTransfer.getInstance() } );
                    }
                    catch ( SWTError error )
                    {
                        // Copy to clipboard failed.
                    }
                } else {
                    StringBuffer buf = new StringBuffer();
                    for (Object o : objects) {
                        buf.append( o.toString() + "\n" );
                    }
                    try
                    {
                        clipboard.setContents( new Object[] { buf.toString() },
                                               new Transfer[] { TextTransfer.getInstance() } );
                    }
                    catch ( SWTError error )
                    {
                        // Copy to clipboard failed.
                    }
                    
                }
            }
            return null;
        }
    }
    
    public class PasteFavoritesHandler
        extends AbstractHandler
    {
        protected Object execute( ExecutionEvent evt, Clipboard clipboard )
            throws ExecutionException
        {
            if (!paste( clipboard, URLTransfer.getInstance() ) ) {
                paste( clipboard, TextTransfer.getInstance() );
            }
            return null;
        }
        
 
        private boolean paste( Clipboard clipboard, Transfer transfer )
        {
            Object elements = clipboard.getContents( transfer );
            if ( elements != null) {
                String u = elements.toString();
                try
                {
                    if (HudsonUtils.startMonitoringJob( u, monitor )) {
                        update();
                    }
                }
                catch ( URISyntaxException e )
                {
                    StatusManager.getManager().handle( new Status( IStatus.ERROR, HudsonUIActivator.PLUGIN_ID, "Wrong Hudson server url (" + u + ")", e ), StatusManager.LOG );
                    return false;
                }
                return true;
            }
            return false;
        }


        public Object execute( ExecutionEvent event )
            throws ExecutionException
        {
            Clipboard clip = new Clipboard( HandlerUtil.getActiveShell(event)
                                            .getDisplay());
            try {
                return execute (event, clip);
            } finally {
                clip.dispose();
            }
        }
    }

    public boolean isUIUp()
    {
        return true;
    }
}
