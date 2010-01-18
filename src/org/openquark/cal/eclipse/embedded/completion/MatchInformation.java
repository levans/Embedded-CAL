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
 * MatchInformation.java
 * Created: Jun 18, 2007
 * By: Greg McClement
 */
package org.openquark.cal.eclipse.embedded.completion;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.openquark.cal.eclipse.ui.CALEclipseUIPlugin;
import org.openquark.cal.services.AutoCompleteHelper;
import org.openquark.util.Pair;

/**
 * This is a helper for determining if the current position matches the given completion proposal.
 * @author Greg McClement
 */
class MatchInformation{
    final boolean partialHierarchicalName;
    String[] scopingComponents = null;
    final List<Integer> componentPositions;
    final int startOfReplacementZone;        
    // True if the last name in the hierarchical names is only
    // partially entered.
    String prefix;
    private boolean matchesPrefix;
    
    MatchInformation(final IDocument document, final int offset) throws BadLocationException{
        AutoCompleteHelper ach = new AutoCompleteHelper(
                new AutoCompleteHelper.Document() {
                    public char getChar(int offset) {
                        try {
                            return document.getChar(offset);
                        } catch (BadLocationException e) {
                            // This should not occur except as a programmer error
                            CALEclipseUIPlugin.log(new Status(IStatus.ERROR, CALEclipseUIPlugin.PLUGIN_ID, IStatus.OK, "", e)); //$NON-NLS-1$
                            return 0;
                        }
                    }

                    public String get(int startIndex, int length) {
                        try {
                            return document.get(startIndex, length);
                        } catch (BadLocationException e) {
                            // This should not occur except as a programmer error
                            CALEclipseUIPlugin.log(new Status(IStatus.ERROR, CALEclipseUIPlugin.PLUGIN_ID, IStatus.OK, "", e)); //$NON-NLS-1$
                            return null;
                        }
                    }
                });
        
        prefix = ach.getLastIncompleteIdentifier(offset);
        ach.getIdentifierScoping(offset);
        final Pair<String, List<Integer>> scopingAndOffset = ach.getIdentifierScoping(offset);
        String scoping = scopingAndOffset.fst();
        componentPositions = scopingAndOffset.snd();
        startOfReplacementZone = componentPositions.get(0).intValue();
        matchesPrefix = prefix.length() > 0;
        
        // If the prefix looks like a hierarchical name then adjust
        // things.
        if (prefix.length() > 0 && Character.isUpperCase(prefix.charAt(0))) {
            if (scoping.length() == 0) {
                scoping = prefix;
                scopingComponents = new String[1];
                scopingComponents[0] = prefix;
            } else {
                final String components[] = scoping.split("\\.");
                scopingComponents = new String[components.length + 1];
                System.arraycopy(components, 0, scopingComponents, 0,
                        components.length);
                scopingComponents[components.length] = prefix;
            }
            prefix = "";
            partialHierarchicalName = true;
        } else {
            partialHierarchicalName = false;
            if (scoping.length() > 0) {
                scopingComponents = scoping.split("\\.");
            }
        }            
    }

    public boolean getMatchesPrefix(){
        return this.matchesPrefix;
    }
    
    public boolean resetMatchesPrefix(){
        boolean oldValue = matchesPrefix;
        matchesPrefix = prefix.length() > 0;
        return oldValue;
    }
    
    public void setMatchesPrefix(boolean matchesPrefix){
        this.matchesPrefix = matchesPrefix;
    }
}