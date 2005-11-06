/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.util.PropertyChangeEvent;



public class XMLTagScanner extends RuleBasedScanner {
	private Token fStringToken;
	public XMLTagScanner(IColorManager manager) {
		fStringToken = new Token(new TextAttribute(manager.getColor(IPDEColorConstants.P_STRING)));
		
		IRule[] rules = new IRule[3];
		// Add rule for single and double quotes
		rules[0] = new MultiLineRule("\"", "\"", fStringToken, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("'", "'", fStringToken, '\\'); //$NON-NLS-1$ //$NON-NLS-2$
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new XMLWhitespaceDetector());
		setRules(rules);
	}
	protected void adaptToColorChange(IColorManager colorManager, PropertyChangeEvent event, Token token) {
		colorManager.handlePropertyChangeEvent(event);
		TextAttribute attr= (TextAttribute) token.getData();
		token.setData(new TextAttribute(colorManager.getColor(event.getProperty()), attr.getBackground(), attr.getStyle()));
	}

    public void adaptToPreferenceChange(IColorManager colorManager,PropertyChangeEvent event) {
    	String property= event.getProperty();
    	if (property.startsWith(IPDEColorConstants.P_TAG) || property.startsWith(IPDEColorConstants.P_STRING)) {
    			adaptToColorChange(colorManager, event, getTokenAffected(event));
    	}
    }
    
    private Token getTokenAffected(PropertyChangeEvent event) {
    	String property= event.getProperty();
    	if (property.startsWith(IPDEColorConstants.P_STRING)) 
    		return fStringToken;
    	return (Token)fDefaultReturnToken;
    }
}
