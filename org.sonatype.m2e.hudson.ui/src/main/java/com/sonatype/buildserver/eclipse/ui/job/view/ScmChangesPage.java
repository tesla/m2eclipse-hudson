/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.job.view;

import java.beans.Beans;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.AbstractHudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.HudsonJobBuildsEvent;
import com.sonatype.buildserver.monitor.HudsonJobChangesEvent;
import com.sonatype.buildserver.monitor.HudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJobListener;
import com.sonatype.buildserver.monitor.ErrorJob;
import org.hudsonci.rest.model.build.BuildDTO;
import org.hudsonci.rest.model.build.BuildStateDTO;
import org.hudsonci.rest.model.build.ChangeEntryDTO;
import org.hudsonci.rest.model.build.ChangeFileDTO;
import org.hudsonci.rest.model.build.ChangesDTO;
import org.hudsonci.rest.model.project.ProjectDTO;

public class ScmChangesPage
    extends FormPage
{
    private static Logger log = LoggerFactory.getLogger( ScmChangesPage.class );

    private HudsonJob job;

    private boolean initialized = false;

    private ScrolledForm form;

    private SingleJobView parent;

    private Composite body;

    private FormToolkit toolkit;

    private Tree tree;

    private TreeViewer treeViewer;

    private Composite composite;

    private Section sctnScmProperties;
    private Link link;

    private Action openAction;

    private Action openRecent;

    private Action openBuildAction;

    private Action openEntryAction;

    /**
     * Create the composite.
     * 
     * @param parent
     * @param style
     */
    public ScmChangesPage( FormEditor parent )
    {
        super( parent, SummaryPage.getPageId( "scm" ), Messages.ScmChangesPage_title ); //$NON-NLS-1$
        if ( !Beans.isDesignTime() )
        {
            this.parent = (SingleJobView) parent;
        }
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

        body = form.getBody();
        body.setLayout( new GridLayout() );

        sctnScmProperties = managedForm.getToolkit().createSection( body, Section.TITLE_BAR );
        sctnScmProperties.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );

        managedForm.getToolkit().paintBordersFor( sctnScmProperties );
        sctnScmProperties.setText( Messages.ScmChangesPage_section_title );

        Composite compMain = new Composite( sctnScmProperties, SWT.NONE );
        managedForm.getToolkit().adapt( compMain );
        managedForm.getToolkit().paintBordersFor( compMain );
        sctnScmProperties.setClient( compMain );

        treeViewer = new TreeViewer( compMain, SWT.BORDER );
        tree = treeViewer.getTree();
        treeViewer.setContentProvider( new BCP() );
        treeViewer.setLabelProvider( new BuildsLabelProvider() );

        managedForm.getToolkit().paintBordersFor( tree );

        composite = new Composite( compMain, SWT.NONE );
        composite.setLayout( new GridLayout( 1, true ) );

        managedForm.getToolkit().adapt( composite );
        managedForm.getToolkit().paintBordersFor( composite );
        
        link = new Link(compMain, SWT.NONE);
        managedForm.getToolkit().adapt(link, true, true);
        link.setText( Messages.ScmChangesPage_recent_changes );

        GroupLayout gl_compMain = new GroupLayout( compMain );
        gl_compMain.setHorizontalGroup(
            gl_compMain.createParallelGroup(GroupLayout.TRAILING)
                .add(gl_compMain.createSequentialGroup()
                    .addContainerGap()
                    .add(gl_compMain.createParallelGroup(GroupLayout.LEADING)
                        .add(GroupLayout.TRAILING, tree, GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
                        .add(composite, GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
                        .add(link))
                    .addContainerGap())
        );
        gl_compMain.setVerticalGroup(
            gl_compMain.createParallelGroup(GroupLayout.LEADING)
                .add(gl_compMain.createSequentialGroup()
                    .addContainerGap()
                    .add(link)
                    .addPreferredGap(LayoutStyle.RELATED)
                    .add(tree, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.RELATED)
                    .add(composite, GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                    .addContainerGap())
        );
        compMain.setLayout( gl_compMain );

        parent.constructToolbar( form );

        initialize();
    }

    private void initialize()
    {
        initialized = true;
        treeViewer.addSelectionChangedListener( new ISelectionChangedListener()
        {
            public void selectionChanged( SelectionChangedEvent event )
            {
                TreeSelection ts = (TreeSelection) event.getSelection();
                Object sel = ts.getFirstElement();
                for ( Control c : composite.getChildren() )
                {
                    c.dispose();
                }
                if ( sel instanceof ChangeEntryDTO )
                {
                    ChangeEntryDTO e = (ChangeEntryDTO) sel;
                    ChangeEntryPanel cep = new ChangeEntryPanel( composite, e, job );
                    cep.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
                }
                if ( sel instanceof ChangeFileDTO )
                {
                    ChangeFileDTO e = (ChangeFileDTO) sel;
                    ChangeEntryFilePanel cep = new ChangeEntryFilePanel( composite, e, job );
                    cep.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
                }
                
                if ( sel instanceof BuildDTO )
                {
                    BuildDTO e = (BuildDTO) sel;
                    ChangeBuildPanel cep = new ChangeBuildPanel( composite, e, job );
                    cep.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true ) );
                }
                composite.layout();
                form.reflow( true );
            }
        } );
        link.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                openRecent.run();
            }
        });
        openAction = new Action ( Messages.ScmChangesPage_open_action) 
        {
            public void run() 
            {
                TreeSelection ts = (TreeSelection) treeViewer.getSelection();
                Object sel = ts.getFirstElement();
                if (sel instanceof ChangeFileDTO) {
                    ChangeFileDTO file = (ChangeFileDTO)sel;
                    openInEditor( file );
                }
            }
        };
        openEntryAction = new Action ( Messages.ScmChangesPage_open_all_action) 
        {
            public void run() 
            {
                TreeSelection ts = (TreeSelection) treeViewer.getSelection();
                Object sel = ts.getFirstElement();
                if (sel instanceof ChangeEntryDTO) {
                    ChangeEntryDTO end = (ChangeEntryDTO)sel;
                    for (ChangeFileDTO file : end.getFiles()) {
                        openInEditor( file );
                    }
                }
            }
        }; 
        openRecent = new Action (Messages.ScmChangesPage_show_recent_action) 
        {
            public void run() 
            {
                String url = job.getJobDetails().getUrl();
                if (! url.endsWith( "/" ) ) { //$NON-NLS-1$
                    url = url + "/"; //$NON-NLS-1$
                }
                url = url + "changes";  //$NON-NLS-1$
                HudsonUtils.openUrl( url );
            }
            
        };
        openBuildAction = new Action( Messages.ScmChangesPage_build_changes_action )
        {
            public void run()
            {
                TreeSelection ts = (TreeSelection) treeViewer.getSelection();
                Object sel = ts.getFirstElement();
                if ( sel instanceof BuildDTO )
                {
                    BuildDTO build = (BuildDTO) sel;
                    String url = job.getJobDetails().getUrl();
                    if ( !url.endsWith( "/" ) ) //$NON-NLS-1$
                    {
                        url = url + "/"; //$NON-NLS-1$
                    }
                    url = url + build.getNumber() + "/changes"; //$NON-NLS-1$
                    HudsonUtils.openUrl( url );
                }
            }
        };
        
        treeViewer.addDoubleClickListener( new IDoubleClickListener()
        {
            public void doubleClick( DoubleClickEvent event )
            {
                openAction.run();
            }
        } );
        
        createContextMenu();
        
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

        Menu menu = menuMgr.createContextMenu( treeViewer.getControl() );
        treeViewer.getControl().setMenu( menu );
        getSite().registerContextMenu( menuMgr, treeViewer );
    }

    protected void populateContextMenu( IMenuManager menuManager )
    {
        TreeSelection selection = (TreeSelection) treeViewer.getSelection();

        if ( selection.isEmpty() || selection.size() > 1)
        {
            menuManager.add( openRecent );
        } else {
            if (selection.getFirstElement() instanceof ChangeFileDTO) {
                menuManager.add( openAction );
            }
            if (selection.getFirstElement() instanceof BuildDTO) {
                menuManager.add( openBuildAction );
            }
            if (selection.getFirstElement() instanceof ChangeEntryDTO) {
                menuManager.add( openEntryAction );
            }
        }

        // drillDownAdapter.addNavigationActions( menuManager );
        menuManager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }    
    
    private void openInEditor(ChangeFileDTO file) {
        IResource res = findInWorkspace( file.getPath() );
        log.debug( "Resource " + res + " for entry" + file.getPath() ); //$NON-NLS-1$ //$NON-NLS-2$
        if ( res != null && res instanceof IFile )
        {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            try
            {
                IDE.openEditor( page, (IFile) res, true );
            }
            catch ( PartInitException e )
            {
                log.error( "Failed to open editor for " + res.getFullPath(), e ); //$NON-NLS-1$
                //TODO how to report error to the user here?
            }
        } else {
            //TODO how to report error to the user here?
        }
    }
    
    //aggregator/xyz/zoo.txt 
    //xyz/zoo.txt (searched path)
    //searched xyz/zoo.txt
    public IResource findInWorkspace( String searchedPath )
    {
        IPath searchedElement = new Path( searchedPath );
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource match = null;
        boolean moreSpecific = false;
        while ( !searchedElement.isEmpty() )
        {
            IResource tmp = root.findMember( searchedElement );
            if ( tmp != null && moreSpecific)
            {
                return tmp;
            }
            if (tmp == null && moreSpecific)
                return match;

            //Continue the search
            if ( tmp != null)
            {
                match = tmp;
                moreSpecific = true;
            }
            searchedElement = searchedElement.removeFirstSegments( 1 );
        }
        return null;
    }

    public void setJob( final HudsonJob job )
    {
        this.job = job;
        if ( !initialized )
        {
            return;
        }
        form.setText( job.getJobName() );
        form.setImage( HudsonImages.getHealthImage( job ) );
        ProjectDTO details = job.getJobDetails();
        if ( details != null && !( details instanceof ErrorJob ) )
        {
            if (details.isQueued()) {
                form.setText( job.getJobName()  + " [New build queued]");
            }
        }        

        if ( treeViewer.getInput() == null )
        {
            treeViewer.setInput( job );
        }

    }

    private class BuildsLabelProvider
        extends LabelProvider
    {

        public String getText( Object element )
        {
            String text = element.toString();
            if (element instanceof BuildDTO) {
                BuildDTO b = (BuildDTO)element;
                text = Messages.ScmChangesPage_build_prefix + b.getNumber();
//                if (parts != null) {
//                    text = text + " contains " + parts.size() + " changesets";
//                    Set<String> set = new HashSet<String>(b.getParticipants());
//                    for (String p : set) {
//                        text = text + p + ",";
//                    }
//                }
                
            }
            if ( element instanceof ChangeEntryDTO )
            {
                text =
                    ( (ChangeEntryDTO) element ).getAuthor().getId() + " : " + HudsonUtils.stripHtml( ( (ChangeEntryDTO) element ).getMessage()); //$NON-NLS-1$
            }
            if ( element instanceof ChangeFileDTO )
            {
                text = "" + ( (ChangeFileDTO) element ).getPath(); //$NON-NLS-1$
            }
            return text;
        }

        public Image getImage( Object element )
        {
            if ( element instanceof BuildDTO )
            {
                return SummaryPage.getBuildStatusImage( (BuildDTO) element );
            }
            if ( element instanceof ChangeFileDTO )
            {
                ChangeFileDTO f = (ChangeFileDTO) element;
                switch ( f.getType() )
                {
                    case ADD:
                        return HudsonImages.ADD_CHANGE;
                    case DELETE:
                        return HudsonImages.DELETE_CHANGE;
                    case EDIT:
                        return HudsonImages.EDIT_CHANGE;
                    default:
                        return null;
                }
            }
            if ( element instanceof ChangeEntryDTO )
            {
                return HudsonImages.CHANGESET;
            }
            return null;
        }

    }

    private class BCP
        implements ITreeContentProvider, HudsonJobListener
    {

        private BuildDTO[] current = new BuildDTO[0];

        private boolean initialized = false;

        public void dispose()
        {
        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput )
        {
        }

        public Object[] getElements( Object inputElement )
        {
            if ( inputElement instanceof HudsonJob )
            {
                HudsonJob job = (HudsonJob) inputElement;
                if ( !initialized )
                {
                    job.addListener( this );
                    initialized = true;
                }
                if ( !job.hasBuildsLoaded() )
                {
                    job.loadBuilds();
                    ProjectDTO det = job.getJobDetails();
                    current = new BuildDTO[0];
                    if (det != null) {
                        BuildDTO last = det.getLastBuild();
                        if (last != null && last.isChangesAvailable()) {
                            current = new BuildDTO[] {last};
                        }
                    }
                    return current;
                }
                else
                {
                    List<BuildDTO> withChanges = new ArrayList<BuildDTO>();
                    for ( BuildDTO b : job.getBuilds() )
                    {
                        if ( b.isChangesAvailable() )
                        {
                        	withChanges.add( b );
                        }
                    }
                    current = withChanges.toArray( new BuildDTO[0] );
                    return current;
                }
            }
            return null;
        }

        public Object[] getChildren( Object parentElement )
        {
            if ( parentElement instanceof BuildDTO )
            {
                BuildDTO bld = (BuildDTO) parentElement;
                if ( job.hasChangesLoaded( bld ) )
                {
                    ChangesDTO chngs = job.getChanges( bld );
                    return chngs.getEntries().toArray();
                }
                else
                {
                    job.loadChanges( bld );
                }
                return new Object[] { Messages.ScmChangesPage_loading };
            }
            if ( parentElement instanceof ChangeEntryDTO )
            {
                ChangeEntryDTO en = (ChangeEntryDTO) parentElement;
                return en.getFiles().toArray();
            }
            return null;

        }

        public Object getParent( Object element )
        {
            return null;
        }

        public boolean hasChildren( Object element )
        {
            if ( element instanceof ChangeFileDTO )
            {
                return false;
            }
            if ( element instanceof String )
            {
                return false;
            }
            return true;
        }

        public void getModified( final AbstractHudsonJobEvent event )
        {
            Display.getDefault().asyncExec( new Runnable()
            {
                public void run()
                {
                    //when hidden, no more refreshes..
                    if (body != null && body.isDisposed()) {
                        return;
                    }

                    if ( event instanceof HudsonJobBuildsEvent )
                    {
                        // only add/remove build instances that changed..
                        final List<BuildDTO> newList = ( (HudsonJobBuildsEvent) event ).getBuilds();
                        if (newList == null) {
                            return;
                        }
                        final List<BuildDTO> oldList = Arrays.asList( current );
                        Iterator<BuildDTO> newIt = newList.iterator();
                        Iterator<BuildDTO> oldIt = oldList.iterator();
                        BuildDTO n = newIt.hasNext() ? newIt.next() : null;
                        BuildDTO o = oldIt.hasNext() ? oldIt.next() : null;
                        int index = 0;
                        List<BuildDTO> withChangesDTO = new ArrayList<BuildDTO>();
                        
                        while ( n != null || o != null )
                        {
                            if ( n != null )
                            {
                                if ( o != null && n.getNumber() == o.getNumber() )
                                {
                                    // for some reason build state is null
                                    // HUDSON-38
                                    if ( n.isChangesAvailable() && o.getState() != null && !BuildStateDTO.COMPLETED.equals( o.getState() ) )
                                    {
                                        treeViewer.insert( job, n, index );
                                        treeViewer.remove( o );
                                        withChangesDTO.add( n );
                                    } else {
                                        withChangesDTO.add( o );
                                    }
                                    n = newIt.hasNext() ? newIt.next() : null;
                                    o = oldIt.hasNext() ? oldIt.next() : null;
                                }
                                else
                                {
                                    if ( n.isChangesAvailable() )
                                    {
                                        treeViewer.insert( job, n, index );
                                        withChangesDTO.add( n );
                                    }
                                    n = newIt.hasNext() ? newIt.next() : null;
                                }
                            }
                            else if ( o != null )
                            {
                                treeViewer.remove( o );
                                o = oldIt.hasNext() ? oldIt.next() : null;
                            }
                            index++;
                        }
                        current = withChangesDTO.toArray( new BuildDTO[0] );
                    }

                    if ( event instanceof HudsonJobChangesEvent )
                    {
                        HudsonJobChangesEvent e = (HudsonJobChangesEvent) event;
                        if (e.getChanges() != null) {
                            //ignore failures..
                            treeViewer.add( e.getBuild(), e.getChanges().getEntries().toArray() );
                        }
                        treeViewer.remove( e.getBuild(), new Object[] { Messages.ScmChangesPage_loading } );
                    }
                    if ( event instanceof HudsonJobEvent )
                    {
                        if ( !job.hasBuildsLoaded() )
                        {
                            job.loadBuilds();
                        }
                    }

                }
            } );
        }

        public boolean isUIUp()
        {
            return true;
        }

    }
}
