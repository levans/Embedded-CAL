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
 * EmbeddedAnnotation.java
 * Created: Jul 19, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;

import org.eclipse.jface.text.source.Annotation;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;

public class EmbeddedAnnotation extends Annotation {
    
    private final ContainedEditorManager containedEditor;
    
    public EmbeddedAnnotation(ContainedEditorManager containedEditor) {
        this.containedEditor = containedEditor;
    }

    public ContainedEditorManager getContainedEditor() {
        return containedEditor;
    }
    
    /**
     * text of the annotation is the first 45 characters of the contents of the editor
     */
    @Override
    public void setText(String text) {
        String newText = text.length() <= 45 ? text : text.substring(0, 45) + " ...";
        super.setText(newText);
    }
}
