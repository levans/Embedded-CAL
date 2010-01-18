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
 * CALExpressionEditorProperties.java
 * Created: Jul 10, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.openquark.cal.compiler.SourceModelUtilities;
import org.openquark.cal.compiler.SourceModel.ModuleDefn;
import org.openquark.cal.eclipse.embedded.StringUtils;
import org.openquark.cal.eclipse.embedded.containing.ControlManager;
import org.openquark.cal.eclipse.embedded.exported.IEmbeddedCalConstants;

public class CALModuleEditorProperties extends ContainedEditorProperties {

    /** the output policy of this embedded editor */
    private String moduleName;
    
    public CALModuleEditorProperties() {
        super();
        moduleName = null;
    }
    
    /**
     * @param mi the method invocation AST node that contains all the information
     * about what is in this contained editor 
     */
    @SuppressWarnings("unchecked")
    public CALModuleEditorProperties(MethodInvocation mi) {
        this();  // set to the default values first
        
        List<Expression> arguments = mi.arguments();
        int numArgs = arguments.size();

        // get text
        if (numArgs > 0) {
            Expression subexpr = arguments.get(0);
            if (subexpr.getNodeType() == ASTNode.STRING_LITERAL) {
                setCalContents(((StringLiteral) subexpr).getLiteralValue());
            }
        }

        // this argument is the module name
        // it does not get displayed, but it is extracted from the 
        // CALContents
        if (numArgs > 1) {
            Expression subexpr = arguments.get(1);
            if (subexpr.getNodeType() == ASTNode.STRING_LITERAL) {
                moduleName = ((StringLiteral) subexpr).getLiteralValue();
            } else {
                moduleName = null;
            } 
        }


        // get height
        if (numArgs > 2) {
            Expression subexpr = arguments.get(2);
            if (subexpr.getNodeType() == ASTNode.NUMBER_LITERAL) {
                NumberLiteral heightLit = (NumberLiteral) subexpr;
                try {
                    setHeight(Math.max(Integer.parseInt(heightLit.getToken()), 10));
                } catch (NumberFormatException nfe) {
                    // value stays as default
                }
            }
        } // if this argument doesn't exist, then keep the default height

        // get width
        if (numArgs > 3) {
            Expression subexpr = arguments.get(3);
            if (subexpr.getNodeType() == ASTNode.NUMBER_LITERAL) {
                NumberLiteral widthLit = (NumberLiteral) subexpr;
                try {
                    setWidth(Math.max(Integer.parseInt(widthLit.getToken()), 10));
                } catch (NumberFormatException nfe) {
                    // value stays as default
                }
            }
        } // if this argument doesn't exist, then keep the default width

        if (numArgs > 4) {
            Expression subexpr = arguments.get(4);
            if (subexpr.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
                setValid(((BooleanLiteral) subexpr).booleanValue());
            }
        } // if this argument doesn't exist, then the expression is not valid
        
        if (numArgs > 5) {
            Expression subexpr = arguments.get(5);
            if (subexpr.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
                setShowAll(((BooleanLiteral) subexpr).booleanValue());
            }
        } // if this argument doesn't exist, then the expression is not showing all
        setDirty(false);
    }
    
    

    public String serializeEmbeddedEditor(ControlManager cm) {
        
        ModuleDefn mod = SourceModelUtilities.TextParsing
                .parseModuleDefnIntoSourceModel(getCalContents());
        
        StringBuffer sb = new StringBuffer();
        sb.append(IEmbeddedCalConstants.MODULE_EDITOR_NAME + "(");

        // the first argument is the text
        sb.append("\"" + StringUtils.escape(getCalContents()) + "\"");
        sb.append(", ");

        // the second arg is the module name
        if (mod != null) {
            moduleName = mod.getModuleName().toSourceText();
            sb.append("\"" + moduleName + "\"");
        } else {
            sb.append("\"\"");
        }

        // third and fourth args are height and width
        sb.append(", " + getHeight());
        sb.append(", " + getWidth());

        // fifth arg is isValid
        if (mod != null) { 
            sb.append(", " + true);
        } else {
            sb.append(", " + false);
        }
        

        // sixth and final arg is showAll
        sb.append(", " + isShowingAll());
        
        sb.append(" ) ");

        setDirty(false);
        return 
            IEmbeddedCalConstants.EMBEDDED_REGION_START + 
            sb.toString() + 
            IEmbeddedCalConstants.EMBEDDED_REGION_END;
    }



    
    public String getModuleName() {
        if (isDirty()) {
            // this might be slow, but let's see...
            ModuleDefn mod = SourceModelUtilities.TextParsing
                .parseModuleDefnIntoSourceModel(getCalContents());
            if (mod != null) {
                moduleName = mod.getModuleName().toString();
            }
        }
        return moduleName;
    }
    
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}
