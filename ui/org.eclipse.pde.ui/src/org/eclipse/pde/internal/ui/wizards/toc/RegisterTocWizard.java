/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.toc;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * NewRegisterCSWizard
 *
 */
public class RegisterTocWizard extends Wizard implements INewWizard {

	private RegisterTocWizardPage fMainPage;

	private IModel fModel;

	/**
	 * 
	 */
	public RegisterTocWizard(IModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new RegisterTocWizardPage(fModel);
		addPage(fMainPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			getContainer().run(false, true, getOperation());
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/**
	 * @return
	 */
	private IRunnableWithProgress getOperation() {
		return new RegisterTocOperation(fMainPage, getShell());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(PDEUIMessages.RegisterTocWizard_registerTOC);
		// TODO: MP: LOW: TOC: New register table of contents wizard image
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_CHEATSHEET_WIZ);
		setNeedsProgressMonitor(true);
	}

}