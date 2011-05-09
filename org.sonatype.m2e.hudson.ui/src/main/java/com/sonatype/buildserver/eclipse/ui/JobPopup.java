/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.buildserver.eclipse.ui.view.JobView;
import com.sonatype.buildserver.monitor.HudsonJob;
import com.sonatype.buildserver.monitor.CompositeMonitor;

public class JobPopup
    extends PopupDialog
{
    private static final int POPUP_OFFSET = 20;
    private static Logger log = LoggerFactory.getLogger( JobPopup.class );

    private Composite dialogArea;

    private HudsonJob[] jobs;

    private final CompositeMonitor monitor;

    private StatusLineCLabelContribution label;

	private int additionalCount;

    public JobPopup( Shell parentShell, CompositeMonitor monitor, StatusLineCLabelContribution cLabel )
    {
        super( parentShell, PopupDialog.HOVER_SHELLSTYLE | SWT.MODELESS, true, true, true, false, false, Messages.jobPopup_title,
               null );
        this.monitor = monitor;
        this.label = cLabel;
    }

    void setJobs( HudsonJob[] jobs, int additionalCount )
    {
        this.jobs = new HudsonJob[jobs.length];
        System.arraycopy( jobs, 0, this.jobs, 0, jobs.length );
        this.additionalCount = additionalCount;
    }

    protected Control createDialogArea( Composite parent )
    {
        dialogArea = new Composite( parent, SWT.NONE );
        dialogArea.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        dialogArea.setLayout( layout );

        for ( HudsonJob job : jobs )
        {
            if ( job == null )
                break;
            getLatestBuildIcon( dialogArea, job ).setLayoutData( new GridData( GridData.FILL_BOTH ) );
            getHealthIcon( dialogArea, job ).setLayoutData( new GridData( GridData.FILL_BOTH ) );

            Link l = new Link( dialogArea, SWT.MULTI | SWT.WRAP | SWT.RIGHT );
            l.setText( "<a>" + job.getJobName() + "</a>" ); //$NON-NLS-1$ //$NON-NLS-2$
            l.setToolTipText("Open " + job.getJobName() + " details page.");
            l.setLayoutData( new GridData( GridData.FILL_BOTH ) );
            l.addSelectionListener( new LinkListener( job ) );

            Label time = new Label( dialogArea, SWT.NONE );
            time.setText( HudsonUtils.getFormattedLastRun( job ) );
            time.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        }
        if (additionalCount > 0) {
            Label time = new Label( dialogArea, SWT.NONE );
            time.setLayoutData( new GridData( GridData.FILL_BOTH ) );
            Label space = new Label( dialogArea, SWT.NONE );
            space.setLayoutData( new GridData( GridData.FILL_BOTH ) );
            
            Link l = new Link( dialogArea, SWT.MULTI | SWT.WRAP | SWT.RIGHT );
            l.setText( "<a>... and " + additionalCount + " more</a>" ); //$NON-NLS-1$ //$NON-NLS-2$
            l.setToolTipText("Open the Hudson Jobs view.");
            l.setLayoutData( new GridData( GridData.FILL_BOTH ) );
            l.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					JobPopup.this.close();
					try {
						IViewPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(JobView.ID);
//						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(part);
					} catch (PartInitException e1) {
						log.error("Cannot activate jobview ", e1);
					}
				}
			});
        }
        return dialogArea;
    }

    private Control getHealthIcon( Composite parent, HudsonJob job )
    {
        Label label = new Label( dialogArea, SWT.NONE );
        label.setImage( HudsonImages.getHealthImage( job ) );
        return label;
    }

    private Control getLatestBuildIcon( Composite parent, HudsonJob job )
    {
        Label label = new Label( dialogArea, SWT.NONE );
        label.setImage( HudsonImages.getResultImage( job ) );
        return label;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#getInitialLocation(org.eclipse.swt.graphics.Point)
     */
    protected Point getInitialLocation( Point initialSize )
    {
        Shell parent = getParentShell();
        Point parentSize, parentLocation;

        if ( parent != null )
        {
            parentSize = parent.getSize();
            parentLocation = parent.getLocation();
        }
        else
        {
            Rectangle bounds = getShell().getDisplay().getBounds();
            parentSize = new Point( bounds.width, bounds.height );
            parentLocation = new Point( 0, 0 );
        }
        
        //if possible show the popup window right above the status line component
        
        Point point = label.getLabel().toDisplay( new Point (0, 0) );
        int x = parentSize.x - initialSize.x + parentLocation.x - POPUP_OFFSET;
        int y = parentSize.y - initialSize.y + parentLocation.y - POPUP_OFFSET;
        if ( x > point.x) {
            x = point.x;
        }
        if (y > point.y - initialSize.y) {
            y = point.y - initialSize.y;
        }
        
        // We have to take parent location into account because SWT considers all
        // shell locations to be in display coordinates, even if the shell is parented.
        return new Point( x, y );
    }

    private class LinkListener
        implements SelectionListener
    {

        private HudsonJob job;

        public LinkListener( HudsonJob job )
        {
            this.job = job;
        }

        public void widgetSelected( SelectionEvent e )
        {
            HudsonUtils.openJob( job );
        }

        public void widgetDefaultSelected( SelectionEvent e )
        {
            HudsonUtils.openJob( job );
        }
    }
}
