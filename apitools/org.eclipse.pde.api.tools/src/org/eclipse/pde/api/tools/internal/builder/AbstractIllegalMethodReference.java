/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.pde.api.tools.internal.model.MethodKey;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Abstract implementation for illegal method references i.e. method calls,
 * constructor invocation, etc
 * 
 * @since 1.1
 */
public abstract class AbstractIllegalMethodReference extends AbstractProblemDetector {

	/**
	 * Map of {@link org.eclipse.pde.api.tools.internal.model.MethodKey} to
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor}
	 */
	private Map<MethodKey, IMethodDescriptor> fIllegalMethods = new HashMap<MethodKey, IMethodDescriptor>();

	/**
	 * Map of
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor}
	 * to associated component IDs
	 */
	private Map<IMethodDescriptor, String> fMethodComponents = new HashMap<IMethodDescriptor, String>();

	/**
	 * Adds the given type as not to be extended.
	 * 
	 * @param type a type that is marked no extend
	 * @param componentId the component the type is located in
	 */
	void addIllegalMethod(IMethodDescriptor method, String componentId) {
		fIllegalMethods.put(new MethodKey(method.getEnclosingType().getQualifiedName(), method.getName(), method.getSignature(), true), method);
		fMethodComponents.put(method, componentId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#
	 * considerReference
	 * (org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	@Override
	public boolean considerReference(IReference reference) {
		MethodKey key = new MethodKey(reference.getReferencedTypeName(), reference.getReferencedMemberName(), reference.getReferencedSignature(), true);
		if (super.considerReference(reference) && fIllegalMethods.containsKey(key)) {
			retainReference(reference);
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected boolean isProblem(IReference reference) {
		if (!super.isProblem(reference)) {
			return false;
		}
		IApiMember method = reference.getResolvedReference();
		String componentId = fMethodComponents.get(method.getHandle());
		// TODO: would it be faster to store component objects and use identity
		// instead of equals?
		return isReferenceFromComponent(reference, componentId);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getElementType
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected int getElementType(IReference reference) {
		return IElementDescriptor.METHOD;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getProblemFlags
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected int getProblemFlags(IReference reference) {
		IApiMethod method = (IApiMethod) reference.getResolvedReference();
		if (method.isConstructor()) {
			return IApiProblem.CONSTRUCTOR_METHOD;
		}
		return IApiProblem.METHOD;
	}

}
