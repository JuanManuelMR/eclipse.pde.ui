/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.OSGiFrameworkManager;

/**
 * A launch delegate for launching OSGi frameworks
 * <p>
 * Clients may subclass and instantiate this class.
 * </p>
 * @since 3.3
 */
public class OSGiLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	/**
	 * Delegates to the launcher delegate associated with the OSGi framwork
	 * selected in the launch configuration.
	 * 
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		OSGiFrameworkManager manager = PDEPlugin.getDefault().getOSGiFrameworkManager();
		String id = configuration.getAttribute(IPDELauncherConstants.OSGI_FRAMEWORK_ID, manager.getDefaultFramework());
		LaunchConfigurationDelegate launcher = manager.getFrameworkLauncher(id);
		if (launcher != null) {
			launcher.launch(configuration, mode, launch, monitor);
		} else {
			String name = manager.getFrameworkName(id);
			if (name == null)
				name = PDEUIMessages.OSGiLaunchConfiguration_selected;
			String message = NLS.bind(PDEUIMessages.OSGiLaunchConfiguration_cannotFindLaunchConfiguration, name);
			IStatus status = new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.OK, message, null);
			throw new CoreException(status);
		}
	}

}