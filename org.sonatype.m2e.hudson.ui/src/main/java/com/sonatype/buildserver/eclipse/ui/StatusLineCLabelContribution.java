/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.sonatype.buildserver.eclipse.ui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * From org.eclipse.equinox.internal.p2.ui.sdk.scheduler.StatusLineCLabelContribution
 */
public class StatusLineCLabelContribution
    extends ContributionItem
{
    private static Logger log = LoggerFactory.getLogger( StatusLineCLabelContribution.class );
    
    private IStatusLineManager statusLineManager;

    public final static int DEFAULT_CHAR_WIDTH = 40;

    private CLabel label;

    private Image image;

    private String text = ""; //$NON-NLS-1$

    private String tooltip;

    private List<EventTuple> events = new ArrayList<EventTuple>(); 

    public StatusLineCLabelContribution( String id )
    {
        super( id );
        setVisible( false ); // no text to start with
    }
    
    CLabel getLabel() {
        return label;
    }

    public void fill( Composite parent )
    {
        label = new CLabel( parent, SWT.DEFAULT );

        label.setText( stripText( text ) );
        label.setImage( image );
        for (EventTuple tuple : events) {
            label.addListener( tuple.type, tuple.listener );
        }
        if ( tooltip != null )
        {
            label.setToolTipText( tooltip );
        }
    }

    public void addListener( int type, Listener labelListener )
    {
        events.add( new EventTuple(type, labelListener ) );
    }

    public void setText( String text )
    {
        if ( text == null )
            throw new NullPointerException();

        this.text = text;

        if ( label != null && !label.isDisposed() )
            label.setText( stripText( text ));

        if ( this.text.length() == 0 )
        {
            if ( isVisible() )
            {
                setVisible( false );
                IContributionManager contributionManager = getParent();

                if ( contributionManager != null )
                    contributionManager.update( true );
            }
        }
        else
        {
            if ( !isVisible() )
            {
                setVisible( true );
                IContributionManager contributionManager = getParent();

                if ( contributionManager != null )
                    contributionManager.update( true );
            }
        }
    }
    
    private String stripText(String original) {
        if (original.length() > 20) {
            return original.substring( 0, 17) + "...";
        }
        return original;
    }

    public void setTooltip( String tooltip )
    {
        if ( tooltip == null )
            throw new NullPointerException();

        this.tooltip = tooltip;

        if ( label != null && !label.isDisposed() )
        {
            label.setToolTipText( this.tooltip );
        }
    }

    public void setImage( Image image )
    {
        if ( image == null )
            throw new NullPointerException();

        this.image = image;
        if ( label != null && !label.isDisposed() ) {
            label.setImage( this.image );
        }

        if ( !isVisible() )
        {
            setVisible( true );
            IContributionManager contributionManager = getParent();

            if ( contributionManager != null ) {
                contributionManager.update( true );
            }
        }
    }

    /*
     * From org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdater
     */
    IStatusLineManager getStatusLineManager()
    {
        if ( statusLineManager != null )
            return statusLineManager;
        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if ( activeWindow == null )
            return null;

        try
        {
            Method method = activeWindow.getClass().getDeclaredMethod( "getStatusLineManager", new Class[0] ); //$NON-NLS-1$
            try
            {
                Object statusLine = method.invoke( activeWindow, new Object[0] );
                if ( statusLine instanceof IStatusLineManager )
                {
                    statusLineManager = (IStatusLineManager) statusLine;
                    return statusLineManager;
                }
            }
            catch ( InvocationTargetException e )
            {
                log.error( "Could not find getStatusLineManagerMethod", e ); //$NON-NLS-1$
            }
            catch ( IllegalAccessException e )
            {
                log.error( "Could not find getStatusLineManagerMethod", e ); //$NON-NLS-1$
            }
        }
        catch ( NoSuchMethodException e )
        {
            log.error( "Could not find getStatusLineManagerMethod", e ); //$NON-NLS-1$
        }

        IWorkbenchPartSite site = activeWindow.getActivePage().getActivePart().getSite();
        if ( site instanceof IViewSite )
        {
            statusLineManager = ( (IViewSite) site ).getActionBars().getStatusLineManager();
        }
        else if ( site instanceof IEditorSite )
        {
            statusLineManager = ( (IEditorSite) site ).getActionBars().getStatusLineManager();
        }
        return statusLineManager;
    }
    
    private class EventTuple {
        final int type;
        final Listener listener;
        public EventTuple( int type, Listener labelListener )
        {
            this.type = type;
            this.listener = labelListener;
        }
    }
}