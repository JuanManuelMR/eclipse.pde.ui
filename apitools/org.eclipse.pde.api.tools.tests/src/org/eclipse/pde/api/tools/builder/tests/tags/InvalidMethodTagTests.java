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
package org.eclipse.pde.api.tools.builder.tests.tags;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Test unsupported javadoc tags on methods in classes, interfaces, enums and
 * annotations
 * 
 * @since 1.0
 */
public class InvalidMethodTagTests extends TagTest {

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public InvalidMethodTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(InvalidMethodTagTests.class.getName());
		collectTests(suite);
		return suite;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath
	 * ()
	 */
	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("method"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.builder.tests.tags.TagTest#getDefaultProblemId
	 * ()
	 */
	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiProblem.NO_FLAGS);
	}

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		Class<?>[] classes = new Class[] {
				InvalidAnnotationMethodTagTests.class,
				InvalidEnumMethodTagTests.class,
				InvalidClassMethodTagTests.class,
				InvalidClassConstructorTagTests.class,
				InvalidInterfaceMethodTagTests.class };
		return classes;
	}

	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 * 
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder
		// tests...
		Class<?>[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (int i = 0, length = classes.length; i < length; i++) {
			Class<?> clazz = classes[i];
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite", new Class[0]); //$NON-NLS-1$
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(null, new Object[0]);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				continue;
			}
			suite.addTest((Test) test);
		}
	}
}
