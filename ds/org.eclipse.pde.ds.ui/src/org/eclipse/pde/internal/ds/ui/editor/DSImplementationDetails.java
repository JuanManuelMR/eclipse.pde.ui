/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.swt.widgets.Composite;

public class DSImplementationDetails extends DSAbstractDetails {
	
	IDSImplementation fImplementation;

	public DSImplementationDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
	}

	public void createDetails(Composite parent) {
		// TODO Auto-generated method stub
		
	}

	public void hookListeners() {
		// TODO Auto-generated method stub
		
	}

	public void updateFields() {
		// TODO Auto-generated method stub
		
	}

}