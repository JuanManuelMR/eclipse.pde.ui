/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

/**
 * Editable model is an editable object that can be saved. The classes
 * that implement this interface are responsible for calling the
 * method <code>save</code> of <code>IEditable</code> and supplying
 * the required <code>PrintWriter</code> object.
 * 
 * @since 2.0
 */
public interface IEditableModel extends IEditable {
/**
 * Saves the editable model using the mechanism suitable for the 
 * concrete model implementation. It is responsible for 
 * wrapping the <code>IEditable.save(PrintWriter)</code> operation
 * and providing the print writer.
 */
	void save();
}
