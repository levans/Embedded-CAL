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
 * OpenDeclarationAction.java
 * Created: Aug 15, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.handlers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.openquark.cal.compiler.CompilerMessageLogger;
import org.openquark.cal.compiler.MessageLogger;
import org.openquark.cal.compiler.ModuleName;
import org.openquark.cal.compiler.Name;
import org.openquark.cal.compiler.QualifiedName;
import org.openquark.cal.compiler.SourceIdentifier;
import org.openquark.cal.compiler.SourceRange;
import org.openquark.cal.compiler.SearchResult.Precise;
import org.openquark.cal.compiler.SourceIdentifier.Category;
import org.openquark.cal.eclipse.core.CALModelManager;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer;
import org.openquark.cal.eclipse.embedded.contained.CALModuleEditorManager;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorSourceViewer;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;
import org.openquark.cal.eclipse.embedded.exported.Pair;
import org.openquark.cal.eclipse.ui.actions.ActionMessages;
import org.openquark.cal.eclipse.ui.util.CoreUtility;


/**
 * 
 * @author aeisenberg
 * This class performs an open declaration action from within an embedded editor
 */
public class OpenDeclarationAction extends TextEditorAction {

    public OpenDeclarationAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
        super(bundle, prefix, editor);
    }
    
    @Override
    public void update() {
        setEnabled(true);
    }


    /*
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        // update has been called by the framework
        if (!isEnabled()) {
            return;
        }
        if (!CoreUtility.builderEnabledCheck(ActionMessages.OpenDeclarationAction_error_title)){
            return;
        }

        CoreUtility.initializeCALBuilder(null, 100, 100);
        
        ContainingEditor containingEditor = (ContainingEditor) getTextEditor();
        
        ISourceViewer viewer = (ISourceViewer)
            containingEditor.getAdapter(ITextOperationTarget.class);
        
        if (! (viewer instanceof ContainedEditorSourceViewer)) {
            return;
        }
        ContainedEditorSourceViewer containedViewer = (ContainedEditorSourceViewer) viewer;
        
        TextSelection sel = (TextSelection) containedViewer.getSelection();
        IDocument doc = containedViewer.getDocument();
        Precise result;
        String word = null;
        try {
            int line = doc.getLineOfOffset(sel.getOffset());
            int column = sel.getOffset() - doc.getLineInformation(line).getOffset();
            word = getWord(doc, line, column);
            result = searchForIdentifierSource(word, containedViewer);
        } catch (BadLocationException e) {
            showErrorMessage(e, false);
            return;
        }
        if (result == null) {
            return;
        }
        
        CALModelManager cmm = CALModelManager.getCALModelManager();
        
        if (result.getName() instanceof ModuleName) {
            // result is a module
            IStorage definitionFile = cmm.getInputSourceFile((ModuleName) result.getName());
            try {
                CoreUtility.openInEditor(definitionFile, true);
            } catch (PartInitException e) {
                showErrorMessage(e, true);
            }
        } else {
            // result is some other kind of entity look for it 
            CompilerMessageLogger messageLogger = new MessageLogger();
            final List definitions = cmm.getSourceMetrics().findDefinition(result, messageLogger);
            
            // if definitions.size() > 1, there is some abiguitiy in the name.  For
            // now, just choose the first one.
            if (definitions.size() > 0) {
                Precise result2 = (Precise) definitions.get(0);
                IStorage definitionFile = cmm.getInputSourceFile(result.getName().getModuleName());
                try {
                    IEditorPart editorPart = CoreUtility.openInEditor(definitionFile, true);
                    CoreUtility.showPosition(editorPart, definitionFile, result2.getSourceRange());
                } catch (PartInitException e) {
                    showErrorMessage(e, true);
                }
                
                
            } else {
                
                // the identifier cannot be found in the CAL Model.  
                // perhaps it is in the Embedded module editor in this file.
                SourceRange range = findLocationInModuleEditor(word, containedViewer.getCodeAnalyzer());
                if (range != null) {
                    CALModuleEditorManager moduleEditor = 
                        containingEditor.getControlManager().getModuleEditor();
                    
                    if (moduleEditor != null) {
                        try {
                            // ADE there is an off-by-one issue going on when converting from 
                            // source range to position.  The line number is off by one and the 
                            // column positions are off by one.  That is the origin of the "-1" 
                            // in the code below.
                            
                            // conver from SourceRange (line and column) to position (offset and length)
                            IDocument moduleDoc = moduleEditor.getViewer().getDocument();
                            int start = moduleDoc.getLineInformation(range.getStartLine()-1).getOffset() + 
                                    range.getStartColumn() - 1;
                            int length = moduleDoc.getLineInformation(range.getEndLine()-1).getOffset() - start + 
                                    range.getEndColumn() - 1;
                            moduleEditor.setSelection(new Position(start, length));
                            moduleEditor.setFocus();
                        } catch (BadLocationException e) {
                            showErrorMessage(e, true);
                        }
                    }
                } else {
                    showErrorMessage(null, true);
                }
            }
        }

    }
    
    protected Precise searchForIdentifierSource(String word,
            ContainedEditorSourceViewer viewer) {
        
        CodeAnalyzer analyzer = viewer.getCodeAnalyzer();
        Pair<QualifiedName, Category> pair = analyzer.getQualifiedName(word);

        if (pair != null) {
           return createPrecise(pair.fst(), pair.snd());
        } else {
            return null;
        }
    }
    
    protected SourceRange findLocationInModuleEditor(String word, CodeAnalyzer analyzer) {
        return analyzer.getSourceRangeForTopLevelIdentifier(word);
    }
    
    /**
     * Iterates over a part of a document to find the nearest full word
     * that is being signified by lineNum and column.  '.' is considered
     * a word character here.
     * 
     * If nothing is being signified, then return an empty string
     * @param doc the document to look in
     * @param lineNum the line number to search
     * @param column the offset into the line
     * @return the word being signified, or an empty string if there is nothing
     */
    private String getWord(IDocument doc, int lineNum, int column) {
        IRegion region;
        try {
            region = doc.getLineInformation(lineNum);
            String lineStr = doc.get(region.getOffset(), region.getLength());
            int start = column;
            char c = lineStr.charAt(start);
            while (Character.isLetterOrDigit(c) || c == '.') {
                start --;
                if (start < 0) { 
                    break;
                }
                c = lineStr.charAt(start);
            }
            start ++;
            
            int end = column;
            c = lineStr.charAt(end);
            while (Character.isLetterOrDigit(c) || c == '.') {
                end ++;
                if (end >= lineStr.length()) break;
                c = lineStr.charAt(end);
            }
            
            if (start <= end && start >= 0 && end <= lineStr.length()) {
                return lineStr.substring(start, end);
            } else {
                return "";
            }
            
        } catch (BadLocationException e) {
            showErrorMessage(e, false);
        }
        
        return "";
    }
    
    private void showErrorMessage(Exception e, boolean logError) {
        if (logError) {
            EmbeddedCALPlugin.logError("Error openning declaration", e);
        }
        CoreUtility.showErrorOnStatusLine(getTextEditor(), ActionMessages.OpenAction_error_messageBadSelection_CAL);
//        getTextEditor().getEditorSite().getShell().getDisplay().beep();
    }
    

    
    
    
    /**
     * Unfortunately, the Precise class has no public constructor, but
     * we need it here.  I don't want to change the interface,
     * so instead, I use reflection.  Same goes with the SourceRange 
     * class.
     * 
     * In the future, we may want to make public constructors for them.
     * 
     * TODOEL This is a comment put here by Andrew E to remind Edward to change this method so that 
     * it does not use reflection.  However the requirement is that the sourceRange part of the
     * sourceModel becomes public 
     */
    private static Constructor<SourceRange> sourceRangeCons = null;
    private static Constructor<Precise> preciseCons = null;
    private Precise createPrecise(Name name, SourceIdentifier.Category category) {
        if (preciseCons == null) {
            try {
                preciseCons = Precise.class.getDeclaredConstructor(SourceRange.class, Name.class, 
                        SourceIdentifier.Category.class, boolean.class);
                preciseCons.setAccessible(true);
            } catch (SecurityException e) {
                showErrorMessage(e, true);
            } catch (NoSuchMethodException e) {
                showErrorMessage(e, true);
            }
        }
        if (sourceRangeCons == null) {
            try {
                sourceRangeCons = SourceRange.class.getDeclaredConstructor(String.class);
                sourceRangeCons.setAccessible(true);
            } catch (SecurityException e) {
                showErrorMessage(e, true);
            } catch (NoSuchMethodException e) {
                showErrorMessage(e, true);
            }
        }
        
        
        if (preciseCons != null && sourceRangeCons != null) {
            try {
                return preciseCons.newInstance(sourceRangeCons.newInstance(""), 
                        name, category, false);
            } catch (IllegalArgumentException e) {
                showErrorMessage(e, true);
            } catch (InstantiationException e) {
                showErrorMessage(e, true);
            } catch (IllegalAccessException e) {
                showErrorMessage(e, true);
            } catch (InvocationTargetException e) {
                showErrorMessage(e, true);
            }
        }
        return null;
    }
}