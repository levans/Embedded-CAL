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
 * ContainedEditorSourceViewer.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.openquark.cal.compiler.ModuleName;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer;
import org.openquark.cal.eclipse.embedded.contained.reconciler.CALExpressionReconcilingStrategy;
import org.openquark.cal.eclipse.embedded.contained.reconciler.CALModuleReconcilingStrategy;
import org.openquark.cal.eclipse.embedded.containing.ControlManager;
import org.openquark.cal.eclipse.ui.caleditor.CALEditor;
import org.openquark.cal.eclipse.ui.caleditor.CALSourceViewer;


public class ContainedEditorSourceViewer extends CALSourceViewer {

    /** used to help with content assist and text hover */
    private final ControlManager cm;


    
    interface ITextConverter {
        void customizeDocumentCommand(IDocument document, DocumentCommand command);
    }

    private List fTextConverters;
    private boolean fIgnoreTextConverters= false;

    private String[] configuredContentTypes;

    private String[] indentPrefixes;

    public ContainedEditorSourceViewer(Composite parent, 
            IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, 
            boolean showAnnotationsOverview, int styles, 
            IPreferenceStore store, ControlManager cm) {
        super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles, store);
        this.cm = cm;
    }

    public ModuleName getModuleName(){
        return ModuleName.make(cm.getModuleName());
    }
    
    public ControlManager getControlManager() {
        return cm;
    }


    @SuppressWarnings("restriction")
    public org.openquark.cal.eclipse.core.CALModelManager.SourceManagerFactory
    getSourceManagerFactory(boolean updateDocumentIfPossible){
        return CALEditor.getSourceManagerFactory(updateDocumentIfPossible, 
                getTextWidget().getShell(), null);
    }

    /*
     * @see ITextOperationTarget#doOperation(int)
     */
    @Override
    public void doOperation(int operation) {

        if (getTextWidget() == null)
            return;


        switch (operation) {
        case UNDO:
        case REDO:
            fIgnoreTextConverters= true;
            super.doOperation(operation);
            fIgnoreTextConverters= false;
            return;
        }

        super.doOperation(operation);
    }





    public CodeAnalyzer getCodeAnalyzer() {
        return cm.getContainingEditor().getAnalyzer();
    }


    /*
     * @see TextViewer#customizeDocumentCommand(DocumentCommand)
     */
    @Override
    protected void customizeDocumentCommand(DocumentCommand command) {
        super.customizeDocumentCommand(command);
        if (!fIgnoreTextConverters && fTextConverters != null) {
            for (Iterator e = fTextConverters.iterator(); e.hasNext(); )
                ((ITextConverter)e.next()).customizeDocumentCommand(getDocument(), command);
        }
    }

    // http://dev.eclipse.org/bugs/show_bug.cgi?id=19270
    public void updateIndentationPrefixes() {
        for (int i= 0; i < configuredContentTypes.length; i++) {
            if (indentPrefixes != null && indentPrefixes.length > 0)
                setIndentPrefixes(indentPrefixes, configuredContentTypes[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requestWidgetToken(IWidgetTokenKeeper requester) {
        if (PlatformUI.getWorkbench().getHelpSystem().isContextHelpDisplayed())
            return false;
        return super.requestWidgetToken(requester);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requestWidgetToken(IWidgetTokenKeeper requester, int priority) {
        if (PlatformUI.getWorkbench().getHelpSystem().isContextHelpDisplayed())
            return false;
        return super.requestWidgetToken(requester, priority);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(SourceViewerConfiguration configuration) {
        super.configure(configuration);

        configuredContentTypes = configuration.getConfiguredContentTypes(this);
        indentPrefixes = configuration.getIndentPrefixes(this, "");
    }
    
    /**
     * forces the viewer to reconcile its contents
     * 
     * runs reconciling in the background
     */
    public void reconcile() {
        getTextWidget().getDisplay().asyncExec(new Runnable() {
            public void run() {
                if (fReconciler instanceof MonoReconciler) {
                    MonoReconciler monoRec = (MonoReconciler) fReconciler;
                    IReconcilingStrategy strat = monoRec.getReconcilingStrategy("");
                    if (strat instanceof CALExpressionReconcilingStrategy ||
                            strat instanceof CALModuleReconcilingStrategy) {
                        strat.reconcile(null);
                    }
                }
            }
        });
    }
}