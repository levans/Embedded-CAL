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
 * ContainedEditorManager.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

import static org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS;
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
import static org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds.COPY;
import static org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds.CUT;
import static org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds.PASTE;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextNavigationAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.openquark.cal.eclipse.embedded.EmbeddedCALPlugin;
import org.openquark.cal.eclipse.embedded.contained.IContainedEditorListener.ExitDirection;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;
import org.openquark.cal.eclipse.embedded.containing.SwitchableAction;
import org.openquark.cal.eclipse.embedded.handlers.OpenDeclarationAction;
import org.openquark.cal.eclipse.embedded.handlers.ShowTooltipDescriptionAction;
import org.openquark.cal.eclipse.ui.CALEclipseUIPlugin;
import org.openquark.cal.eclipse.ui.caleditor.CALEditorMessages;
import org.openquark.cal.eclipse.ui.caleditor.CALSourceViewer;
import org.openquark.cal.eclipse.ui.caleditor.ToggleCommentAction;
import org.openquark.cal.eclipse.ui.text.CALPartitions;


/**
 * 
 * @author Andrew Eisenberg
 * @created Apr 25, 2007
 *
 * This class represents an editor that is contained in a larger one
 * This class is the abstract base class of any kind of editor that can
 * be contained.  To add a new kind of editor, clients extend this methods:
 * <ul>
 * <li>initListeners()</li>
 * <li>createMenuContext()</li>
 * <li>createSubControls()</li>
 * <li>getPreferredSize()</li>
 * <li>getEditorKind()</li>
 * </ul>
 * 
 * The class name ContainedEditorManager is used (rather than ContainedEditor) 
 * because this class is not derived from {@link org.eclipse.ui.texteditor.ITextEditor }
 * and it's name makes the distinction clear.
 * <p>
 * Each ContainedEditorManager, keeps track of the StyledText that contains the
 * CAL text.  It handles things like resize events, editor saves, and the 
 * context menu.
 * <p>
 * There is a {@link CALSourceViewer} and 
 * {@link ContainedSourceViewerConfiguration } that handles operations and actions
 * like syntax highlighting, reconciling, copy, and paste.
 * 
 */
public abstract class ContainedEditorManager implements IAdaptable {


    
    protected static final FormAttachment TOP_AND_LEFT = new FormAttachment(0, 0);

    protected static final FormAttachment UNEXPANDED_BOTTOM = new FormAttachment(100, -0);
    
    protected static final FormAttachment RIGHT = new FormAttachment(100, -0);

    protected static final FormAttachment EXPANDED_BOTTOM = new FormAttachment(100, -42);

    private final class Resizer extends MouseAdapter implements MouseMoveListener {
        boolean resizing = false;
        boolean inResizableLocation = false;
        

        @Override
        public void mouseDown(MouseEvent e) {
            if (inResizableLocation &&
                    e.button == 1) {
                resizing = true;
            }
        }
        @Override
        public void mouseUp(MouseEvent e) {
            if (e.button == 1) {
                resizeControl(true);
                resizing = false;
            }
        }

        public void mouseMove(MouseEvent e) {

            if (resizing) {
                props.setHeight(e.y+5);
                props.setWidth(e.x+5);
                resizeControl(false);

            } else {
                Point size = control.getSize();
                int lowerRightX = size.x;
                int lowerRightY = size.y;
                if (Math.abs(e.x - lowerRightX) < 15 &&
                        Math.abs(e.y - lowerRightY) < 15 ) {
                    // it is possible to resize
                    control.setCursor(resizeCursor);
                    inResizableLocation = true;
                } else {
                    control.setCursor(arrowCursor);
                    inResizableLocation = false;
                }
            }
        }

        @Override
        public void mouseDoubleClick(MouseEvent e) {
            if ( (e.stateMask ^ SWT.CTRL) == 0) {
                attemptDelete();
            }
        }
    }
    
    /**
     * This class handles key strokes that would otherwise move the cursor to the edge
     * of the embedded editor.  Eg- a right arrow key press when the caret is at the 
     * last character of the editor will cause the editor to lose focus and select 
     * the next character in the containing editor
     * 
     * @author aeisenberg
     *
     */
    private final class EditorEdgeListener implements VerifyKeyListener {

        public void verifyKey(VerifyEvent event) {
            if (event.stateMask != SWT.NONE) {
                return;
            }
            
            switch (event.keyCode) {
            case SWT.ARROW_RIGHT:
            case SWT.ARROW_LEFT:
            case SWT.ARROW_UP:
            case SWT.ARROW_DOWN:
                handleArrowPress(event.keyCode);
            }
        }
        
        void handleArrowPress(int keyCode) {
            int offset = styledText.getCaretOffset();
            if (keyCode == SWT.ARROW_RIGHT) {
                if (offset == styledText.getText().length()) {
                    exitRight();
                }
            } else if (keyCode == SWT.ARROW_LEFT) {
                if (offset == 0) {
                    exitLeft();
                }
                
            } else {
                int line = styledText.getLineAtOffset(offset);
                if (keyCode == SWT.ARROW_UP) {
                    if (line == 0) {
                        exitUp();
                    }
                } else if (keyCode == SWT.ARROW_DOWN) {
                    if (line == styledText.getLineCount()-1) {
                        exitDown();
                    }
                }
            }
        }
        
        void exitUp() {
            fireExitingEditor(ExitDirection.UP);
        }
        void exitDown() {
            fireExitingEditor(ExitDirection.DOWN);
        }
        void exitLeft() {
            fireExitingEditor(ExitDirection.LEFT);
        }
        void exitRight() {
            fireExitingEditor(ExitDirection.RIGHT);
        }
    }
        
    
    /**
     * stores the previous selection when focus is lost
     */
    Point rememberedSelection = null;

    private List<IContainedEditorListener> listeners;

    private final ContainedEditorProperties props;
    protected StyledText styledText;


    private ContainedEditorSourceViewer viewer;
    protected Composite control;

    /**
     * maps a java editor action to the equivalent action to be used
     * by this contained editor
     */
    private Map<String, IAction> actionMap = new HashMap<String, IAction>();

    private final static Color black = new Color(Display.getCurrent(), new RGB(0, 0, 0));
    private final Clipboard cb = new Clipboard(Display.getCurrent());

    private final Cursor arrowCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW);
    private final Cursor resizeCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_SIZENWSE);


    /**
     * containingPath is the directory that contains the containing editor
     * containedFileName is the filename from the containingPath to the styledText
     * that will be displayed
     * @param props Contains information on the contents, height, width, and validity of the
     * editor
     */
    public ContainedEditorManager(ContainedEditorProperties props) {
        this.props = props;
    }


    /**
     * creates the swt control that shows the contents of the embedded editor
     * 
     * sub-classes should extend to include their own controls
     * @param parent
     * @param containingEditor
     */
    public final void createControl(StyledText parent, ContainingEditor containingEditor) {
        control = new Composite(parent, SWT.BORDER);

        // Source viewer
        IPreferenceStore store = CALEclipseUIPlugin.getDefault().getCombinedPreferenceStore();
        viewer = new ContainedEditorSourceViewer(control, null, null, 
                false, SWT.MULTI, store, 
                containingEditor.getControlManager());
        
        createSubControls();
        createContextMenu();
        initListeners();
        resizeControl(false);
    }


    /**
     * subclasses extend to add their own controls
     */
    protected void createSubControls() {
        control.setCursor(arrowCursor);
        FormLayout layout = new FormLayout();
        layout.marginWidth = 1;
        layout.marginHeight = 1;
        layout.spacing = 5;
        control.setLayout(layout);
        control.setToolTipText("Ctrl Dbl-Click to delete box");

        // Styled Text
        styledText = viewer.getTextWidget();
        styledText.setFont(control.getParent().getFont());

        FormData styledTextData = new FormData();
        styledTextData.top = TOP_AND_LEFT;
        styledTextData.left = TOP_AND_LEFT;
        styledTextData.right = RIGHT;
        styledText.setLayoutData(styledTextData);

        
        
        
        if (props.isShowingAll()) {
            // expanded view, styled text is smallerr
            styledTextData.bottom = EXPANDED_BOTTOM;
        } else {
            // unexpanded view, styled text takes up entire area
            styledTextData.bottom = UNEXPANDED_BOTTOM;
        }
    }




    /**
     * creates the listeners for all the controls 
     * sub-classes should extend
     */
    protected void initListeners() {
        styledText.addExtendedModifyListener(new ExtendedModifyListener() {
            /**
             * maybe not most efficient way of doing things, but 
             * update the properties on every keystroke
             */
            public void modifyText(ExtendedModifyEvent event) {
                props.setCalContents(viewer.getDocument().get());
                autoresize();
                fireEditorChanged();
            }
        });

        // add a paint listener to draw a little resize triangle
        control.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e) {
                Rectangle r = control.getClientArea();
                int x = r.width-10;
                int y = r.height-10;
                GC graphics = e.gc;
                graphics.setBackground(black);
                graphics.fillPolygon(new int[] { 
                        x, y+10, 
                        x+10, y+10, 
                        x+10, y });
            }
        });

        // add focus listener to handle selection changes when
        // focus is gained or lost.
        // we want to remember the previous selection on next enter
        styledText.addFocusListener(new FocusListener() {
            public void focusLost(FocusEvent e) {
                rememberedSelection = styledText.getSelection();
                styledText.setSelection(0,0);
                if (listeners != null) {
                    for (final IContainedEditorListener listener : listeners) {
                        listener.editorFocusLost(ContainedEditorManager.this, props);
                    }
                }
            }

            public void focusGained(FocusEvent e) {
                if (rememberedSelection != null) {
                    styledText.setSelection(rememberedSelection);
                    rememberedSelection = null;
                }

                if (listeners != null) {
                    for (final IContainedEditorListener listener : listeners) {
                        listener.editorFocusGained(ContainedEditorManager.this, props);
                    }
                }
            }
        });
        
        
        // add a listener that allows resize when in the right region
        Resizer sizer = new Resizer();
        control.addMouseMoveListener(sizer);
        control.addMouseListener(sizer);
        styledText.addVerifyKeyListener(new EditorEdgeListener());
    }


    public ContainedEditorSourceViewer getViewer() {
        return viewer;
    }
    
    /**
     * sub classes should extend to add entries to the menu
     */ 
    protected Menu createContextMenu() {
        Menu m = new Menu(styledText);
        MenuItem delete = new MenuItem(m, 0);
        delete.setText("&Delete Embedded Editor");
        delete.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) { }
            public void widgetSelected(SelectionEvent e) {
                attemptDelete();
            }
        });
        delete.setImage(EmbeddedCALPlugin.getImageDescriptor("icons/delete_edit.gif").createImage());
        new MenuItem(m, SWT.SEPARATOR);
        MenuItem cut = new MenuItem(m, 0);
        cut.setText("C&ut");
        cut.setImage(EmbeddedCALPlugin.getImageDescriptor("icons/cut_edit.gif").createImage());
        cut.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                String textData = styledText.getSelectionText();
                if (textData.length() > 0) {
                    TextTransfer textTransfer = TextTransfer.getInstance();
                    cb.setContents(new Object[]{textData}, new Transfer[]{textTransfer});

                    int start = styledText.getSelection().x;
                    int length = styledText.getSelection().y - start;
                    styledText.replaceTextRange(start, length, "");
                }
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
        MenuItem copy = new MenuItem(m, 0);
        copy.setText("&Copy");
        copy.setImage(EmbeddedCALPlugin.getImageDescriptor("icons/copy_edit.gif").createImage());
        copy.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                String textData = styledText.getSelectionText();
                if (textData.length() > 0) {
                    TextTransfer textTransfer = TextTransfer.getInstance();
                    cb.setContents(new Object[]{textData}, new Transfer[]{textTransfer});
                }
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
        MenuItem paste = new MenuItem(m, 0);
        paste.setText("&Paste");
        paste.setImage(EmbeddedCALPlugin.getImageDescriptor("icons/paste_edit.gif").createImage());
        paste.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                TextTransfer transfer = TextTransfer.getInstance();
                String data = (String)cb.getContents(transfer);
                if (data != null) {
                    styledText.insert(data);
                    int sel = styledText.getSelection().x + data.length();
                    styledText.setSelection(sel, sel);
                }
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
             
        new MenuItem(m, SWT.SEPARATOR);
        MenuItem openDeclaration = new MenuItem(m, 0);
        openDeclaration.setText("&Open Declaration");
        openDeclaration.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                actionMap.get("OpenEditor").run();
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });

        styledText.setMenu(m);
        control.setMenu(m);
        return m;
    }


    public void initializeEditorContents(ContainingEditor containingEditor) {
        setEditorContents();

        // set up the viewer configuration
        IPreferenceStore store = CALEclipseUIPlugin.getDefault().getCombinedPreferenceStore();
        configuration = new ContainedSourceViewerConfiguration(
                CALEclipseUIPlugin.getDefault().getCALTextTools().getColorManager(), 
                store, containingEditor, CALPartitions.CAL_PARTITIONING, this);
        viewer.configure(configuration);
        
        // set up the undo and redo context
        IUndoContext containedUndoContext = 
            DocumentUndoManagerRegistry.getDocumentUndoManager(viewer.getDocument()).getUndoContext();
        ObjectUndoContext containingUndoContext = 
            (ObjectUndoContext) DocumentUndoManagerRegistry.getDocumentUndoManager(
                    containingEditor.getContainingViewer().getDocument())
                    .getUndoContext();
        containingUndoContext.addMatch(containedUndoContext);
        
        // do the initial reconciling of the viewer's contents
        viewer.reconcile();
    }

    
    /**
     * @return a constant that describes what kind of contained editor this is (ie- module or expression)
     */
    public abstract String editorKind();
    
    /**
     * method is called when this embedded editor will be
     * saved to an annotation
     */
    public void doSave() {
        if (props.isDirty() && listeners != null) {
            for (final IContainedEditorListener listener : listeners) {
                listener.editorSaved(this, props);
            }
        }
    }

    /**
     * resizes the contained editor to the size specified in the properties
     */
    private void resizeControl(boolean fireEvent) {
        
        Point p = control.getSize();
        
        if (p.x != props.getWidth() || p.y != props.getHeight()) {
            control.setSize(props.getWidth(), props.getHeight());
    
            if (fireEvent && listeners != null) {
                for (final IContainedEditorListener listener : listeners) {
                    listener.editorResized(ContainedEditorManager.this, props);
                }
            }
        }
    }

    /**
     * determines the correct size of the contained editor based on its contents
     * and then 
     * resizes the contained editor to the size specified in the properties
     * 
     */
    protected final void autoresize() {
        Point p = getPreferredSize();
        props.setWidth(p.x);
        props.setHeight(p.y);
        resizeControl(true);
    }

    /**
     * sub-classes should extend to add their own behavior
     * @return the preferred size of this embedded editor
     */
    protected Point getPreferredSize() {
        styledText.pack(true);
        Point p = styledText.getSize();
        p.x += 8;
        p.y += 8;
        return p;
    }
    
    private void setEditorContents() {
        IDocument doc = new Document(props.getCalContents());
        viewer.setInput(doc);
        CALEclipseUIPlugin.getDefault().getCALTextTools().
        setupCALDocumentPartitioner(doc, CALPartitions.CAL_PARTITIONING);

    }

    public Composite getControl() {
        return control;
    }


    /**
     * attempts a deletion of this editor
     */
    private void attemptDelete() {
        MessageBox mb = new MessageBox(control.getShell(), SWT.YES | SWT.NO | SWT.ICON_QUESTION );
        mb.setText("Really delete?");
        mb.setMessage("Click YES to delete this embedded " +
        "editor. \n\n Click NO to ignore.");

        int res = mb.open();

        if (res == SWT.YES) {
            if (listeners != null) {
                // make a copy so that when a listener is removed, a concurrent modificatio error is not  
                // raised.
                IContainedEditorListener[] listenersCopy = listeners.toArray(new IContainedEditorListener[listeners.size()]);
                for (final IContainedEditorListener l : listenersCopy) {
                    l.editorDeleted(this, props);
                }
            }
            this.dispose();
        }
    }



    public void dispose() {
        control.dispose();
        cb.dispose();
        arrowCursor.dispose();
        resizeCursor.dispose();
        listeners = null;
    }

    public void setLocation(Point location) {
        control.setLocation(location);
    }

    /**
     * gives focus to this contained editor
     * @return true if focus was received, false otherwise
     */
    public boolean setFocus() {
        return control.setFocus();
    }

    public void addListener(IContainedEditorListener listener) {
        if (listeners == null) {
            listeners = new LinkedList<IContainedEditorListener>();
        }
        listeners.add(listener);
    }

    public void removeListener(IContainedEditorListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Registers all relevant actions with the internal action registry
     * @param containingEditor
     */
    public void registerActions(ITextEditor containingEditor) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);

        // navigation and selection actions
        registerAction(LINE_START, 
                new SwitchableAction.SmartLineStartAction(viewer, false));

        registerAction(SELECT_LINE_START, 
                new SwitchableAction.SmartLineStartAction(viewer, true));

        registerAction(LINE_END, 
                new SwitchableAction.LineEndAction(viewer, false));

        registerAction(SELECT_LINE_END, 
                new SwitchableAction.LineEndAction(viewer, true));

        registerAction(ITextEditorActionDefinitionIds.SELECT_ALL, 
                new TextOperationAction(bundle, 
                        "Editor.SelectAll.", containingEditor, ITextOperationTarget.SELECT_ALL));

        registerAction(WORD_PREVIOUS, 
                new SwitchableAction.NavigatePreviousSubWordAction(viewer, true));

        registerAction(WORD_NEXT, 
                new SwitchableAction.NavigateNextSubWordAction(viewer, true));

        registerAction(SELECT_WORD_PREVIOUS, 
                new SwitchableAction.SelectPreviousSubWordAction(viewer, true));

        registerAction(SELECT_WORD_NEXT, 
                new SwitchableAction.SelectNextSubWordAction(viewer, true));

        registerAction(DELETE_PREVIOUS_WORD, 
                new SwitchableAction.DeletePreviousSubWordAction(viewer, true));

        registerAction(DELETE_NEXT_WORD, 
                new SwitchableAction.DeleteNextSubWordAction(viewer, true));

        registerAction(TEXT_START, 
                new TextNavigationAction(styledText, ST.TEXT_START));

        registerAction(TEXT_END, 
                new TextNavigationAction(styledText, ST.TEXT_END));

        registerAction(ITextEditorActionDefinitionIds.SHOW_INFORMATION,
                new ShowTooltipDescriptionAction(
                        CALEditorMessages.getBundleForConstructedKeys(),
                        "ShowTooltipDescription.",
                        containingEditor));
        
        // styledText actions
        registerAction(PASTE, 
                new TextOperationAction(bundle, 
                        "Editor.Paste.", containingEditor, ITextOperationTarget.PASTE)); //$NON-NLS-1$);

        registerAction(COPY, 
                new TextOperationAction(bundle, 
                        "Editor.Copy.", containingEditor, ITextOperationTarget.COPY)); //$NON-NLS-1$);

        registerAction(CUT, 
                new TextOperationAction(bundle, 
                        "Editor.Cut.", containingEditor, ITextOperationTarget.CUT)); //$NON-NLS-1$);

        registerAction(CONTENT_ASSIST_PROPOSALS, 
                new ContentAssistAction(CALEditorMessages.getBundleForConstructedKeys(), 
                        "ContentAssistProposal.", containingEditor));
        
        ToggleCommentAction toggleCommentAction = new ToggleCommentAction(CALEditorMessages.getBundleForConstructedKeys(), "ToggleComment.", containingEditor);
        registerAction(IJavaEditorActionDefinitionIds.TOGGLE_COMMENT, 
                toggleCommentAction);
        toggleCommentAction.configure(viewer, configuration);
        
        registerAction("OpenEditor", new OpenDeclarationAction(bundle, "OpenDeclaration.", containingEditor));
        registerAction(IJavaEditorActionDefinitionIds.OPEN_EDITOR, new OpenDeclarationAction(bundle, "OpenDeclaration.", containingEditor));
    }
    private static final String BUNDLE_FOR_CONSTRUCTED_KEYS= "org.eclipse.ui.texteditor.ConstructedEditorMessages";//$NON-NLS-1$

    private SourceViewerConfiguration configuration;

    private void registerAction(String actionId, IAction action) {
        actionMap.put(actionId, action);
    }


    public IAction getAction(IAction javaEditorAction) {
        return actionMap.get(javaEditorAction.getActionDefinitionId());
    }

    
    public ContainedEditorProperties getPropertiess() {
        return props;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == ITextOperationTarget.class || adapter == ContainedEditorSourceViewer.class) {
            return viewer;
        } else if (adapter == StyledText.class) {
            return styledText;
        }
        return null;
    }
    
    public Position getSelection() {
        Point sel = styledText.getSelectionRange();
        return new Position(sel.x, sel.y);
    }

    public void setSelection(Position p) {
        styledText.setSelectionRange(p.offset, p.length);
        rememberedSelection = null;
    }
    
    public String getCalContents() {
        return props.getCalContents();
    }

    protected final void fireEditorChanged() {
        if (listeners != null) {
            for (final IContainedEditorListener l : listeners) {
                l.editorChanged(ContainedEditorManager.this, props);
            }
        }
    }


    protected final void fireExitingEditor(ExitDirection dir) {
        if (listeners != null) {
            for (final IContainedEditorListener listener : listeners) {
                listener.exitingEditor(ContainedEditorManager.this, props, dir);
            }
        }
    }
}
