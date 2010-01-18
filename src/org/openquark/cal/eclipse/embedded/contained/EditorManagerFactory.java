/*******************************************************************************
 * Copyright (c) 2007 Business Objects Software Limited and others.
 * All rights reserved. 
 * This file is made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Business Objects Software Limited - initial API and implementation
 *******************************************************************************/


/*
 * EditorManagerFactory.java
 * Created: Jul 10, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.openquark.cal.eclipse.embedded.exported.RunQuark;

/**
 * This factory class creates a editor properties object from an expression.
 * It looks at the method name of the expression and determines the type that way.
 * @author aeisenberg
 *
 */
public class EditorManagerFactory {
    
    public static ContainedEditorProperties createProperties(ASTNode node) {
        if (node.getNodeType() == ASTNode.METHOD_INVOCATION) {
            MethodInvocation mi = (MethodInvocation) node;
            if (mi.getName().toString().startsWith(RunQuark.EVALUATE_EXPRESSION)) {
                return new CALExpressionEditorProperties(mi);
            } else {
                return new CALModuleEditorProperties(mi);
            }
        }
        return new CALExpressionEditorProperties();
    }
    
    
    public static ContainedEditorManager createManager(ContainedEditorProperties props) {
        if (props instanceof CALExpressionEditorProperties) {
            return new CALExpressionEditorManager((CALExpressionEditorProperties) props);
        } else {
            return new CALModuleEditorManager((CALModuleEditorProperties) props);
        }
    }
        
}
