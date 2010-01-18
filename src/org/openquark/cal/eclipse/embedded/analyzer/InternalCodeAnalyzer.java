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
 * ExternalCodeAnalyzer.java
 * Created: Jul 10, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.analyzer;

import org.eclipse.jdt.core.JavaModelException;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorProperties;
import org.openquark.cal.eclipse.embedded.contained.IContainedEditorListener;
import org.openquark.cal.eclipse.embedded.containing.ControlManager;

/**
 * Code analyzer that analyzes a module defined internal to the 
 * compilation  unit (in an embedded module editor) 
 * 
 * @author aeisenberg
 */
public class InternalCodeAnalyzer extends CodeAnalyzer implements IContainedEditorListener {

    
    private final ControlManager cm;
    
    public InternalCodeAnalyzer(ControlManager cm) {
        this.cm = cm;
    }
    
    @SuppressWarnings("restriction")
    public String getModuleText() throws JavaModelException {
        return cm.getModuleEditor().getCalContents();
    }

    protected void installListener() { }

    public void editorChanged(ContainedEditorManager editor,
            ContainedEditorProperties props) {
        setStale();
    }

    public void editorDeleted(ContainedEditorManager editor,
            ContainedEditorProperties props) {
    }

    public void editorFocusGained(ContainedEditorManager editor,
            ContainedEditorProperties props) {
    }

    public void editorFocusLost(ContainedEditorManager editor,
            ContainedEditorProperties props) {
    }

    public void editorResized(ContainedEditorManager editor,
            ContainedEditorProperties props) {
    }

    public void editorSaved(ContainedEditorManager editor,
            ContainedEditorProperties props) {
    }

    public void exitingEditor(ContainedEditorManager editor,
            ContainedEditorProperties props, ExitDirection dir) {
    }

    

}
