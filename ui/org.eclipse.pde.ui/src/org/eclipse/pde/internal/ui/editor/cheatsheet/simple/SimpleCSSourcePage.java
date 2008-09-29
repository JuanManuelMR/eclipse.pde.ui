/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Les Jones <lesojones@gmail.com> - Bug 214511
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.icheatsheet.simple.*;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSPreviewAction;

/**
 * SimpleCSSourcePage
 *
 */
public class SimpleCSSourcePage extends XMLSourcePage {

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public SimpleCSSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#isQuickOutlineEnabled()
	 */
	public boolean isQuickOutlineEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineComparator()
	 */
	public ViewerComparator createOutlineComparator() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineContentProvider()
	 */
	public ITreeContentProvider createOutlineContentProvider() {
		return new SimpleCSContentProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#createOutlineLabelProvider()
	 */
	public ILabelProvider createOutlineLabelProvider() {
		return PDEPlugin.getDefault().getLabelProvider();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#isSelectionListener()
	 */
	protected boolean isSelectionListener() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#updateSelection(java.lang.Object)
	 */
	public void updateSelection(Object object) {
		if ((object instanceof IDocumentElementNode) && (((IDocumentElementNode) object).isErrorNode() == false)) {
			setSelectedObject(object);
			setHighlightRange((IDocumentElementNode) object, true);
			setSelectedRange((IDocumentElementNode) object, false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#findRange()
	 */
	protected IDocumentRange findRange() {

		Object selectedObject = getSelection();

		if (selectedObject instanceof IDocumentElementNode) {
			return (IDocumentElementNode) selectedObject;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#getRangeElement(int, boolean)
	 */
	public IDocumentRange getRangeElement(int offset, boolean searchChildren) {
		IDocumentElementNode rootNode = ((SimpleCSModel) getInputContext().getModel()).getSimpleCS();
		return findNode(rootNode, offset, searchChildren);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESourcePage#synchronizeOutlinePage(int)
	 */
	protected void synchronizeOutlinePage(int offset) {
		IDocumentRange range = getRangeElement(offset, true);
		updateHighlightRange(range);
		range = adaptRange(range);
		updateOutlinePageSelection(range);
	}

	/**
	 * @param range
	 */
	public IDocumentRange adaptRange(IDocumentRange range) {
		// Adapt the range to node that is viewable in the outline view
		if (range instanceof IDocumentAttributeNode) {
			// Attribute
			return adaptRange(((IDocumentAttributeNode) range).getEnclosingElement());
		} else if (range instanceof IDocumentTextNode) {
			// Content
			return adaptRange(((IDocumentTextNode) range).getEnclosingElement());
		} else if (range instanceof IDocumentElementNode) {
			// Element
			if (range instanceof ISimpleCS) {
				return range;
			} else if (range instanceof ISimpleCSItem) {
				return range;
			} else if (range instanceof ISimpleCSSubItemObject) {
				return range;
			} else if (range instanceof ISimpleCSIntro) {
				return range;
			} else if (range instanceof ISimpleCSPerformWhen) {
				return range;
			} else {
				return adaptRange(((IDocumentElementNode) range).getParentNode());
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		// Get the editor
		PDEFormEditor editor = (PDEFormEditor) getEditor();
		// Get the form editor contributor
		SimpleCSEditorContributor contributor = (SimpleCSEditorContributor) editor.getContributor();
		// Get the model
		// TODO: MP: SimpleCS:  Preview does not show unsaved changes made to source page, 
		// check if fixed after implementing text edit operations
		ISimpleCSModel model = (ISimpleCSModel) editor.getAggregateModel();
		// Get the preview action
		SimpleCSPreviewAction previewAction = contributor.getPreviewAction();
		// Set the cheat sheet object
		previewAction.setDataModelObject(model.getSimpleCS());
		// Set the editor input
		previewAction.setEditorInput(editor.getEditorInput());
		// Add the preview action to the context menu
		menu.add(previewAction);
		menu.add(new Separator());
		super.editorContextMenuAboutToShow(menu);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#setPartName(java.lang.String)
	 */
	protected void setPartName(String partName) {
		super.setPartName(PDEUIMessages.EditorSourcePage_name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextEditor#initializeEditor()
	 */
	protected void initializeEditor() {
		super.initializeEditor();
		setHelpContextId(IHelpContextIds.SIMPLE_CS_EDITOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEProjectionSourcePage#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (IHyperlinkDetector.class.equals(adapter)) {
			return new SimpleCSHyperlinkDetector(this);
		}
		return super.getAdapter(adapter);
	}

}