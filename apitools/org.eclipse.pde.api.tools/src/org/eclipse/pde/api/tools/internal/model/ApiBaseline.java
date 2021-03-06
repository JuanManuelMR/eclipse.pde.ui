/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.PropertyChangeEvent;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.api.tools.internal.AnyValue;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.CoreMessages;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import com.ibm.icu.text.MessageFormat;

/**
 * Implementation of an {@link IApiBaseline}
 * 
 * @since 1.0
 */
public class ApiBaseline extends ApiElement implements IApiBaseline, IVMInstallChangedListener {

	/**
	 * Empty array of component
	 */
	private static final IApiComponent[] EMPTY_COMPONENTS = new IApiComponent[0];

	/**
	 * OSGi bundle state
	 */
	private State fState;

	/**
	 * Execution environment identifier
	 */
	private String fExecutionEnvironment;

	/**
	 * Component representing the system library
	 */
	private IApiComponent fSystemLibraryComponent;

	/**
	 * Whether an execution environment should be automatically resolved as API
	 * components are added.
	 */
	private boolean fAutoResolve = false;

	/**
	 * Contains the location of the baseline if the baseline was created with a
	 * location.
	 */
	private String fLocation;
	/**
	 * Execution environment status
	 */
	private IStatus fEEStatus = null;

	/**
	 * Constant to match any value for ws, os, arch.
	 */
	private AnyValue ANY_VALUE = new AnyValue("*"); //$NON-NLS-1$

	/**
	 * Cache of resolved packages.
	 * <p>
	 * Map of <code>PackageName -> Map(componentName -> IApiComponent[])</code>
	 * </p>
	 * For each package the cache contains a map of API components that provide
	 * that package, by source component name (including the <code>null</code>
	 * component name).
	 */
	private HashMap<String, HashMap<IApiComponent, IApiComponent[]>> fComponentsProvidingPackageCache = null;

	/**
	 * Maps component id's to components.
	 * <p>
	 * Map of <code>componentId -> {@link IApiComponent}</code>
	 * </p>
	 */
	private HashMap<String, IApiComponent> fComponentsById = null;
	/**
	 * Maps project name's to components.
	 * <p>
	 * Map of <code>project name -> {@link IApiComponent}</code>
	 * </p>
	 */
	private HashMap<String, IApiComponent> fComponentsByProjectNames = null;
	/**
	 * Cache of system package names
	 */
	private HashSet<String> fSystemPackageNames = null;

	/**
	 * The VM install this baseline is bound to for system libraries or
	 * <code>null</code>. Only used in the IDE when OSGi is running.
	 */
	private IVMInstall fVMBinding = null;

	/**
	 * Constructs a new API baseline with the given name.
	 * 
	 * @param name baseline name
	 */
	public ApiBaseline(String name) {
		super(null, IApiElement.BASELINE, name);
		fAutoResolve = true;
		fEEStatus = new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, CoreMessages.ApiBaseline_0);
	}

	/**
	 * Constructs a new API baseline with the given attributes.
	 * 
	 * @param name baseline name
	 * @param eeDescriptoin execution environment description file
	 * @throws CoreException if unable to create a baseline with the given
	 *             attributes
	 */
	public ApiBaseline(String name, File eeDescription) throws CoreException {
		this(name, eeDescription, null);
	}

	/**
	 * Constructs a new API baseline with the given attributes.
	 * 
	 * @param name baseline name
	 * @param eeDescriptoin execution environment description file
	 * @param location the given baseline location
	 * @throws CoreException if unable to create a baseline with the given
	 *             attributes
	 */
	public ApiBaseline(String name, File eeDescription, String location) throws CoreException {
		this(name);
		if (eeDescription != null) {
			fAutoResolve = false;
			ExecutionEnvironmentDescription ee = new ExecutionEnvironmentDescription(eeDescription);
			String profile = ee.getProperty(ExecutionEnvironmentDescription.CLASS_LIB_LEVEL);
			initialize(ee);
			fEEStatus = new Status(IStatus.OK, ApiPlugin.PLUGIN_ID, MessageFormat.format(CoreMessages.ApiBaseline_1, new Object[] { profile }));
		}
		this.fLocation = location;
	}

	/**
	 * Initializes this baseline to resolve in the execution environment
	 * associated with the given description.
	 * 
	 * @param ee execution environment description
	 * @throws CoreException if unable to initialize based on the given id
	 */
	private void initialize(ExecutionEnvironmentDescription ee) throws CoreException {
		Properties properties = null;
		String environmentId = ee.getProperty(ExecutionEnvironmentDescription.CLASS_LIB_LEVEL);
		if (ApiPlugin.isRunningInFramework()) {
			properties = getJavaProfileProperties(environmentId);
		} else {
			properties = Util.getEEProfile(environmentId);
		}
		if (properties == null) {
			abort("Unknown execution environment: " + environmentId, null); //$NON-NLS-1$
		} else {
			initialize(properties, ee);
		}
	}

	/**
	 * Returns the property file for the given environment or <code>null</code>.
	 * 
	 * @param ee execution environment symbolic name
	 * @return properties file or <code>null</code> if none
	 */
	public static Properties getJavaProfileProperties(String ee) {
		Bundle osgiBundle = Platform.getBundle("org.eclipse.osgi"); //$NON-NLS-1$
		if (osgiBundle == null) {
			return null;
		}
		URL profileURL = osgiBundle.getEntry(ee.replace('/', '_') + ".profile"); //$NON-NLS-1$
		if (profileURL != null) {
			InputStream is = null;
			try {
				profileURL = FileLocator.resolve(profileURL);
				URLConnection openConnection = profileURL.openConnection();
				openConnection.setUseCaches(false);
				is = openConnection.getInputStream();
				if (is != null) {
					Properties profile = new Properties();
					profile.load(is);
					return profile;
				}
			} catch (IOException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return null;
	}

	/**
	 * Initializes this baseline from the given properties.
	 * 
	 * @param profile OGSi profile properties
	 * @param description execution environment description
	 * @throws CoreException if unable to initialize
	 */
	private void initialize(Properties profile, ExecutionEnvironmentDescription description) throws CoreException {
		String value = profile.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
		String[] systemPackages = null;
		if (value != null) {
			systemPackages = value.split(","); //$NON-NLS-1$
		}
		if (!(this instanceof WorkspaceBaseline)) {
			Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
			dictionary.put(Constants.FRAMEWORK_SYSTEMPACKAGES, value);
			value = profile.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
			if (value != null) {
				dictionary.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, value);
			}
			fExecutionEnvironment = profile.getProperty("osgi.java.profile.name"); //$NON-NLS-1$
			if (fExecutionEnvironment == null) {
				abort("Profile file missing 'osgi.java.profile.name'", null); //$NON-NLS-1$
			}
			dictionary.put("osgi.os", ANY_VALUE); //$NON-NLS-1$
			dictionary.put("osgi.arch", ANY_VALUE); //$NON-NLS-1$
			dictionary.put("osgi.ws", ANY_VALUE); //$NON-NLS-1$
			dictionary.put("osgi.nl", ANY_VALUE); //$NON-NLS-1$

			getState().setPlatformProperties(dictionary);
		}
		// clean up previous system library
		if (fSystemLibraryComponent != null && fComponentsById != null) {
			fComponentsById.remove(fSystemLibraryComponent.getSymbolicName());
		}
		if (fSystemPackageNames != null) {
			fSystemPackageNames.clear();
			fSystemPackageNames = null;
		}
		clearComponentsCache();
		// set new system library
		fSystemLibraryComponent = new SystemLibraryApiComponent(this, description, systemPackages);
		addComponent(fSystemLibraryComponent);
	}

	/**
	 * Clears the package -> components cache and sets it to <code>null</code>
	 */
	private synchronized void clearComponentsCache() {
		if (fComponentsProvidingPackageCache != null) {
			fComponentsProvidingPackageCache.clear();
			fComponentsProvidingPackageCache = null;
		}
	}

	/**
	 * Adds an {@link IApiComponent} to the fComponentsById mapping
	 * 
	 * @param component
	 */
	protected void addComponent(IApiComponent component) {
		if (component == null) {
			return;
		}
		if (fComponentsById == null) {
			fComponentsById = new HashMap<String, IApiComponent>();
		}
		fComponentsById.put(component.getSymbolicName(), component);
		if (component instanceof ProjectComponent) {
			ProjectComponent projectApiComponent = (ProjectComponent) component;
			if (this.fComponentsByProjectNames == null) {
				this.fComponentsByProjectNames = new HashMap<String, IApiComponent>();
			}
			this.fComponentsByProjectNames.put(projectApiComponent.getJavaProject().getProject().getName(), component);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * IApiBaseline#addApiComponents(org.eclipse.pde.api.tools.model.component
	 * .IApiComponent[], boolean)
	 */
	@Override
	public void addApiComponents(IApiComponent[] components) throws CoreException {
		HashSet<String> ees = new HashSet<String>();
		for (int i = 0; i < components.length; i++) {
			BundleComponent component = (BundleComponent) components[i];
			if (component.isSourceComponent()) {
				continue;
			}
			BundleDescription description = component.getBundleDescription();
			getState().addBundle(description);
			addComponent(component);
			ees.addAll(Arrays.asList(component.getExecutionEnvironments()));
		}
		resolveSystemLibrary(ees);
		getState().resolve();
	}

	/**
	 * Resolves and initializes the system library to use based on API component
	 * requirements. Only works when running in the framework. Has no effect if
	 * not running in the framework.
	 */
	protected void resolveSystemLibrary(HashSet<String> ees) {
		if (ApiPlugin.isRunningInFramework() && fAutoResolve) {
			IStatus error = null;
			IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
			Map<IVMInstall, Set<String>> VMsToEEs = new HashMap<IVMInstall, Set<String>>();
			for (String ee : ees) {
				IExecutionEnvironment environment = manager.getEnvironment(ee);
				if (environment != null) {
					IVMInstall[] compatibleVMs = environment.getCompatibleVMs();
					for (int i = 0; i < compatibleVMs.length; i++) {
						IVMInstall vm = compatibleVMs[i];
						Set<String> EEs = VMsToEEs.get(vm);
						if (EEs == null) {
							EEs = new HashSet<String>();
							VMsToEEs.put(vm, EEs);
						}
						EEs.add(ee);
					}
				}
			}
			// select VM that is compatible with most required environments
			IVMInstall bestFit = null;
			int bestCount = 0;
			for (Entry<IVMInstall, Set<String>> entry : VMsToEEs.entrySet()) {
				Set<String> EEs = entry.getValue();
				if (EEs.size() > bestCount) {
					bestCount = EEs.size();
					bestFit = entry.getKey();
				}
			}
			String systemEE = null;
			if (bestFit != null) {
				// find the EE this VM is strictly compatible with
				IExecutionEnvironment[] environments = manager.getExecutionEnvironments();
				for (int i = 0; i < environments.length; i++) {
					IExecutionEnvironment environment = environments[i];
					if (environment.isStrictlyCompatible(bestFit)) {
						systemEE = environment.getId();
						break;
					}
				}
				if (systemEE == null) {
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=383261
					// we don't need to compute anything here, in all cases if
					// we fail to find a compatible EE, fall back to highest
					// known.
					// TODO this should be updated for each new EE that gets
					// added
					systemEE = "JavaSE-1.7"; //$NON-NLS-1$
				}
				// only update if different from current or missing VM binding
				if (!systemEE.equals(getExecutionEnvironment()) || fVMBinding == null) {
					try {
						File file = Util.createEEFile(bestFit, systemEE);
						JavaRuntime.addVMInstallChangedListener(this);
						fVMBinding = bestFit;
						ExecutionEnvironmentDescription ee = new ExecutionEnvironmentDescription(file);
						initialize(ee);
					} catch (CoreException e) {
						error = new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, CoreMessages.ApiBaseline_2, e);
					} catch (IOException e) {
						error = new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, CoreMessages.ApiBaseline_2, e);
					}
				}
			} else {
				// no VMs match any required EE
				error = new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, CoreMessages.ApiBaseline_6);
			}
			if (error == null) {
				// build status for unbound required EE's
				Set<String> missing = new HashSet<String>(ees);
				Set<String> covered = new HashSet<String>(VMsToEEs.get(bestFit));
				missing.removeAll(covered);
				if (missing.isEmpty()) {
					fEEStatus = new Status(IStatus.OK, ApiPlugin.PLUGIN_ID, MessageFormat.format(CoreMessages.ApiBaseline_1, new Object[] { systemEE }));
				} else {
					MultiStatus multi = new MultiStatus(ApiPlugin.PLUGIN_ID, 0, CoreMessages.ApiBaseline_4, null);
					for (String id : missing) {
						multi.add(new Status(IStatus.WARNING, ApiPlugin.PLUGIN_ID, MessageFormat.format(CoreMessages.ApiBaseline_5, new Object[] { id })));
					}
					fEEStatus = multi;
				}
			} else {
				fEEStatus = error;
			}
		}
	}

	/**
	 * Returns true if the {@link IApiBaseline} has its information loaded
	 * (components) false otherwise. This is a handle only method that will not
	 * load information from disk.
	 * 
	 * @return true if the {@link IApiBaseline} has its information loaded
	 *         (components) false otherwise.
	 */
	public boolean peekInfos() {
		return fComponentsById != null;
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#getApiComponents()
	 */
	@Override
	public IApiComponent[] getApiComponents() {
		loadBaselineInfos();
		if (fComponentsById == null) {
			return EMPTY_COMPONENTS;
		}
		Collection<IApiComponent> values = fComponentsById.values();
		return values.toArray(new IApiComponent[values.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#resolvePackage(IApiComponent, String)
	 */
	@Override
	public synchronized IApiComponent[] resolvePackage(IApiComponent sourceComponent, String packageName) throws CoreException {
		HashMap<IApiComponent, IApiComponent[]> componentsForPackage = null;
		if (fComponentsProvidingPackageCache != null) {
			componentsForPackage = fComponentsProvidingPackageCache.get(packageName);
		} else {
			fComponentsProvidingPackageCache = new HashMap<String, HashMap<IApiComponent, IApiComponent[]>>(8);
		}
		IApiComponent[] cachedComponents = null;
		if (componentsForPackage != null) {
			cachedComponents = componentsForPackage.get(sourceComponent);
			if (cachedComponents != null && cachedComponents.length > 0) {
				return cachedComponents;
			}
		} else {
			componentsForPackage = new HashMap<IApiComponent, IApiComponent[]>(8);
			fComponentsProvidingPackageCache.put(packageName, componentsForPackage);
		}
		// check system packages first
		if (isSystemPackage(packageName)) {
			if (fSystemLibraryComponent != null) {
				cachedComponents = new IApiComponent[] { fSystemLibraryComponent };
			} else {
				return EMPTY_COMPONENTS;
			}
		} else {
			if (sourceComponent != null) {
				ArrayList<IApiComponent> componentsList = new ArrayList<IApiComponent>();
				resolvePackage0(sourceComponent, packageName, componentsList);
				if (componentsList.size() != 0) {
					cachedComponents = new IApiComponent[componentsList.size()];
					componentsList.toArray(cachedComponents);
				}
			}
		}
		if (cachedComponents == null) {
			cachedComponents = EMPTY_COMPONENTS;
		}
		if (cachedComponents.length == 0) {
			return EMPTY_COMPONENTS;
		}
		componentsForPackage.put(sourceComponent, cachedComponents);
		return cachedComponents;
	}

	/**
	 * Resolves the listing of {@link IApiComponent}s that export the given
	 * package name. The collection of {@link IApiComponent}s is written into
	 * the specified list <code>componentList</code>
	 * 
	 * @param component
	 * @param packageName
	 * @param componentsList
	 * @throws CoreException
	 */
	private void resolvePackage0(IApiComponent component, String packageName, List<IApiComponent> componentsList) throws CoreException {
		if (component instanceof BundleComponent) {
			BundleDescription bundle = ((BundleComponent) component).getBundleDescription();
			if (bundle != null) {
				StateHelper helper = getState().getStateHelper();
				ExportPackageDescription[] visiblePackages = helper.getVisiblePackages(bundle);
				for (int i = 0, max = visiblePackages.length; i < max; i++) {
					ExportPackageDescription pkg = visiblePackages[i];
					if (packageName.equals(pkg.getName())) {
						BundleDescription bundleDescription = pkg.getExporter();
						IApiComponent exporter = getApiComponent(bundleDescription.getSymbolicName());
						if (exporter != null) {
							componentsList.add(exporter);
						}
					}
				}
				if (component.isFragment()) {
					// a fragment can see all the packages from the host
					HostSpecification host = bundle.getHost();
					BundleDescription[] hosts = host.getHosts();
					for (int i = 0, max = hosts.length; i < max; i++) {
						BundleDescription currentHost = hosts[i];
						IApiComponent apiComponent = component.getBaseline().getApiComponent(currentHost.getName());
						if (apiComponent != null) {
							resolvePackage0(apiComponent, packageName, componentsList);
						}
					}
				}
				// check for package within the source component
				String[] packageNames = component.getPackageNames();
				int index = Arrays.binarySearch(packageNames, packageName, null);
				if (index >= 0) {
					componentsList.add(component);
				}
			}
		}
	}

	/**
	 * Returns all of the visible dependent components from the current state
	 * 
	 * @param components
	 * @return the listing of visible dependent components to the given ones
	 * @throws CoreException
	 */
	public IApiComponent[] getVisibleDependentComponents(IApiComponent[] components) throws CoreException {
		ArrayList<BundleDescription> bundles = getBundleDescriptions(components);
		BundleDescription[] descs = getState().getStateHelper().getDependentBundles(bundles.toArray(new BundleDescription[bundles.size()]));
		HashSet<BundleDescription> visible = new HashSet<BundleDescription>();
		ExportPackageDescription[] packages = null;
		for (int i = 0; i < descs.length; i++) {
			packages = getState().getStateHelper().getVisiblePackages(descs[i]);
			for (int j = 0; j < packages.length; j++) {
				if (bundles.contains(packages[j].getSupplier())) {
					visible.add(descs[i]);
				}
			}
		}
		return getApiComponents(visible.toArray(new BundleDescription[visible.size()]));
	}

	/**
	 * Returns whether the specified package is supplied by the system library.
	 * 
	 * @param packageName package name
	 * @return whether the specified package is supplied by the system library
	 */
	private boolean isSystemPackage(String packageName) {
		if (packageName.startsWith("java.")) { //$NON-NLS-1$
			return true;
		}
		if (fSystemPackageNames == null) {
			ExportPackageDescription[] systemPackages = getState().getSystemPackages();
			fSystemPackageNames = new HashSet<String>(systemPackages.length);
			for (int i = 0; i < systemPackages.length; i++) {
				fSystemPackageNames.add(systemPackages[i].getName());
			}
		}
		return fSystemPackageNames.contains(packageName);
	}

	/**
	 * @return the OSGi state for this {@link IApiBaseline}
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public State getState() {
		if (fState == null) {
			fState = StateObjectFactory.defaultFactory.createState(true);
		}
		return fState;
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#getApiComponent(String)
	 */
	@Override
	public IApiComponent getApiComponent(String id) {
		loadBaselineInfos();
		if (fComponentsById == null) {
			return null;
		}
		return fComponentsById.get(id);
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#getExecutionEnvironment()
	 */
	@Override
	public String getExecutionEnvironment() {
		return fExecutionEnvironment;
	}

	/**
	 * Loads the information from the *.profile file the first time the baseline
	 * is accessed
	 */
	private void loadBaselineInfos() {
		if (fComponentsById != null) {
			return;
		}
		try {
			ApiBaselineManager.getManager().loadBaselineInfos(this);
		} catch (CoreException ce) {
			ApiPlugin.log(ce);
		}
	}

	/**
	 * Returns all errors in the state.
	 * 
	 * @return state errors
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public ResolverError[] getErrors() {
		List<ResolverError> errs = null;
		BundleDescription[] bundles = getState().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			ResolverError[] errors = getState().getResolverErrors(bundles[i]);
			for (int j = 0; j < errors.length; j++) {
				if (errs == null) {
					errs = new ArrayList<ResolverError>();
				}
				errs.add(errors[j]);
			}
		}
		if (errs != null) {
			return errs.toArray(new ResolverError[errs.size()]);
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.model.ApiElement#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiBaseline) {
			IApiBaseline baseline = (IApiBaseline) obj;
			return this.getName().equals(baseline.getName());
		}
		return super.equals(obj);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#dispose()
	 */
	@Override
	public void dispose() {
		if (fState == null) {
			// already disposed or nothing to dispose
			return;
		}
		doDispose();
		fState = null;
	}

	/**
	 * performs the actual dispose of mappings and cached elements
	 */
	protected void doDispose() {
		if (ApiPlugin.isRunningInFramework()) {
			JavaRuntime.removeVMInstallChangedListener(this);
		}
		clearCachedElements();
		IApiComponent[] components = getApiComponents();
		for (int i = 0; i < components.length; i++) {
			components[i].dispose();
		}
		clearComponentsCache();
		if (fComponentsById != null) {
			fComponentsById.clear();
			fComponentsById = null;
		}
		if (fComponentsByProjectNames != null) {
			fComponentsByProjectNames.clear();
			fComponentsByProjectNames = null;
		}
		if (fSystemPackageNames != null) {
			fSystemPackageNames.clear();
		}
		if (fSystemLibraryComponent != null) {
			fSystemLibraryComponent.dispose();
			fSystemLibraryComponent = null;
		}
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline#close()
	 */
	@Override
	public void close() throws CoreException {
		clearCachedElements();
		IApiComponent[] components = getApiComponents();
		for (int i = 0; i < components.length; i++) {
			components[i].close();
		}
	}

	/**
	 * Clears all element information from the cache for this baseline
	 * 
	 * @since 1.1
	 */
	void clearCachedElements() {
		ApiModelCache.getCache().removeElementInfo(this);
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#getDependentComponents(IApiComponent[])
	 */
	@Override
	public IApiComponent[] getDependentComponents(IApiComponent[] components) throws CoreException {
		ArrayList<BundleDescription> bundles = getBundleDescriptions(components);
		BundleDescription[] bundleDescriptions = getState().getStateHelper().getDependentBundles(bundles.toArray(new BundleDescription[bundles.size()]));
		return getApiComponents(bundleDescriptions);
	}

	/**
	 * Returns an array of API components corresponding to the given bundle
	 * descriptions.
	 * 
	 * @param bundles bundle descriptions
	 * @return corresponding API components
	 */
	private IApiComponent[] getApiComponents(BundleDescription[] bundles) {
		ArrayList<IApiComponent> dependents = new ArrayList<IApiComponent>(bundles.length);
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription bundle = bundles[i];
			IApiComponent component = getApiComponent(bundle.getSymbolicName());
			if (component != null) {
				dependents.add(component);
			}
		}
		return dependents.toArray(new IApiComponent[dependents.size()]);
	}

	/**
	 * Returns an array of bundle descriptions corresponding to the given API
	 * components.
	 * 
	 * @param components API components
	 * @return corresponding bundle descriptions
	 */
	private ArrayList<BundleDescription> getBundleDescriptions(IApiComponent[] components) throws CoreException {
		ArrayList<BundleDescription> bundles = new ArrayList<BundleDescription>(components.length);
		for (int i = 0; i < components.length; i++) {
			IApiComponent component = components[i];
			if (component instanceof BundleComponent) {
				bundles.add(((BundleComponent) component).getBundleDescription());
			}
		}
		return bundles;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.IApiBaseline#getPrerequisiteComponents(org.
	 * eclipse.pde.api.tools.IApiComponent[])
	 */
	@Override
	public IApiComponent[] getPrerequisiteComponents(IApiComponent[] components) throws CoreException {
		ArrayList<BundleDescription> bundles = getBundleDescriptions(components);
		BundleDescription[] bundlesDescriptions = getState().getStateHelper().getPrerequisites(bundles.toArray(new BundleDescription[bundles.size()]));
		return getApiComponents(bundlesDescriptions);
	}

	/**
	 * Clear cached settings for the given package.
	 * 
	 * @param packageName
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public synchronized void clearPackage(String packageName) {
		if (fComponentsProvidingPackageCache != null) {
			fComponentsProvidingPackageCache.remove(packageName);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#getExecutionEnvironmentStatus()
	 */
	@Override
	public IStatus getExecutionEnvironmentStatus() {
		return fEEStatus;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jdt.launching.IVMInstallChangedListener#defaultVMInstallChanged
	 * (org.eclipse.jdt.launching.IVMInstall,
	 * org.eclipse.jdt.launching.IVMInstall)
	 */
	@Override
	public void defaultVMInstallChanged(IVMInstall previous, IVMInstall current) {
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jdt.launching.IVMInstallChangedListener#vmAdded(org.eclipse
	 * .jdt.launching.IVMInstall)
	 */
	@Override
	public void vmAdded(IVMInstall vm) {
		if (!(vm instanceof VMStandin)) {
			// there may be a better fit for VMs/EEs
			try {
				rebindVM();
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jdt.launching.IVMInstallChangedListener#vmChanged(org.eclipse
	 * .jdt.launching.PropertyChangeEvent)
	 */
	@Override
	public void vmChanged(PropertyChangeEvent event) {
		if (!(event.getSource() instanceof VMStandin)) {
			String property = event.getProperty();
			if (IVMInstallChangedListener.PROPERTY_INSTALL_LOCATION.equals(property) || IVMInstallChangedListener.PROPERTY_LIBRARY_LOCATIONS.equals(property)) {
				try {
					rebindVM();
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
	}

	/**
	 * Re-binds the VM this baseline is bound to.
	 */
	private void rebindVM() throws CoreException {
		fVMBinding = null;
		IApiComponent[] components = getApiComponents();
		HashSet<String> ees = new HashSet<String>();
		for (int i = 0; i < components.length; i++) {
			ees.addAll(Arrays.asList(components[i].getExecutionEnvironments()));
		}
		resolveSystemLibrary(ees);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jdt.launching.IVMInstallChangedListener#vmRemoved(org.eclipse
	 * .jdt.launching.IVMInstall)
	 */
	@Override
	public void vmRemoved(IVMInstall vm) {
		if (vm.equals(fVMBinding)) {
			try {
				rebindVM();
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#getLocation()
	 */
	@Override
	public String getLocation() {
		return this.fLocation;
	}

	/*
	 * (non-Javadoc)
	 * @see IApiBaseline#setLocation(String)
	 */
	@Override
	public void setLocation(String location) {
		this.fLocation = location;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline#
	 * getApiComponent(org.eclipse.core.resources.IProject)
	 */
	@Override
	public IApiComponent getApiComponent(IProject project) {
		loadBaselineInfos();
		if (fComponentsByProjectNames == null) {
			return null;
		}
		return fComponentsByProjectNames.get(project.getName());
	}
}
