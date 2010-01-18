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
 * BindingTrackingSourceModelTraverser.java
 * Creation date: (Feb 14, 2006)
 * By: James Wright
 * 
 * Moved to the CAL_Eclipse_EmbeddedEditor project by Andrew Eisenberg
 * June 20, 2007
 */
package org.openquark.cal.eclipse.embedded.analyzer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.collections.ArrayStack;
import org.openquark.cal.compiler.FieldName;
import org.openquark.cal.compiler.ModuleName;
import org.openquark.cal.compiler.ModuleNameResolver;
import org.openquark.cal.compiler.QualifiedName;
import org.openquark.cal.compiler.SourceMetricsManager;
import org.openquark.cal.compiler.SourceModel;
import org.openquark.cal.compiler.SourceModelTraverser;
import org.openquark.cal.compiler.SourceRange;
import org.openquark.cal.compiler.SourceModel.ArgBindings;
import org.openquark.cal.compiler.SourceModel.FieldPattern;
import org.openquark.cal.compiler.SourceModel.Import;
import org.openquark.cal.compiler.SourceModel.LocalDefn;
import org.openquark.cal.compiler.SourceModel.ModuleDefn;
import org.openquark.cal.compiler.SourceModel.Name;
import org.openquark.cal.compiler.SourceModel.Pattern;
import org.openquark.cal.compiler.SourceModel.Expr.Lambda;
import org.openquark.cal.compiler.SourceModel.Expr.Let;
import org.openquark.cal.compiler.SourceModel.Expr.Case.Alt.UnpackDataCons;
import org.openquark.cal.compiler.SourceModel.Expr.Case.Alt.UnpackListCons;
import org.openquark.cal.compiler.SourceModel.Expr.Case.Alt.UnpackRecord;
import org.openquark.cal.compiler.SourceModel.Expr.Case.Alt.UnpackTuple;
import org.openquark.cal.compiler.SourceModel.FunctionDefn.Algebraic;
import org.openquark.cal.compiler.SourceModel.Import.UsingItem;
import org.openquark.cal.compiler.SourceModel.LocalDefn.Function.Definition;


/**
 * An implementation of SourceModelTraverser that tracks the current local definitions that
 * are in scope.  The current top-level function is considered to be in scope.
 * 
 * @param <R> the return type. If the return value is not used, specify {@link Void}.
 * 
 * @author James Wright
 */
class BindingVisitor<R> extends SourceModelTraverser<Object, R> { 

    /**
     * Name of the module currently being processed.
     * This will be set when a ModuleDefn is visited.
     */
    private ModuleName moduleName;

    /** 
     * ArrayStack of Maps (String -> SourceElement) that map from bindings
     * currently in scope to the corresponding source element.
     */
    private final ArrayStack currentBindings;

    /**
     * ArrayStacks of Maps (String -> LocalFunctionIdentifier) that map from
     * bindings of local function names currently in scope to the corresponding
     * LocalFunctionIdentifier.
     */
    private final ArrayStack currentLocalFunctionIdentifierBindings;

    /** Used for generating LocalFunctionIdentifiers when we encounter local functions. */
    private final LocalFunctionIdentifierGenerator localFunctionIdentifierGenerator;

    /** Map (String -> QualifiedName) from function names declared in using clauses to the QualifiedName of the entity that they refer to */
    final Map usingFunctionNames = new HashMap();

    /** Map (String -> QualifiedName) from datacons names declared in using clauses to the QualifiedName of the entity that they refer to */
    final Map usingDataconsNames = new HashMap();

    /** Map (String -> QualifiedName) from typecons names declared in using clauses to the QualifiedName of the entity that they refer to */
    final Map usingTypeconsNames = new HashMap();

    /** Map (String -> QualifiedName) from type class names declared in using clauses to the QualifiedName of the entity that they refer to */
    final Map usingTypeClassNames = new HashMap();

    /**
     * A base class for implementing algorithms that handle the processing of bindings for local definitions (both local function
     * definitions and local pattern match declarations).
     * 
     * @param <T> the argument type. If the visitation argument is not used, specify {@link Void}.
     * @param <R> the return type. If the return value is not used, specify {@link Void}.
     * 
     * @author Joseph Wong
     */
    static abstract class LocalBindingsProcessor<T, R> extends SourceModelTraverser<T, R> {

        /**
         * Processes a local definition binding.
         * @param name the name being bound.
         * @param localDefinition the source element corresponding to the definition/binding.
         * @param arg any visitation argument.
         */
        abstract void processLocalDefinitionBinding(final String name, final SourceModel.SourceElement localDefinition, final T arg);
        
        /**
         * Performs additional processing for a local function definition.
         * @param function the local function definition.
         * @param arg any visitation argument.
         */
        void additionallyProcessLocalDefnFunctionDefinition(final LocalDefn.Function.Definition function, final T arg) {}
        
        /**
         * Performs additional processing for a pattern-bound variable in a local pattern match declaration.
         * @param var the pattern-bound variable.
         * @param arg any visitation argument.
         */
        void additionallyProcessPatternVar(final Pattern.Var var, final T arg) {}

        /**
         * Performs additional processing for a punned field pattern in a local pattern mathc declaration.
         * @param fieldName the punned field name.
         * @param fieldNameSourceRange the source range for the field name. Can be null.
         * @param arg any visitation argument.
         */
        void additionallyProcessPunnedTextualFieldPattern(final FieldName.Textual fieldName, final SourceRange fieldNameSourceRange, final T arg) {}
        
        /**
         * {@inheritDoc}
         */
        public R visit_LocalDefn_Function_Definition(final LocalDefn.Function.Definition function, final T arg) {
            processLocalDefinitionBinding(function.getName(), function, arg); // the function defn is the bound element
            additionallyProcessLocalDefnFunctionDefinition(function, arg);
            return null;
        }
        
        /**
         * {@inheritDoc}
         */
        public R visit_Pattern_Var(final Pattern.Var var, final T arg) {
            processLocalDefinitionBinding(var.getName(), var, arg); // the pattern var is the bound element
            additionallyProcessPatternVar(var, arg);
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public R visit_FieldPattern(final FieldPattern fieldPattern, final T arg) {
            // Handle punning
            if (fieldPattern.getPattern() == null) {
                // punning.
                
                // Textual field names become Vars of the same name.
                // Ordinal field names become wildcards ("_").
                final FieldName fieldName = fieldPattern.getFieldName().getName();
                if (fieldName instanceof FieldName.Textual) {
                    processLocalDefinitionBinding(fieldName.getCalSourceForm(), fieldPattern, arg); // the field pattern is the bound element
                    additionallyProcessPunnedTextualFieldPattern((FieldName.Textual)fieldName, SourceMetricsManager.getSourceRange(fieldPattern.getFieldName()), arg);
                }
            }
            
            // call the superclass impl to reach the pattern and visit it (if it is non-null)
            return super.visit_FieldPattern(fieldPattern, arg);
        }
        
        /**
         * {@inheritDoc}
         */
        public R visit_LocalDefn_PatternMatch_UnpackDataCons(final LocalDefn.PatternMatch.UnpackDataCons unpackDataCons, final T arg) {
            // visit only the patterns
            unpackDataCons.getArgBindings().accept(this, arg);
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public R visit_LocalDefn_PatternMatch_UnpackListCons(final LocalDefn.PatternMatch.UnpackListCons unpackListCons, final T arg) {
            // visit only the patterns
            unpackListCons.getHeadPattern().accept(this, arg);
            unpackListCons.getTailPattern().accept(this, arg);
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public R visit_LocalDefn_PatternMatch_UnpackRecord(final LocalDefn.PatternMatch.UnpackRecord unpackRecord, final T arg) {
            // visit only the field patterns (and not the base record pattern - since we do not support them in local pattern match decl)
            final int nFieldPatterns = unpackRecord.getNFieldPatterns();
            for (int i = 0; i < nFieldPatterns; i++) {
                unpackRecord.getNthFieldPattern(i).accept(this, arg);
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public R visit_LocalDefn_PatternMatch_UnpackTuple(final LocalDefn.PatternMatch.UnpackTuple unpackTuple, final T arg) {
            // visit only the patterns
            final int nPatterns = unpackTuple.getNPatterns();
            for (int i = 0; i < nPatterns; i++) {
                unpackTuple.getNthPattern(i).accept(this, arg);
            }
            return null;
        }
    }
    
    public BindingVisitor() {
        moduleName = null;
        localFunctionIdentifierGenerator = new LocalFunctionIdentifierGenerator();
        currentBindings = new ArrayStack();
        currentLocalFunctionIdentifierBindings = new ArrayStack();
    }

    /** {@inheritDoc} */
    public R visit_ModuleDefn(ModuleDefn defn, Object arg) {
        // Save modulename for clients
        moduleName = SourceModel.Name.Module.toModuleName(defn.getModuleName());
        return super.visit_ModuleDefn(defn, arg);
    }

    /** 
     * {@inheritDoc}
     * This implementation of visitImport passes the name of the imported module
     * to each of its child UsingItems.
     * 
     */
    public R visit_Import(Import importStmt, Object arg) {

        ModuleName importedModuleName = SourceModel.Name.Module.toModuleName(importStmt.getImportedModuleName());
        UsingItem[] usingItems = importStmt.getUsingItems();

        for(int i = 0, nUsingItems = usingItems.length; i < nUsingItems; i++) {
            // We pass the name of the imported module to using clause visitors so that
            // they can fill the using maps; we're overriding the default traversal method
            // here, so we don't call the superclass method.
            usingItems[i].accept(this, importedModuleName);
        }
    
        return null;
    }
    
    /** {@inheritDoc} */
    public R visit_Import_UsingItem_Function(UsingItem.Function usingItemFunction, Object arg) {

        if(arg != null && arg instanceof ModuleName) {
            // Fill up the using maps for name resolution
            ModuleName importedModuleName = (ModuleName)arg;
            String[] usingNames = usingItemFunction.getUsingNames();

            for(int i = 0, nNames = usingNames.length; i < nNames; i++) {
                usingFunctionNames.put(usingNames[i], QualifiedName.make(importedModuleName, usingNames[i]));
            }
        }

        return super.visit_Import_UsingItem_Function(usingItemFunction, arg);
    }

    /** {@inheritDoc} */
    public R visit_Import_UsingItem_DataConstructor(UsingItem.DataConstructor usingItemDataConstructor, Object arg) {

        if(arg != null && arg instanceof ModuleName) {
            // Fill up the using maps for name resolution
            ModuleName importedModuleName = (ModuleName)arg;
            String[] usingNames = usingItemDataConstructor.getUsingNames();

            for(int i = 0, nNames = usingNames.length; i < nNames; i++) {
                usingDataconsNames.put(usingNames[i], QualifiedName.make(importedModuleName, usingNames[i]));
            }
        }

        return super.visit_Import_UsingItem_DataConstructor(usingItemDataConstructor, arg);
    }

    /** {@inheritDoc} */
    public R visit_Import_UsingItem_TypeConstructor(UsingItem.TypeConstructor usingItemTypeConstructor, Object arg) {

        if(arg != null && arg instanceof ModuleName) {
            // Fill up the using maps for name resolution
            ModuleName importedModuleName = (ModuleName)arg;
            String[] usingNames = usingItemTypeConstructor.getUsingNames();

            for(int i = 0, nNames = usingNames.length; i < nNames; i++) {
                usingTypeconsNames.put(usingNames[i], QualifiedName.make(importedModuleName, usingNames[i]));
            }
        }

        return super.visit_Import_UsingItem_TypeConstructor(usingItemTypeConstructor, arg);
    }

    /** {@inheritDoc} */
    public R visit_Import_UsingItem_TypeClass(UsingItem.TypeClass usingItemTypeClass, Object arg) {

        if(arg != null && arg instanceof ModuleName) {
            // Fill up the using maps for name resolution
            ModuleName importedModuleName = (ModuleName)arg;
            String[] usingNames = usingItemTypeClass.getUsingNames();

            for(int i = 0, nNames = usingNames.length; i < nNames; i++) {
                usingTypeClassNames.put(usingNames[i], QualifiedName.make(importedModuleName, usingNames[i]));
            }
        }

        return super.visit_Import_UsingItem_TypeClass(usingItemTypeClass, arg);
    }

    /** {@inheritDoc} */
    public R visit_FunctionDefn_Algebraic(Algebraic algebraic, Object arg) {

        enterScope();

        localFunctionIdentifierGenerator.reset(algebraic.getName());

        for(int i = 0; i < algebraic.getNParameters(); i++) {
            SourceModel.Parameter param = algebraic.getNthParameter(i);
            addRegularBinding(param.getName(), param);
        }

        R ret = super.visit_FunctionDefn_Algebraic(algebraic, arg);

        localFunctionIdentifierGenerator.reset(null);

        leaveScope();
        return ret;
    }

    /** {@inheritDoc} */
    public R visit_Expr_Lambda(Lambda lambda, Object arg) {

        enterScope();

        for(int i = 0; i < lambda.getNParameters(); i++) {
            SourceModel.Parameter param = lambda.getNthParameter(i);
            addRegularBinding(param.getName(), param);
        }

        R ret = super.visit_Expr_Lambda(lambda, arg);
        leaveScope();
        return ret;
    }

    /** {@inheritDoc} */
    public R visit_Expr_Let(Let let, Object arg) {

        enterScope();

        // Let expressions are mutually recursive, so we want to bind the
        // function names before we enter the new scope associated with each local function.

        /**
         * Handles the adding of bindings for local definitions (both local functions and local pattern match declarations).
         * This is done by walking the local definitions and adding a binding for each local function / pattern-bound variable
         * encountered. The synthetic local function generated by the compiler for desugaring a local pattern match declaration
         * is also taken into account.
         * 
         * @author Joseph Wong
         */
        class LocallyDefinedNamesCollector extends LocalBindingsProcessor<LinkedHashSet<String>, R> {

            /**
             * {@inheritDoc}
             */
            void processLocalDefinitionBinding(final String name, final SourceModel.SourceElement localDefinition, final LinkedHashSet<String> arg) {
                addLocalDefinitionBinding(name, localDefinition);
            }
            
            /**
             * {@inheritDoc}
             */
            void additionallyProcessPatternVar(final Pattern.Var var, final LinkedHashSet<String> patternVarNames) {
                patternVarNames.add(var.getName());
            }

            /**
             * {@inheritDoc}
             */
            void additionallyProcessPunnedTextualFieldPattern(final FieldName.Textual fieldName, final SourceRange fieldNameSourceRange, final LinkedHashSet<String> patternVarNames) {
                patternVarNames.add(fieldName.getCalSourceForm());
            }
            
            /**
             * Adds an additional binding for the synthetic local function which is generated by the compiler to host the defining
             * expression of a local pattern match declaration. This is done to keep the local function identifier generator in
             * sync with what the compiler would do.
             * 
             * @param patternMatchDecl the pattern match declaration.
             * @param patternVarNames the LinkedHashSet of the pattern variable names, in source order.
             */
            private void addBindingForSyntheticLocalDefinition(final LocalDefn.PatternMatch patternMatchDecl, final LinkedHashSet<String> patternVarNames) {
                addLocalDefinitionBinding(makeTempVarNameForDesugaredLocalPatternMatchDecl(patternVarNames), patternMatchDecl);
            }

            /**
             * {@inheritDoc}
             */
            public R visit_LocalDefn_PatternMatch_UnpackDataCons(final LocalDefn.PatternMatch.UnpackDataCons unpackDataCons, final LinkedHashSet<String> arg) {
                // visit only the patterns
                final LinkedHashSet<String> patternVarNames = new LinkedHashSet<String>();
                super.visit_LocalDefn_PatternMatch_UnpackDataCons(unpackDataCons, patternVarNames);
                // add the synthetic definition last
                addBindingForSyntheticLocalDefinition(unpackDataCons, patternVarNames);
                return null;
            }

            /**
             * {@inheritDoc}
             */
            public R visit_LocalDefn_PatternMatch_UnpackListCons(final LocalDefn.PatternMatch.UnpackListCons unpackListCons, final LinkedHashSet<String> arg) {
                // visit only the patterns
                final LinkedHashSet<String> patternVarNames = new LinkedHashSet<String>();
                super.visit_LocalDefn_PatternMatch_UnpackListCons(unpackListCons, patternVarNames);
                // add the synthetic definition last
                addBindingForSyntheticLocalDefinition(unpackListCons, patternVarNames);
                return null;
            }

            /**
             * {@inheritDoc}
             */
            public R visit_LocalDefn_PatternMatch_UnpackRecord(final LocalDefn.PatternMatch.UnpackRecord unpackRecord, final LinkedHashSet<String> arg) {
                // visit only the field patterns (and not the base record pattern - since we do not support them in local pattern match decl)
                final LinkedHashSet<String> patternVarNames = new LinkedHashSet<String>();
                super.visit_LocalDefn_PatternMatch_UnpackRecord(unpackRecord, patternVarNames);
                // add the synthetic definition last
                addBindingForSyntheticLocalDefinition(unpackRecord, patternVarNames);
                return null;
            }

            /**
             * {@inheritDoc}
             */
            public R visit_LocalDefn_PatternMatch_UnpackTuple(final LocalDefn.PatternMatch.UnpackTuple unpackTuple, final LinkedHashSet<String> arg) {
                // visit only the patterns
                final LinkedHashSet<String> patternVarNames = new LinkedHashSet<String>();
                super.visit_LocalDefn_PatternMatch_UnpackTuple(unpackTuple, patternVarNames);
                // add the synthetic definition last
                addBindingForSyntheticLocalDefinition(unpackTuple, patternVarNames);
                return null;
            }
        }

        // Use the LocallyDefinedNamesCollector to visit the let definitions
        final LocallyDefinedNamesCollector locallyDefinedNamesCollector = new LocallyDefinedNamesCollector();
        final int nLocalFunctions = let.getNLocalDefinitions();
        for (int i = 0; i < nLocalFunctions; i++) {
            let.getNthLocalDefinition(i).accept(locallyDefinedNamesCollector, null);
        }

        // Now call the superclass implementation to walk through the let expression with the right name bindings
        R ret = super.visit_Expr_Let(let, arg);
        leaveScope();
        return ret;
    }

    /** {@inheritDoc} */
    public R visit_LocalDefn_Function_Definition(Definition function, Object arg) {

        enterScope();

        for(int i = 0; i < function.getNParameters(); i++) {
            SourceModel.Parameter param = function.getNthParameter(i); 
            addRegularBinding(param.getName(), param);
        }

        R ret = super.visit_LocalDefn_Function_Definition(function, arg);

        leaveScope();            
        return ret;
    }

    /** {@inheritDoc} */
    public R visit_Expr_Case_Alt_UnpackDataCons(UnpackDataCons cons, Object arg) {
        enterScope();
        handleArgBindings(cons.getArgBindings());

        R ret = super.visit_Expr_Case_Alt_UnpackDataCons(cons, arg);
        leaveScope();
        return ret;
    }

    /**
     * Adds all of the bindings in argBindings to the current scope.
     *    
     * visitCaseExprUnpackDataConsAlt and visitCaseExprUnpackDataConsGroupAlt both
     * do exactly the same (rather involved) processing on their arg bindings.  This
     * common processing is factored into handleArgBindings.
     * 
     * @param argBindings ArgBindings the bindings to process 
     */
    private void handleArgBindings(ArgBindings argBindings) {

        if(argBindings instanceof ArgBindings.Matching) {

            ArgBindings.Matching matchingArgBindings = (ArgBindings.Matching)argBindings;

            for (int i = 0; i < matchingArgBindings.getNFieldPatterns(); i++) {
                FieldPattern fieldPattern = matchingArgBindings.getNthFieldPattern(i);
                Pattern pattern = fieldPattern.getPattern();

                if (pattern == null) {
                    // punning.

                    // Textual field names become Vars of the same name.
                    // Ordinal field names become wildcards ("_").
                    FieldName fieldName = fieldPattern.getFieldName().getName();
                    if (fieldName instanceof FieldName.Textual) {
                        pattern = Pattern.Var.make(fieldName.getCalSourceForm());
                    }
                }

                if (pattern instanceof Pattern.Var) {
                    Pattern.Var patternVar = (Pattern.Var)pattern;
                    addRegularBinding(patternVar.getName(), patternVar);
                }
            }

        } else if (argBindings instanceof ArgBindings.Positional) {

            ArgBindings.Positional positionalArgBindings = (ArgBindings.Positional)argBindings;

            for (int i = 0; i < positionalArgBindings.getNPatterns(); i++) {
                Pattern pattern = positionalArgBindings.getNthPattern(i);
                if (pattern instanceof Pattern.Var) {
                    Pattern.Var patternVar = (Pattern.Var)pattern;
                    addRegularBinding(patternVar.getName(), patternVar);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public R visit_Expr_Case_Alt_UnpackListCons(UnpackListCons cons, Object arg) {

        enterScope();

        if(cons.getHeadPattern() instanceof Pattern.Var) {
            Pattern.Var patternVar = (Pattern.Var)cons.getHeadPattern();
            addRegularBinding(patternVar.getName(), patternVar);
        }

        if(cons.getTailPattern() instanceof Pattern.Var) {
            Pattern.Var patternVar = (Pattern.Var)cons.getTailPattern();
            addRegularBinding(patternVar.getName(), patternVar);
        }

        R ret = super.visit_Expr_Case_Alt_UnpackListCons(cons, arg);
        leaveScope();
        return ret;
    }

    /** {@inheritDoc} */
    public R visit_Expr_Case_Alt_UnpackRecord(UnpackRecord record, Object arg) {

        enterScope();

        for(int i = 0; i < record.getNFieldPatterns(); i++) {
            FieldPattern pattern = record.getNthFieldPattern(i);

            if(pattern.getPattern() != null && 
                    pattern.getPattern() instanceof Pattern.Var) {
                Pattern.Var patternVar = (Pattern.Var)pattern.getPattern();
                addRegularBinding(patternVar.getName(), patternVar);

            } else if (pattern.getPattern() == null) {
                addRegularBinding(pattern.getFieldName().getName().getCalSourceForm(), pattern);
            }
        }

        if(record.getBaseRecordPattern() != null) {
            if(record.getBaseRecordPattern() instanceof Pattern.Var) {
                Pattern.Var patternVar = (Pattern.Var)record.getBaseRecordPattern();
                addRegularBinding(patternVar.getName(), patternVar);
            }
        }

        R ret = super.visit_Expr_Case_Alt_UnpackRecord(record, arg);
        leaveScope();
        return ret;
    }

    /** {@inheritDoc} */
    public R visit_Expr_Case_Alt_UnpackTuple(UnpackTuple tuple, Object arg) {

        enterScope();

        for(int i = 0; i < tuple.getNPatterns(); i++) {
            Pattern pattern = tuple.getNthPattern(i);
            if(pattern instanceof Pattern.Var) {
                Pattern.Var patternVar = (Pattern.Var)pattern;
                addRegularBinding(patternVar.getName(), patternVar);
            }
        }

        R ret = super.visit_Expr_Case_Alt_UnpackTuple(tuple, arg);
        leaveScope();
        return ret;
    }

    /**
     * Adds a binding to the current innermost scope
     * @param name Name of the binding to add
     * @param sourceElement SourceElement that name should be bound to
     */
    private void addRegularBinding(String name, SourceModel.SourceElement sourceElement) {
        Map currentScope = (Map)currentBindings.peek();
        currentScope.put(name, sourceElement);
    }

    /**
     * Adds a binding to the current innermost scope and records the unique identifier for
     * a local definition (a function or a pattern variable in a pattern match declaration).
     * @param name String name of the local definition to add a binding for
     * @param localDefinition SourceModel for the local function
     */
    private void addLocalDefinitionBinding(String name, SourceModel.SourceElement localDefinition) {
        addRegularBinding(name, localDefinition);

        Map currentIdentifierScope = (Map)currentLocalFunctionIdentifierBindings.peek();

        // Don't try to track local function identifiers without a module name and toplevel function
        if(moduleName != null && getCurrentFunction() != null) {
            LocalFunctionIdentifier localFunctionIdentifier = localFunctionIdentifierGenerator.generateLocalFunctionIdentifier(moduleName, name);
            currentIdentifierScope.put(name, localFunctionIdentifier);
        }
    }

    /**
     * Adds a new scope to the current bindings.
     */
    private void enterScope() {
        currentBindings.push(new HashMap());
        currentLocalFunctionIdentifierBindings.push(new HashMap());
    }

    /**
     * Removes the current scope from the bindings.
     */
    private void leaveScope() {
        currentBindings.pop();
        currentLocalFunctionIdentifierBindings.pop();
    }

    /** @return The name of the currently in-scope function, if any */
    String getCurrentFunction() {
        return localFunctionIdentifierGenerator.getCurrentFunction();
    }

    /** @return The name of the module being processed */
    ModuleName getModuleName() {
        return moduleName;
    }

    /**
     * @param name Name to fetch the qualifiedName for 
     * @param moduleNameResolver the module name resolver to use for resolving module names.
     * @return QualifiedName of the top-level entity that name refers to, or null if name does not refer to
     *          a top-level entity.
     */
    QualifiedName getQualifiedName(Name.Qualifiable name, ModuleNameResolver moduleNameResolver) {

        String unqualifiedName = name.getUnqualifiedName();
        if(name.getModuleName() != null) {
            ModuleNameResolver.ResolutionResult resolution = moduleNameResolver.resolve(SourceModel.Name.Module.toModuleName(name.getModuleName()));
            ModuleName resolvedModuleName = resolution.getResolvedModuleName();
            return QualifiedName.make(resolvedModuleName, name.getUnqualifiedName());
        }

        if(isBound(unqualifiedName)) {
            return null;
        }

        if(name instanceof Name.Function && usingFunctionNames.containsKey(unqualifiedName)) {
            return (QualifiedName)usingFunctionNames.get(unqualifiedName);
        }

        if(name instanceof Name.DataCons && usingDataconsNames.containsKey(unqualifiedName)) {
            return (QualifiedName)usingDataconsNames.get(unqualifiedName);
        }

        if(name instanceof Name.TypeCons && usingTypeconsNames.containsKey(unqualifiedName)) {
            return (QualifiedName)usingTypeconsNames.get(unqualifiedName);
        }

        if(name instanceof Name.TypeClass && usingTypeClassNames.containsKey(unqualifiedName)) {
            return (QualifiedName)usingTypeClassNames.get(unqualifiedName);
        }

        if(name instanceof Name.WithoutContextCons) {
            if(usingDataconsNames.containsKey(unqualifiedName)) {
                return (QualifiedName)usingDataconsNames.get(unqualifiedName);
            }

            if(usingTypeconsNames.containsKey(unqualifiedName)) {
                return (QualifiedName)usingTypeconsNames.get(unqualifiedName);
            }

            if(usingTypeClassNames.containsKey(unqualifiedName)) {
                return (QualifiedName)usingTypeClassNames.get(unqualifiedName);
            }
        }

        return QualifiedName.make(getModuleName(), unqualifiedName);
    }

    /** @return A clone of the LocalFunctionIdentifierGenerator */
    LocalFunctionIdentifierGenerator getLocalFunctionNameGenerator() {
        return (LocalFunctionIdentifierGenerator)localFunctionIdentifierGenerator.clone();
    }

    /** 
     * @return True if name is currently bound, or false otherwise.
     * @param name String name to check
     */
    protected boolean isBound(String name) {

        for(int i = 0, nBindings = currentBindings.size(); i < nBindings; i++) {
            Map currentScope = (Map)currentBindings.peek(i);
            if(currentScope.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param name Name of binding to retrieve the defining SourceElement for
     * @return The SourceElement that defines the current binding specified by
     *          name, or null if name is not currently bound.
     */
    SourceModel.SourceElement getBoundElement(String name) {

        for(int i = 0, nBindings = currentBindings.size(); i < nBindings; i++) {
            Map currentScope = (Map)currentBindings.peek(i);
            Object sourceElement = currentScope.get(name);
            if(sourceElement != null) {
                return (SourceModel.SourceElement)sourceElement;
            }
        }

        return null;
    }

    /**
     * @param name Name of a local function definition that is current in scope
     * @return The LocalFunctionIdentifier that corresponds to the current binding of name,
     *          or null if name is not currently bound to a local function.
     */
    LocalFunctionIdentifier getBoundLocalFunctionIdentifier(String name) {
        for(int i = 0, nBindings = currentBindings.size(); i < nBindings; i++) {
            Map currentScope = (Map)currentBindings.peek(i);
            Object sourceElement = currentScope.get(name);

            if(sourceElement == null) {
                continue;
            }

            if(!(sourceElement instanceof LocalDefn.Function || sourceElement instanceof Pattern.Var || sourceElement instanceof FieldPattern)) {
                return null;
            }

            Map currentIdentifierScope = (Map)currentLocalFunctionIdentifierBindings.peek(i);
            return (LocalFunctionIdentifier)currentIdentifierScope.get(name);
        }

        return null;
    }

    /**
     * Constructs a name, based on the pattern-bound variable names, for the synthetic local function for
     * hosting the defining expression of the pattern match declaration, which is to be added to the desugared tree.
     * 
     * @param patternVarNames a Collection of the pattern variable names, in source order.
     * @return a name for the synthetic local function.
     */
    static String makeTempVarNameForDesugaredLocalPatternMatchDecl(Collection/*String*/ patternVarNames) {
        final StringBuffer nameBuffer = new StringBuffer("$pattern");
        for (Iterator it = patternVarNames.iterator(); it.hasNext(); ) {
            final String patternVarName = (String)it.next();
            nameBuffer.append('_').append(patternVarName);
        }
        return nameBuffer.toString();
    }

}
