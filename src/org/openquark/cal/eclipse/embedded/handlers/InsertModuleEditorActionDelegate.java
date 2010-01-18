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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorProperties;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;
import org.openquark.cal.eclipse.embedded.containing.ControlManager;
import org.openquark.cal.eclipse.embedded.exported.IEmbeddedCalConstants;
import org.openquark.cal.eclipse.embedded.exported.Pair;

/**
 * 
 * @author aeisenberg
 * 
 * When this action is run, an embedded CAL editor is inserted into the currently open
 * Java editor
 * 
 */
public class InsertModuleEditorActionDelegate implements IEditorActionDelegate {

    
    private final static String embeddedRegionPrefix = 
        IEmbeddedCalConstants.EMBEDDED_REGION_START +
        " " +
        IEmbeddedCalConstants.MODULE_EDITOR_NAME +
        "(\"module "; 
        
    private final static String embeddedRegionMidfix = 
        ";\\n\\nimport Cal.Core.Prelude;\", \"";
        
        
    private final static String embeddedRegionPostfix = 
        "\", 59, 200, true, false) " + 
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

        boolean doIt = checkForDuplicate();
        
        if (!doIt) return ;
        
        ISelectionProvider provider = currentEditor.getSelectionProvider();
        IDocument doc = currentEditor.internalGetSourceViewer().getDocument();
        Pair<String, ICompilationUnit> pair = getCompUnitName();
        String editorText = embeddedRegionPrefix + pair.fst() + embeddedRegionMidfix + pair.fst() + embeddedRegionPostfix;
        
        if (doc != null && provider != null && provider.getSelection() instanceof TextSelection) {
            try {
                TextSelection sel = (TextSelection) provider.getSelection();
                doc.replace(sel.getOffset(), sel.getLength(), editorText);
                
                ControlManager cm = currentEditor.getControlManager();
                StyleRange[] ranges = cm.createAndAddControl(sel.getOffset(), editorText.length());
                TextPresentation singlePres = new TextPresentation();
                singlePres.addStyleRange(ranges[0]);
                singlePres.addStyleRange(ranges[1]);
                currentEditor.internalGetSourceViewer().changeTextPresentation(singlePres, true);
                
                
                
                ContainedEditorProperties props = cm.getModuleEditor().getPropertiess();
                
                // add the proper import statements
                if (pair.snd() != null) {
                    props.requiresImport(pair.snd());
                }
                
                // focus on the new editor
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

    @SuppressWarnings("restriction")
    private Pair<String, ICompilationUnit> getCompUnitName() {
        ICompilationUnit unit = 
            ((org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider) 
                    currentEditor.getDocumentProvider())
            .getWorkingCopy(currentEditor.getEditorInput());
        if (unit != null) {
            try {
                return new Pair<String, ICompilationUnit>(
                        unit.getTypes()[0].getElementName(), unit);
            } catch (JavaModelException e) {
                EmbeddedCALPlugin.logError("Error getting name of " + unit, e);
            }
        } 
        return new Pair<String, ICompilationUnit>("X", unit);
    }

    /**
     * Checks to see if this ContainingEditor already has a module editor in 
     * it.  If it does, asks programmer to confirm the creation of a second one.
     * 
     * @return true if there is a go ahead for inserting the editor
     * false if should not be inserted
     */
    private boolean checkForDuplicate() {
        ControlManager cm = currentEditor.getControlManager();
        if (cm.getModuleEditor() != null) {
            MessageBox mb = new MessageBox(currentEditor.internalGetSourceViewer().getTextWidget().getShell(),
                    SWT.YES | SWT.NO | SWT.ICON_QUESTION );
            mb.setText("Really insert?");
            mb.setMessage("This class already contains an embedded module editor.\n" +
            		"Having two or more module editors in a single compilation unit will" +
            		"cause unpredictable results at runtime.");
            int res = mb.open();

            if (res == SWT.NO) {   
                return false;
            }
        }
        return true;
    }
}
