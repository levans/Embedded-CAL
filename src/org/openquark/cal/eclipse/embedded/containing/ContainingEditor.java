/*******************************************************************************
 * Copyright (c) 2006 Business Objects Software Limited and others.
 * All rights reserved. 
 * This file is made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Business Objects Software Limited - initial API and implementation based on Eclipse 3.1.2 code for
 *     org.eclipse.jdt.ui/ui/org/eclipse/jdt/internal/ui/javaeditor/CompilationUnitEditor.java
 *                           Eclipse source is available at: http://www.eclipse.org/downloads/
 *******************************************************************************/


/*
 * ContainingEditor.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.ui.actions.FoldingActionGroup;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaOutlinePage;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.IWidgetTokenKeeper;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.history.IRefactoringExecutionListener;
import org.eclipse.ltk.core.refactoring.history.IRefactoringHistoryService;
import org.eclipse.ltk.core.refactoring.history.RefactoringExecutionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CommandNotMappedException;
import org.eclipse.ui.actions.ContributedAction;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ConfigurationElementSorter;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openquark.cal.eclipse.embedded.ContainingStyledText;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer;
import org.openquark.cal.eclipse.embedded.analyzer.ExternalCodeAnalyzer;
import org.openquark.cal.eclipse.embedded.analyzer.InternalCodeAnalyzer;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;

/**
 * 
 * @author Andrew Eisenberg
 * @created Apr 19, 2007
 *
 * 
 * The top-level editor that contains one or more embedded CAL 
 * editors.
 */
@SuppressWarnings("restriction")
public class ContainingEditor extends CompilationUnitEditor {
    private static final boolean CODE_ASSIST_DEBUG= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jdt.ui/debug/ResultCollector"));  //$NON-NLS-1$//$NON-NLS-2$
    
    static final int EMBEDDED_REPAINT = 32;
    
    /**
     * The same as CompilationUnitEditor.AdaptedSourceViewer,
     * Only need this class because we want to add some functionality to the cource viewer,
     * but CompilationUnitEditor.AdaptedSourceViewer is package protected
     */
    class ContainingAdaptedSourceViewer extends JavaSourceViewer  {

        public ContainingAdaptedSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles, IPreferenceStore store) {
            super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles, store);
        }

        public IContentAssistant getContentAssistant() {
            return fContentAssistant;
        }

        /*
         * @see ITextOperationTarget#doOperation(int)
         */
        @Override
        public void doOperation(int operation) {

            if (getTextWidget() == null)
                return;

            switch (operation) {
                case CONTENTASSIST_PROPOSALS:
                    long time= CODE_ASSIST_DEBUG ? System.currentTimeMillis() : 0;
                    String msg= fContentAssistant.showPossibleCompletions();
                    if (CODE_ASSIST_DEBUG) {
                        long delta= System.currentTimeMillis() - time;
                        System.err.println("Code Assist (total): " + delta); //$NON-NLS-1$
                    }
                    setStatusLineErrorMessage(msg);
                    return;
                case QUICK_ASSIST:
                    /*
                     * XXX: We can get rid of this once the SourceViewer has a way to update the status line
                     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=133787
                     */
                    msg= fQuickAssistAssistant.showPossibleQuickAssists();
                    setStatusLineErrorMessage(msg);
                    return;
            }

            super.doOperation(operation);
        }

        /*
         * @see IWidgetTokenOwner#requestWidgetToken(IWidgetTokenKeeper)
         */
        @Override
        public boolean requestWidgetToken(IWidgetTokenKeeper requester) {
            if (PlatformUI.getWorkbench().getHelpSystem().isContextHelpDisplayed())
                return false;
            return super.requestWidgetToken(requester);
        }

        /*
         * @see IWidgetTokenOwnerExtension#requestWidgetToken(IWidgetTokenKeeper, int)
         * @since 3.0
         */
        @Override
        public boolean requestWidgetToken(IWidgetTokenKeeper requester, int priority) {
            if (PlatformUI.getWorkbench().getHelpSystem().isContextHelpDisplayed())
                return false;
            return super.requestWidgetToken(requester, priority);
        }

        /*
         * @see org.eclipse.jface.text.source.SourceViewer#createFormattingContext()
         * @since 3.0
         */
        @Override
        public IFormattingContext createFormattingContext() {
            IFormattingContext context= new CommentFormattingContext();

            Map preferences;
            IJavaElement inputJavaElement= getInputJavaElement();
            IJavaProject javaProject= inputJavaElement != null ? inputJavaElement.getJavaProject() : null;
            if (javaProject == null)
                preferences= new HashMap(JavaCore.getOptions());
            else
                preferences= new HashMap(javaProject.getOptions(true));

            context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, preferences);

            return context;
        }
        
        @Override
        public void setSelectedRange(int selectionOffset, int selectionLength) {
            super.setSelectedRange(selectionOffset, selectionLength);
            controlManager.paint(EMBEDDED_REPAINT);
        }
        
    }
    
    private class RefactoringListener implements IRefactoringExecutionListener {
        public void executionNotification(RefactoringExecutionEvent event) {
            System.out.println(event.getEventType());
        }
    }

    /**
     * rather than making changes to the super class, make changes to this
     * we want to ensure that the class ContainingAdaptedSourceViewer stays
     * as close as possible to the JDT class CompilationUnitEditor.AdaptedSourceViewer
     */
    public class ContainingSourceViewer extends ContainingAdaptedSourceViewer {
        
        public ContainingSourceViewer(Composite parent,
                IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
                boolean showAnnotationsOverview, int styles,
                IPreferenceStore store) {
            super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles,
                    store);
        }
        
        @Override
        protected StyledText createTextWidget(Composite parent, int styles) {
            return new ContainingStyledText(parent, styles);
        }
    }
    

    private IDocument doc;
    ColorManager colorManager = new ColorManager();
    private ControlManager controlManager;
    final static int MARGIN = 2;  // margin is 0, but we can bump this up if we want
    private StyledText styledText;
    boolean isTextConfigured = false;
    private PaintManager paintManager;
    private RefactoringListener refactoringListener;  // this is just a test
    
    /** analyzes code of embedded cal editors */
    private CodeAnalyzer analyzer;

    private boolean internalDirty = false;

    private IEditorStatusLine editorStatusLine;

    @Override
    public void dispose() {
        colorManager.dispose();
        IRefactoringHistoryService service = RefactoringCore.getHistoryService();
        service.removeExecutionListener(refactoringListener);
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        controlManager.initializeAnnotationModel();
        controlManager.generateControls();
        createCodeAnalyzer();
    }

    /**
     * Creates the code analyzer for this editor.
     * 
     * Whenever an embedded module editor is added or removed, 
     * this method must be called
     */
    void createCodeAnalyzer() {
        if (controlManager.getModuleName() == null) {
            ICompilationUnit unit = 
                ((CompilationUnitDocumentProvider) getDocumentProvider()).getWorkingCopy(getEditorInput());
            analyzer = new ExternalCodeAnalyzer(unit);
        } else {
            analyzer = new InternalCodeAnalyzer(controlManager);
        }
    }

    public ISourceViewer internalGetSourceViewer() {
        return getSourceViewer();
    }

    PaintManager getPaintManager() {
        return paintManager;
    }


    /**
     * extend this method so that the control manager can register all
     * actions so that they can be switchable.
     * 
     * @see SwitchableAction
     */
    @Override
    protected void createActions() {
        super.createActions();

        // now that the standard actions have been created, 
        // go ahead and change some of them so that
        // we can execute contained editor actions 
        // when appropriate.
        controlManager.registerActions();
    }
    
    @Override
    public IAction getAction(String actionID) {
        IAction action = super.getAction(actionID);
        if (action == null) {
            action = findCompilationUnitContributedAction(actionID);
        }
        return action;
    }
    
    
    /**
     * ADE this is copied from AbstractTextEditor.  We need to be able to find the same contributed actions
     * (eg- dbl-click on rulter) as the compilation unit has.  This is how we do it.
     * 
     * Returns the action with the given action id that has been contributed via XML to this editor.
     * The lookup honors the dependencies of plug-ins.
     *
     * @param actionID the action id to look up
     * @return the action that has been contributed
     * @since 2.0
     */
    private IAction findCompilationUnitContributedAction(String actionID) {
        List actions= new ArrayList();
        IConfigurationElement[] elements= Platform.getExtensionRegistry().getConfigurationElementsFor(PlatformUI.PLUGIN_ID, "editorActions"); //$NON-NLS-1$
        for (int i= 0; i < elements.length; i++) {
            IConfigurationElement element= elements[i];
            if ("editorContribution".equals(element.getName())) {
                if (!"org.eclipse.jdt.ui.CompilationUnitEditor".equals(element.getAttribute("targetID"))) //$NON-NLS-1$
                    continue;

                IConfigurationElement[] children= element.getChildren("action"); //$NON-NLS-1$
                for (int j= 0; j < children.length; j++) {
                    IConfigurationElement child= children[j];
                    if (actionID.equals(child.getAttribute("actionID"))) //$NON-NLS-1$
                        actions.add(child);
                }
            }
        }
        int actionSize= actions.size();
        if (actionSize > 0) {
            IConfigurationElement element;
            if (actionSize > 1) {
                IConfigurationElement[] actionArray= (IConfigurationElement[])actions.toArray(new IConfigurationElement[actionSize]);
                ConfigurationElementSorter sorter= new ConfigurationElementSorter() {
                    /*
                     * @see org.eclipse.ui.texteditor.ConfigurationElementSorter#getConfigurationElement(java.lang.Object)
                     */
                    @Override
                    public IConfigurationElement getConfigurationElement(Object object) {
                        return (IConfigurationElement)object;
                    }
                };
                sorter.sort(actionArray);
                element= actionArray[0];
            } else
                element= (IConfigurationElement)actions.get(0);

            try {
                return new ContributedAction(getSite(), element);
            } catch (CommandNotMappedException e) {
                // out of luck, no command action mapping
            }
        }

        return null;
    }

    @Override
    protected JavaSourceViewerConfiguration createJavaSourceViewerConfiguration() {
        return new ContainingEditorConfiguration(super
                .createJavaSourceViewerConfiguration(), this);
    }

    /**
     * XXX We are getting a problem with the semantic highlighting of static
     * method invocations. change the preference store so that these are
     * disabled.
     * 
     * This is a hack. What is going on here means that semantic highlighting of
     * static methods will not be performed.
     */
    @Override
    protected void setPreferenceStore(IPreferenceStore store) {
        class MyChainedStore extends ChainedPreferenceStore {
            public MyChainedStore(IPreferenceStore store) {
                super(new IPreferenceStore[] { store });
            }

            @Override
            public boolean getBoolean(String name) {
                if ("semanticHighlighting.staticMethodInvocation.enabled".equals(name)) {
                    return false;
                }
                return super.getBoolean(name);
            }
        }

        ChainedPreferenceStore newStore = new MyChainedStore(store);

        super.setPreferenceStore(newStore);
    }

    /**
     * Extend this method to add a few more listeners and create the control
     * manager
     */
    @Override
    protected ISourceViewer createJavaSourceViewer(Composite parent,
            IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
            boolean isOverviewRulerVisible, int styles, IPreferenceStore store) {

        // create custom configuration
        ContainingEditorConfiguration config = (ContainingEditorConfiguration) createJavaSourceViewerConfiguration();
        setSourceViewerConfiguration(config);
        
        ISourceViewer viewer = new ContainingSourceViewer(parent, verticalRuler, overviewRuler,
              isOverviewRulerVisible, styles, store);

        // set up assorted fields
        doc = this.getDocumentProvider().getDocument(
                this.getEditorInput());

        styledText = viewer.getTextWidget();

        // ensure that text can't be typed in the embedded editor
        styledText.addVerifyListener(new VerifyListener() {
            public void verifyText(VerifyEvent e) {
                if (controlManager.isPositionBehindEditor(e.start, e.end-e.start)) {
                    // actually, should bring up a dialog box that
                    // will ask if should be deleted
                    e.doit = false;
                }
            }
        });
        
        // Whenever selection is completely behind an embedded editor, 
        // give focus to the editor
        final ISelectionProvider provider = viewer.getSelectionProvider();
        final ISelectionChangedListener focusOnEmbeddedListener = new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent e) {
                if (e.getSelection() instanceof TextSelection) {
                    provider.removeSelectionChangedListener(this);
                    TextSelection sel = (TextSelection) e.getSelection();
                    focusOnContainedEditor(sel.getOffset(), sel.getLength());
                    provider.addSelectionChangedListener(this);
                }
            }
        };
        provider.addSelectionChangedListener(focusOnEmbeddedListener);
        
        // This listener does two things
        //
        // page up and page down must trigger refresh, 
        // so editors are properly redrawn
        //
        // ensures that arrow keys can navigate into embedded editors
        styledText.addKeyListener(new KeyListener() {
            public void keyReleased(KeyEvent e) {
                switch (e.keyCode) {
                    case SWT.PAGE_UP:
                    case SWT.PAGE_DOWN:
                    case SWT.HOME:
                    case SWT.END:
                        controlManager.paint(EMBEDDED_REPAINT);
                        break;
                        
                    case SWT.ARROW_UP:
                    case SWT.ARROW_DOWN:
                    case SWT.ARROW_LEFT:
                    case SWT.ARROW_RIGHT:
                        provider.removeSelectionChangedListener(focusOnEmbeddedListener);
                        focusOnContainedEditor(getSourceViewer().getSelectedRange().x, 0);
                        provider.addSelectionChangedListener(focusOnEmbeddedListener);
                }
                
            }
            public void keyPressed(KeyEvent e) { }
        });

        // XXX for some reason clicking on initializers in the outline view does
        // not select anywhere in the java editor.  I think this is a bug in 
        // eclipse.  This means that the display is not repainted correctly when
        // an initializer is clicked.
        // To counteract, if the initializer is clicked twice, then the second
        // time, the display will repaint properly.  That's what the code below does
        JavaOutlinePage outline = (JavaOutlinePage) getAdapter(IContentOutlinePage.class);
        outline.addPostSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                controlManager.paint(EMBEDDED_REPAINT);
            }
        });
        
        ScrollBar verticalBar = styledText.getVerticalBar();
        if (verticalBar != null) {
            verticalBar.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    controlManager.paint(EMBEDDED_REPAINT);
                }
            });
        }
        ScrollBar horizontalBar = styledText.getHorizontalBar();
        if (horizontalBar != null) {
            horizontalBar.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    controlManager.paint(EMBEDDED_REPAINT);
                }
            });
        }
        controlManager = new ControlManager(this, styledText, doc);
        controlManager.installPartitioner(viewer);


        // redraw the embedded editors whenever there is a repaint
        viewer.addTextListener(new ITextListener() {
            public void textChanged(TextEvent event) {
                // ensure that this is a valid text change
//                if (event.getReplacedText() != null || event.getLength() != event.getText().length()) {
                if (event.getDocumentEvent() != null) {
                    DocumentEvent de = event.getDocumentEvent();
                    
//                    try {
//                        // do the full line
//                        IRegion start = doc.getLineInformationOfOffset(event.getOffset());
//                        IRegion end = doc.getLineInformationOfOffset(event.getOffset() + event.getLength());
//                        controlManager.generateControls(start.getOffset(), start.getLength());
                        
                        controlManager.generateControls(de.fOffset, de.getText().length());
                        controlManager.paint(IPainter.TEXT_CHANGE);
//                    } catch (BadLocationException e) {
//                        EmbeddedCALPlugin.logError("Error generating controls after text changed", e);
//                    }
                }
            }
        });
        
        // set up the paint listener
        try {
            Method fPaintManager;
            fPaintManager = TextViewer.class.getDeclaredMethod("getPaintManager");
            fPaintManager.setAccessible(true);
            paintManager = (PaintManager) fPaintManager.invoke(viewer);
            paintManager.addPainter(controlManager);
        } catch (SecurityException e) {
            EmbeddedCALPlugin.logError("Error initializing editor", e);
        } catch (IllegalArgumentException e) {
            EmbeddedCALPlugin.logError("Error initializing editor", e);
        } catch (IllegalAccessException e) {
            EmbeddedCALPlugin.logError("Error initializing editor", e);
        } catch (NoSuchMethodException e) {
            EmbeddedCALPlugin.logError("Error initializing editor", e);
        } catch (InvocationTargetException e) {
            EmbeddedCALPlugin.logError("Error initializing editor", e);
        }
        
        
        refactoringListener = new RefactoringListener();
        IRefactoringHistoryService service = RefactoringCore.getHistoryService();
        service.addExecutionListener(refactoringListener);
        
        // ensures that we have variable height
        styledText.setLineSpacing(styledText.getLineSpacing());

        return viewer;
    }

    public CodeAnalyzer getAnalyzer() {
        return analyzer;
    }

    // XXX Don't know if all the junk I'm doing is helping anything
    // I'm trying to remember the position after a save, but it keeps 
    // getting lost
    @Override
    protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
//        ISelection selection = this.getSelectionProvider().getSelection();
//        Position containedSel = null;
//        if (controlManager.getCurrentlyActiveEditor() != null) {
//            containedSel = controlManager.getCurrentlyActiveEditor().getSelection();
//        }
        saveContainedEditors();
        super.performSave(overwrite, progressMonitor);
//        getSelectionProvider().setSelection(selection);
//        if (containedSel != null) {
//            controlManager.getCurrentlyActiveEditor().setSelection(containedSel);
//        }
    }

    // XXX Don't know if all the junk I'm doing is helping anything
    // I'm trying to remember the position after a save, but it keeps 
    // getting lost
    @Override
    protected void performSaveAs(IProgressMonitor progressMonitor) {
//        ISelection selection = this.getSelectionProvider().getSelection();
//        Position containedSel = null;
//        if (controlManager.getCurrentlyActiveEditor() != null) {
//            containedSel = controlManager.getCurrentlyActiveEditor().getSelection();
//        }
        saveContainedEditors();
        super.performSaveAs(progressMonitor);
//        getSelectionProvider().setSelection(selection);
//        if (containedSel != null) {
//            controlManager.getCurrentlyActiveEditor().setSelection(containedSel);
//        }
    }

    /**
     * "saves" the contained editors by serializing their contents
     * and updating the underlying java document
     */
    private void saveContainedEditors() {
        styledText.setRedraw(false);
        controlManager.saveAllEditors();
        controlManager.dispose();
        controlManager.generateControls();
        internalDirty = false;
        styledText.setRedraw(true);
    }

    @Override
    protected void handleCursorPositionChanged() {
        super.handleCursorPositionChanged();
        styledText.getCaret();
        int offset = styledText.getCaretOffset();
        changeCaretHeight(offset);
        
        // focusOnContained is getting called twice.
        // comment this out so that the calls are not redundant.
//        ISelection sel = getSelectionProvider().getSelection();
//        if (sel instanceof TextSelection) {
//            TextSelection textSel = (TextSelection) sel;
//            focusOnContainedEditor(textSel.getOffset(), textSel.getLength());
//        }
    }
    
    
  
    @Override
    protected void selectAndReveal(int selectionStart, int selectionLength,
            int revealStart, int revealLength) {
        super.selectAndReveal(selectionStart, selectionLength, revealStart,
                revealLength);
        controlManager.paint(EMBEDDED_REPAINT);
    }
    
    
    /**
     * brings the focus to a contained editor if the position passed in
     * is completely enclosed by an editor
     * @param offset
     * @param length
     */
    private void focusOnContainedEditor(int offset, int length) {
        ContainedEditorManager editor = 
            controlManager.findEditor(offset, length, false);
        focusOnContainedEditor(editor);
    }

    public void focusOnContainedEditor(ContainedEditorManager editor) {
        if (editor != null) {
            editor.setFocus();
//            Position p = controlManager.getEditorPosition(editor);
//            getViewer().revealRange(p.offset, p.length);
            controlManager.revealSelection(editor, editor.getSelection().offset);
        }
    }

    /**
     * ensures that the cursor changes its line height after it is moved.
     */
    private void changeCaretHeight(int offset) {
        try {

            // for some reason, this line is not working for me
            // instead we will go through all style ranges of 
            // the line and see if three are any special ones
//          line = doc.getLineOfOffset(offset);
//          int lineHeight = styledText.getLineHeight(line);

            Position projected = new Position(offset);
            Position model = controlManager.projectedToModel(projected);
            
            IRegion region = doc.getLineInformationOfOffset(model.offset);
            
            Position model2 = new Position(region.getOffset(), region.getLength());
            Position projected2 = controlManager.modelToProjected(model2);
            
            if (projected2 != null) { // null if position is behind a fold
                int finalOffset = Math.min(projected2.offset, styledText.getText().length());
                int finalLength = Math.min(finalOffset + projected2.length, styledText.getText().length()) - finalOffset;
                
                StyleRange[] styles = styledText.getStyleRanges(finalOffset, finalLength);
                int lineHeight = styledText.getLineHeight();
                for (final StyleRange styleRange : styles) {
                    if (styleRange.metrics == null) {
                        lineHeight = Math.max(lineHeight, styleRange.rise);
                    } else {
                        lineHeight = Math.max(lineHeight, styleRange.metrics.ascent + styleRange.metrics.descent);
                    }
                }
    
                Caret c = styledText.getCaret();
                Rectangle r = c.getBounds();
                if (r.height != lineHeight) {
                    c.setSize(r.width, lineHeight);
                }
            }
        } catch (BadLocationException e) {
            // shouldn't happen
        }
    }

    /**
     * extended to ensure that a change to a contained editor will dirty
     * the containing editor
     */
    public void setDirty() {
        boolean fireChange = !internalDirty;
        internalDirty = true;

        if (fireChange) {
            firePropertyChange(PROP_DIRTY);
        }
    }

    
    /** Preference key for code formatter tab size */
    private final static String CODE_FORMATTER_TAB_SIZE= DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE;
    /** Preference key for inserting spaces rather than tabs */
    private final static String SPACES_FOR_TABS= DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR;
    /** Preference key for automatically closing strings */
    private final static String CLOSE_STRINGS= PreferenceConstants.EDITOR_CLOSE_STRINGS;
    /** Preference key for automatically closing brackets and parenthesis */
    private final static String CLOSE_BRACKETS= PreferenceConstants.EDITOR_CLOSE_BRACKETS;

    @Override
    protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
        try {

            ContainingAdaptedSourceViewer asv= (ContainingAdaptedSourceViewer) getSourceViewer();
            if (asv != null) {

                String p= event.getProperty();

                if (CLOSE_BRACKETS.equals(p)) {
//                    fBracketInserter.setCloseBracketsEnabled(getPreferenceStore().getBoolean(p));
                    return;
                }

                if (CLOSE_STRINGS.equals(p)) {
//                    fBracketInserter.setCloseStringsEnabled(getPreferenceStore().getBoolean(p));
                    return;
                }

                if (JavaCore.COMPILER_SOURCE.equals(p)) {
//                    boolean closeAngularBrackets= JavaCore.VERSION_1_5.compareTo(getPreferenceStore().getString(p)) <= 0;
//                    fBracketInserter.setCloseAngularBracketsEnabled(closeAngularBrackets);
                }

                if (SPACES_FOR_TABS.equals(p)) {
                    if (isTabsToSpacesConversionEnabled())
                        installTabsToSpacesConverter();
                    else
                        uninstallTabsToSpacesConverter();
                    return;
                }

                if (PreferenceConstants.EDITOR_SMART_TAB.equals(p)) {
                    if (getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_TAB)) {
                        setActionActivationCode("IndentOnTab", '\t', -1, SWT.NONE); //$NON-NLS-1$
                    } else {
                        removeActionActivationCode("IndentOnTab"); //$NON-NLS-1$
                    }
                }

                IContentAssistant c= asv.getContentAssistant();
                if (c instanceof ContentAssistant)
                    ContentAssistPreference.changeConfiguration((ContentAssistant) c, getPreferenceStore(), event);

                if (CODE_FORMATTER_TAB_SIZE.equals(p) && isTabsToSpacesConversionEnabled()) {
                    uninstallTabsToSpacesConverter();
                    installTabsToSpacesConverter();
                }
            }

        } finally {
            try {
                super.handlePreferenceStoreChanged(event);
            } catch (ClassCastException e) {
                // the super method causes a class cast exception because 
                // we are using a different source viewer.
                // wrap this in a try/catch so that this exception isn't propagated
            }
        }
    }

    @Override
    public Object getAdapter(Class required) {

        if (IEditorStatusLine.class.equals(required)) {
            if (editorStatusLine == null) {
                IStatusLineManager statusLineManager= getStatusLineManager();
                ISelectionProvider selectionProvider= getSelectionProvider();
                if (statusLineManager != null && selectionProvider != null)
                    editorStatusLine= new ContainingEditorStatusLine(statusLineManager, selectionProvider);
            }
            return editorStatusLine;
        }

        if (ITextOperationTarget.class.equals(required)) {
            ContainedEditorManager editor = controlManager.getCurrentlyActiveEditor();
            if (editor != null) {
                return editor.getAdapter(required);
            }
        }
        
        return super.getAdapter(required);
    }

    /**
     * Make this method visible to other classes in this package
     */
    @Override
    protected void updateSelectionDependentActions() {
        super.updateSelectionDependentActions();
    }

    /**
     * Make this method visible to other classes in this package
     */
    @Override
    protected void updateStateDependentActions() {
        super.updateStateDependentActions();
    }

    /**
     * override here because when the StyledText gets focus
     * it immediately passes it onto its first child for focus
     * which in this case is an embedded editor.  This is not th
     * behavior we want
     */
    @Override
    public void setFocus() { }

    @Override
    public boolean isDirty() {
        return internalDirty || super.isDirty();
    }
    
    public ControlManager getControlManager() {
        return controlManager;
    }
    
    public Position getContainedEditorPosition(ContainedEditorManager editor) {
        return controlManager.getEditorPosition(editor);
    }
    
    /**
     * get rid of this method
     */
    @Override
    protected FoldingActionGroup getFoldingActionGroup() {
        return super.getFoldingActionGroup();
    }
    
    public ContainingSourceViewer getContainingViewer() {
        return (ContainingSourceViewer) super.getViewer();
    }
}
