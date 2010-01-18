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
 * LocalFunctionIdentifier.java
 * Creation date: (Mar 10, 2006)
 * By: James Wright
 */
package org.openquark.cal.eclipse.embedded.analyzer;


import org.openquark.cal.compiler.QualifiedName;



/**
 * Represents the name of a local function, disambiguated by index within
 * a toplevel function.
 * <p>
 * The index is constructed by a pre-order traversal of a toplevel function's
 * local definitions.  ie, we process all local function names in a let expression,
 * then we recursively process each local function in order.  
 * So, for example, the following code:
 * <pre>  
 *   toplevelFunction =
 *       let
 *           foo =
 *               let
 *                   foo = 10;
 *                   bar = 20;
 *               in
 *                   foo + bar;
 *           
 *           bar =
 *               let
 *                   foo = 20;
 *                   bar = 25;
 *               in
 *                   foo + bar;
 *       in
 *           foo + bar;
 * </pre>          
 * produces the following LocalFunctionIdentifiers:
 * <pre>
 *   toplevelFunction =
 *       let
 *           toplevelFunction@foo@0 = 
 *               let
 *                   toplevelFunction@foo@1 = 10;
 *                   toplevelFunction@bar@1 = 20;
 *               in
 *                   toplevelFunction@foo@1 + toplevelFunction@bar@1;
 *           
 *           toplevelFunction@bar@0 = 
 *              let
 *                  toplevelFunction@foo@2 = 20;
 *                  toplevelFunction@bar@2 = 25;
 *              in
 *                  toplevelFunction@foo@2 + toplevelFunction@bar@2;
 *       in
 *           toplevelFunction@foo@0 + toplevelFunction@foo@0;
 * </pre>
 * @author James Wright
 */
final class LocalFunctionIdentifier implements Comparable {

    /** Name of the toplevel function in which this local function occurs. */
    private final QualifiedName toplevelFunctionName;

    /** Name of the local function */
    private final String localFunctionName;

    /** 
     * 0-based index of local functions with this name in the current toplevel
     * function.  This is nearly always zero, because very few toplevel functions
     * contain local functions that have the same names as each other.
     */
    private final int index;

    /**
     * @param toplevelFunctionName QualifiedName of the toplevel function that the local function is defined within
     * @param localFunctionName String name of the local function
     * @param index pre-order index of the local function (see class-level comment for details)
     */
    LocalFunctionIdentifier(QualifiedName toplevelFunctionName, String localFunctionName, int index) {

        if(toplevelFunctionName == null || localFunctionName == null) {
            throw new NullPointerException("no null arguments allowed for LocalFunctionIdentifier's constructor");
        }

        if(index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }

        this.toplevelFunctionName = toplevelFunctionName;
        this.localFunctionName = localFunctionName;
        this.index = index;
    }

    /** @return QualifiedName of the toplevel function that the local function was declared in */
    public QualifiedName getToplevelFunctionName() {
        return toplevelFunctionName;
    }

    /** @return name of the local function */
    public String getLocalFunctionName() {
        return localFunctionName;
    }

    /** @return pre-order index of the local function (details on this index are available in LocalFunctionIdentifier's class comment) */
    public int getIndex() {
        return index;
    }

    /** {@inheritDoc} */
    public String toString() {
        return toplevelFunctionName + "@" + localFunctionName + "@" + index; 
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return toString().hashCode(); 
    }

    /** {@inheritDoc} */
    public int compareTo(Object otherObj) {
        LocalFunctionIdentifier other = (LocalFunctionIdentifier)otherObj; // The Comparable.compareTo method is documented to throw a ClassCastException on incompatible types

        int topLevelRelationship = toplevelFunctionName.compareTo(other.toplevelFunctionName);
        if(topLevelRelationship != 0) {
            return topLevelRelationship;
        }

        int localNameRelationship = localFunctionName.compareTo(other.localFunctionName);
        if(localNameRelationship != 0) {
            return localNameRelationship;
        }

        if(index < other.index) {
            return -1;
        }

        if(index > other.index) {
            return 1;
        }

        return 0;
    }

    /** {@inheritDoc} */
    public boolean equals(Object otherObj) {
        if(!(otherObj instanceof LocalFunctionIdentifier)) {
            return false;
        }

        return compareTo(otherObj) == 0;
    }

}
