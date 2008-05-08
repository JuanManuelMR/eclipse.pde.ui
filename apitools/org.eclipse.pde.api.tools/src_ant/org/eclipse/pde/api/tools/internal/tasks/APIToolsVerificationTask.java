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
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class APIToolsVerificationTask extends CommonUtilsTask {
	
	/**
	 * This filter store is only used to filter problem using existing filters.
	 * It doesn't add or remove any filters.
	 */
	private static class AntFilterStore implements IApiFilterStore {
		private static final String GLOBAL = "!global!"; //$NON-NLS-1$
		private Map fFilterMap;
		private boolean debug;

		public AntFilterStore(boolean debug, String filtersRoot, String componentID) {
			this.initialize(filtersRoot, componentID);
		}

		/**
		 * Initialize the filter store using the given component id
		 */
		private void initialize(String filtersRoot, String componentID) {
			if(fFilterMap != null) {
				return;
			}
			if(this.debug) {
				System.out.println("null filter map, creating a new one"); //$NON-NLS-1$
			}
			fFilterMap = new HashMap(5);
			String xml = null;
			InputStream contents = null;
			try {
				File filterFileParent = new File(filtersRoot, componentID);
				if (!filterFileParent.exists()) {
					return;
				}
				contents = new BufferedInputStream(new FileInputStream(new File(filterFileParent, IApiCoreConstants.API_FILTERS_XML_NAME)));
				xml = new String(Util.getInputStreamAsCharArray(contents, -1, IApiCoreConstants.UTF_8));
			}
			catch(IOException ioe) {}
			finally {
				if (contents != null) {
					try {
						contents.close();
					} catch(IOException e) {
						// ignore
					}
				}
			}
			if(xml == null) {
				return;
			}
			Element root = null;
			try {
				root = Util.parseDocument(xml);
			}
			catch(CoreException ce) {
				ApiPlugin.log(ce);
			}
			if (!root.getNodeName().equals(IApiXmlConstants.ELEMENT_COMPONENT)) {
				return;
			}
			String component = root.getAttribute(IApiXmlConstants.ATTR_ID);
			if(component.length() == 0) {
				return;
			}
			String versionValue = root.getAttribute(IApiXmlConstants.ATTR_VERSION);
			int version = 0;
			if(versionValue.length() != 0) {
				try {
					version = Integer.parseInt(versionValue);
				} catch (NumberFormatException e) {
					// ignore
				}
			}
			if (version < 2) {
				// we discard all filters since there is no way to retrieve the type name
				return;
			}
			NodeList resources = root.getElementsByTagName(IApiXmlConstants.ELEMENT_RESOURCE);
			ArrayList newfilters = new ArrayList();
			for(int i = 0; i < resources.getLength(); i++) {
				Element element = (Element) resources.item(i);
				String typeName = element.getAttribute(IApiXmlConstants.ATTR_TYPE);
				if(typeName == null || typeName.length() == 0) {
					continue;
				}
				NodeList filters = element.getElementsByTagName(IApiXmlConstants.ELEMENT_FILTER);
				for(int j = 0; j < filters.getLength(); j++) {
					element = (Element) filters.item(j);
					int id = loadIntegerAttribute(element, IApiXmlConstants.ATTR_ID);
					if(id <= 0) {
						continue;
					}
					String[] messageargs = null;
					NodeList elements = element.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
					if (elements.getLength() != 1) continue;
					Element messageArguments = (Element) elements.item(0);
					NodeList arguments = messageArguments.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
					int length = arguments.getLength();
					messageargs = new String[length];
					for (int k = 0; k < length; k++) {
						Element messageArgument = (Element) arguments.item(k);
						messageargs[k] = messageArgument.getAttribute(IApiXmlConstants.ATTR_VALUE);
					}
					newfilters.add(ApiProblemFactory.newApiProblem(null, typeName, messageargs, null, null, -1, -1, -1, id));
				}
			}
			internalAddFilters(componentID, (IApiProblem[]) newfilters.toArray(new IApiProblem[newfilters.size()]));
			newfilters.clear();
		}

		/**
		 * Internal use method that allows auto-persisting of the filter file to be turned on or off
		 * @param problems the problems to add the the store
		 * @param persist if the filters should be auto-persisted after they are added
		 */
		private void internalAddFilters(String componentID, IApiProblem[] problems) {
			if(problems == null) {
				if(this.debug) {
					System.out.println("null problems array not addding filters"); //$NON-NLS-1$
				}
				return;
			}
			for(int i = 0; i < problems.length; i++) {
				IApiProblem problem = problems[i];
				IApiProblemFilter filter = new ApiProblemFilter(componentID, problem);
				String typeName = problem.getTypeName();
				if (typeName == null) {
					typeName = GLOBAL;
				}
				Set filters = (Set) fFilterMap.get(typeName);
				if(filters == null) {
					filters = new HashSet();
					fFilterMap.put(typeName, filters);
				}
				filters.add(filter);
			}
		}

		public void addFilters(IApiProblemFilter[] filters) {
			// do nothing
		}

		public void addFilters(IApiProblem[] problems) {
			// do nothing
		}

		public void dispose() {
			// do nothing
		}

		public IApiProblemFilter[] getFilters(IResource resource) {
			return null;
		}

		public IResource[] getResources() {
			return null;
		}

		public boolean isFiltered(IApiProblem problem) {
			if (this.fFilterMap == null || this.fFilterMap.isEmpty()) return false;
			String typeName = problem.getTypeName();
			Set filters = (Set) this.fFilterMap.get(typeName);
			if (filters == null) return false;
			for (Iterator iterator = filters.iterator(); iterator.hasNext();) {
				IApiProblemFilter filter = (IApiProblemFilter) iterator.next();
				if (filter.getUnderlyingProblem().equals(problem)) {
					return true;
				}
			}
			return false;
		}

		public boolean removeFilters(IApiProblemFilter[] filters) {
			return false;
		}
		private static int loadIntegerAttribute(Element element, String name) {
			String value = element.getAttribute(name);
			if(value.length() == 0) {
				return -1;
			}
			try {
				int number = Integer.parseInt(value);
				return number;
			}
			catch(NumberFormatException nfe) {}
			return -1;
		}
	}
	
	private static class Summary {
		List apiUsageProblems = new ArrayList();
		List apiCompatibilityProblems = new ArrayList();
		List apiBundleVersionProblems = new ArrayList();
		String componentID;

		public Summary(String componentID, IApiProblem[] apiProblems) {
			this.componentID = componentID;
			for (int i = 0, max = apiProblems.length; i < max; i++) {
				IApiProblem problem = apiProblems[i];
				switch(problem.getCategory()) {
					case IApiProblem.CATEGORY_COMPATIBILITY :
						apiCompatibilityProblems.add(problem);
						break;
					case IApiProblem.CATEGORY_USAGE :
						apiUsageProblems.add(problem);
						break;
					case IApiProblem.CATEGORY_VERSION :
						apiBundleVersionProblems.add(problem);
				}
			}
		}
		
		private void dumpProblems(String title, List problemsList,
				PrintWriter printWriter) {
			printWriter.println(title);
			if (problemsList.size() != 0) {
				for (Iterator iterator = problemsList.iterator(); iterator.hasNext(); ) {
					IApiProblem problem = (IApiProblem) iterator.next();
					printWriter.println(problem.getMessage());
				}
			} else {
				printWriter.println("None"); //$NON-NLS-1$
			}
		}

		public String getTitle() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			printTitle(printWriter);

			printWriter.flush();
			printWriter.close();
			return String.valueOf(writer.getBuffer());
		}

		public String getDetails() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);

			printWriter.println("=================================================================================="); //$NON-NLS-1$
			printWriter.println("Details for " + this.componentID + ":"); //$NON-NLS-1$//$NON-NLS-2$
			printWriter.println("=================================================================================="); //$NON-NLS-1$
			dumpProblems("Usage", apiUsageProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Compatibility", apiCompatibilityProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Bundle versions", apiBundleVersionProblems, printWriter); //$NON-NLS-1$
			printWriter.println("=================================================================================="); //$NON-NLS-1$
			printWriter.flush();
			printWriter.close();
			return String.valueOf(writer.getBuffer());
		}

		public String toString() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			printTitle(printWriter);

			dumpProblems("Usage", apiUsageProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Compatibility", apiCompatibilityProblems, printWriter); //$NON-NLS-1$
			dumpProblems("Bundle versions", apiBundleVersionProblems, printWriter); //$NON-NLS-1$

			printWriter.flush();
			printWriter.close();
			return String.valueOf(writer.getBuffer());
		}

		private void printTitle(PrintWriter printWriter) {
			printWriter.print("Results for " + this.componentID + " : "); //$NON-NLS-1$ //$NON-NLS-2$
			printWriter.print('(');
			printWriter.print("total: "); //$NON-NLS-1$
			printWriter.print(
					  apiUsageProblems.size()
					+ apiBundleVersionProblems.size()
					+ apiCompatibilityProblems.size());
			printWriter.print(',');
			printWriter.print("usage: "); //$NON-NLS-1$
			printWriter.print(apiUsageProblems.size());
			printWriter.print(',');
			printWriter.print("compatibility: "); //$NON-NLS-1$
			printWriter.print(apiCompatibilityProblems.size());
			printWriter.print(',');
			printWriter.print("bundle version: "); //$NON-NLS-1$
			printWriter.print(apiBundleVersionProblems.size());
			printWriter.println(')');
		}
	}
	private static final String REFERENCE = "reference"; //$NON-NLS-1$
	private static final String CURRENT = "currentProfile"; //$NON-NLS-1$
	private static final String REFERENCE_PROFILE_NAME = "reference_profile"; //$NON-NLS-1$
	private static final String CURRENT_PROFILE_NAME = "current_profile"; //$NON-NLS-1$

	private boolean debug;

	private String referenceLocation;
	private String profileLocation;
	private String reportLocation;
	private String eeFileLocation;
	private String filterStoreRoot;

	public void setProfile(String profileLocation) {
		this.profileLocation = profileLocation;
	}
	public void setReference(String referenceLocation) {
		this.referenceLocation = referenceLocation;
	}
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
	}
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	public void setFilterStoreRoot(String filterStoreRoot) {
		this.filterStoreRoot = filterStoreRoot; 
	}
	public void execute() throws BuildException {
		if (this.debug) {
			System.out.println("reference : " + this.referenceLocation); //$NON-NLS-1$
			System.out.println("profile to compare : " + this.profileLocation); //$NON-NLS-1$
			System.out.println("report location : " + this.reportLocation); //$NON-NLS-1$
		}
		if (this.referenceLocation == null
				|| this.profileLocation == null
				|| this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println("Missing arguments :"); //$NON-NLS-1$
			writer.print("reference location :"); //$NON-NLS-1$
			writer.println(this.referenceLocation);
			writer.print("current profile location :"); //$NON-NLS-1$
			writer.println(this.profileLocation);
			writer.print("report location :"); //$NON-NLS-1$
			writer.println(this.reportLocation);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		// unzip reference
		long time = 0;
		if (this.debug) {
			time = System.currentTimeMillis();
		}
		File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		
		File referenceInstallDir = new File(tempDir, REFERENCE);
		extractSDK(referenceInstallDir, this.referenceLocation);

		File profileInstallDir = new File(tempDir, CURRENT);
		extractSDK(profileInstallDir, this.profileLocation);
		if (this.debug) {
			System.out.println("Extraction of both archives : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		// run the comparison
		// create profile for the reference
		IApiProfile referenceProfile = createProfile(REFERENCE_PROFILE_NAME, getInstallDir(tempDir, REFERENCE), this.eeFileLocation);
		IApiProfile currentProfile = createProfile(CURRENT_PROFILE_NAME, getInstallDir(tempDir, CURRENT), this.eeFileLocation);
		
		if (this.debug) {
			System.out.println("Creation of both profiles : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		Map allProblems = new HashMap();
		List allBundles = null;
		if (this.debug) {
			allBundles = new ArrayList();
		}
		try {
			IApiComponent[] apiComponents = currentProfile.getApiComponents();
			int length = apiComponents.length;
			int apiToolsComponents = 0;
			Set visitedApiComponentNames = new HashSet();
			for (int i = 0; i < length; i++) {
				IApiComponent apiComponent = apiComponents[i];
				String name = apiComponent.getId();
				visitedApiComponentNames.add(name);
				if (!isApiToolsComponent(apiComponent)) {
					if (debug) {
						allBundles.add("NOT API TOOLS COMPONENT: " + apiComponent.getId()); //$NON-NLS-1$
					}
					continue;
				}
				if (apiComponent.isSystemComponent()) continue;
				apiToolsComponents++;
				if (debug) {
					allBundles.add("API TOOLS COMPONENT: " + name); //$NON-NLS-1$
				}
				BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
				try {
					analyzer.analyzeComponent(null, getFilterStore(name), referenceProfile, apiComponent, null, null, new NullProgressMonitor());
					IApiProblem[] problems = analyzer.getProblems();
					if (problems.length != 0) {
						allProblems.put(name, problems);
					}
				} catch(RuntimeException e) {
					ApiPlugin.log(e);
					throw e;
				} finally {
					analyzer.dispose();
				}
			}
			if (debug) {
				Collections.sort(allBundles);
				for (Iterator iterator = allBundles.iterator(); iterator.hasNext(); ) {
					System.out.println(iterator.next());
				}
				System.out.println("Total number of components in current profile :" + length); //$NON-NLS-1$
				System.out.println("Total number of api tools components in current profile :" + apiToolsComponents); //$NON-NLS-1$
				System.out.println("Total number of non-api tools components in current profile :" + (length - apiToolsComponents)); //$NON-NLS-1$
			}
			IApiComponent[] baselineApiComponents = referenceProfile.getApiComponents();
			for (int i = 0, max = baselineApiComponents.length; i < max; i++) {
				IApiComponent apiComponent = baselineApiComponents[i];
				String id = apiComponent.getId();
				if (!visitedApiComponentNames.remove(id)) {
//					if (!isApiToolsComponent(apiComponent) || apiComponent.isSystemComponent()) continue;
					//remove component in the current profile
					IApiProblem problem = ApiProblemFactory.newApiProblem(id,
							null,
							new String[] { id },
							new String[] {
								IApiMarkerConstants.MARKER_ATTR_HANDLE_ID,
								IApiMarkerConstants.API_MARKER_ATTR_ID
							},
							new Object[] {
								id,
								new Integer(IApiMarkerConstants.COMPATIBILITY_MARKER_ID),
							},
							0,
							-1,
							-1,
							IApiProblem.CATEGORY_COMPATIBILITY,
							IDelta.API_PROFILE_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.API_COMPONENT);
					allProblems.put(id, new IApiProblem[] { problem });
				}
			}
		} finally {
			if (this.debug) {
				System.out.println("API tools verification check : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
			referenceProfile.dispose();
			currentProfile.dispose();
			Util.delete(referenceInstallDir);
			Util.delete(profileInstallDir);
			if (this.debug) {
				System.out.println("Cleanup : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
				time = System.currentTimeMillis();
			}
		}
		dumpAllProblems(allProblems);
	}
	private IApiFilterStore getFilterStore(String name) {
		if (this.filterStoreRoot == null) return null;
		return new AntFilterStore(this.debug, this.filterStoreRoot, name);
	}
	private boolean isApiToolsComponent(IApiComponent apiComponent) {
		if (apiComponent.isSystemComponent()) return false;
		File file = new File(apiComponent.getLocation());
		if (file.exists()) {
			if (file.isDirectory()) {
				// directory binary bundle
				File apiDescription = new File(file, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
				return apiDescription.exists();
			}
			ZipFile zipFile = null;
			try {
				zipFile = new ZipFile(file);
				return zipFile.getEntry(IApiCoreConstants.API_DESCRIPTION_XML_NAME) != null;
			} catch (ZipException e) {
				// ignore
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					if (zipFile != null) zipFile.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return false;
	}
	private void dumpAllProblems(Map allProblems) {
		Set entrySet = allProblems.entrySet();
		List allEntries = new ArrayList();
		allEntries.addAll(entrySet);
		Collections.sort(allEntries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry entry1 = (Map.Entry) o1;
				Map.Entry entry2 = (Map.Entry) o2;
				return ((String) entry1.getKey()).compareTo(entry2.getKey());
			}
		});
		int size = allEntries.size();
		if (size == 0) {
			return;
		}
		Summary[] summaries = new Summary[size];
		int i = 0;
		for (Iterator iterator = allEntries.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			summaries[i++] = createProblemSummary((String) entry.getKey(), (IApiProblem[]) entry.getValue());
		}
		if (this.debug) {
			dumpSummaries(summaries);
		}
		dumpReport(summaries);
	}
	private void dumpReport(Summary[] summaries) {
		for (int i = 0, max = summaries.length; i < max; i++) {
			Summary summary = summaries[i];
			String contents = null;
			String componentID = summary.componentID;
			try {
				Document document = Util.newDocument();
				Element report = document.createElement(IApiXmlConstants.ELEMENT_API_TOOL_REPORT);
				report.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_PROBLEM_CURRENT_VERSION);
				report.setAttribute(IApiXmlConstants.ATTR_COMPONENT_ID, componentID);
				document.appendChild(report);
				
				Element category = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEM_CATEGORY);
				category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_COMPATIBILITY));
				category.setAttribute(IApiXmlConstants.ATTR_VALUE, "compatibility"); //$NON-NLS-1$
				insertAPIProblems(category, document, summary.apiCompatibilityProblems);
				report.appendChild(category);

				category = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEM_CATEGORY);
				category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_USAGE));
				category.setAttribute(IApiXmlConstants.ATTR_VALUE, "usage"); //$NON-NLS-1$
				insertAPIProblems(category, document, summary.apiUsageProblems);
				report.appendChild(category);
				
				category = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEM_CATEGORY);
				category.setAttribute(IApiXmlConstants.ATTR_KEY, Integer.toString(IApiProblem.CATEGORY_VERSION));
				category.setAttribute(IApiXmlConstants.ATTR_VALUE, "bundleVersion"); //$NON-NLS-1$
				insertAPIProblems(category, document, summary.apiBundleVersionProblems);
				report.appendChild(category);

				contents = Util.serializeDocument(document);
			} catch (DOMException e) {
				throw new BuildException(e);
			} catch (CoreException e) {
				throw new BuildException(e);
			}
			if (contents != null) {
				saveReport(componentID, contents);
			}
		}
	}
	private void saveReport(String componentID, String contents) {
		File dir = new File(this.reportLocation);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new BuildException("Could not create report directory : " + this.reportLocation); //$NON-NLS-1$
			}
		}
		File reportComponentIDDir = new File(dir, componentID);
		if (!reportComponentIDDir.exists()) {
			if (!reportComponentIDDir.mkdirs()) {
				throw new BuildException("Could not create report directory : " + reportComponentIDDir); //$NON-NLS-1$
			}
		}
		File reportFile = new File(reportComponentIDDir, "report.xml"); //$NON-NLS-1$
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(reportFile));
			writer.write(contents);
			writer.flush();
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}
	private Summary createProblemSummary(String componentID, IApiProblem[] apiProblems) {
		return new Summary(componentID, apiProblems);
	}
	
	private void dumpSummaries(Summary[] summaries) {
		for (int i = 0, max = summaries.length; i < max; i++) {
			System.out.println(summaries[i].getTitle());
		}
		for (int i = 0, max = summaries.length; i < max; i++) {
			System.out.println(summaries[i].getDetails());
		}
	}
	
	
	/**
	 * Returns an element that contains all the api problem nodes.
	 *
	 * @param document the given xml document
	 * @param problems the given problem to dump into the document
	 * @return an element that contains all the api problem nodes or null if an error occured
	 */
	private void insertAPIProblems(Element root, Document document, List problems) throws CoreException {
		Element apiProblems = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEMS);
		root.appendChild(apiProblems);
		Element element = null;
		for(Iterator iterator = problems.iterator(); iterator.hasNext(); ) {
			IApiProblem problem = (IApiProblem) iterator.next();
			element = document.createElement(IApiXmlConstants.ELEMENT_API_PROBLEM);
			element.setAttribute(IApiXmlConstants.ATTR_TYPE_NAME, String.valueOf(problem.getTypeName()));
			element.setAttribute(IApiXmlConstants.ATTR_PROBLEM_ID, Integer.toString(problem.getId()));
			element.setAttribute(IApiXmlConstants.ATTR_LINE_NUMBER, Integer.toString(problem.getLineNumber()));
			element.setAttribute(IApiXmlConstants.ATTR_CHAR_START, Integer.toString(problem.getCharStart()));
			element.setAttribute(IApiXmlConstants.ATTR_CHAR_END, Integer.toString(problem.getCharEnd()));
			element.setAttribute(IApiXmlConstants.ATTR_ELEMENT_KIND, Integer.toString(problem.getElementKind()));
			element.setAttribute(IApiXmlConstants.ATTR_KIND, Integer.toString(problem.getKind()));
			element.setAttribute(IApiXmlConstants.ATTR_FLAGS, Integer.toString(problem.getFlags()));
			element.setAttribute(IApiXmlConstants.ATTR_MESSAGE, problem.getMessage());
			String[] extraMarkerAttributeIds = problem.getExtraMarkerAttributeIds();
			if (extraMarkerAttributeIds != null && extraMarkerAttributeIds.length != 0) {
				int length = extraMarkerAttributeIds.length;
				Object[] extraMarkerAttributeValues = problem.getExtraMarkerAttributeValues();
				Element extraArgumentsElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENTS);
				for (int j = 0; j < length; j++) {
					Element extraArgumentElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENT);
					extraArgumentElement.setAttribute(IApiXmlConstants.ATTR_ID, extraMarkerAttributeIds[j]);
					extraArgumentElement.setAttribute(IApiXmlConstants.ATTR_VALUE, String.valueOf(extraMarkerAttributeValues[j]));
					extraArgumentsElement.appendChild(extraArgumentElement);
				}
				element.appendChild(extraArgumentsElement);
			}
			String[] messageArguments = problem.getMessageArguments();
			if (messageArguments != null && messageArguments.length != 0) {
				int length = messageArguments.length;
				Element messageArgumentsElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
				for (int j = 0; j < length; j++) {
					Element messageArgumentElement = document.createElement(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
					messageArgumentElement.setAttribute(IApiXmlConstants.ATTR_VALUE, String.valueOf(messageArguments[j]));
					messageArgumentsElement.appendChild(messageArgumentElement);
				}
				element.appendChild(messageArgumentsElement);
			}
			apiProblems.appendChild(element);
		}
	}
}