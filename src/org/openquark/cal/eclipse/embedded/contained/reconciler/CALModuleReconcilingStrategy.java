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
 * CALExpressionReconcilingStrategy.java
 * Created: Jun 20, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained.reconciler;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.openquark.cal.compiler.MessageLogger;
import org.openquark.cal.compiler.SourceModelUtilities;
import org.openquark.cal.compiler.CompilerMessage.Severity;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;


/**
 * 
 * This reconciling strategy runs in the background and looks for unbound modifiers
 * in an embedded editor. 
 * 
 * @author aeisenberg
 *
 */
public class CALModuleReconcilingStrategy implements IReconcilingStrategy {

//    private static Color blue = new Color(Display.getCurrent(), new RGB(0, 0, 255));
    private static Color red = new Color(Display.getCurrent(), new RGB(255, 0, 0));
    private Color origBackground;
    private IDocument doc;


    /**
     * the styled that will be enhanced with notifications of unbound 
     * references  
     */
    private final StyledText text;


    /**
     * resource listener to notify when contents of file has changed.
     * @param editor The containing text editor
     * @param text the styled text of the embedded editor
     */
    public CALModuleReconcilingStrategy(ContainingEditor editor, StyledText text) {
        this.text = text;
        this.origBackground = text.getParent().getBackground();
    }





    public void reconcile(IRegion partition) {
        doReconcile();
    }

    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        doReconcile();
    }

    /**
     * simply calls the code analyzer and determines what needs to be highlighted
     */
    private void doReconcile() {
        try {
            if (doc != null) {
                MessageLogger logger = new MessageLogger();
                SourceModelUtilities.TextParsing.parseModuleDefnIntoSourceModel(
                        doc.get(), false, logger);
                highlight(logger);
            }
        } catch (Exception e) {
            EmbeddedCALPlugin.logError("Error during reconciling", e);
        }
    }


    /**
     * Highlights the results of the code analysis:
     * 
     * imported references are in italicized blue
     * unbound identifiers are in bold
     * 
     * The update must be done asynchronously on the UI thread
     * @param results
     */
    private void highlight(MessageLogger logger) {
        Display display = text.getDisplay();
        if (logger.getMaxSeverity() == Severity.ERROR || 
                logger.getMaxSeverity() == Severity.FATAL) {
            display.asyncExec(new Runnable() {
                public void run() {
                    text.getParent().setBackground(red);
                }
            });
        } else {
            display.asyncExec(new Runnable() {
                public void run() {
                    text.getParent().setBackground(origBackground);
                }
            });
        }
    }
    


    public void setDocument(IDocument document) {
        this.doc = document;
    }
}