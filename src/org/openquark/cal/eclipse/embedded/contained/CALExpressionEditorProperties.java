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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.openquark.cal.eclipse.embedded.StringUtils;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer.AnalysisResults;
import org.openquark.cal.eclipse.embedded.containing.ControlManager;
import org.openquark.cal.eclipse.embedded.exported.IEmbeddedCalConstants;
import org.openquark.cal.eclipse.embedded.exported.RunQuark;

public class CALExpressionEditorProperties extends ContainedEditorProperties {

    // a null input policy will trigger input policy inference
    private static final String DEFAULT_INPUT_POLICY_STR = "null";

    /** the output policy of this embedded editor */
    private String outputPolicyReference;
    
    /**
     * true if CALExecutorException should be thrown on the evaluateExpression method
     * false if they should be caught (and exceptions thrown to stderr
     */
    private boolean throwsException;
    
    private String moduleName;


    public CALExpressionEditorProperties() {
        super();
        outputPolicyReference = null;
        throwsException = false;
        moduleName = "";
    }
    
    /**
     * @param mi the method invocation AST node that has all of the information needed
     * to construct this contained editor
     */
    @SuppressWarnings("unchecked")
    public CALExpressionEditorProperties(MethodInvocation mi) {
        this();  // set to the default values first

        List<Expression> arguments = mi.arguments();
        int numArgs = arguments.size();
        int nextArg = 0;
        
        if (mi.getName().toString().equals(RunQuark.EVALUATE_EXPRESSION_THROWING)) {
            throwsException = true;
        }

        // get text
        if (numArgs > nextArg) {
            Expression expr = arguments.get(nextArg);
            if (expr.getNodeType() == ASTNode.STRING_LITERAL) {
                setCalContents(((StringLiteral) expr).getLiteralValue());
            }
        }
        nextArg++;

        // this argument is the list of arguments.
        // it gets recreated on save.  do nothing with it here.
        if (numArgs > nextArg) {
        }
        nextArg++;
        
        // this argument is the output policy
        // it gets displayed in the dropdown
        if (numArgs > nextArg) {
            Expression expr = arguments.get(nextArg);
            if (expr.getNodeType() == ASTNode.NULL_LITERAL) {
                outputPolicyReference = null;
            } else {
                outputPolicyReference = expr.toString();
            }
        }
        nextArg++;

        // module name is not used
        if (numArgs > nextArg) {
            Expression expr = arguments.get(nextArg);
            if (expr.getNodeType() == ASTNode.STRING_LITERAL) {
                moduleName = ((StringLiteral) expr).getLiteralValue();
            }
        }
        nextArg++;

        // get height
        if (numArgs > nextArg) {
            Expression expr = arguments.get(nextArg);
            if (expr.getNodeType() == ASTNode.NUMBER_LITERAL) {
                NumberLiteral heightLit = (NumberLiteral) expr;
                try {
                    setHeight(Math.max(Integer.parseInt(heightLit.getToken()), 10));
                } catch (NumberFormatException nfe) {
                    // value stays as default
                }
            }
        } // if this argument doesn't exist, then keep the default height
        nextArg++;
        
        // get width
        if (numArgs > nextArg) {
            Expression expr = arguments.get(nextArg);
            if (expr.getNodeType() == ASTNode.NUMBER_LITERAL) {
                NumberLiteral widthLit = (NumberLiteral) expr;
                try {
                    setWidth(Math.max(Integer.parseInt(widthLit.getToken()), 10));
                } catch (NumberFormatException nfe) {
                    // value stays as default
                }
            }
        } // if this argument doesn't exist, then keep the default width
        nextArg++;
        
        if (numArgs > nextArg) {
            Expression expr = arguments.get(nextArg);
            if (expr.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
                setValid(((BooleanLiteral) expr).booleanValue());
            }
        } // if this argument doesn't exist, then the expression is not valid
        nextArg++;
        
        if (numArgs > nextArg) {
            Expression expr = arguments.get(nextArg);
            if (expr.getNodeType() == ASTNode.BOOLEAN_LITERAL) {
                setShowAll(((BooleanLiteral) expr).booleanValue());
            }
        } // if this argument doesn't exist, then the expression is not showing all
        setDirty(false);
    }

    public String serializeEmbeddedEditor(ControlManager cm) {
        StringBuffer sb = new StringBuffer();
        sb.append(IEmbeddedCalConstants.EXPRESSION_EDITOR_NAME_START + 
                (throwsException ? RunQuark.EVALUATE_EXPRESSION_THROWING : RunQuark.EVALUATE_EXPRESSION) + 
                "(");

        // the first argument is the text
        sb.append("\"" + StringUtils.escape(getCalContents()) + "\"");
        sb.append(", ");

        // second argument is the input tuples
        sb.append("new InputTuple[] { ");
        Set<String> args = findArgs(cm.getContainingEditor().getAnalyzer());

        if (args != null) {
            Iterator<String> argIter = args.iterator();
            while (argIter.hasNext()) {
                String arg = argIter.next();
                sb.append(createArg(arg));
                if (argIter.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        sb.append(" }, ");

        // the third arg is the output policy
        sb.append(outputPolicyReference);

        // the fourth arg is the module name that is associated 
        // with this file
        String moduleName = cm.getModuleName();
        if (moduleName != null) {
            sb.append(", \"" + moduleName + "\"");
        } else {
            sb.append(", null");
        }
        
        
        // fifth and sixth args are height and width
        sb.append(", " + getHeight());
        sb.append(", " + getWidth());

        // fifth arg is isValid
        if (args != null) {
            sb.append(", " + true);
        } else {
            sb.append(", " + false);
        }

        // seventh and final arg is showAll
        sb.append(", " + isShowingAll());
        
        sb.append(" ) ");

        setDirty(false);
        return 
            IEmbeddedCalConstants.EMBEDDED_REGION_START + 
            sb.toString() + 
            IEmbeddedCalConstants.EMBEDDED_REGION_END;
    }

    /**
     * returns all of the unbound arguments of the cal contents
     * we assume that these are to be valid java identifiers
     * @return a set of unbound identifiers or <code>null</code> 
     * if the contents is not valid.
     */
    private Set<String> findArgs(CodeAnalyzer analyzer) {
        AnalysisResults results = analyzer.doAnalysis(getCalContents());
        if (results.isValid()) {
            return results.unboundIds.keySet();
        } else {
            return null;
        }
    }

    /**
     * creates a synthetic argument for the cal function
     * for now, the input policy is always default
     * 
     * @param arg the argument (a Java expression) that will be passed to CAL
     * We still need to do some checking on the argument to ensure that it is valid
     * @return a wrapped argument
     */
    private String createArg(String arg) {
        return "new InputTuple(" + arg + 
        ", " + DEFAULT_INPUT_POLICY_STR + ", \"" + arg + "\")" ;
    }


    
    public String getOuputPolicyReference() {
        return outputPolicyReference;
    }
    
    public void setOuputPolicyReference(String ouputPolicyReference) {
        this.outputPolicyReference = ouputPolicyReference;
        setDirty(true);
    }

    public boolean isThrowsException() {
        return throwsException;
    }

    public void setThrowsException(boolean throwsException) {
        this.throwsException = throwsException;
    }

    /**
     * checks the output policy for a variety of standard imports
     * 
     * in the future, we will want to do more.  
     * 
     * import RunQuark
     * import InputTuple
     */
    @Override
    public void requiresImport(ICompilationUnit unit) {
        super.requiresImport(unit);
        if (outputPolicyReference != null) {
            if (outputPolicyReference.endsWith("_OUTPUT_POLICY")) {
                createStaticImport(unit, "org.openquark.cal.compiler.io.OutputPolicy." + outputPolicyReference);
            }
        }
        createImport(unit, "org.openquark.cal.eclipse.embedded.exported.RunQuark.InputTuple");
    }
    
    public String getModuleName() {
        return moduleName;
    }
}
