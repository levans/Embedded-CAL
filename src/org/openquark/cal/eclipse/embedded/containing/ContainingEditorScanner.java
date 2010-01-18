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
 * ContainingEditorScanner.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;


import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;
import org.openquark.cal.eclipse.embedded.exported.IEmbeddedCalConstants;



public class ContainingEditorScanner 
        extends RuleBasedPartitionScanner implements IEmbeddedCalConstants {

    public final static String CONTAINED_EDITOR = "__contained_editor";  // serves as the partition name
    public final static String NOT_CONTAINED_EDITOR = "__not_contained_editor";  // serves as the partition name
    public final static IToken EDITOR_TOKEN = new Token(CONTAINED_EDITOR);
    public final static IToken NON_EDITOR_TOKEN = new Token(NOT_CONTAINED_EDITOR);


    /**
     * the opening of an embedded region looks like this: /*<--*/  /**
     * The closing of an embedded region looks like this: /*-->*/  /**
     */
    public ContainingEditorScanner() {

        IPredicateRule[] rules = new IPredicateRule[1];
        rules[0] = new MultiLineRule(EMBEDDED_REGION_START, EMBEDDED_REGION_END, EDITOR_TOKEN, '\\', true);
        setPredicateRules(rules);

        setDefaultReturnToken(NON_EDITOR_TOKEN);
    }
}

