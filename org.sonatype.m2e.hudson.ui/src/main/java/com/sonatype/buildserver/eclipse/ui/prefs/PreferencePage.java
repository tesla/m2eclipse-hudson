/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package com.sonatype.buildserver.eclipse.ui.prefs;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import com.sonatype.buildserver.eclipse.ui.HudsonUIActivator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class PreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	private BooleanFieldEditor ed;
	private StringFieldEditor subEd;

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(HudsonUIActivator.getDefault().getPreferenceStore());
		setDescription("Hudson UI Preferences");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		ed = new BooleanFieldEditor(
				PreferenceConstants.PREF_TRIGGERED_BY_USER_ONLY,
				"&Only notify incoming builds triggered by current user",
				getFieldEditorParent());
 
		addField( ed );
		subEd = new StringFieldEditor(
				PreferenceConstants.PREF_SCM_USERNAMES, 
				"Comma-separated list of scm user names to match:", 
				getFieldEditorParent()) {
					@Override
					protected void doFillIntoGrid(Composite parent,
							int numColumns) {
						// TODO Auto-generated method stub
						super.doFillIntoGrid(parent, numColumns);
						Label lbl = this.getLabelControl();
				        GridData gd = new GridData();
						gd.horizontalIndent = 24;
						lbl.setLayoutData(gd);
					}
		};
		subEd.setEnabled( HudsonUIActivator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREF_TRIGGERED_BY_USER_ONLY), getFieldEditorParent());
		
		addField (subEd );
		addField(
				new BooleanFieldEditor(
					PreferenceConstants.PREF_SHOW_MESSAGE,
					"&Show a messagebox for incoming builds when IDE not active",
					getFieldEditorParent()) {
					@Override
					protected void doFillIntoGrid(Composite parent,
							int numColumns) {
						super.doFillIntoGrid(parent, numColumns);
						Button lbl = this.getChangeControl(parent);
						GridData gd = (GridData) lbl.getLayoutData();
						gd.verticalIndent = 24;
					}
		});
	}

	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		if (ed != null && !ed.getBooleanValue()) {
			subEd.setEnabled( false, getFieldEditorParent());
		}
		if (ed != null && ed.getBooleanValue()) {
			subEd.setEnabled( true, getFieldEditorParent());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}