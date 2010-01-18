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
 * FindUnbound.java
 * Created: Jun 20, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.analyzer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openquark.cal.compiler.ModuleName;
import org.openquark.cal.compiler.ModuleNameResolver;
import org.openquark.cal.compiler.QualifiedName;
import org.openquark.cal.compiler.SourceMetricsManager;
import org.openquark.cal.compiler.SourceModel;
import org.openquark.cal.compiler.SourceRange;
import org.openquark.cal.compiler.SourceIdentifier.Category;
import org.openquark.cal.compiler.SourceModel.Expr;
import org.openquark.cal.compiler.SourceModel.Import;
import org.openquark.cal.compiler.SourceModel.ModuleDefn;
import org.openquark.cal.compiler.SourceModel.Name;
import org.openquark.cal.compiler.SourceModel.TopLevelSourceElement;
import org.openquark.cal.compiler.SourceModel.Expr.Var;
import org.openquark.cal.compiler.SourceModel.FunctionDefn.Algebraic;
import org.openquark.cal.compiler.SourceModel.FunctionDefn.Foreign;
import org.openquark.cal.compiler.SourceModel.FunctionDefn.Primitive;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer.AnalysisResults;
import org.openquark.cal.eclipse.embedded.exported.Pair;

class UnboundReferenceFinder {
    
    
    /**
     * This class records all import statements and top-level
     * declarations
     */
    private class BindingRecorder extends BindingVisitor<Set<ModuleName>> {
        /** 
         * Captures the imports in the module definition. Later a module name 
         * resolver is contructed based on them. 
         */
        public Set<ModuleName> visit_Import(Import importStmt, Object arg) {
            Set<ModuleName> visibleModuleNames = (Set<ModuleName>) arg;
            visibleModuleNames.add(SourceModel.Name.Module.toModuleName(importStmt.getImportedModuleName()));
            // don't visit this branch any further
            return super.visit_Import(importStmt, arg);
        }
    
        
        /**
         * stores a top level declaration
         */
        @Override
        public Set<ModuleName> visit_FunctionDefn_Algebraic(Algebraic algebraic, Object arg) {
            topLevelDeclarationNames.put(algebraic.getName(), 
                    QualifiedName.make(getModuleName(), algebraic.getName()));
            // don't visit this branch any further
            return (Set<ModuleName>) arg;
        }
        
        /**
         * stores a top level declaration
         */
        @Override
        public Set<ModuleName> visit_FunctionDefn_Foreign(Foreign foreign, Object arg) {
            topLevelDeclarationNames.put(foreign.getName(), 
                    QualifiedName.make(getModuleName(), foreign.getName()));
            // don't visit this branch any further
            return (Set<ModuleName>) arg;
        }
        
        /**
         * stores a top level declaration
         */
        @Override
        public Set<ModuleName> visit_FunctionDefn_Primitive(Primitive primitive, Object arg) {
            topLevelDeclarationNames.put(primitive.getName(), 
                    QualifiedName.make(getModuleName(), primitive.getName()));
            // don't visit this branch any further
            return (Set<ModuleName>) arg;
        }
    }

    
    private class ExpressionSearcher extends BindingVisitor<AnalysisResults> {
        /**
         * checks to see if var is a free variable or if it is an import
         * 
         * @param var the variable to check
         * 
         * @param arg a pair of maps that maps a variable name to a list of locations in the document
         * where it is found.
         */
        @Override
        public AnalysisResults visit_Expr_Var(Var var, Object arg) {
        
            if (arg != null) { 
                AnalysisResults unbound_imported = (AnalysisResults) arg;
        
                String varName = var.getVarName().getUnqualifiedName();
        
                boolean isImported = isImported(var.getVarName());
                boolean isBound = isInCurrentModule(var.getVarName());
                boolean isLocallyBound = isBound(varName);
                
                
                if (!isBound && !isImported && !isLocallyBound) {
                    // not bound and not imported means it will be treated as a java identifier
                    addToMap(var, varName, unbound_imported.unboundIds);
        
                } else if (!isBound && isImported) {
                    // not bound and imported means treated as an import
                    addToMap(var, varName, unbound_imported.importedIds);
        
                } else if (isBound || isLocallyBound) {
        
                    // bound and not imported means treat as locally bound identifier
                    addToMap(var, varName, unbound_imported.locallyBoundIds);
                }
            }
            return super.visit_Expr_Var(var, arg);
        }
    }


    /** Map (String -> QualifiedName) from function names declared in using clauses to the QualifiedName of the entity that they refer to */
    private Map<String, QualifiedName> topLevelDeclarationNames = 
        new HashMap<String, QualifiedName>();


    private BindingRecorder recorder;
    
    private ModuleNameResolver moduleNameResolver; 
    
    private boolean isInitialized = false;
    
    /**
     * Finds all unbound references in an expression
     * Assumes imports and top level declarations have 
     * already been initialized
     * @param expr
     * @return the results of the unbound checking.  Has information on
     * which references are bound, imported, or unbound
     */
    public AnalysisResults findUnbound(Expr expr) {
        AnalysisResults unbound_imported = new AnalysisResults();
        expr.accept(new ExpressionSearcher(), unbound_imported);
        return unbound_imported;
    }
    
    public AnalysisResults findAllIdentifiers(Expr expr) {
        AnalysisResults allIds = new AnalysisResults();
        
        for (String topLevelName : topLevelDeclarationNames.keySet()) {
            allIds.locallyBoundIds.put(topLevelName, new LinkedList<SourceRange>());
        }
        for (String topLevelName : (Iterable<String>) recorder.usingDataconsNames.keySet()) {
            allIds.importedIds.put(topLevelName, new LinkedList<SourceRange>());
        }
        for (String topLevelName : (Iterable<String>) recorder.usingFunctionNames.keySet()) {
            allIds.importedIds.put(topLevelName, new LinkedList<SourceRange>());
        }
        for (String topLevelName : (Iterable<String>) recorder.usingTypeClassNames.keySet()) {
            allIds.importedIds.put(topLevelName, new LinkedList<SourceRange>());
        }
        for (String topLevelName : (Iterable<String>) recorder.usingTypeconsNames.keySet()) {
            allIds.importedIds.put(topLevelName, new LinkedList<SourceRange>());
        }
        
        if (expr != null) {
            // get the local names and the unbound names
            expr.accept(new ExpressionSearcher(), allIds);
        }
        return allIds;
    }

    /**
     * Entry point into this class for starting the analysis
     * 
     * All of the top level declarations and import statements
     * are stored 
     * @param module
     */
    public void initialize(ModuleDefn module) {
        recorder = new BindingRecorder();
         Set<ModuleName> visibleModuleNames = new HashSet<ModuleName>();
         module.accept(recorder, visibleModuleNames);
        
        // the imported module names have been recorded. 
        // use them to create a module resolver
        visibleModuleNames.add(SourceModel.Name.Module.toModuleName(module.getModuleName()));
        moduleNameResolver = ModuleNameResolver.make(visibleModuleNames);
        
        isInitialized = true;
    }
    
    
    private void addToMap(Var var, String varName,
            Map<String, List<SourceRange>> map) {
        List<SourceRange> ranges;
        if (map.containsKey(varName)) {
            ranges = map.get(varName);
        } else {
            ranges = new LinkedList<SourceRange>();
            map.put(varName, ranges);
        }
        ranges.add(SourceMetricsManager.getSourceRange(var));
    }

    private boolean isImported(Name.Qualifiable varName) {
        if (!isInitialized) {
            initializeDefault();
        }
        QualifiedName qual = recorder.getQualifiedName(varName, moduleNameResolver);
        return qual != null && 
            !qual.getModuleName().equals(recorder.getModuleName());
    }

    /**
     * this version of isBound also looks at top level declarations
     */
    protected boolean isInCurrentModule(Name.Function name) {
        String unqualifiedName = name.getUnqualifiedName();
        if (name.getModuleName() == null) {
            return recorder.isBound(unqualifiedName) || isTopLevelDeclaration(unqualifiedName);
        } else if (name.getModuleName().equals(recorder.getModuleName())) {
            return isTopLevelDeclaration(unqualifiedName);
        } else {
            return false;
        }
    }
    
    boolean isTopLevelDeclaration(String varName) {
        return topLevelDeclarationNames.containsKey(varName);
    }
    
    /**
     * when there is no EmbeddedCal module, the moduleNameResolver and module name will
     * not be initialized.  Instead, initialize it with an empty module
     */
    private void initializeDefault() {
        ModuleDefn defaultModule = ModuleDefn.make(ModuleName.make("Cal.EmbeddedCal"), 
                new Import[] { Import.make(ModuleName.make("Cal.Core.Prelude")) }, new TopLevelSourceElement[0] );
        initialize(defaultModule);
    }
    
    Pair<QualifiedName, Category> findQualifiedName(String name) {
        if (!isInitialized) return null;
        
        if (recorder.usingDataconsNames.containsKey(name)) {
            return new Pair<QualifiedName, Category>(
                    (QualifiedName) recorder.usingDataconsNames.get(name), 
                    Category.DATA_CONSTRUCTOR);
        } else if (recorder.usingFunctionNames.containsKey(name)) {
            return new Pair<QualifiedName, Category>(
                    (QualifiedName) recorder.usingFunctionNames.get(name), 
                    Category.TOP_LEVEL_FUNCTION_OR_CLASS_METHOD);
        } else if (recorder.usingTypeClassNames.containsKey(name)) {
            return new Pair<QualifiedName, Category>(
                    (QualifiedName) recorder.usingTypeClassNames.get(name), 
                    Category.TYPE_CLASS);
        } else if (recorder.usingTypeconsNames.containsKey(name)) {
            return new Pair<QualifiedName, Category>(
                    (QualifiedName) recorder.usingTypeconsNames.get(name), 
                    Category.TYPE_CONSTRUCTOR);
        } else if (topLevelDeclarationNames.containsKey(name)) { 
            return new Pair<QualifiedName, Category>(
                    topLevelDeclarationNames.get(name), 
                    Category.TOP_LEVEL_FUNCTION_OR_CLASS_METHOD);
        } else {
            // we may have a qualified name
            try {
                String[] parts = name.split("\\.");
                int numParts = parts.length-1;
                while (numParts > 0) {
                    String qualifier = concat(parts, numParts);
                    ModuleName mName = ModuleName.make(qualifier);
                    mName = moduleNameResolver.resolve(mName).getResolvedModuleName();
                    if (mName != null) {
                        return new Pair<QualifiedName, Category> (
                                QualifiedName.make(mName.toString(), parts[numParts]),
                                null);
                    }
                    numParts--;
                }
            } catch (IllegalArgumentException e) {
                // occurs when the name is a Java name in a foreign import statement
                // we can safely ignore
            }
            return null;
        }
    }
    
    private String concat(String[] parts, int end) {
        StringBuffer sb = new StringBuffer();
        for (int cnt = 0; cnt < end && cnt < parts.length; cnt++) {
            sb.append(parts[cnt]);
            if (cnt < end-1) {
                sb.append(".");
            }
        }
        return sb.toString();
    }
}