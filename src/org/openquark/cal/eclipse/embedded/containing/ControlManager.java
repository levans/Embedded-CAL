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
 * ControlManager.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;

import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.DELETE_NEXT_WORD;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.DELETE_PREVIOUS_WORD;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.LINE_END;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.LINE_START;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.SELECT_LINE_END;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.SELECT_LINE_START;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.SELECT_WORD_NEXT;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.TEXT_END;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.TEXT_START;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.WORD_NEXT;
import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.WORD_PREVIOUS;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IPaintPositionManager;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.undo.DocumentUndoEvent;
import org.eclipse.text.undo.DocumentUndoManager;
import org.eclipse.text.undo.IDocumentUndoListener;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer;
import org.openquark.cal.eclipse.embedded.analyzer.InternalCodeAnalyzer;
import org.openquark.cal.eclipse.embedded.contained.CALExpressionEditorManager;
import org.openquark.cal.eclipse.embedded.contained.CALExpressionEditorProperties;
import org.openquark.cal.eclipse.embedded.contained.CALModuleEditorManager;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorProperties;
import org.openquark.cal.eclipse.embedded.contained.EditorManagerFactory;
import org.openquark.cal.eclipse.embedded.contained.IContainedEditorListener;
import org.openquark.cal.eclipse.embedded.exported.IEmbeddedCalConstants;

/**
 * 
 * @author Andrew Eisenberg
 * @created Apr 25, 2007
 *
 * This class creates and manages the positions of the contained editors in the 
 * containing editor
 * <p>
 * The field editorPositionMap maps an contained editor to a position 
 * (offset and length) in the containing editor.
 */
public class ControlManager implements IPainter, ITextPresentationListener, IContainedEditorListener {

    /**
     * The paint position manager used by this paint manager. The paint position
     * manager is installed on a single document and control the creation/disposed
     * and updating of a position category that will be used for managing positions.
     * 
     * This one is essentially the same as Eclipse's PositionManager, but 
     * the IPositionUpdater must be a default one, not one used by the 
     * default PositionManager
     * 
     */
    static class ControlPositionManager implements IPaintPositionManager {

        /** The document this position manager works on */
        private IDocument fDocument;
        /** The position updater used for the managing position category */
        private IPositionUpdater fPositionUpdater;
        /** The managing position category */
        private String fCategory;

        /**
         * Creates a new position manager. Initializes the managing
         * position category using its class name and its hash value.
         * @param doc the document managed by this control manager
         */
        public ControlPositionManager(IDocument doc) {
            fCategory = getClass().getName() + hashCode();
            fPositionUpdater = new DefaultPositionUpdater(fCategory);
            install(doc);
        }

        public String getCategory() {
            return fCategory;
        }

        /**
         * Installs this position manager in the given document. The position manager stays
         * active until <code>uninstall</code> or <code>dispose</code>
         * is called.
         *
         * @param document the document to be installed on
         */
        public void install(IDocument document) {
            if (document != null && this.fDocument != document) {
                fDocument= document;
                fDocument.addPositionCategory(fCategory);
                fDocument.addPositionUpdater(fPositionUpdater);
            }
        }

        /**
         * Disposes this position manager. The position manager is automatically
         * removed from the document it has previously been installed
         * on.
         */
        public void dispose() {
            uninstall(fDocument);
        }

        /**
         * Uninstalls this position manager form the given document. If the position
         * manager has no been installed on this document, this method is without effect.
         *
         * @param document the document form which to uninstall
         */
        public void uninstall(IDocument document) {
            if (document == fDocument && document != null) {
                try {
                    fDocument.removePositionUpdater(fPositionUpdater);
                    fDocument.removePositionCategory(fCategory);
                } catch (BadPositionCategoryException x) {
                    // should not happen
                    EmbeddedCALPlugin.logError("Error managing document:\n" + document.get(), x);
                }
                fDocument= null;
            }
        }

        /*
         * @see IPositionManager#addManagedPosition(Position)
         */
        public void managePosition(Position position) {
            try {
                fDocument.addPosition(fCategory, position);
            } catch (BadPositionCategoryException x) {
                // should not happen
                EmbeddedCALPlugin.logError("Error managing position " + position, x);
            } catch (BadLocationException x) {
                // should not happen
                EmbeddedCALPlugin.logError("Error managing position " + position, x);
            }
        }

        /*
         * @see IPositionManager#removeManagedPosition(Position)
         */
        public void unmanagePosition(Position position) {
            try {
                fDocument.removePosition(fCategory, position);
            } catch (BadPositionCategoryException x) {
                // should not happen
                EmbeddedCALPlugin.logError("Error managing position " + position, x);
            }
        }
    }
    
    
    //delete me
    class UndoListener implements IDocumentUndoListener {
        public void documentUndoNotification(DocumentUndoEvent event) {
            if ((event.getEventType() & DocumentUndoEvent.UNDONE) != 0 ||
                    (event.getEventType() & DocumentUndoEvent.REDONE) != 0) {
                // does this event look like the undoing of an embedded editor?
                if (event.getText().startsWith(IEmbeddedCalConstants.EMBEDDED_REGION_START) &&
                        event.getText().endsWith(IEmbeddedCalConstants.EMBEDDED_REGION_END)) {
                    paint(ContainingEditor.EMBEDDED_REPAINT);   
                }
            }
        }
    }

    /** 
     * Tiny font for holding text that should be invisible.  Used for text after
     * new lines that should be behind embedded editors.
     * really, the text height should be 0, but this is the closest we can get
     */
    private final static Font TINY_FONT = new Font(Display.getDefault(), new FontData("Times", 1, 0));

    private final ContainingEditorScanner scanner = new ContainingEditorScanner();
    private final ContainingEditor containingEditor;
    private final IDocument doc;
    private final StyledText styledText;
    private IPaintPositionManager ppManager;
    private Map<ContainedEditorManager, Position> editorPositionMap = new HashMap<ContainedEditorManager, Position>();
    private Map<ContainedEditorManager, EmbeddedAnnotation> editorAnnotationMap = new HashMap<ContainedEditorManager, EmbeddedAnnotation>();
    
    private CALModuleEditorManager moduleEditor;  // should have at most one module editor
    private ContainedEditorManager currentlyActiveEditor = null;
    private UndoListener undoListener;
    
    private IAnnotationModel annotationModel;
    
    /**
     * Creates a new Control manager for the given containing editor
     * @param embeddedEditor
     * @param doc
     * @param styledText
     */
    @SuppressWarnings("restriction")
    ControlManager(ContainingEditor containingEditor, StyledText styledText, IDocument doc) {
        this.containingEditor = containingEditor;
        this.styledText = styledText;
        this.doc = doc;

    }

    void initializeAnnotationModel() {
        // fold the ranges that are covered by the control
        // do this by creating projection annotations
        // also creates annotations in the gutters
        annotationModel = containingEditor.internalGetSourceViewer().getAnnotationModel();
    }
    
    // delete me
    public void installUndoListener(DocumentUndoManager undoManager) {
        if (undoListener == null) {
            undoListener = new UndoListener();
        }
        undoManager.addDocumentUndoListener(undoListener);
    }
    
    // delete me
    public void uninstallUndoListener(DocumentUndoManager undoManager) {
        undoManager.removeDocumentUndoListener(undoListener);
    }
    
    /**
     * Installs a document partitioner on the containing editor's document.
     * This partitioner will determine where contained editors should go.
     * @param viewer
     */
    public void installPartitioner(ISourceViewer viewer) { 
        FastPartitioner partitioner = new FastPartitioner(scanner, 
                new String[] {ContainingEditorScanner.CONTAINED_EDITOR} );
        partitioner.connect(doc);

        ((IDocumentExtension3) doc).setDocumentPartitioner(
                ContainingEditorScanner.CONTAINED_EDITOR, partitioner);

        if (viewer instanceof ITextViewerExtension4) {
            ((ITextViewerExtension4) viewer).addTextPresentationListener(this);
        } else {
            EmbeddedCALPlugin.logWarning("Cannot install Presentation Listener.  " +
                    "The display may not update properly.", 
                    new RuntimeException());
        }
    }


    /**
     * extended so that we use our own position manager, not the one that
     * is passed in.
     */
    public void setPositionManager(IPaintPositionManager manager) {
        // ignore the passed in position manager
        this.ppManager = new ControlPositionManager(doc);
        this.containingEditor.getPaintManager().inputDocumentChanged(null, doc);
    }

    /**
     * this is the workhorse method that goes through the entire document and generates 
     * the controls for the contained editors where they are supposed to be located.
     */
    boolean generateControls() {
        return generateControls(0, doc.getLength());
    }
    
    boolean generateControls(int start, int length) {

        // if null then the control manager is not initialized
        if (annotationModel == null) return false;
        
        // set up a scan for the entire document.
        scanner.setPartialRange(doc, start, length, IDocument.DEFAULT_CONTENT_TYPE, start);

        boolean controlCreated = false;
        
        // create the controls,
        // determine their ranges, 
        // add them to the StyledText
        IToken tok;
        while (! ( tok = scanner.nextToken() ).isEOF()) {
            if (tok == ContainingEditorScanner.EDITOR_TOKEN) {
                StyleRange[] ranges = createAndAddControl(
                        scanner.getTokenOffset(), scanner.getTokenLength());
                TextPresentation singlePres = new TextPresentation();
                singlePres.addStyleRange(ranges[0]);
                singlePres.addStyleRange(ranges[1]);
                this.containingEditor.internalGetSourceViewer().changeTextPresentation(singlePres, true);
                
                controlCreated = true;
            }
        }
        
        return controlCreated;
    }

    /**
     * Maps a position from the model; (complete) document to the projected document
     * that may have some folded elements
     * 
     * Use modelToProjected or projectedToModel when doing translation from screen to the document.  
     * The following are specified in model coordinates
     * <ul>
     * <li>Document offsets
     * <li>Style ranges
     * </ul>
     * The following are specified in projected coordinates
     * <ul>
     * <li>pixels on the screen (eg, all points, sizes, and locations)
     * <li>lexical offsets into the styled text
     * </ul>
     * @param modelPosition
     * @return
     */
    Position modelToProjected(Position modelPosition) {
        ISourceViewer viewer = containingEditor.getContainingViewer();
        if (viewer instanceof ProjectionViewer) {
            ProjectionViewer projViewer = (ProjectionViewer) viewer;
            IRegion region = projViewer.modelRange2WidgetRange(
                    new Region(modelPosition.offset, modelPosition.length));
            
            if (region == null) {
                // region is hidden in a fold
                return null;
            } else {
                return new Position(region.getOffset(), region.getLength());
            }
        } else {
            return modelPosition;
        }
    }

    /**
     * Maps a position from the underlying (complete) document to the projected document
     * that may have some folded elements
     * @param projectedPosition
     * @return
     * @see ControlManager#modelToProjected(Position)
     */
    Position projectedToModel(Position projectedPosition) {
        ISourceViewer viewer = containingEditor.getContainingViewer();
        if (viewer instanceof ProjectionViewer) {
            ProjectionViewer projViewer = (ProjectionViewer) viewer;
            IRegion region = projViewer.widgetRange2ModelRange(
                    new Region(projectedPosition.offset, projectedPosition.length));
            if (region != null) {
                return new Position(region.getOffset(), region.getLength());
            } else {
                return null;
            }
        } else {
            return projectedPosition;
        }
    }

    
    
    /**
     * triggers a repaint of the styled text of the containing editor whenever
     * the text has changed.
     * 
     * The repaint will update the positions of all of the embedded controls. 
     * 
     * XXX this method is being called too many times.  It is being called more than
     * once after each cursor change.  I need to take a good look at this and determine
     * exactly when and where it should be called
     */
    public void paint(int reason) {
        if (reason != TEXT_CHANGE && reason != ContainingEditor.EMBEDDED_REPAINT) {
            return;
        }
        List<ContainedEditorManager> toRemove = new LinkedList<ContainedEditorManager>();

        for (final ContainedEditorManager c : editorPositionMap.keySet()) {
            Position model = editorPositionMap.get(c);
            if (!model.isDeleted()) {
                // map from the model to the actual display (takes into account folding)
                Position projected = modelToProjected(model);
                if (projected == null) {
                    // position is hidden behind folding
                    c.getControl().setVisible(false);
                } else {
                    try {
                        Point location = styledText.getLocationAtOffset(projected.offset);
                        location.x += ContainingEditor.MARGIN;
                        location.y += ContainingEditor.MARGIN;
                        c.getControl().setVisible(true);
                        c.getControl().setLocation(location);
                    } catch (IllegalArgumentException e) {
                        EmbeddedCALPlugin.logError("Error repainting", e);
                    }
                }
            } else {
                toRemove.add(c);
            }
        }
        for (final ContainedEditorManager c : toRemove) {
            removeControl(c, true);
        }
        styledText.getParent().getParent().redraw();
    }

    
    /**
     * disposes all of the controls and unremembers their positions
     */
    public void dispose() {
        for (final ContainedEditorManager c : editorPositionMap.keySet()) {
            removeControl(c, false);
        }
        editorPositionMap.clear();
    }



    public void deactivate(boolean redraw) { }

    /**
     * Removes the control from this manager.  Unmanages this position, 
     * @param c the control to remove
     * @param doRemove whether or not this control should be completely 
     * removed, or just temporarily (eg- during a save)
     * 
     * @return the position of the removed control
     */
    private Position removeControl(ContainedEditorManager c, boolean doRemove) {
        Position p; 
        if (doRemove) {
            p = editorPositionMap.remove(c);
        } else {
            p = editorPositionMap.get(c);
        }
        try {
            ppManager.unmanagePosition(p);
            
            if (doRemove && !p.isDeleted()) {
//                Position projected = modelToProjected(p);
//                styledText.replaceStyleRanges(projected.offset, projected.length, new StyleRange[0]);
                styledText.replaceStyleRanges(p.offset, p.length, new StyleRange[0]);
            }
        } catch (NullPointerException e) {
            // do nothing...trying to unmanage posn after
            // document has already been uninstalled.
        }
        c.removeListener(this);
        c.dispose();
        return p;
    }



    /*
     * Adds a control at the given position
     */
    private ContainedEditorManager addControl(int offset, int length) {
        // create the control propoerties
        ContainedEditorProperties props;
        try {
            // get the contents by reading from the editor at the given position
            ASTNode editorExpression = ContainedEditorProperties.toASTNode(doc.get(offset, length));
            props = EditorManagerFactory.createProperties(
                    editorExpression);
        } catch (BadLocationException e) {
            // something about the contents was bad, create an empty editor instead
            EmbeddedCALPlugin.logError("Error trying to create ContainedEditorProperties: offset: " + 
                    offset + " length: " + length, e);
            props = new CALExpressionEditorProperties();
        }

        ContainedEditorManager contained = 
            EditorManagerFactory.createManager(props);
        
        if (contained.editorKind() ==  CALModuleEditorManager.EDITOR_KIND) {
            replaceModuleEditor((CALModuleEditorManager) contained);
        }
        
        contained.createControl(styledText, containingEditor);
        contained.initializeEditorContents(containingEditor);

        // determine the location of the contained editor
        Position projected = modelToProjected(new Position(offset, 0));
        Point location = styledText.getLocationAtOffset(projected.offset);
        location.x += ContainingEditor.MARGIN;
        location.y += ContainingEditor.MARGIN;
        contained.setLocation(location);
        
        return contained;

    }

    private void replaceModuleEditor(CALModuleEditorManager contained) {
        if (moduleEditor != null) {
            CodeAnalyzer analyzer = containingEditor.getAnalyzer();
            // ADE I don't like this instanceof test.  Can we make CodeAnalyzer implement EditorListener?
            // yes we can, but I don't have time now
            if (analyzer instanceof InternalCodeAnalyzer) {
                moduleEditor.removeListener((InternalCodeAnalyzer) analyzer);
            }
        }
        moduleEditor = contained;
        containingEditor.createCodeAnalyzer();
        
        if (moduleEditor != null) {
            CodeAnalyzer analyzer = containingEditor.getAnalyzer();
            // ADE I don't like this instanceof test.  Can we make CodeAnalyzer implement EditorListener?
            // yes we can, but I don't have time now
            if (analyzer instanceof InternalCodeAnalyzer) {
                moduleEditor.addListener((InternalCodeAnalyzer) analyzer);
            }
        }
    }

    /**
     * creates the style range of the StyledText for the range of the contained editor
     * 
     * XXX problem will occur if there is a newline in the position.
     * Working on this!  code folding.
     */
    private StyleRange[] createStyleRange(ContainedEditorManager c, Position p) {
        int offset = p.offset;
        int length = p.length;

        Rectangle rect = c.getControl().getBounds();   
        int ascent = rect.height-4;
        int descent = 4;    

        // use two style ranges

        // first style range covers the entire size of the contained editor
        StyleRange first = new StyleRange();
        first.start = offset;
        first.length = Math.min(1, length);
        first.background = this.containingEditor.colorManager.getColor(new RGB(255, 255, 255));
        first.metrics = new GlyphMetrics(
                ascent + ContainingEditor.MARGIN, 
                descent + ContainingEditor.MARGIN, 
                rect.width + 2*ContainingEditor.MARGIN );

        // this style range is hidden.  the height and width are 0
        StyleRange second = new StyleRange();
        second.start = offset+1;
        second.length = length-1;
        second.background = this.containingEditor.colorManager.getColor(new RGB(255, 255, 255));
        second.metrics = new GlyphMetrics(0,0,0);
        second.font = TINY_FONT;

        return new StyleRange[] { first , second };
    }
    
    

    /**
     * Creates and adds a single control at the given position
     * @param offset
     * @param length
     * @return pair of style ranges that covers this embedded editor
     */
    public StyleRange[] createAndAddControl(int offset, int length) {
        StyleRange[] styles = null;
        Position pos = new Position(offset, length);
        if (!editorPositionMap.containsValue(pos)) {
            ContainedEditorManager newContainedEditor = addControl(offset, length);
            newContainedEditor.addListener(this);
            styles = createStyleRange(newContainedEditor, pos);
            newContainedEditor.registerActions(containingEditor);
            editorPositionMap.put(newContainedEditor, pos);
            ppManager.managePosition(pos);
            
            // add the annotation in the gutter
            EmbeddedAnnotation annotation = new EmbeddedAnnotation(newContainedEditor);
            if (newContainedEditor.editorKind() == CALExpressionEditorManager.EDITOR_KIND) {
                annotation.setType("org.openquark.cal.eclipse.embedded.expressionAnnotation");
            } else {
                annotation.setType("org.openquark.cal.eclipse.embedded.moduleAnnotation");
            }
            annotation.setText(newContainedEditor.getCalContents());
            
            // must create a new position otherwise the document object is tracking the same position
            // in two partitions
            annotationModel.addAnnotation(annotation, new Position(offset, length));
//            annotationModel.collapse(annotation);
            editorAnnotationMap.put(newContainedEditor, annotation);
        } else {
            for (final ContainedEditorManager c : editorPositionMap.keySet()) {
                if (editorPositionMap.get(c).equals(pos)) {
                    styles = createStyleRange(c, pos);
                    break;
                }
            }
        }
        return styles;
    }

    /**
     * Checks to see if a projected position is behind an embedded editor
     * @param embeddedOffset
     * @param embeddedLength
     * @return true if the given position overlaps with any contained editor.
     * false otherwise
     */
    public boolean isPositionBehindEditor(int embeddedOffset, int embeddedLength) {
        // map from projected document (with folding) to the model
        // document (complete, without folding)
        Position model = projectedToModel(new Position(embeddedOffset, embeddedLength));
        for (final Position editorPos : editorPositionMap.values()) {
            if (model.offset + model.length > editorPos.offset && 
                    model.offset < editorPos.offset + editorPos.length) {
                return true;
            }
        }
        return false;
    }

    /**
     * finds a contained editor that is covered by this position
     * @param offset
     * @param length 
     * @param overlapOK whether or not the position passed in must be fully 
     * contained by the position of the editor or if the positions need to 
     * merely overlap (if <code>true</code> then overlap is OK.  if <code>false</code>
     * then position must be completely contained by the editor
     * 
     * @return the editor covered by the passed in position, or null if there
     * is none.
     */
    public ContainedEditorManager findEditor(int offset, int length, boolean overlapOK) {
        Position selectedModelPosition = new Position(offset, length);
        for (final ContainedEditorManager editor : editorPositionMap.keySet()) {
            Position editorPosition = editorPositionMap.get(editor);
            if (overlapOK) {
                if (editorPosition.overlapsWith(selectedModelPosition.offset, selectedModelPosition.length)) {
                    return editor;
                }
            } else {
                if (editorPosition.offset < selectedModelPosition.offset && 
                    editorPosition.offset + editorPosition.length > 
                            selectedModelPosition.offset + selectedModelPosition.length) {
                    return editor;
                }
            }
        }
        return null;
    }
    
    /**
     * finds a contained editor that is covered by this position
     * The position is in projected coordinates (ie- that of the styled text, not of document
     * coordinates)
     * @param offset of projected position
     * @param length of projected position
     * @param overlapOK whether or not the position passed in must be fully 
     * contained by the position of the editor or if the positions need to 
     * merely overlap (if <code>true</code> then overlap is OK.  if <code>false</code>
     * then position must be completely contained by the editor
     * 
     * @return the editor covered by the passed in position, or null if there
     * is none.
     */
    public ContainedEditorManager findEditorProjected(int offset, int length, boolean overlapOK) {
        Position model = projectedToModel(new Position(offset, length));
        return findEditor(model.offset, model.length, overlapOK);
    }
    
    /**
     * Extended to remove any style ranges that overlap with an embedded editor.
     * 
     * We don't want to change style ranges over the region where there is a
     * contained editor, because thet will remove the GlyphMetrics that we 
     * created earlier
     */
    public void applyTextPresentation(TextPresentation textPresentation) {
        // need to check if any of the ranges of the textPresentation overlaps
        // with an embedded editor region.

        // for now, we can assume that there won't be many store positions, so we can 
        // just go through them sequentially.
        // if this turns out to be expensive, we can be more intelligent later
        Collection<Position> values = editorPositionMap.values();
        for (Iterator<StyleRange> rangeIter = textPresentation.getAllStyleRangeIterator(); rangeIter.hasNext(); ) {
            StyleRange range = rangeIter.next();
            Position overlapPosition = null;
            for (final Position editorPosition : values) {
                if (editorPosition.overlapsWith(range.start, range.length)) {
                    overlapPosition = editorPosition;
                    break;
                }
            }
            
            if (overlapPosition != null) {
                textPresentation.replaceStyleRanges(createStyleRange(getEditor(overlapPosition), 
                        overlapPosition));
            }
        }
    }
    /**
     * get the key from a given value.  O(n), but we don't expect this map
     * to be very large.  can change this later if it is a bottleneck.
     */
    private ContainedEditorManager getEditor(Position p) {
        for (final ContainedEditorManager editor : editorPositionMap.keySet()) {
            if (p.equals(editorPositionMap.get(editor))) {
                return editor;
            }
        }
        return null;
    }
    
    public Position getEditorPosition(ContainedEditorManager editor) {
        return editorPositionMap.get(editor);
    }

    /**
     * Copies the contents of the contained editor into the containing editor.
     * 
     * The contents of the contained editor are serialized (ie- converted to java text) and 
     * overwrites the old serialization.
     * 
     * @param editor the editor to serialize
     * @param props the editor properties from which the serialization can be obtained
     */
    public void updateSerialization(ContainedEditorManager editor, ContainedEditorProperties props) {
        
        String serialization = props.serializeEmbeddedEditor(this);
        Position p = editorPositionMap.get(editor);
        try {
            doc.replace(p.offset, p.length, serialization);
        } catch (BadLocationException e) {
            EmbeddedCALPlugin.logError("Error updating annotation", e);
        }
        
        @SuppressWarnings("restriction")
        ICompilationUnit unit = ((org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider) 
                containingEditor.getDocumentProvider()).getWorkingCopy(
                        containingEditor.getEditorInput());
        
        if (unit != null) {
            props.requiresImport(unit);
        }
    }

    public void saveAllEditors() {
        String moduleName = getModuleName();
        if (moduleName != null) {
            for (final ContainedEditorManager editor : editorPositionMap.keySet()) {
                // check to see if the module name is up to date
                if (editor.editorKind() == CALExpressionEditorManager.EDITOR_KIND) {
                    CALExpressionEditorProperties exprProps = (CALExpressionEditorProperties) 
                            editor.getPropertiess();
                    if (! exprProps.getModuleName().equals(moduleName)) {
                        // module name has changed, must re-serialize this expression editor.
                        exprProps.setDirty(true);
                    }
                }
                editor.doSave();
            }
        }
    }

    
    /**
     * 
     * @return the name of the module associated with this compilation unit
     * there should be only one per compilation unit.  right now not checking for 
     * that, but will.
     * If no module is specified, then null is returned
     */
    public String getModuleName() {
        if (moduleEditor != null) {
            return moduleEditor.getModuleName();
        } else {
            return null;
        }
    }
    
    public CALModuleEditorManager getModuleEditor() {
        return moduleEditor;
    }

    /**
     * Converts a bunch of actions on the ContainingEditor into SwitchableActions.  This
     * way, when appropriate, the relevant action on the ContainedEditor will be executed
     * in place of the standard JDT action.
     * @param newContainedEditor The contained editor on which to register the actions
     */
    void registerActions() {
        IAction oldAction;  // the original JDT editor action
        IAction newAction;

        // navigation actions
        oldAction = originalAction(containingEditor.getAction(LINE_START));
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(LINE_START, newAction);

        oldAction = containingEditor.getAction(SELECT_LINE_START);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(SELECT_LINE_START, newAction);

        oldAction = containingEditor.getAction(LINE_END);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(LINE_END, newAction);

        oldAction = containingEditor.getAction(SELECT_LINE_END);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(SELECT_LINE_END, newAction);

        oldAction = containingEditor.getAction(WORD_PREVIOUS);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(WORD_PREVIOUS, newAction);

        oldAction = containingEditor.getAction(WORD_NEXT);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(WORD_NEXT, newAction);

        oldAction = containingEditor.getAction(SELECT_WORD_PREVIOUS);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(SELECT_WORD_PREVIOUS, newAction);

        oldAction = containingEditor.getAction(SELECT_WORD_NEXT);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(SELECT_WORD_NEXT, newAction);

        oldAction = containingEditor.getAction(DELETE_PREVIOUS_WORD);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(DELETE_PREVIOUS_WORD, newAction);

        oldAction = containingEditor.getAction(DELETE_NEXT_WORD);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(DELETE_NEXT_WORD, newAction);

        oldAction = containingEditor.getAction(TEXT_START);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(TEXT_START, newAction);

        oldAction = containingEditor.getAction(TEXT_END);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(TEXT_END, newAction);

        oldAction = containingEditor.getAction(ITextEditorActionConstants.SELECT_ALL);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(ITextEditorActionDefinitionIds.SELECT_ALL, newAction);
        containingEditor.setAction(ITextEditorActionConstants.SELECT_ALL, newAction);
        containingEditor.markAsStateDependentAction(ITextEditorActionConstants.SELECT_ALL, true);
        // Text actions

        // note- we use "paste", not PASTE since the former
        // is what the JavaEditor uses
        // the same goes for the other text operations

        oldAction = containingEditor.getAction("paste");
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction("paste", newAction);

        oldAction = containingEditor.getAction("copy");
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction("copy", newAction);

        oldAction = containingEditor.getAction("cut");
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction("cut", newAction);

        // hover
        oldAction = containingEditor.getAction(ITextEditorActionConstants.SHOW_INFORMATION);
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction(ITextEditorActionDefinitionIds.SHOW_INFORMATION, newAction);
        containingEditor.setAction(ITextEditorActionConstants.SHOW_INFORMATION, newAction);
        
        // toggle comment
        oldAction = containingEditor.getAction("ToggleComment");
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction("ToggleComment", newAction);
        containingEditor.setAction(IJavaEditorActionDefinitionIds.TOGGLE_COMMENT, newAction);
        
        // content assist
        oldAction = containingEditor.getAction("ContentAssistProposal");
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction("ContentAssistProposal", newAction);

        // open declaration
        oldAction = containingEditor.getAction("OpenEditor");
        newAction = new SwitchableAction(oldAction, this);
        containingEditor.setAction("OpenEditor", newAction);

        
//      oldAction = containingEditor.getAction(IJavaEditorActionDefinitionIds.FORMAT);
//      newAction = new SwitchableAction(oldAction, this);
//      containingEditor.setAction(IJavaEditorActionDefinitionIds.FORMAT, newAction);



    }


    /**
     * accesses the original JavaEditorAction
     * unwraps an action replaced by 
     * @param oldAction
     * @return
     */
    private IAction originalAction(IAction oldAction) {
        return oldAction instanceof SwitchableAction ? ((SwitchableAction) oldAction).getJavaEditorAction() : oldAction;
    }

    /**
     * Called whenever a contained editor is resized
     */
    public void editorResized(ContainedEditorManager editor,
            ContainedEditorProperties props) {
//        updateSerialization(editor, props);
    }


    public void editorSaved(ContainedEditorManager editor,
            ContainedEditorProperties props) {
        updateSerialization(editor, props);
        editorAnnotationMap.get(editor).setText(editor.getCalContents());
    }


    // XXX This method is still influx.  trying to get the 
    // screen to scroll if the cursor moves off it
    public void editorChanged(ContainedEditorManager editor,
            ContainedEditorProperties props) {
        containingEditor.setDirty();
        
        // ensure that if the cursor moves off the screen, then the control is
        // scrolled back into view
        if (editor == getCurrentlyActiveEditor()) {
            revealSelection(editor, editor.getSelection().offset);
        }
        
        // update the gutter annotation
        editorAnnotationMap.get(editor).setText(editor.getCalContents());
    }

    /**
     * Scrolls the containing editor to the given offset of the contained editor
     * @param editor
     * @param scrollTo text offset in the embedded editor that should be revealed
     */
    public void revealSelection(ContainedEditorManager editor, int scrollTo) {
        StyledText containedStyledText = (StyledText) editor.getAdapter(StyledText.class);
        
        // this progression determines the location of the offset in the coordinate system
        // of the containing styledText
        Point containedLoc = containedStyledText.getLocationAtOffset(scrollTo);
        Point displayLoc = containedStyledText.toDisplay(containedLoc);
        Point containingLoc = styledText.toControl(displayLoc);
        
        
        // next, we determine if this location is in the visible area.
        Point containingSize = styledText.getSize();
        Rectangle containingBounds = new Rectangle(0, 0, containingSize.x, containingSize.y);
        if (!containingBounds.contains(containingLoc) ) {
            // pad a little to the left and a little bit down
            containingLoc.x -= 50;
            containingLoc.y += 100;

            // if not, then perform a scroll.
            styledText.setTopPixel(styledText.getTopPixel() + containingLoc.y 
                    - containingSize.y);
            
            // do the same for horizontal
            styledText.setHorizontalPixel(styledText.getHorizontalPixel() 
                    + containingLoc.x);
        }
        paint(ContainingEditor.EMBEDDED_REPAINT);
    }


    public void editorDeleted(ContainedEditorManager editor,
            ContainedEditorProperties props) {
        
        IEditorStatusLine statusLine = (IEditorStatusLine) 
                containingEditor.getAdapter(IEditorStatusLine.class);
        statusLine.setMessage(false, "Editor Deleted", null);
        
        Position p = removeControl(editor, true);
        if (p != null) {
            try {
                containingEditor.getSelectionProvider().setSelection(new TextSelection(doc, p.offset, 0));
                doc.replace(p.offset, p.length, "");
            } catch (BadLocationException e) {
                // it's OK to ignore this
                // shouldn't happen anyway
            }
        }
        if (editor == moduleEditor) {
            // look for new module editor, or else it becomes null.
            replaceModuleEditor(null);
            for (final ContainedEditorManager contained : editorPositionMap.keySet()) {
                if (contained.editorKind() == CALModuleEditorManager.EDITOR_KIND) {
                    replaceModuleEditor((CALModuleEditorManager) contained);
                    break;
                }
            }
        }
        
        // remove this control's annotation
        EmbeddedAnnotation annotation = editorAnnotationMap.remove(editor);
        annotationModel.removeAnnotation(annotation);
        
        // remove listener
        editor.removeListener(this);
    }


    public void editorFocusGained(ContainedEditorManager editor,
            ContainedEditorProperties props) {
        Position p = editorPositionMap.get(editor);
        currentlyActiveEditor = editor;
        if (p != null) {
            containingEditor.getContainingViewer().setSelectedRange(p.offset, p.length);
        }
        containingEditor.updateSelectionDependentActions();
        containingEditor.updateStateDependentActions();
    }


    public void editorFocusLost(ContainedEditorManager editor,
            ContainedEditorProperties props) {
        currentlyActiveEditor = null;
        
        // only change the text if there is a change from previous serialization
        if (props.isDirty()) {
            updateSerialization(editor, props);
            // ensure that all line heights are redrawn to their correct size
            containingEditor.internalGetSourceViewer().invalidateTextPresentation();
        }
        
        // ensure that the actions are reset to correspond to the ContainingEditor, not the ContainedEditor
        containingEditor.updateSelectionDependentActions();
        containingEditor.updateStateDependentActions();
        editorAnnotationMap.get(editor).setText(editor.getCalContents());
    }

    public void exitingEditor(ContainedEditorManager editor,
            ContainedEditorProperties props, ExitDirection dir) {
    
        Position controlPosition = editorPositionMap.get(editor);
        switch (dir) {
        case UP:
            // not handled for now
        case DOWN:
            // not handled for now
            break;
        case LEFT:
            containingEditor.getSelectionProvider()
                .setSelection(new TextSelection(controlPosition.offset, 0));
            break;
        case RIGHT:
            containingEditor.getSelectionProvider()
                .setSelection(new TextSelection(controlPosition.offset+controlPosition.length, 0));
            break;
        }
        // implicitly triggers a focus lost event on the contained editor
        containingEditor.internalGetSourceViewer().getTextWidget().forceFocus();
    }


    public ContainedEditorManager getCurrentlyActiveEditor() {
        return currentlyActiveEditor;
    }
    public ContainingEditor getContainingEditor() {
        return containingEditor;
    }
}
