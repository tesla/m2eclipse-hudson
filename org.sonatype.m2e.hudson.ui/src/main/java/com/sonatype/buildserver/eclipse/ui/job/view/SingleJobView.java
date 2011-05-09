/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.job.view;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import com.sonatype.buildserver.eclipse.ui.HudsonImages;
import com.sonatype.buildserver.eclipse.ui.HudsonUtils;
import com.sonatype.buildserver.eclipse.ui.Messages;
import com.sonatype.buildserver.monitor.AbstractHudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.HudsonJobEvent;
import com.sonatype.buildserver.monitor.HudsonJobEvent.EventType;
import com.sonatype.buildserver.monitor.HudsonJobListener;

public final class SingleJobView
    extends FormEditor
{
    private HudsonJob job;
    private SummaryPage summaryPage;
    private ScmChangesPage scmChangesPage;

    public SingleJobView() {
        super();
    }
    
    @Override
    public void init( IEditorSite site, IEditorInput input )
        throws PartInitException
    {
        if (input instanceof IHudsonJobEditorInput) {
            IHudsonJobEditorInput ji = (IHudsonJobEditorInput)input;
            setHudsonJob( ji.getJob() );
            ji.getJob().addListener( new HudsonJobListener()
            {
                public void getModified( AbstractHudsonJobEvent event )
                {
                    if (event instanceof HudsonJobEvent) {
                        HudsonJobEvent ev = (HudsonJobEvent)event;
                        if (ev.getType() == EventType.CHANGED) {
                            final HudsonJob modified = event.getModifiedJob();
                            Display.getDefault().asyncExec( new Runnable()
                            {
                                public void run()
                                {
                                    setHudsonJob( modified );
                                }
                            } );
                        }
                    }
                }

                public boolean isUIUp()
                {
                    return true;
                }
            });
        }
        super.init( site, input );
    }
    
    void constructToolbar(ScrolledForm form) {
        Action refreshAction = new Action( Messages.jobView_actions_refresh_title )
        {
            public void run()
            {
                job.refresh();
            }
        };
        refreshAction.setImageDescriptor( HudsonImages.getImageDescriptor( HudsonImages.REFRESH ) );
        refreshAction.setToolTipText( Messages.jobView_actions_refresh_tooltip );

        // TODO hide button if user doesn't have rights to do so.
        Action buildNowAction = new Action( Messages.jobView_actions_build_title )
        {
            @Override
            public void run()
            {
                HudsonUtils.runBuild( job );
            }
        };
        buildNowAction.setToolTipText( Messages.jobView_actions_build_tooltip );
        buildNowAction.setImageDescriptor( HudsonImages.BUILD_DESCRIPTOR );

        form.getToolBarManager().add( buildNowAction );
        form.getToolBarManager().add( refreshAction );
        form.updateToolBar();
    }

    private void setHudsonJob( HudsonJob job ) {
        assert Display.getCurrent() != null;
        
        if (getContainer() != null && getContainer().isDisposed()) {
            return;
        }
            
        this.job = job;
        setPartName( HudsonUtils.getJobTitle( job, null ) );
//        setContentDescription( "Hudson Job named " + job.getJobName() + " on server " + job.getServerName() );
//        setTitleToolTip( getContentDescription() );
        
        if (summaryPage != null) {
            summaryPage.setJob( job );
        }
        if (scmChangesPage != null) {
            scmChangesPage.setJob( job );
        }
    }
    
    public void setPartName (String name) {
        super.setPartName( name );
    }
    
    
    private HudsonJob getJob()
    {
        return job;
    }

    @Override
    protected void addPages()
    {
        summaryPage = new SummaryPage( this );
        scmChangesPage = new ScmChangesPage( this );
        try
        {
            addPage(summaryPage);
            addPage(scmChangesPage);
        }
        catch ( PartInitException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (job != null) {
            summaryPage.setJob( job );
            scmChangesPage.setJob( job );
        }
    }

    @Override
    public void doSave( IProgressMonitor monitor )
    {
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }
    
    private static final String EDITOR_ID = "com.sonatype.buildserver.eclipse.ui.job.view.SingleJobView";  //$NON-NLS-1$
    
    public static IStatus openEditor( HudsonJob job )
    {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        
        IEditorInput input = new HudsonInput( job );

        IEditorReference[] editors = workbenchPage.findEditors( null, EDITOR_ID, IWorkbenchPage.MATCH_ID );

        boolean found = false;
        if ( editors != null && editors.length > 0 )
        {
            IEditorPart editorPart = editors[0].getEditor( true );
            if ( editorPart instanceof SingleJobView ) {
                SingleJobView sjv = (SingleJobView)editorPart;
                HudsonJob current = sjv.getJob();
                if (current.getJobName().equals( job.getJobName() ) && current.getServerName().equals( job.getServerName() )) 
                {
                    sjv.setHudsonJob( job );
                    workbenchPage.activate( editorPart );
                    found = true;
                }
                
            }
        }
        if ( !found )
        {
            try
            {
                workbenchPage.openEditor( input, EDITOR_ID );
            }
            catch ( PartInitException e )
            {
                return e.getStatus();
            }
        }

        return Status.OK_STATUS;
    }
    
    private static class HudsonInput implements IHudsonJobEditorInput {

        private HudsonJob job;

        public HudsonInput( HudsonJob job )
        {
            this.job = job;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( job == null ) ? 0 : job.getJobName().hashCode() );
            result = prime * result + ( ( job == null ) ? 0 : job.getServerName().hashCode() );
            return result;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            HudsonInput other = (HudsonInput) obj;
            if ( job == null )
            {
                return other.job == null;
            }
            return job.getJobName().equals( other.job.getJobName() ) && 
                   job.getServerName().equals( other.job.getServerName() );
        }

        public boolean exists()
        {
            return true;
        }

        public ImageDescriptor getImageDescriptor()
        {
            return HudsonImages.HUDSON_DESCRIPTOR;
        }

        public String getName()
        {
            return job.getJobName();
        }

        public IPersistableElement getPersistable()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public String getToolTipText()
        {
            return NLS.bind( Messages.SingleJobView_tooltip, job.getJobName(), job.getServerName() );
        }

        @SuppressWarnings( { "rawtypes", "unchecked" } )
        public Object getAdapter( Class adapter )
        {
            if (adapter.isAssignableFrom( HudsonJob.class )) {
                return job;
            }
            return null;
        }

        public HudsonJob getJob()
        {
            return job;
        }
        
    }


}    

