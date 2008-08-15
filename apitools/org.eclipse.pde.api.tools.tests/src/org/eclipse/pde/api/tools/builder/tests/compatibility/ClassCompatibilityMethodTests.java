/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder correctly reports compatibility problems
 * for classes.
 * 
 * @since 3.4
 */
public class ClassCompatibilityMethodTests extends ClassCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("org.eclipse.api.tools.tests.compatability.a/src/a/classes/methods");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.methods.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityMethodTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("methods");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityMethodTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.METHOD);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests the removal of a public method from an API class.
	 */
	private void xRemovePublicAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethod.java");
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethod", "publicMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class - incremental.
	 */
	public void testRemovePublicAPIMethodI() throws Exception {
		xRemovePublicAPIMethod(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class - full.
	 */
	public void testRemovePublicAPIMethodF() throws Exception {
		xRemovePublicAPIMethod(false);
	}
	
	/**
	 * Tests the removal of 2 public methods from an API class - incremental.
	 */
	public void testRemoveTwoPublicAPIMethodsI() throws Exception {
		xRemoveTwoPublicAPIMethods(true);
	}	
	
	/**
	 * Tests the removal of 2 public methods from an API class - full.
	 */
	public void testRemoveTwoPublicAPIMethodsF() throws Exception {
		xRemoveTwoPublicAPIMethods(false);
	}	
	
	/**
	 * Tests the removal of a public method from an API class - incremental.
	 */
	private void xRemoveTwoPublicAPIMethods(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveTwoPublicMethods.java");
		int[] ids = new int[] {
			getDefaultProblemId(),
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveTwoPublicMethods", "methodOne(String)"};
		args[1] = new String[]{PACKAGE_PREFIX + "RemoveTwoPublicMethods", "methodTwo(int)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class.
	 */
	private void xRemoveProtectedAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethod.java");
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedMethod", "protectedMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class - incremental.
	 */
	public void testRemoveProtectedAPIMethodI() throws Exception {
		xRemoveProtectedAPIMethod(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class - full.
	 */
	public void testRemoveProtectedAPIMethodF() throws Exception {
		xRemoveProtectedAPIMethod(false);
	}
	
	/**
	 * Tests the removal of a private method from an API class.
	 */
	private void xRemovePrivateAPIMethod(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePrivateMethod.java");
		// there are no expected problems
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class - incremental.
	 */
	public void testRemovePrivateAPIMethodI() throws Exception {
		xRemovePrivateAPIMethod(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class - full.
	 */
	public void testRemovePrivateAPIMethodF() throws Exception {
		xRemovePrivateAPIMethod(false);
	}	
	
	/**
	 * Tests the removal of a public method from an API class annotated as noextend - incremental.
	 */
	private void xRemovePublicAPIMethodNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoExtend.java");
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoExtend", "publicMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class annotated as noextend - incremental.
	 */
	public void testRemovePublicAPIMethodNoExtendI() throws Exception {
		xRemovePublicAPIMethodNoExtend(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class annotated as noextend - full.
	 */
	public void testRemovePublicAPIMethodNoExtendF() throws Exception {
		xRemovePublicAPIMethodNoExtend(false);
	}
		
	/**
	 * Tests the removal of a protected method from an API class annotated as noextend.
	 */
	private void xRemoveProtectedAPIMethodNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoExtend.java");
		// no problems expected since the method is not accessible
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class annotated as noextend - incremental.
	 */
	public void testRemoveProtectedAPIMethodNoExtendI() throws Exception {
		xRemoveProtectedAPIMethodNoExtend(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class annotated as noextend - full.
	 */
	public void testRemoveProtectedAPIMethodNoExtendF() throws Exception {
		xRemoveProtectedAPIMethodNoExtend(false);
	}	
	
	/**
	 * Tests the removal of a public method from an API class annotated as noinstantiate - incremental.
	 */
	private void xRemovePublicAPIMethodNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoInstantiate.java");
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoInstantiate", "publicMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class annotated as noinstantiate - incremental.
	 */
	public void testRemovePublicAPIMethodNoInstantiateI() throws Exception {
		xRemovePublicAPIMethodNoInstantiate(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class annotated as noinstantiate - full.
	 */
	public void testRemovePublicAPIMethodNoInstantiateF() throws Exception {
		xRemovePublicAPIMethodNoInstantiate(false);
	}
		
	/**
	 * Tests the removal of a protected method from an API class annotated as noinstantiate.
	 */
	private void xRemoveProtectedAPIMethodNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoInstantiate.java");
		int[] ids = new int[] {
				getDefaultProblemId()
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedMethodNoInstantiate", "protectedMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class annotated as noinstantiate - incremental.
	 */
	public void testRemoveProtectedAPIMethodNoInstantiateI() throws Exception {
		xRemoveProtectedAPIMethodNoInstantiate(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class annotated as noinstantiate - full.
	 */
	public void testRemoveProtectedAPIMethodNoInstantiateF() throws Exception {
		xRemoveProtectedAPIMethodNoInstantiate(false);
	}
	
	/**
	 * Tests the removal of a public method from an API class annotated as
	 * noextend and noinstantiate.
	 */
	private void xRemovePublicAPIMethodNoExtendNoInstatiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoExtendNoInstantiate.java");
		int[] ids = new int[] {
			getDefaultProblemId()
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoExtendNoInstantiate", "publicMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class annotated as noextend 
	 * and noinstantiate - incremental.
	 */
	public void testRemovePublicAPIMethodNoExtendNoInstantiateI() throws Exception {
		xRemovePublicAPIMethodNoExtendNoInstatiate(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class annotated as noextend
	 * and noinstantiate - full.
	 */
	public void testRemovePublicAPIMethodNoExtendNoInstantiateF() throws Exception {
		xRemovePublicAPIMethodNoExtendNoInstatiate(false);
	}
	
	/**
	 * Tests the removal of a public method from an API class annotated as
	 * noextend and noinstantiate.
	 */
	private void xRemoveProtectedAPIMethodNoExtendNoInstatiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoExtendNoInstantiate.java");
		// no problems expected due to noextend
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class annotated as noextend 
	 * and noinstantiate - incremental.
	 */
	public void testRemoveProtectedAPIMethodNoExtendNoInstantiateI() throws Exception {
		xRemoveProtectedAPIMethodNoExtendNoInstatiate(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class annotated as noextend
	 * and noinstantiate - full.
	 */
	public void testRemoveProtectedAPIMethodNoExtendNoInstantiateF() throws Exception {
		xRemoveProtectedAPIMethodNoExtendNoInstatiate(false);
	}	
	
	/**
	 * Tests the removal of a public method from an API class tagged noreference.
	 */
	private void xRemovePublicAPIMethodNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoReference.java");
		// no problems since no references allowed
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class tagged noreference - incremental.
	 */
	public void testRemovePublicAPIMethodNoReferenceI() throws Exception {
		xRemovePublicAPIMethodNoReference(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class tagged noreference - full.
	 */
	public void testRemovePublicAPIMethodNoReferencF() throws Exception {
		xRemovePublicAPIMethodNoReference(false);
	}
	
	/**
	 * Tests the removal of a protected method from an API class tagged noreference.
	 */
	private void xRemoveProtectedAPIMethodNoReference(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoReference.java");
		// no problems since no references allowed
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class tagged noreference - incremental.
	 */
	public void testRemoveProtectedAPIMethodNoReferenceI() throws Exception {
		xRemoveProtectedAPIMethodNoReference(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class tagged noreference - full.
	 */
	public void testRemoveProtectedAPIMethodNoReferencF() throws Exception {
		xRemoveProtectedAPIMethodNoReference(false);
	}
	
	/**
	 * Tests the removal of a public method from an API class tagged no override.
	 */
	private void xRemovePublicAPIMethodNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemovePublicMethodNoOverride.java");
		int[] ids = new int[] {
				getDefaultProblemId()
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemovePublicMethodNoOverride", "publicMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a public method from an API class tagged no override - incremental.
	 */
	public void testRemovePublicAPIMethodNoOverrideI() throws Exception {
		xRemovePublicAPIMethodNoOverride(true);
	}	
	
	/**
	 * Tests the removal of a public method from an API class tagged no override - full.
	 */
	public void testRemovePublicAPIMethodNoOverrideF() throws Exception {
		xRemovePublicAPIMethodNoOverride(false);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class tagged no override.
	 */
	private void xRemoveProtectedAPIMethodNoOverride(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("RemoveProtectedMethodNoOverride.java");
		int[] ids = new int[] {
				getDefaultProblemId()
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{PACKAGE_PREFIX + "RemoveProtectedMethodNoOverride", "protectedMethod(String)"};
		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	/**
	 * Tests the removal of a protected method from an API class tagged no override - incremental.
	 */
	public void testRemoveProtectedAPIMethodNoOverrideI() throws Exception {
		xRemoveProtectedAPIMethodNoOverride(true);
	}	
	
	/**
	 * Tests the removal of a protected method from an API class tagged no override - full.
	 */
	public void testRemoveProtectedAPIMethodNoOverrideF() throws Exception {
		xRemoveProtectedAPIMethodNoOverride(false);
	}	
}