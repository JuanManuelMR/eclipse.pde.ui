/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;

public abstract class XMLSourcePage extends PDEProjectionSourcePage {

	public XMLSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		setRangeIndicator(new DefaultRangeIndicator());
	}

	public boolean canLeaveThePage() {
		boolean cleanModel = getInputContext().isModelCorrect();
		if (!cleanModel) {
			Display.getCurrent().beep();
			String title = getEditor().getSite().getRegisteredName();
			MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), title, PDEUIMessages.SourcePage_errorMessage);
		}
		return cleanModel;
	}

	protected String[] collectContextMenuPreferencePages() {
		String[] ids = super.collectContextMenuPreferencePages();
		String[] more = new String[ids.length + 1];
		more[0] = "org.eclipse.pde.ui.EditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}

}