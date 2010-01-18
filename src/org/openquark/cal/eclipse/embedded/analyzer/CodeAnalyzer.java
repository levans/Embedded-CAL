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
 * CodeAnalyzer.java
 * Created: Jun 21, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.analyzer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaModelException;
import org.openquark.cal.compiler.CompilerMessage;
import org.openquark.cal.compiler.CompilerMessageLogger;
import org.openquark.cal.compiler.MessageLogger;
import org.openquark.cal.compiler.QualifiedName;
import org.openquark.cal.compiler.SourceModelUtilities;
import org.openquark.cal.compiler.SourceRange;
import org.openquark.cal.compiler.CompilerMessage.Severity;
import org.openquark.cal.compiler.SourceIdentifier.Category;
import org.openquark.cal.compiler.SourceModel.Expr;
import org.openquark.cal.compiler.SourceModel.FunctionDefn;
import org.openquark.cal.compiler.SourceModel.InstanceDefn;
import org.openquark.cal.compiler.SourceModel.ModuleDefn;
import org.openquark.cal.compiler.SourceModel.SourceElement;
import org.openquark.cal.compiler.SourceModel.TopLevelSourceElement;
import org.openquark.cal.compiler.SourceModel.TypeClassDefn;
import org.openquark.cal.compiler.SourceModel.TypeConstructorDefn;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.exported.Pair;

/**
 * 
 * This class analyzes the code in ContainedEditors.  Every ContainingEditor has
 * a single CodeAnalyzer that is shared between all of its ContainedEditors.
 * <p>
 * This class analyses code to find things like unbound references and their locations 
 * in the source.
 * 
 * @author aeisenberg
 *
 */
public abstract class CodeAnalyzer {

    public static class AnalysisResults {
        boolean valid; 
        public final Map<String, List<SourceRange>> unboundIds;
        public final Map<String, List<SourceRange>> importedIds;
        public final Map<String, List<SourceRange>> locallyBoundIds;


        public AnalysisResults(boolean valid) {
            this.valid = valid;
            if (valid) {
                unboundIds = new HashMap<String, List<SourceRange>>();
                importedIds = new HashMap<String, List<SourceRange>>();
                locallyBoundIds = new HashMap<String, List<SourceRange>>();
                
            } else {
                unboundIds = null;
                importedIds = null;
                locallyBoundIds = null;
            }
        }
        public AnalysisResults() {
            this.valid = true;
            unboundIds = new HashMap<String, List<SourceRange>>();
            importedIds = new HashMap<String, List<SourceRange>>();
            locallyBoundIds = new HashMap<String, List<SourceRange>>();
        }        

        /**
         * @return true if the code is syntactically correct, false otherwise
         */
        public boolean isValid() {
            return valid;
        }
    }


    /**
     * the visitor that does the real work of analysis
     */
    private UnboundReferenceFinder referenceFinder;


    /**
     * the contents of the quark module
     * 
     * XXX we only need one copy of this text per project since each project
     * uses only one quark module for this purpose.  but, this is something we
     * can do later if reconciling is too slow.
     */
    private String quarkModuleText;

    /**
     * set to true if the quarkModule has changed since the last analysis
     * 
     */
    private boolean stale = true;


    /**
     * grabs the text out of the module
     */
    @SuppressWarnings("restriction")
    private void doRefreshText() {
        referenceFinder = new UnboundReferenceFinder();
        try {
            CompilerMessageLogger messageLogger = new MessageLogger();
            quarkModuleText = getModuleText();
            ModuleDefn mod = SourceModelUtilities.TextParsing
                .parseModuleDefnIntoSourceModel(quarkModuleText);

            if (mod != null) {
                referenceFinder.initialize(mod);
            }
            outputMessages(messageLogger);
            stale = false;
        } catch (JavaModelException e) {
            e.printStackTrace();
            quarkModuleText = "";
        }
    }

    public abstract String getModuleText() throws JavaModelException;
    
    
    public AnalysisResults doAnalysis(String calCode) {
        // check to see if the module text has changed since last
        // reconcile
        if (stale) {
            doRefreshText();
        }

        CompilerMessageLogger messageLogger = new MessageLogger();
        Expr expr = SourceModelUtilities.TextParsing
        .parseExprIntoSourceModel(calCode, messageLogger);

        outputMessages(messageLogger);

        if (expr != null) {
            AnalysisResults unbound_imported
                = referenceFinder.findUnbound(expr);
            return unbound_imported;

        } else {
            return new AnalysisResults(false) ;
        }
    }
    
    public AnalysisResults findAllIdentifiers(String calCode) {
        // check to see if the module text has changed since last
        // reconcile
        if (stale) {
            doRefreshText();
        }

        CompilerMessageLogger messageLogger = new MessageLogger();
        Expr expr = SourceModelUtilities.TextParsing
        .parseExprIntoSourceModel(calCode, messageLogger);

        outputMessages(messageLogger);

        AnalysisResults unbound_imported
            = referenceFinder.findAllIdentifiers(expr);
        return unbound_imported;
    }

    /**
     * notifies the programmer of any compiler messages for this analysis...
     * for now just outputting to console, but what is the better way?
     * @param messageLogger
     */
    private void outputMessages(CompilerMessageLogger messageLogger) {
        if (messageLogger.getNMessages() > 0) {
            for (final CompilerMessage message : messageLogger.getCompilerMessages()) {
                if (message.getSeverity().compareTo(Severity.ERROR) >= 0) {
                    System.err.println(message.getMessage());
                } else {
                    System.out.println(message.getMessage());
                }
            }
        }
    }
    
    protected void setStale() {
        this.stale = true;
    }

    public Pair<QualifiedName, Category> getQualifiedName(String name) {
        if (referenceFinder == null) {
            doRefreshText();
        }
        return referenceFinder.findQualifiedName(name);
    }
    
    /**
     * This method searches through the quarkModuleText for the declaration of a 
     * top-level function whose name is "identifier".  The returned result 
     * is the source range for that function name.  
     * 
     * Null is returned if the function doesn't exist.
     * 
     * @param identifier the name of an entity that may be a top level-identifier
     * @return null if "identifier" is *not* a top-level name, otherwise 
     * the source range of the name of the defnition of the identifier
     * 
     * TODOEL This is a comment put here by Andrew E to remind Edward to change this method so that 
     * it does not use reflection.  However the requirement is that the sourceRange part of the
     * sourceModel becomes public 
     */
    public SourceRange getSourceRangeForTopLevelIdentifier(String identifier) {
        return internalGetSourceRange(identifier, true);
    }
    
    
    /**
     * Similar to getSourceRangeForTopLevelIdentifier, but returns the entire
     * definition, not just the name
     * @param identifier
     * @return source range for the entire top level definition, or null if
     * identifier is not a top-level decl
     */
    public SourceRange getSourceRangeForTopLevelDefinition(String identifier) {
        return internalGetSourceRange(identifier, false);
    }
    
    private SourceRange internalGetSourceRange(String identifier, boolean nameOnly) {
        if (stale) {
            doRefreshText();
        }

        if (referenceFinder.isTopLevelDeclaration(identifier)) {
            // the identifier exists as a top-level.
            // now we have to find it.
            ModuleDefn module = SourceModelUtilities.TextParsing.parseModuleDefnIntoSourceModel(quarkModuleText);
            if (module != null) {
                try {
                    initReflection();
                    for (TopLevelSourceElement topLevel : module.getTopLevelDefns()) {
                        
                        // ADE actually, the UnboundReferenceFinder is only storing 
                        // FunctionDefns.  All other objects are not stored.
                        // this is fine for now, but should be changed later.
                        if (topLevel instanceof FunctionDefn) {
                            FunctionDefn fd = (FunctionDefn) topLevel;
                            if (fd.getName().equals(identifier)) {
                                
                                if (nameOnly) {
                                    return (SourceRange) 
                                    method_FunctionDefn_getNameSourceRange.invoke(fd);
                                } else {
                                    return (SourceRange) 
                                    method_FunctionDefn_getSourceRange.invoke(fd);
                                }
                            }
                        } else if (topLevel instanceof InstanceDefn) {
                            InstanceDefn id = (InstanceDefn) topLevel;
                            if (id.getTypeClassName().getUnqualifiedName().equals(identifier)) {
                                if (nameOnly) {
                                    return (SourceRange) 
                                    method_InstanceDefn_getSourceRangeOfName.invoke(id);
                                } else {
                                    return (SourceRange) 
                                    method_InstanceDefn_getSourceRange.invoke(id);
                                }
                            }
                        } else if (topLevel instanceof TypeClassDefn) {
                            TypeClassDefn tcd = (TypeClassDefn) topLevel;
                            if (tcd.getTypeClassName().equals(identifier)) {
                                if (nameOnly) {
                                    return (SourceRange) 
                                    method_TypeClassDefn_getSourceRangeOfDefn.invoke(tcd);
                                } else {
                                    return (SourceRange) 
                                    method_TypeClassDefn_getSourceRange.invoke(tcd);
                                }
                            }                        
                        } else if (topLevel instanceof TypeConstructorDefn) {
                            TypeConstructorDefn tcd = (TypeConstructorDefn) topLevel;
                            if (tcd.getTypeConsName().equals(identifier)) {
                                if (nameOnly) {
                                    return (SourceRange) 
                                    method_TypeConstructorDefn_Defn_getSourceRangeOfDefn.invoke(tcd);
                                } else {
                                    return (SourceRange) 
                                    method_TypeConstructorDefn_Defn_getSourceRange.invoke(tcd);
                                }
                            }                        
                        }
                    }
                } catch (Exception e) {
                    EmbeddedCALPlugin.logError("Error with reflection", e);
                }
            }
            
        }
        return null;
    }
    
    
    
    
    private void initReflection() throws Exception {
        // for identifiers
        if (method_FunctionDefn_getNameSourceRange == null) {
            method_FunctionDefn_getNameSourceRange = 
                FunctionDefn.class.getDeclaredMethod("getNameSourceRange");
            method_FunctionDefn_getNameSourceRange.setAccessible(true);
        }
        
        if (method_InstanceDefn_getSourceRangeOfName == null) {
            method_InstanceDefn_getSourceRangeOfName = 
                InstanceDefn.class.getDeclaredMethod("getSourceRangeOfName");
            method_InstanceDefn_getSourceRangeOfName.setAccessible(true);
        }
        
        if (method_TypeClassDefn_getSourceRangeOfDefn == null) {
            method_TypeClassDefn_getSourceRangeOfDefn = 
                TypeClassDefn.class.getDeclaredMethod("getSourceRangeOfDefn");
            method_TypeClassDefn_getSourceRangeOfDefn.setAccessible(true);
        }
        
        if (method_TypeConstructorDefn_Defn_getSourceRangeOfDefn == null) {
            method_TypeConstructorDefn_Defn_getSourceRangeOfDefn = 
                TypeConstructorDefn.class.getDeclaredMethod("getSourceRangeOfDefn");
            method_TypeConstructorDefn_Defn_getSourceRangeOfDefn.setAccessible(true);
        }
        
        // for entire definitions
        if (method_FunctionDefn_getSourceRange == null) {
            method_FunctionDefn_getSourceRange = 
                SourceElement.class.getDeclaredMethod("getSourceRange");
            method_FunctionDefn_getSourceRange.setAccessible(true);
        }
        
        if (method_InstanceDefn_getSourceRange == null) {
            method_InstanceDefn_getSourceRange = 
                SourceElement.class.getDeclaredMethod("getSourceRange");
            method_InstanceDefn_getSourceRange.setAccessible(true);
        }
        
        if (method_TypeClassDefn_getSourceRange == null) {
            method_TypeClassDefn_getSourceRange = 
                SourceElement.class.getDeclaredMethod("getSourceRange");
            method_TypeClassDefn_getSourceRange.setAccessible(true);
        }
        
        if (method_TypeConstructorDefn_Defn_getSourceRange == null) {
            method_TypeConstructorDefn_Defn_getSourceRange = 
                SourceElement.class.getDeclaredMethod("getSourceRange");
            method_TypeConstructorDefn_Defn_getSourceRange.setAccessible(true);
        }
        

    }
    
    // for identifiers
    private static Method method_FunctionDefn_getNameSourceRange;
    private static Method method_InstanceDefn_getSourceRangeOfName;
    private static Method method_TypeClassDefn_getSourceRangeOfDefn;
    private static Method method_TypeConstructorDefn_Defn_getSourceRangeOfDefn;

    // for entire definitions
    private static Method method_FunctionDefn_getSourceRange;
    private static Method method_InstanceDefn_getSourceRange;
    private static Method method_TypeClassDefn_getSourceRange;
    private static Method method_TypeConstructorDefn_Defn_getSourceRange;
    

}
