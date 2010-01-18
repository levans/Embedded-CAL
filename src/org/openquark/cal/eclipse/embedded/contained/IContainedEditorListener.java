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
 * IContainedEditorListener.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

/**
 * 
 * @author Andrew Eisenberg
 * @created May 9, 2007
 *
 * Listens for events on the contained editor
 */
public interface IContainedEditorListener {
    
    public static enum ExitDirection { UP, DOWN, RIGHT, LEFT };
    
    public void editorSaved(ContainedEditorManager editor, ContainedEditorProperties props);

    public void editorResized(ContainedEditorManager editor, ContainedEditorProperties props);

    public void editorChanged(ContainedEditorManager editor, ContainedEditorProperties props);

    public void editorDeleted(ContainedEditorManager editor, ContainedEditorProperties props);

    public void editorFocusGained(ContainedEditorManager editor, ContainedEditorProperties props);

    public void editorFocusLost(ContainedEditorManager editor, ContainedEditorProperties props);
    
    public void exitingEditor(ContainedEditorManager editor, ContainedEditorProperties props, ExitDirection dir);
}
