/*******************************************************************************
 * Copyright (c) Mar 15, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;
import i.INoImpl2;
import i.INoImpl3;
import i.INoImpl5;
import i.INoImpl6;

public class testC11  {
	
	class inner1 {
		void method2() {
			class local3 implements INoImpl3 { //direct illegal implement
			}
			class local4 implements INoImpl6 { //indirect illegal implement
			}
		}
	}
	
	public void method1() {
		class local1 implements INoImpl2 { //direct illegal implement 
		}
		class local2 implements INoImpl5 { //indirect illegal implement
		}
	}
}