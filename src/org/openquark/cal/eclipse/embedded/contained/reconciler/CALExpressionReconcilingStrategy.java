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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.openquark.cal.compiler.SourceRange;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer.AnalysisResults;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;
import org.openquark.cal.eclipse.ui.util.CoreUtility;


/**
 * 
 * This reconciling strategy runs in the background and looks for unbound modifiers
 * in an embedded editor. 
 * 
 * @author aeisenberg
 *
 */
public class CALExpressionReconcilingStrategy implements IReconcilingStrategy {

    private static Color blue = new Color(Display.getCurrent(), new RGB(0, 0, 255));
    private static Color red = new Color(Display.getCurrent(), new RGB(255, 0, 0));
    private Color origBackground;
    private IDocument doc;


    /**
     * the styled that will be enhanced with notifications of unbound 
     * references  
     */
    private final StyledText text;

    private final ContainingEditor editor;

    /**
     * resource listener to notify when contents of file has changed.
     * @param editor the editor on which this strategy will be installed
     * @param text the text widget of the text editor
     */
    public CALExpressionReconcilingStrategy(ContainingEditor editor, StyledText text) {
        this.editor = editor;
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
                AnalysisResults results = editor.getAnalyzer().doAnalysis(doc.get());
    
                // unbound is bold and imported is italicized in blue
                highlight(results);
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
    private void highlight(final AnalysisResults results) {
        Display display = text.getDisplay();
        if (results.isValid()) {
            display.asyncExec(new Runnable() {
                public void run() {
                    applyStyles(results.unboundIds, null, SWT.BOLD);
                    applyStyles(results.importedIds, blue, SWT.ITALIC);
                    applyStyles(results.locallyBoundIds, null, SWT.NONE);
                }
    
    
    
                private void applyStyles(final Map<String, List<SourceRange>> styleMap, Color foreground, int textStyle) {
                    List<StyleRange> styles = new LinkedList<StyleRange>();
                    for (final List<SourceRange> ranges : styleMap.values()) {
                        for (final SourceRange sourceRange : ranges) {
                            try {
                                int start = CoreUtility.toOffset(sourceRange.getStartSourcePosition(), doc);
                                int end = CoreUtility.toOffset(sourceRange.getEndSourcePosition(), doc) - start;
    
                                StyleRange style = new StyleRange(
                                        start,
                                        end,
                                        foreground, null, textStyle);
                                styles.add(style);
                            } catch (BadLocationException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    for (final StyleRange styleRange : styles) {
                        text.setStyleRange(styleRange);
                    }
                    
                    // ensure the proper background is set
                    text.getParent().setBackground(origBackground);
                }
                
            });
        } else {
            display.asyncExec(new Runnable() {
                public void run() {
                    // set the error background
                    text.getParent().setBackground(red);
                }
            });
        }
    }


    public void setDocument(IDocument document) {
        this.doc = document;
    }
}