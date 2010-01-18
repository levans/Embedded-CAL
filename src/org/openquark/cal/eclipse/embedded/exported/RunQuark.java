/*
 * Copyright (c) 2007 BUSINESS OBJECTS SOFTWARE LIMITED
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *  
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *  
 *     * Neither the name of Business Objects nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * RunQuark.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.exported;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openquark.cal.compiler.AdjunctSource;
import org.openquark.cal.compiler.Compiler;
import org.openquark.cal.compiler.CompilerMessage;
import org.openquark.cal.compiler.CompilerMessageLogger;
import org.openquark.cal.compiler.FieldName;
import org.openquark.cal.compiler.MessageLogger;
import org.openquark.cal.compiler.ModuleName;
import org.openquark.cal.compiler.QualifiedName;
import org.openquark.cal.compiler.Scope;
import org.openquark.cal.compiler.SourceModel;
import org.openquark.cal.compiler.SourceModelCopier;
import org.openquark.cal.compiler.SourceModelModuleSource;
import org.openquark.cal.compiler.SourceModelUtilities;
import org.openquark.cal.compiler.CompilerMessage.Severity;
import org.openquark.cal.compiler.SourceModel.Expr;
import org.openquark.cal.compiler.SourceModel.FunctionDefn;
import org.openquark.cal.compiler.SourceModel.Import;
import org.openquark.cal.compiler.SourceModel.ModuleDefn;
import org.openquark.cal.compiler.SourceModel.Parameter;
import org.openquark.cal.compiler.SourceModel.TypeExprDefn;
import org.openquark.cal.compiler.SourceModel.Expr.Var;
import org.openquark.cal.compiler.SourceModel.TypeExprDefn.Record.FieldTypePair;
import org.openquark.cal.compiler.io.EntryPoint;
import org.openquark.cal.compiler.io.EntryPointSpec;
import org.openquark.cal.compiler.io.InputPolicy;
import org.openquark.cal.compiler.io.OutputPolicy;
import org.openquark.cal.machine.CALExecutor;
import org.openquark.cal.machine.Module;
import org.openquark.cal.runtime.CALExecutorException;
import org.openquark.cal.runtime.CalValue;
import org.openquark.cal.services.BasicCALServices;

/**
 * 
 * @author aeisenberg
 * 
 * This class provides a simple wrapper to run quark expressions and functions
 * from the default synthetic module
 * <p>
 * It is used by clients to run the code from embedded editors. Programmers do
 * not need to explicitly enter this code, but rather embedded editors
 * automatically serialize to text that refers to code from this class
 */
public class RunQuark implements IEmbeddedCalConstants {
    /** constant referring to the evaluateExpression method */
    public static final String EVALUATE_EXPRESSION = "evaluateExpression";
    /**
     * constant referring to the evaluateExpressionThrowing method where exceptions must be explicitly handled 
     */
    public static final String EVALUATE_EXPRESSION_THROWING = "evaluateExpressionThrowing";
    private final static ModuleName PRELUDE_NAME = ModuleName
            .make("Cal.Core.Prelude");
    private final static ModuleName EMBEDDED_CAL_NAME = ModuleName
            .make(EMBEDDED_CAL_MODULE);
    /**
     * A simple class to combine a runtime input value (from Java), a CAL input
     * policy, and a CAL variable name.
     * 
     * By default, all input policies are specified as null. This triggers input
     * policy inference at runtime.
     */
    public final static class InputTuple {
        public InputTuple(Object inputValue, InputPolicy policy, String varName) {
            this.inputValue = inputValue;
            this.policy = policy;
            this.varName = varName;
        }

        final Object inputValue;

        final InputPolicy policy;

        final String varName;

        /**
         * Extracts the parameters from an array of input tuples
         * 
         * Ignores any element that is a source model
         * 
         * @param tuples
         * @return array of parameters, one for each tuple
         */
        static Parameter[] params(InputTuple[] tuples) {
            List<Parameter> params = new ArrayList<Parameter>(tuples.length);
            for (int cnt = 0; cnt < tuples.length; cnt++) {
                if (!(tuples[cnt].inputValue instanceof Expr)) {
                    params.add(Parameter.make(tuples[cnt].varName, false));
                }
            }
            return params.toArray(new Parameter[params.size()]);
        }

        /**
         * Extracts the input policies from an array of input tuples. Any input
         * tuple that has a <code>null</code> input policy triggers input
         * policy inference using the input value.
         * 
         * Ignores any element that is a source model
         * 
         * @param tuples
         * @return array of input policies, one for each tuple
         */
        static InputPolicy[] policies(InputTuple[] tuples) {
            List<InputPolicy> policies = new ArrayList<InputPolicy>(
                    tuples.length);
            for (int cnt = 0; cnt < tuples.length; cnt++) {
                if (!(tuples[cnt].inputValue instanceof Expr)) {
                    if (tuples[cnt].policy == null) {
                        policies.add(inferInputPolicy(tuples[cnt].inputValue));
                    } else {
                        policies.add(tuples[cnt].policy);
                    }
                }
            }
            return policies.toArray(new InputPolicy[policies.size()]);
        }

        /**
         * Extracts the input values from an array of input tuples.
         * 
         * Ignores any element that is a source model
         * 
         * @param tuples
         * @return array of input values, one for each tuple
         */
        static Object[] values(InputTuple[] tuples) {
            List<Object> values = new ArrayList<Object>(tuples.length);
            for (int cnt = 0; cnt < tuples.length; cnt++) {
                if (!(tuples[cnt].inputValue instanceof Expr)) {
                    values.add(tuples[cnt].inputValue);
                }
            }
            return values.toArray();
        }

        /**
         * Extracts all the input tuples that correspond to source model these
         * tuples will get spliced in directly to the expression. It is similar
         * to an unquote
         * 
         * @param tuples
         * @return an array of Pairs that correspond to all inputs that are
         *         source models
         */
        static Pair<String, Expr>[] spliceable(InputTuple[] tuples) {
            List<Pair<String, Expr>> splice = new ArrayList<Pair<String, Expr>>(
                    tuples.length);

            for (int cnt = 0; cnt < tuples.length; cnt++) {
                if (tuples[cnt].inputValue instanceof Expr) {
                    splice.add(new Pair<String, Expr>(tuples[cnt].varName,
                            (Expr) tuples[cnt].inputValue));
                }
            }
            return splice.toArray(new Pair[splice.size()]);
        }
    }

    private static CompilerMessageLogger messageLogger = new MessageLogger();

    private static BasicCALServices calServices = null;

    private static Compiler compiler;

    private static CALExecutor executor;

    public static BasicCALServices getCalServices() {
        return calServices;
    }

    /**
     * initialize with the BasicCALServices passed in.
     * 
     * @param services
     *            The BasicCALServices that this class will use
     */
    public static void init(BasicCALServices services) {
        calServices = services;
        if (calServices != null) {
            compiler = calServices.getWorkspaceManager().getCompiler();
            executor = calServices.getWorkspaceManager()
            .makeExecutorWithNewContextAndDefaultProperties();

            // check to see if our Module exists
            Module embeddedModule = calServices.getWorkspaceManager()
            .getModule(EMBEDDED_CAL_NAME);
            if (embeddedModule == null) {
                calServices.addNewModule(new SourceModelModuleSource(ModuleDefn
                        .make(EMBEDDED_CAL_NAME, new Import[] { Import
                                .make(PRELUDE_NAME) },
                                new SourceModel.TopLevelSourceElement[] {})),
                                messageLogger);

                outputMessages();
            }
        } else {
            System.err.println("Error initializing BasicCALServices..." +
            		"invalid workspace file?");
            compiler = null;
            executor = null;
        }
    }
    
    
    /**
     * Creates a new module.  Each time this method is executed, a module 
     * with the same name (if it already exists) is replaced by the module created
     * here.
     * 
     * @param moduleText the text of the module
     * @param moduleNameStr the module name.  only used for editing
     * @param width width of the embedded editor. only used for editing
     * @param height height of the embedded editor. only used for editing
     * @param isValid true if the contents are a syntactically correct module definition. only used for editing
     * @param showAll true if the expanded view of the embedded editor is being shown. only used for editing
     */
    public static void declareModule(String moduleText, String moduleNameStr, 
            int width, int height, boolean isValid, boolean showAll) {
        ModuleDefn defn = SourceModelUtilities.TextParsing.
                parseModuleDefnIntoSourceModel(moduleText, messageLogger);
        
        if (calServices == null) {
          init();
        }


        if (defn != null) {
            calServices.addNewModule(new SourceModelModuleSource(defn), messageLogger);
        } else {
            ModuleName moduleName = ModuleName.make(moduleNameStr);
            calServices.addNewModule(new SourceModelModuleSource(ModuleDefn
                    .make(moduleName, new Import[] { Import
                            .make(PRELUDE_NAME) },
                            new SourceModel.TopLevelSourceElement[] {})),
                            messageLogger);
        }
    }

    /**
     * this is the method that gets called to do the embedding. Note that the
     * last arguments are not used at runtime. Rather, they are only used at
     * edit time to store the state of the embedded editor.
     * 
     * @param exprText
     *            The text of the expression. all escaped values are replaced by
     *            argument names
     * 
     * @param inputs
     *            each tuple contains an argument and its input policy
     * 
     * @param outPolicy
     *            the output policy
     * @param moduleNameStr Name of the module to run this code in
     * 
     * @param width
     *            has no runtime effect. it is used for editor purpose storing
     *            the width of the embedded editor
     * 
     * @param height
     *            has no runtime effect. it is used for editor purpose storing
     *            the height of the embedded editor
     * 
     * @param isValid has no runtime effect
     *            this argument is not used at runtime or compile time it is
     *            used at edit time to denote whether or not the exprText is
     *            valid (syntactically correct) cal text
     * @param showAll has no runtime effect 
     *            true if the contained editor is in the expanded view, false if
     *            it is in compact view
     * 
     * @return the value of the CAL expression
     */
    public static Object evaluateExpression(String exprText,
            InputTuple[] inputs, OutputPolicy outPolicy, String moduleNameStr, 
            int width, int height, boolean isValid, boolean showAll) {

        try {
            return evaluateExpressionThrowing(exprText, inputs, outPolicy, 
                    moduleNameStr, width, height, isValid, showAll);

        } catch (CALExecutorException e) {
            outputMessages();
            e.printStackTrace();
            return null;
        }
    }
    
    
    /**
     * @see #evaluateExpression(String, org.openquark.cal.eclipse.embedded.exported.RunQuark.InputTuple[], OutputPolicy, String, int, int, boolean, boolean)
     * @param exprText
     * @param inputs
     * @param outPolicy
     * @param moduleNameStr
     * @param width
     * @param height
     * @param isValid
     * @param showAll
     * @return the value of the CAL expression
     * @throws CALExecutorException in case of a CAL error
     */
    public static Object evaluateExpressionThrowing(String exprText,
            InputTuple[] inputs, OutputPolicy outPolicy, String moduleNameStr, int width, int height,
            boolean isValid, boolean showAll) throws CALExecutorException {

        messageLogger = new MessageLogger();
        
        if (calServices == null) {
            init();
        }
        
        if (outPolicy == null) {
          outPolicy = OutputPolicy.DEFAULT_OUTPUT_POLICY;
        }

        ModuleName moduleName;
        if (moduleNameStr == null) {
            moduleNameStr = EMBEDDED_CAL_MODULE;
        }
        moduleName = ModuleName.make(moduleNameStr);

        EntryPointSpec spec = EntryPointSpec.make(QualifiedName.make(
                moduleNameStr, "func"), InputTuple.policies(inputs),
                outPolicy);


        EntryPoint point = compiler.getEntryPoint(makeAdjunctSource(
                exprText, inputs), spec, moduleName, messageLogger);

        return executor.exec(point, InputTuple.values(inputs));
    }

    

    /**
     * makes a best guess on what the input policy should be. if it can't be
     * determined, then the failsafe policy is used
     * 
     * if a Collection or Enumerable is passed in, then check to see if all
     * elements are of the same type. If yes, then parameterize the InputPolicy
     * with that type
     * 
     * XXX this isn't quite good enough yet, this should be fully recursive and
     * be able to get the input policy on arrays of arrays of doubles, or lists
     * of arrays of lists of ...
     * 
     * @param arg
     *            the argument whose input policy should be inferred
     * @return the inferred input policy
     */
    public static InputPolicy inferInputPolicy(Object arg) {
    
        if (arg instanceof Map) {
          Map<FieldName, Object> map = (Map<FieldName, Object>) arg;
          FieldTypePair[] fields = new FieldTypePair[map.size()];
          int cnt = 0;
          for (final Entry<FieldName, Object> entry : map.entrySet()) {
            fields[cnt] = FieldTypePair.make(SourceModel.Name.Field.make(entry.getKey()),
                primitiveClassToTypeExpr(entry.getValue().getClass()));
            cnt++;
          }
          
          TypeExprDefn type = TypeExprDefn.Record.make(null, fields);
          
          return InputPolicy.makeTypedDefaultInputPolicy(type);
        } else if (arg instanceof Collection) {
            Collection<?> col = (Collection<?>) arg;
            Iterator<?> vals = col.iterator();
            Class<?> c = null;
    
            // ensure that every value in the collection has the same type
            if (vals.hasNext()) {
                c = vals.next().getClass();
                while (vals.hasNext()) {
                    if (vals.next().getClass() != c) {
                        c = null;
                        break;
                    }
                }
            }
    
            TypeExprDefn listOf;
            listOf = primitiveClassToTypeExpr(c);
    
            if (listOf != null) {
                TypeExprDefn type = SourceModel.TypeExprDefn.List.make(listOf);
                return InputPolicy.makeTypedDefaultInputPolicy(type);
            } else {
                return InputPolicy.DEFAULT_INPUT_POLICY;
            }
        } else if (arg.getClass().isArray()) {
            Class<?> component = arg.getClass().getComponentType();
            TypeExprDefn listOf;
            listOf = primitiveClassToTypeExpr(component);
            if (listOf != null) {
                TypeExprDefn type = SourceModel.TypeExprDefn.List.make(listOf);
                return InputPolicy.makeTypedDefaultInputPolicy(type);
            } else {
                return InputPolicy.DEFAULT_INPUT_POLICY;
            }
    
        } else if (arg instanceof CalValue) {
            return InputPolicy.CAL_VALUE_INPUT_POLICY;
        } else {
            TypeExprDefn type = primitiveClassToTypeExpr(arg.getClass());
            if (type != null) {
              return InputPolicy.makeTypedDefaultInputPolicy(type);
            } else {
              return InputPolicy.DEFAULT_INPUT_POLICY;
            }
        }
    }
    
    /**
     * @return compiler messages (errors, warnings, etc) for the most recent 
     * call into an embedded editor
     */
    public List<CompilerMessage> getCompilerMessages() {
        return messageLogger.getCompilerMessages();
    }

    /**
     * Takes the initial model and splices in all of the pairs where
     * appropriate. Performs an unquote operation
     * 
     * @param initModel
     * @param toSplice
     * @return
     */
    static Expr spliceInSourceModelParams(Expr initModel,
            final Pair<String, Expr>[] toSplice) {
    
        if (toSplice.length > 0) {
            return (Expr) initModel.accept(new SourceModelCopier<Void>() {
                @Override
                public Expr visit_Expr_Var(Var var, Void arg) {
                    Expr foundExpr = null;
                    for (final Pair<String, Expr> pair : toSplice) {
                        if (pair.fst().equals(var.toSourceText())) {
                            foundExpr = pair.snd();
                            break;
                        }
                    }
                    if (foundExpr != null) {
                        return foundExpr;
                    } else {
                        return super.visit_Expr_Var(var, arg);
                    }
                }
            }, null);
        } else {
            return initModel;
        }
    }

    /**
     * initialize the BasicCALServices with the default workspace
     */
    private static void init() {
        init(BasicCALServices.makeCompiled(WORKSPACE_NAME, messageLogger));
    }

    /**
     * Outputs compiler errors, warnings, and infos to STDERR and STDOUT
     */
    private static void outputMessages() {
        if (messageLogger.getNMessages() > 0) {
            for (final CompilerMessage message : messageLogger
                    .getCompilerMessages()) {
                if (message.getSeverity().compareTo(Severity.ERROR) >= 0) {
                    System.err.println(message.getMessage());
                } else {
                    System.out.println(message.getMessage());
                }
            }

            // clear old messages
            messageLogger = new MessageLogger();
        }
    }

    /**
     * Creates the CAL adjunct from a particular expression. The expression is
     * wrapped in a function with parameters specified by the input tuples.
     * 
     * @param exprText
     *            the text to create the adjunct from
     * @param inputs
     *            the input policies, values, and variable names
     * @return the adjunct
     */
    private static AdjunctSource makeAdjunctSource(String exprText,
            InputTuple[] inputs) {

        // create initial expression
        Expr e = SourceModelUtilities.TextParsing.parseExprIntoSourceModel(
                exprText, messageLogger);

        // substitute in source model where necessary
        e = spliceInSourceModelParams(e, InputTuple.spliceable(inputs));

        FunctionDefn f = SourceModel.FunctionDefn.Algebraic.make("func",
                Scope.PUBLIC, InputTuple.params(inputs), e);

        return new AdjunctSource.FromSourceModel(f);
    }

    private static TypeExprDefn primitiveClassToTypeExpr(Class<?> c) {
        if (c == null) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "JObject");
        } else if (c.equals(Integer.class) || c.equals(int.class)) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "Int");
        } else if (c.equals(Double.class) || c.equals(double.class)) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "Double");
        } else if (c.equals(Character.class) || c.equals(char.class)) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "Character");
        } else if (c.equals(Boolean.class) || c.equals(boolean.class)) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "Boolean");
        } else if (c.equals(String.class)) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "String");
        } else if (c.equals(Byte.class) || c.equals(byte.class)) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "Byte");
        } else if (c.equals(Float.class) || c.equals(float.class)) {
            return TypeExprDefn.TypeCons.make(PRELUDE_NAME, "Float");
        } else {
            return null;
        }
    }
}
