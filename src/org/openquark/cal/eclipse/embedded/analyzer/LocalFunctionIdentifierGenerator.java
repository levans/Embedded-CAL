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
 * LocalFunctionIdentifierGenerator.java
 * Creation date: (Mar 10, 2006)
 * By: James Wright
 * 
 * Moved to the CAL_Eclipse_EmbeddedEditor project by Andrew Eisenberg
 * June 20, 2007
 */
package org.openquark.cal.eclipse.embedded.analyzer;

import java.util.HashMap;
import java.util.Map;

import org.openquark.cal.compiler.ModuleName;
import org.openquark.cal.compiler.QualifiedName;

/**
 * A helper class for generating new LocalFunctionIdentifiers.  It does the
 * bookkeeping of tracking the current index associated with a given local function
 * name.
 * 
 * This class is cloneable, so that subclasses of BindingTrackingSourceModelTraverser
 * can clone a private copy to use for predicting the identifiers that will be assigned
 * to local functions by the superclass. 
 * 
 * @author James Wright
 */
final class LocalFunctionIdentifierGenerator {

    private static final Integer ZERO = new Integer(0);

    /** Name of the currently in-scope toplevel function, or null if none. */
    private String currentFunction;

    private final Map localFunctionCounterMap = new HashMap();

    LocalFunctionIdentifierGenerator() {
        currentFunction = null;
    }

    /** Internal constructor for use by clone() */
    private LocalFunctionIdentifierGenerator(String currentFunction, Map localFunctionCounterMap) {
        if(localFunctionCounterMap == null) {
            throw new NullPointerException();
        }
        this.currentFunction = currentFunction;
        this.localFunctionCounterMap.putAll(localFunctionCounterMap);
    }

    /**
     * Reset the counters for local function names and set the current toplevel function name
     * to currentFunction.
     * @param currentFunction String name of the new current toplevel function.
     */
    void reset(String currentFunction) {
        this.currentFunction = currentFunction;
        localFunctionCounterMap.clear();
    }

    /**
     * Generate a new LocalFunctionIdentifier in the current toplevel function.
     * @param moduleName String name of the current module
     * @param localFunctionName String name of the local function to generate an identifier for
     * @return the new LocalFunctionIdentifier
     */
    LocalFunctionIdentifier generateLocalFunctionIdentifier(ModuleName moduleName, String localFunctionName) {
        Integer nextIndexObj = (Integer)localFunctionCounterMap.get(localFunctionName);
        if(nextIndexObj == null) {
            nextIndexObj = ZERO;
        }
        int nextIndex = nextIndexObj.intValue();

        localFunctionCounterMap.put(localFunctionName, new Integer(nextIndex + 1));
        return new LocalFunctionIdentifier(QualifiedName.make(moduleName, currentFunction), localFunctionName, nextIndex);
    }

    /** @return name of the current toplevel function */
    String getCurrentFunction() {
        return currentFunction;
    }

    /** 
     * @return A new LocalFunctionIdentifierGenerator whose internal counters match this instance's current state.
     *          Operations on the new generator won't affect the state of this generator.
     */
    public Object clone() {
        return new LocalFunctionIdentifierGenerator(currentFunction, localFunctionCounterMap);
    }
}
