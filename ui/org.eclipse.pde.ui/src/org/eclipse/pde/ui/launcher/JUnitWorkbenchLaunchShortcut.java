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
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.ui.launcher.LauncherUtils;

/**
 * A launch shortcut capable of launching a Plug-in JUnit test.
 * <p>
 * This class may be substantiated or subclassed by clients.
 * </p>
 * @since 3.3
 */
public class JUnitWorkbenchLaunchShortcut extends JUnitLaunchShortcut {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.JUnitLaunchShortcut#getLaunchConfigurationTypeId()
	 */
	protected String getLaunchConfigurationTypeId() {
		return "org.eclipse.pde.ui.JunitLaunchConfig"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.JUnitLaunchShortcut#createLaunchConfiguration(org.eclipse.jdt.core.IJavaElement)
	 */
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy configuration = super.createLaunchConfiguration(element);
		if (TargetPlatformHelper.usesNewApplicationModel())
			configuration.setAttribute(IPDEUIConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$ 
		else if (TargetPlatformHelper.getTargetVersion() >= 3.2)
			configuration.setAttribute(IPDEUIConstants.LAUNCHER_PDE_VERSION, "3.2a"); //$NON-NLS-1$
		configuration.setAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultJUnitWorkspaceLocation());
		configuration.setAttribute(IPDELauncherConstants.DOCLEAR, true);
		configuration.setAttribute(IPDELauncherConstants.ASKCLEAR, false);
		configuration.setAttribute(IPDEUIConstants.APPEND_ARGS_EXPLICITLY, true);

		// Program to launch
		if (LauncherUtils.requiresUI(configuration)) {
			String product = TargetPlatform.getDefaultProduct();
			if (product != null) {
				configuration.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
				configuration.setAttribute(IPDELauncherConstants.PRODUCT, product);
			}
		} else {
			configuration.setAttribute(IPDELauncherConstants.APPLICATION, IPDEUIConstants.CORE_TEST_APPLICATION);
		}

		// Plug-ins to launch
		configuration.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);

		// Program arguments
		String programArgs = LaunchArgumentsHelper.getInitialProgramArguments();
		if (programArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);

		// VM arguments
		String vmArgs = LaunchArgumentsHelper.getInitialVMArguments();
		if (vmArgs.length() > 0)
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);

		// configuration attributes
		configuration.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, false);
		configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, LaunchArgumentsHelper.getDefaultJUnitConfigurationLocation());
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, true);

		// tracing option
		configuration.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);

		// source path provider
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);

		return configuration;
	}

}