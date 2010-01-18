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
 * ContainedTextHover.java
 * Created: Jul 12, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.openquark.cal.caldoc.CALDocToTooltipHTMLUtilities;
import org.openquark.cal.compiler.CompilerMessageLogger;
import org.openquark.cal.compiler.FunctionalAgent;
import org.openquark.cal.compiler.MessageLogger;
import org.openquark.cal.compiler.ModuleName;
import org.openquark.cal.compiler.ModuleSourceDefinition;
import org.openquark.cal.compiler.ModuleTypeInfo;
import org.openquark.cal.compiler.Name;
import org.openquark.cal.compiler.QualifiedName;
import org.openquark.cal.compiler.SourceIdentifier;
import org.openquark.cal.compiler.SourceMetricsManager;
import org.openquark.cal.compiler.SourceRange;
import org.openquark.cal.compiler.CompilerMessage.Severity;
import org.openquark.cal.compiler.SearchResult.Precise;
import org.openquark.cal.compiler.SourceIdentifier.Category;
import org.openquark.cal.eclipse.core.CALModelManager;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer;
import org.openquark.cal.eclipse.embedded.exported.Pair;
import org.openquark.cal.eclipse.ui.text.CALTextHover;
import org.openquark.cal.services.ProgramModelManager;
import org.openquark.cal.services.Status;


/**
 * This class implements hovering capabilities for a contained 
 * expression editor.
 * <p>
 * It does a bit of cheating, but it works, so no big complaints.
 * Since the CAL code being hovered over is in a synthetic module
 * and is not in the CAL Model, we have to do some different things  
 * Here's how:
 * <ol>
 * <li>determine the name of the synthetic module
 * <li>determine the imports of the synthetic module.
 *     this is done through the CodeAnalyzer that is
 *     attached to all ContainedEditors
 * <li>determine the word being hovered over.  This is done
 *     by looking at the document iterating through its text
 * <li>look in the imports to see if this word corresponds to
 *     an imported identifier.
 * <li>if not, then do nothing,
 * <li>if so, then wrap that in a search results object and
 *     send to the super class to complete the hover
 * </ol>
 * 
 * 
 * @author aeisenberg
 *
 */
public class ContainedTextHover extends CALTextHover {
    
    private final ContainedEditorSourceViewer viewer;
    
    public ContainedTextHover(boolean updateOffset, int stateMask, ContainedEditorSourceViewer viewer) {
        super(updateOffset, stateMask);
        this.viewer = viewer;
    }
    
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        // search for value in CALModel
        String hoverInfo = super.getHoverInfo(textViewer, hoverRegion);
        if (hoverInfo != null && ! hoverInfo.equals("CAL Model not built")) {
            return hoverInfo;
        }
        
        // value not found in CALModel. Search for it in ModuleEditor
        try {
            IDocument doc = getDocument();
            // convert to line number and column
            int line = doc.getLineOfOffset(hoverRegion.getOffset());
            int lineOffset = doc.getLineOffset(line);
            int column = hoverRegion.getOffset() - lineOffset; 
            String word = getWord(doc, line, column);
            if (showInBrowser) {
                // show the CALDoc
                return findCALDoc(word, viewer.getCodeAnalyzer().getModuleText(), viewer.getModuleName());
            } else {
                // show the source code
                return findSourceCode(word, viewer.getCodeAnalyzer());                
            }
        } catch (BadLocationException e) {
        } catch (JavaModelException e) {
            return "Compilation Error";
        }
        return null;
    }
    
    private String findCALDoc(String word, final String moduleText, final ModuleName moduleName) {
        ProgramModelManager program = 
            CALModelManager.getCALModelManager().getProgramModelManager();
        MessageLogger logger = new MessageLogger();
        String result = null;
        ModuleSourceDefinition sourceDef = new ModuleSourceDefinition(moduleName) {
        
            @Override
            public long getTimeStamp() {
                return 0;
            }
        
            @Override
            public InputStream getInputStream(Status status) {
                return new StringBufferInputStream(moduleText);
            }
        
            @Override
            public String getDebugInfo() {
                return "Synthetic Module: " + getModuleName().toSourceText();
            }
        
        };
        
        Severity severity = program.makeModule(sourceDef, logger);
        ModuleTypeInfo mti = program.getModuleTypeInfo(moduleName);
        if (mti != null) {
            FunctionalAgent agent = mti.getFunctionalAgent(word);
            if (agent != null) {
                result = CALDocToTooltipHTMLUtilities.getHTMLForCALDocCommentOfScopedEntity(program, agent);
            }
        }
        program.removeModule(moduleName);
        return result; 
    }

    @Override
    protected IDocument getDocument() {
        return viewer.getDocument();
    }
    
    protected String findSourceCode(String word, CodeAnalyzer analyzer) throws BadLocationException {
        SourceRange range = analyzer.getSourceRangeForTopLevelDefinition(word);
        // ADE there is an off-by-one issue going on when converting from 
        // source range to position.  The line number is off by one and the 
        // column positions are off by one.  That is the origin of the "-1" 
        // in the code below.
        CALModuleEditorManager moduleEditor = 
            viewer.getControlManager().getModuleEditor();

        if (range != null) {
            
            // conver from SourceRange (line and column) to position (offset and length)
            IDocument moduleDoc = moduleEditor.getViewer().getDocument();
            int start = moduleDoc.getLineInformation(range.getStartLine()-1).getOffset() + 
                    range.getStartColumn() - 1;
            int length = moduleDoc.getLineInformation(range.getEndLine()-1).getOffset() - start + 
                    range.getEndColumn() - 1;
            
            return moduleDoc.get(start, length);
        }
        return null;
    }

    
    @Override
    @SuppressWarnings("restriction")
    protected ModuleName getModuleName(org.openquark.cal.eclipse.core.CALModelManager cmm) {
        return viewer.getModuleName();
    }

    


    /**
     * instead of searching through the CAL model, just figure out the word that is being hovered 
     * over and pray that it is a CAL identifier, if not, then nothing appears.
     * 
     * Note that identifiers defined within the embedded editor will not be displayed, only those
     * that are displayed outside of the embedded editor will
     */
    @Override
    protected Precise searchForIdentifier(int firstLine, int column,
            ModuleName moduleName, CompilerMessageLogger messageLogger,
            SourceMetricsManager sourceMetrics) {
        
        IDocument doc = viewer.getDocument();
        CodeAnalyzer analyzer = viewer.getCodeAnalyzer();
        String word = getWord(doc, firstLine, column);
        Pair<QualifiedName, Category> pair = analyzer.getQualifiedName(word);

        if (pair != null) {
           return createPrecise(pair.fst(), pair.snd());
        } else {
            return null;
        }
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
            EmbeddedCALPlugin.logError("Error getting text hover", e);
        } catch (StringIndexOutOfBoundsException e) {
        }
        
        return "";
    }


    /*
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
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            } catch (NoSuchMethodException e) {
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            }
        }
        if (sourceRangeCons == null) {
            try {
                sourceRangeCons = SourceRange.class.getDeclaredConstructor(String.class);
                sourceRangeCons.setAccessible(true);
            } catch (SecurityException e) {
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            } catch (NoSuchMethodException e) {
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            }
        }
        
        
        if (preciseCons != null && sourceRangeCons != null) {
            try {
                return preciseCons.newInstance(sourceRangeCons.newInstance(""), 
                        name, category, false);
            } catch (IllegalArgumentException e) {
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            } catch (InstantiationException e) {
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            } catch (IllegalAccessException e) {
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            } catch (InvocationTargetException e) {
                EmbeddedCALPlugin.logError("Error getting text hover", e);
            }
        }
        return null;
    }
    
    @Override
    protected boolean isSyntheticModule() {
        return true;
    }
}
