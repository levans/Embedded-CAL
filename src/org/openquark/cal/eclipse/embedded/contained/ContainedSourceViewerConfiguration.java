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
 * ContainedSourceViewerConfiguration.java
 * Created: Jun 19, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;
import org.openquark.cal.eclipse.embedded.completion.CompletionProcessor;
import org.openquark.cal.eclipse.embedded.contained.reconciler.CALExpressionReconcilingStrategy;
import org.openquark.cal.eclipse.embedded.contained.reconciler.CALModuleReconcilingStrategy;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;
import org.openquark.cal.eclipse.ui.text.CALSourceViewerConfiguration;
import org.openquark.cal.eclipse.ui.text.ColorManager;

/**
 * This class is used by all embedded editors.  Currently there is not
 * enough difference between them to warrant using separate classes.
 * @author aeisenberg
 *
 */
public class ContainedSourceViewerConfiguration extends
        CALSourceViewerConfiguration {
    
    private final ContainedEditorManager editorManager;
    private ContentAssistant assistant;
    
    public ContainedSourceViewerConfiguration(ColorManager colorManager,
            IPreferenceStore preferenceStore, ITextEditor editor, String partitioning, 
            ContainedEditorManager editorManager) {
        super(colorManager, preferenceStore, editor, partitioning);
        this.editorManager = editorManager;
    }

    /**
     * no hyperlinks
     */
    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        return new IHyperlinkDetector[0];
    }
    
    /**
     * the contained source viewer has no content assistant installed
     */
    @Override 
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        if (assistant == null) {
            assistant = new ContentAssistant();
            assistant.enableAutoActivation(true);
            assistant.enableAutoInsert(true);
            IContentAssistProcessor completionProcessor = getContainedCompletionProcessor();

            assistant.setContentAssistProcessor(completionProcessor, 
                    IDocument.DEFAULT_CONTENT_TYPE);

        }
        return assistant;
    }

    private IContentAssistProcessor getContainedCompletionProcessor() {
        return new CompletionProcessor(editorManager, (ContainingEditor) getEditor(), fPreferenceStore);
    }

    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        ContainingEditor editor = (ContainingEditor) getEditor();

        IReconcilingStrategy strat;
        if (editorManager.editorKind() == CALExpressionEditorManager.EDITOR_KIND) {
            strat = new CALExpressionReconcilingStrategy(
                    editor, sourceViewer.getTextWidget());
        } else {
            strat = new CALModuleReconcilingStrategy(
                    editor, sourceViewer.getTextWidget());
        }
        
        IReconciler rec = new MonoReconciler(strat, false);
        return rec;
    }
    
    /*
     * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String, int)
     */
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
        return new ContainedTextHover(sourceViewer != null || contentType != null, stateMask, editorManager.getViewer());
    }
    
    /*
     * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String)
     */
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return getTextHover(sourceViewer, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
    } 
}
