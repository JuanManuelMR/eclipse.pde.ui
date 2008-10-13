/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.wizards.product.ProductDefinitionWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;

public class ProductInfoSection extends PDESection implements IRegistryChangeListener, IStateDeltaListener {

	private FormEntry fNameEntry;
	private FormEntry fVersionEntry;
	private ExtensionIdComboPart fAppCombo;
	private ExtensionIdComboPart fProductCombo;
	private Button fPluginButton;
	private Button fFeatureButton;

	private static int NUM_COLUMNS = 3;

	class ExtensionIdComboPart extends ComboPart implements SelectionListener {
		private String fRemovedId;

		public void createControl(Composite parent, FormToolkit toolkit, int style) {
			super.createControl(parent, toolkit, style);
			addSelectionListener(this);
		}

		public void widgetSelected(SelectionEvent e) {
			if (getSelectionIndex() != getItemCount() - 1)
				fRemovedId = null;
		}

		public void widgetDefaultSelected(SelectionEvent e) {
		}

		public boolean isValidId(String id) {
			return true;
		}

		private void addItem(String item, int index) {
			int selection = getSelectionIndex();
			super.add(item, index);
			if (item.equals(fRemovedId)) {
				select(index);
				fRemovedId = null;
			} else if (selection >= index) {
				select(selection + 1);
			}
		}

		private void removeItem(int index) {
			int selection = getSelectionIndex();
			if (index == selection) {
				fRemovedId = getSelection();
				select(getItemCount() - 2);
			}
			super.remove(index);
			if (selection > index)
				select(selection - 1);
		}

		public void handleExtensionDelta(IExtensionDelta[] deltas) {
			for (int i = 0; i < deltas.length; i++) {
				IExtension extension = deltas[i].getExtension();
				if (extension == null)
					return;
				String id = extension.getUniqueIdentifier();
				if (id == null || !isValidId(id))
					continue;
				if (deltas[i].getKind() == IExtensionDelta.ADDED) {
					int index = computeIndex(id);
					// index of -1 means id is already in combo
					if (index >= 0)
						addItem(id, index);
				} else {
					int index = indexOf(id);
					if (index >= 0)
						removeItem(index);
				}
			}
		}

		private int computeIndex(String newId) {
			// Easy linear search to compute the index to insert.  If this take too much time (ie. long list) suggest binary search
			int i = 0;
			String[] entries = getItems();
			// go to entries.length -1 because last entry in both ComboParts is a ""
			for (; i < entries.length - 1; i++) {
				int compareValue = entries[i].compareTo(newId);
				if (compareValue == 0)
					return -1;
				else if (compareValue > 0)
					break;
			}
			return i;
		}

		public void reload(String newItems[]) {
			if (fRemovedId == null)
				fRemovedId = getSelection();
			setItems(newItems);
			int index = indexOf(fRemovedId);
			if (index > 0) {
				select(index);
				fRemovedId = null;
			}
		}

	}

	public ProductInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ProductInfoSection_title);
		section.setDescription(PDEUIMessages.ProductInfoSection_desc);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		data.colspan = 2;
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, NUM_COLUMNS));

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();

		createNameEntry(client, toolkit, actionBars);
		createIdEntry(client, toolkit, actionBars);
		createVersionEntry(client, toolkit, actionBars);
		createApplicationEntry(client, toolkit, actionBars);
		createConfigurationOption(client, toolkit);

		toolkit.paintBordersFor(client);
		section.setClient(client);

		getModel().addModelChangedListener(this);
		PDECore.getDefault().getExtensionsRegistry().addListener(this);
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
	}

	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		PDECore.getDefault().getExtensionsRegistry().removeListener(this);
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
		super.dispose();
	}

	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		createLabel(client, toolkit, PDEUIMessages.ProductInfoSection_titleLabel);

		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.ProductInfoSection_productname, null, false);
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getProduct().setName(entry.getValue().trim());
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		fNameEntry.getText().setLayoutData(gd);
		fNameEntry.setEditable(isEditable());
	}

	private void createVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		createLabel(client, toolkit, ""); //$NON-NLS-1$
		createLabel(client, toolkit, PDEUIMessages.ProductInfoSection_versionTitle);

		fVersionEntry = new FormEntry(client, toolkit, PDEUIMessages.ProductInfoSection_version, null, false);
		fVersionEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getProduct().setVersion(entry.getValue().trim());
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		fVersionEntry.getText().setLayoutData(gd);
		fVersionEntry.setEditable(isEditable());
	}

	private void createIdEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		createLabel(client, toolkit, ""); //$NON-NLS-1$
		createLabel(client, toolkit, PDEUIMessages.ProductInfoSection_prodIdLabel);

		Label label = toolkit.createLabel(client, PDEUIMessages.ProductInfoSection_id);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fProductCombo = new ExtensionIdComboPart();
		fProductCombo.createControl(client, toolkit, SWT.READ_ONLY);
		fProductCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fProductCombo.setItems(TargetPlatform.getProducts());
		fProductCombo.add(""); //$NON-NLS-1$
		fProductCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getProduct().setId(fProductCombo.getSelection());
			}
		});

		Button button = toolkit.createButton(client, PDEUIMessages.ProductInfoSection_new, SWT.PUSH);
		button.setEnabled(isEditable());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleNewDefinition();
			}
		});
		fProductCombo.getControl().setEnabled(isEditable());
	}

	private void handleNewDefinition() {
		ProductDefinitionWizard wizard = new ProductDefinitionWizard(getProduct());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		if (dialog.open() == Window.OK) {
			String id = wizard.getProductId();
			IProduct product = getProduct();
			product.setId(id);
			product.setApplication(wizard.getApplication());
		}
	}

	private void createApplicationEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		createLabel(client, toolkit, ""); //$NON-NLS-1$
		createLabel(client, toolkit, PDEUIMessages.ProductInfoSection_appLabel);

		Label label = toolkit.createLabel(client, PDEUIMessages.ProductInfoSection_app, SWT.WRAP);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));

		fAppCombo = new ExtensionIdComboPart();
		fAppCombo.createControl(client, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = NUM_COLUMNS - 1;
		fAppCombo.getControl().setLayoutData(gd);
		fAppCombo.setItems(TargetPlatform.getApplications());
		fAppCombo.add(""); //$NON-NLS-1$
		fAppCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getProduct().setApplication(fAppCombo.getSelection());
			}
		});

		fAppCombo.getControl().setEnabled(isEditable());
	}

	private void createConfigurationOption(Composite client, FormToolkit toolkit) {
		createLabel(client, toolkit, ""); //$NON-NLS-1$

		Composite comp = toolkit.createComposite(client);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = layout.marginHeight = 0;
		comp.setLayout(layout);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		comp.setLayoutData(gd);

		FormText text = toolkit.createFormText(comp, true);
		text.setText(PDEUIMessages.Product_overview_configuration, true, true);
		text.addHyperlinkListener(new IHyperlinkListener() {
			public void linkEntered(HyperlinkEvent e) {
				getStatusLineManager().setMessage(e.getLabel());
			}

			public void linkExited(HyperlinkEvent e) {
				getStatusLineManager().setMessage(null);
			}

			public void linkActivated(HyperlinkEvent e) {
				String pageId = fPluginButton.getSelection() ? ConfigurationPage.PLUGIN_ID : ConfigurationPage.FEATURE_ID;
				getPage().getEditor().setActivePage(pageId);
			}
		});

		fPluginButton = toolkit.createButton(comp, PDEUIMessages.ProductInfoSection_plugins, SWT.RADIO);
		gd = new GridData();
		gd.horizontalIndent = 25;
		fPluginButton.setLayoutData(gd);
		fPluginButton.setEnabled(isEditable());
		fPluginButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fPluginButton.getSelection();
				IProduct product = getProduct();
				if (selected == product.useFeatures()) {
					product.setUseFeatures(!selected);
					((ProductEditor) getPage().getEditor()).updateConfigurationPage();
				}
			}
		});

		fFeatureButton = toolkit.createButton(comp, PDEUIMessages.ProductInfoSection_features, SWT.RADIO);
		gd = new GridData();
		gd.horizontalIndent = 25;
		fFeatureButton.setLayoutData(gd);
		fFeatureButton.setEnabled(isEditable());
	}

	private void createLabel(Composite client, FormToolkit toolkit, String text) {
		Label label = toolkit.createLabel(client, text, SWT.WRAP);
		GridData gd = new GridData();
		gd.horizontalSpan = NUM_COLUMNS;
		label.setLayoutData(gd);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fNameEntry.commit();
		fVersionEntry.commit();
		super.commit(onSave);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		fVersionEntry.cancelEdit();
		super.cancelEdit();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		IProduct product = getProduct();
		if (product.getName() != null) {
			fNameEntry.setValue(product.getName(), true);
		}
		if (product.getVersion() != null) {
			fVersionEntry.setValue(product.getVersion(), true);
		}
		if (product.getId() != null) {
			refreshProductCombo(product.getId());
		}
		if (product.getApplication() != null) {
			fAppCombo.setText(product.getApplication());
		}
		fPluginButton.setSelection(!product.useFeatures());
		fFeatureButton.setSelection(product.useFeatures());
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
			return;
		}

		String prop = e.getChangedProperty();
		if (prop == null)
			return;
		if (prop.equals(IProduct.P_ID)) {
			refreshProductCombo(e.getNewValue().toString());
		} else if (prop.equals(IProduct.P_NAME)) {
			fNameEntry.setValue(e.getNewValue().toString(), true);
		} else if (prop.equals(IProduct.P_VERSION)) {
			fVersionEntry.setValue(e.getNewValue().toString(), true);
		} else if (prop.equals(IProduct.P_APPLICATION)) {
			fAppCombo.setText(e.getNewValue().toString());
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Store selection before refresh
		boolean previousFeatureSelected = fFeatureButton.getSelection();
		// Perform the refresh
		refresh();
		// Note:  A deferred selection event is fired from radio buttons when
		// their value is toggled, the user switches to another page, and the
		// user switches back to the same page containing the radio buttons
		// This appears to be a result of a SWT bug.
		// If the radio button is the last widget to have focus when leaving 
		// the page, an event will be fired when entering the page again.
		// An event is not fired if the radio button does not have focus.
		// The solution is to redirect focus to a stable widget.
		getPage().setLastFocusControl(fNameEntry.getText());
		// Revert the configuration page if necessary
		revertConfigurationPage(previousFeatureSelected);
	}

	/**
	 * @param previousFeatureSelected
	 */
	private void revertConfigurationPage(boolean previousFeatureSelected) {
		// Compare selection from before and after the refresh
		boolean currentFeatureSelected = fFeatureButton.getSelection();
		if (previousFeatureSelected == currentFeatureSelected) {
			// No update required
			return;
		}
		// The configuration page needs to be updated
		IFormPage currentPage = getPage().getEditor().getActivePageInstance();
		// If the current page is the configuration page, switch to the 
		// overview page before doing the update; otherwise, widget disposed
		// errors may result
		if (currentPage instanceof ConfigurationPage) {
			getPage().getEditor().setActivePage(OverviewPage.PAGE_ID);
		}
		((ProductEditor) getPage().getEditor()).updateConfigurationPage();
	}

	/**
	 * @param productId
	 */
	private void refreshProductCombo(String productID) {
		if (productID == null) {
			productID = ""; //$NON-NLS-1$
		} else if (fProductCombo.indexOf(productID) == -1) {
			fProductCombo.add(productID, 0);
		}
		fProductCombo.setText(productID);
	}

	private IStatusLineManager getStatusLineManager() {
		IEditorSite site = getPage().getEditor().getEditorSite();
		return site.getActionBars().getStatusLineManager();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	public void registryChanged(IRegistryChangeEvent event) {
		final IExtensionDelta[] applicationDeltas = event.getExtensionDeltas("org.eclipse.core.runtime", "applications"); //$NON-NLS-1$ //$NON-NLS-2$
		final IExtensionDelta[] productDeltas = event.getExtensionDeltas("org.eclipse.core.runtime", "products"); //$NON-NLS-1$ //$NON-NLS-2$
		if (applicationDeltas.length + productDeltas.length == 0)
			return;

		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				fAppCombo.handleExtensionDelta(applicationDeltas);
				fProductCombo.handleExtensionDelta(productDeltas);
			}
		});
	}

	public void stateChanged(State newState) {
		String[] products = TargetPlatform.getProducts();
		final String[] finalProducts = new String[products.length + 1];
		System.arraycopy(products, 0, finalProducts, 0, products.length);
		finalProducts[products.length] = ""; //$NON-NLS-1$od

		String[] apps = TargetPlatform.getApplications();
		final String[] finalApps = new String[apps.length + 1];
		System.arraycopy(apps, 0, finalApps, 0, apps.length);
		finalApps[apps.length] = ""; //$NON-NLS-1$

		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				fAppCombo.reload(finalApps);
				fProductCombo.reload(finalProducts);
			}
		});
	}

	public void stateResolved(StateDelta delta) {
	}

}