/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itarget.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.FormPage;

public class TargetOutlinePage extends FormOutlinePage {

	public class TargetContentNode {

		private ITarget fTarget;
		private boolean fFeatureBased = false;

		public String toString() {
			return fFeatureBased ? PDEUIMessages.TargetOutlinePage_features : PDEUIMessages.TargetOutlinePage_plugins;
		}

		public TargetContentNode(ITarget target, boolean featureBased) {
			fTarget = target;
			fFeatureBased = featureBased;
		}

		public ITargetObject[] getModels() {
			if (fTarget.useAllPlugins())
				return new ITargetObject[0];
			if (fFeatureBased)
				return fTarget.getFeatures();
			return fTarget.getPlugins();
		}

		public boolean isFeatureBased() {
			return fFeatureBased;
		}

	}

	private TargetContentNode pNode;
	private TargetContentNode fNode;

	public TargetOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	public void modelChanged(IModelChangedEvent event) {
		if (ITarget.P_ALL_PLUGINS.equals(event.getChangedProperty()) || event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			super.modelChanged(event);
			return;
		}

		if (event.getChangeType() == IModelChangedEvent.INSERT || event.getChangeType() == IModelChangedEvent.REMOVE) {
			Object object = event.getChangedObjects()[0];
			if (object instanceof ITargetPlugin)
				getTreeViewer().refresh(pNode);
			else
				getTreeViewer().refresh(fNode);
			return;
		}
	}

	protected Object[] getChildren(Object parent) {
		if (parent instanceof ContentPage) {
			ContentPage page = (ContentPage) parent;
			ITarget target = ((ITargetModel) page.getModel()).getTarget();
			if (target.useAllPlugins())
				return new Object[0];

			if (pNode == null)
				pNode = new TargetContentNode(target, false);
			if (fNode == null)
				fNode = new TargetContentNode(target, true);
			return new Object[] {pNode, fNode};
		}
		if (parent instanceof TargetContentNode)
			return ((TargetContentNode) parent).getModels();
		return new Object[0];
	}

	public ILabelProvider createLabelProvider() {
		return new BasicLabelProvider() {
			public Image getImage(Object element) {
				PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
				if (element instanceof TargetContentNode) {
					if (((TargetContentNode) element).isFeatureBased())
						return provider.get(PDEPluginImages.DESC_FEATURE_OBJ);
					return provider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
				}
				if (element instanceof FormPage)
					return super.getImage(element);
				return provider.getImage(element);
			}
		};
	}

}