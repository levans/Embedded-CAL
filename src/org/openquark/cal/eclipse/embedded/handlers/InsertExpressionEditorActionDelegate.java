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
 * InsertExpressionEditorActionDelegate.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.handlers;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;
import org.openquark.cal.eclipse.embedded.containing.ControlManager;
import org.openquark.cal.eclipse.embedded.exported.IEmbeddedCalConstants;

/**
 * 
 * @author aeisenberg
 * 
 * When this action is run, an embedded CAL editor is inserted into the currently open
 * Java editor
 * 
 */
public class InsertExpressionEditorActionDelegate implements IEditorActionDelegate {

    
    private final static String embeddedRegionMarker = 
        IEmbeddedCalConstants.EMBEDDED_REGION_START + 
        "RunQuark.evaluateExpression()" +
        IEmbeddedCalConstants.EMBEDDED_REGION_END;

    private ContainingEditor currentEditor;

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        if (targetEditor instanceof ContainingEditor) {
            currentEditor = (ContainingEditor) targetEditor;
        }
    }

    public void selectionChanged(IAction action, ISelection selection) { }

    public void run(IAction action) {
        if (currentEditor == null) {
            Shell[] shells = Display.getCurrent().getShells();
            Shell shell = null;
            if (shells.length > 0) {
                shell = shells[0];
            }
            MessageDialog.openError(shell, "Invalid editor", 
                    "Cannot embedded CAL editors in this Java editor.\n" +
                    "Close this editor, and re-open as an embeddable editor.");
            return;
        }
        ISelectionProvider provider = currentEditor.getSelectionProvider();
        IDocument doc = currentEditor.getContainingViewer().getDocument();
        if (doc != null && provider != null && provider.getSelection() instanceof TextSelection) {
            try {
                TextSelection sel = (TextSelection) provider.getSelection();
                doc.replace(sel.getOffset(), sel.getLength(), embeddedRegionMarker);
                
                ControlManager cm = currentEditor.getControlManager();
                StyleRange[] ranges = cm.createAndAddControl(sel.getOffset(), embeddedRegionMarker.length());
                TextPresentation singlePres = new TextPresentation();
                singlePres.addStyleRange(ranges[0]);
                singlePres.addStyleRange(ranges[1]);
                currentEditor.internalGetSourceViewer().changeTextPresentation(singlePres, true);
                
                // Focus on the new editor
                ContainedEditorManager containedEditor = 
                    cm.findEditorProjected(ranges[0].start, ranges[0].length, true);
                if (containedEditor != null) {
                    containedEditor.setFocus();
                }
                
            } catch (BadLocationException e) {
                EmbeddedCALPlugin.logError("Error inserting embedded editor", e);
            }
        }
    }
}
