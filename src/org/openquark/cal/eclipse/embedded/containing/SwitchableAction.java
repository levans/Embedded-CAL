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
 * SwitchableAction.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;

import java.text.BreakIterator;
import java.text.CharacterIterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextNavigationAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.openquark.cal.compiler.LanguageInfo;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;
import org.openquark.cal.eclipse.ui.text.CALPartitions;
import org.openquark.cal.eclipse.ui.text.CALWordIterator;
import org.openquark.cal.eclipse.ui.text.DocumentCharacterIterator;


/**
 * This action delegates to an action in a java editor or in a
 * contained editor.
 * 
 * Whenever an action method is called, the getRelevantAction() 
 * method is called to determine whether delegation should go to the java
 * editor or to the currently active contained editor.
 * 
 * @author Andrew Eisenberg
 */
public class SwitchableAction implements IAction, IUpdate {

    /****************************************************************
     * The following classes are copied from super classes and 
     * changed slightly so that this class can have access to them.
     */

    /**
     * This action implements smart end.
     * Instead of going to the end of a line it does the following:
     * - if smart home/end is enabled and the caret is before the line's last non-whitespace and then the caret is moved directly after it
     * - if the caret is after last non-whitespace the caret is moved at the end of the line
     * - if the caret is at the end of the line the caret is moved directly after the line's last non-whitespace character
     * @since 2.1 (in 3.3 the access modifier changed from package visibility to protected)
     */
    public static class LineEndAction extends TextNavigationAction {

        /** boolean flag which tells if the text up to the line end should be selected. */
        private boolean fDoSelect;

        private final ISourceViewer fSourceViewer;

        /**
         * Create a new line end action.
         * @param viewer the source viewer
         * @param doSelect a boolean flag which tells if the text up to the line end should be selected
         */
        public LineEndAction(ISourceViewer viewer, boolean doSelect) {
            super(viewer.getTextWidget(), ST.LINE_END);
            fDoSelect= doSelect;
            this.fSourceViewer = viewer;
        }

        /**
         * Computes the offset of the line end position.
         * <p>
         * XXX: will become protected in 3.4.
         * </p>
         *
         * @param document the document where to compute the line end position
         * @param line the line to determine the end position of
         * @param length the length of the line
         * @param offset the caret position in the document
         * @return the offset of the line end
         * @since 3.3
         */
        int getLineEndPosition(final IDocument document, final String line, final int length, final int offset) {
            int index= length - 1;
            while (index > -1 && Character.isWhitespace(line.charAt(index)))
                index--;
            index++;

            LinkedModeModel model= LinkedModeModel.getModel(document, offset);
            if (model != null) {
                LinkedPosition linkedPosition= model.findPosition(new LinkedPosition(document, offset, 0));
                if (linkedPosition != null) {
                    int linkedPositionEnd= linkedPosition.getOffset() + linkedPosition.getLength();
                    int lineOffset;
                    try {
                        lineOffset= document.getLineInformationOfOffset(offset).getOffset();
                        if (offset != linkedPositionEnd && linkedPositionEnd - lineOffset < index)
                            index= linkedPositionEnd - lineOffset;
                    } catch (BadLocationException e) {
                        //should not happen
                    }
                }
            }
            return index;
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
        public void run() {
            boolean isSmartHomeEndEnabled= true;
//          IPreferenceStore store= getPreferenceStore();
//          if (store != null)
//          isSmartHomeEndEnabled= store.getBoolean(AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END);

            StyledText st= getTextWidget();
            if (st == null || st.isDisposed())
                return;
            int caretOffset= st.getCaretOffset();
            int lineNumber= st.getLineAtOffset(caretOffset);
            int lineOffset= st.getOffsetAtLine(lineNumber);

            int lineLength;
            int caretOffsetInDocument;
            final IDocument document= fSourceViewer.getDocument();

            try {
                caretOffsetInDocument= widgetOffset2ModelOffset(fSourceViewer, caretOffset);
                lineLength= document.getLineInformationOfOffset(caretOffsetInDocument).getLength();
            } catch (BadLocationException ex) {
                return;
            }
            int lineEndOffset= lineOffset + lineLength;

            int delta= lineEndOffset - st.getCharCount();
            if (delta > 0) {
                lineEndOffset -= delta;
                lineLength -= delta;
            }

            String line= ""; //$NON-NLS-1$
            if (lineLength > 0)
                line= st.getText(lineOffset, lineEndOffset - 1);

            // Remember current selection
            Point oldSelection= st.getSelection();

            // The new caret position
            int newCaretOffset= -1;

            if (isSmartHomeEndEnabled) {
                // Compute the line end offset
                int i= getLineEndPosition(document, line, lineLength, caretOffsetInDocument);

                if (caretOffset - lineOffset == i)
                    // to end of line
                    newCaretOffset= lineEndOffset;
                else
                    // to end of text
                    newCaretOffset= lineOffset + i;

            } else {

                if (caretOffset < lineEndOffset)
                    // to end of line
                    newCaretOffset= lineEndOffset;

            }

            if (newCaretOffset == -1)
                newCaretOffset= caretOffset;
            else
                st.setCaretOffset(newCaretOffset);

            st.setCaretOffset(newCaretOffset);
            if (fDoSelect) {
                if (caretOffset < oldSelection.y)
                    st.setSelection(oldSelection.y, newCaretOffset);
                else
                    st.setSelection(oldSelection.x, newCaretOffset);
            } else
                st.setSelection(newCaretOffset);

            fireSelectionChanged(oldSelection);
        }
    }

    /**
     * This action implements smart home.
     * Instead of going to the start of a line it does the following:
     * - if smart home/end is enabled and the caret is after the line's first non-whitespace then the caret is moved directly before it
     * - if the caret is before the line's first non-whitespace the caret is moved to the beginning of the line
     * - if the caret is at the beginning of the line the caret is moved directly before the line's first non-whitespace character
     * @since 2.1
     */
    public static class LineStartAction extends TextNavigationAction {

        /** boolean flag which tells if the text up to the beginning of the line should be selected. */
        private final boolean fDoSelect;
        private final ISourceViewer fSourceViewer;

        /**
         * Creates a new line start action.
         *
         * @param viewer the source viewer
         * @param doSelect a boolean flag which tells if the text up to the beginning of the line should be selected
         */
        public LineStartAction(ISourceViewer viewer, final boolean doSelect) {
            super(viewer.getTextWidget(), ST.LINE_START);
            fDoSelect= doSelect;
            fSourceViewer = viewer;
        }

        /**
         * Computes the offset of the line start position.
         *
         * @param document the document where to compute the line start position
         * @param line the line to determine the start position of
         * @param length the length of the line
         * @param offset the caret position in the document
         * @return the offset of the line start
         * @since 3.0
         */
        protected int getLineStartPosition(final IDocument document, final String line, final int length, final int offset) {
            int index= 0;
            while (index < length && Character.isWhitespace(line.charAt(index)))
                index++;

            LinkedModeModel model= LinkedModeModel.getModel(document, offset);
            if (model != null) {
                LinkedPosition linkedPosition= model.findPosition(new LinkedPosition(document, offset, 0));
                if (linkedPosition != null) {
                    int linkedPositionOffset= linkedPosition.getOffset();
                    int lineOffset;
                    try {
                        lineOffset= document.getLineInformationOfOffset(offset).getOffset();
                        if (offset != linkedPositionOffset && index < linkedPositionOffset - lineOffset)
                            index= linkedPositionOffset - lineOffset;
                    } catch (BadLocationException e) {
                        //should not happen
                    }
                }
            }
            return index;
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
        public void run() {
//          boolean isSmartHomeEndEnabled= false;
//          IPreferenceStore store= getPreferenceStore();
//          if (store != null)
//          isSmartHomeEndEnabled= store.getBoolean(AbstractTextEditor.PREFERENCE_NAVIGATION_SMART_HOME_END);
            boolean isSmartHomeEndEnabled= true;

            StyledText st= getTextWidget();
            if (st == null || st.isDisposed())
                return;

            int caretOffset= st.getCaretOffset();
            int lineNumber= st.getLineAtOffset(caretOffset);
            int lineOffset= st.getOffsetAtLine(lineNumber);

            int lineLength;
            int caretOffsetInDocument;
            final IDocument document= fSourceViewer.getDocument();

            try {
                caretOffsetInDocument= widgetOffset2ModelOffset(fSourceViewer, caretOffset);
                lineLength= document.getLineInformationOfOffset(caretOffsetInDocument).getLength();
            } catch (BadLocationException ex) {
                return;
            }

            String line= ""; //$NON-NLS-1$
            if (lineLength > 0) {
                int end= lineOffset + lineLength - 1;
                end= Math.min(end, st.getCharCount() -1);
                line= st.getText(lineOffset, end);
            }

            // Remember current selection
            Point oldSelection= st.getSelection();

            // The new caret position
            int newCaretOffset= -1;

            if (isSmartHomeEndEnabled) {

                // Compute the line start offset
                int index= getLineStartPosition(document, line, lineLength, caretOffsetInDocument);

                if (caretOffset - lineOffset == index)
                    // to beginning of line
                    newCaretOffset= lineOffset;
                else
                    // to beginning of text
                    newCaretOffset= lineOffset + index;

            } else {

                if (caretOffset > lineOffset)
                    // to beginning of line
                    newCaretOffset= lineOffset;
            }

            if (newCaretOffset == -1)
                newCaretOffset= caretOffset;
            else
                st.setCaretOffset(newCaretOffset);

            if (fDoSelect) {
                if (caretOffset < oldSelection.y)
                    st.setSelection(oldSelection.y, newCaretOffset);
                else
                    st.setSelection(oldSelection.x, newCaretOffset);
            } else
                st.setSelection(newCaretOffset);

            fireSelectionChanged(oldSelection);
        }

    }

    /**
     * This action implements smart home.
     * 
     * Instead of going to the start of a line it does the following:
     *  - if smart home/end is enabled and the caret is after the line's first non-whitespace then the caret is moved
     * directly before it, taking CALDoc and multi-line comments into account. - if the caret is before the line's
     * first non-whitespace the caret is moved to the beginning of the line - if the caret is at the beginning of the
     * line see first case.
     * 
     * @author Edward Lam
     */
    public static class SmartLineStartAction extends LineStartAction {

        /**
         * Creates a new smart line start action
         *
         * @param viewer the source viewer
         * @param doSelect a boolean flag which tells if the text up to the beginning of the line should be selected
         */
        public SmartLineStartAction(ISourceViewer viewer, final boolean doSelect) {
            super(viewer, doSelect);
        }

        /*
         * @see org.eclipse.ui.texteditor.AbstractTextEditor.LineStartAction#getLineStartPosition(java.lang.String, int, java.lang.String)
         */
        protected int getLineStartPosition(final IDocument document, final String line, final int length, final int offset) {

            String type = IDocument.DEFAULT_CONTENT_TYPE;
            try {
                type = TextUtilities.getContentType(document, CALPartitions.CAL_PARTITIONING, offset, true);
            } catch (BadLocationException exception) {
                // Should not happen
            }

            int index = super.getLineStartPosition(document, line, length, offset);
            if (type.equals(CALPartitions.CAL_DOC) || type.equals(CALPartitions.CAL_MULTI_LINE_COMMENT)) {
                if (index < length - 1 && line.charAt(index) == '*' && line.charAt(index + 1) != '/') {
                    do {
                        ++index;
                    } while (index < length && LanguageInfo.isCALWhitespace(line.charAt(index)));
                }
            } else {
                if (index < length - 1 && line.charAt(index) == '/' && line.charAt(index + 1) == '/') {
                    index++;
                    do {
                        ++index;
                    } while (index < length && LanguageInfo.isCALWhitespace(line.charAt(index)));
                }
            }
            return index;
        }
    }


    /**
     * Text navigation action to navigate to the next sub-word.
     * 
     * @author Edward Lam
     */
    public static abstract class NextSubWordAction extends
    TextNavigationAction {

        protected CALWordIterator fIterator = new CALWordIterator();

        private final boolean doSubWordNavigation;

        protected final ISourceViewer viewer;

        /**
         * Creates a new next sub-word action.
         * 
         * @param code
         *          Action code for the default operation. Must be an action code
         *          from
         * @see org.eclipse.swt.custom.ST
         */
        protected NextSubWordAction(int code, ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(viewer.getTextWidget(), code);
            this.doSubWordNavigation = doSubWordNavigation;
            this.viewer = viewer;
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
        public void run() {
            if (!doSubWordNavigation) {
                super.run();
                return;
            }

            final IDocument document = viewer.getDocument();
            fIterator.setText((CharacterIterator) new DocumentCharacterIterator(
                    document));
            int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget()
                    .getCaretOffset());
            if (position == -1)
                return;

            int next = findNextPosition(position);
            if (next != BreakIterator.DONE) {
                setCaretPosition(next);
                getTextWidget().showSelection();
                fireSelectionChanged();
            }

        }

        /**
         * Finds the next position after the given position.
         * 
         * @param position
         *          the current position
         * @return the next position
         */
        protected int findNextPosition(int position) {
            int widget = -1;
            while (position != BreakIterator.DONE && widget == -1) {
                position = fIterator.following(position);
                if (position != BreakIterator.DONE)
                    widget = widgetOffset2ModelOffset(viewer, position);
            }
            return position;
        }

        /**
         * Sets the caret position to the sub-word boundary given with
         * <code>position</code>.
         * 
         * @param position
         *          Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }

    /**
     * Text navigation action to navigate to the next sub-word.
     * 
     * @author Edward Lam
     */
    public static class NavigateNextSubWordAction extends NextSubWordAction {

        /**
         * Creates a new navigate next sub-word action.
         * @param viewer source view
         * @param doSubWordNavigation true if should naviagate into partial words 
         */
        public NavigateNextSubWordAction(ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(ST.WORD_NEXT, viewer, doSubWordNavigation);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.NextSubWordAction#setCaretPosition(int)
         */
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(
                    SwitchableAction.modelOffset2WidgetOffset(viewer, position));
        }
    }

    /**
     * Text operation action to delete the next sub-word.
     * 
     * @author Edward Lam
     */
    public static class DeleteNextSubWordAction extends NextSubWordAction implements
    IUpdate {

        /**
         * Creates a new delete next sub-word action.
         * @param viewer source view
         * @param doSubWordNavigation true if should naviagate into partial words 
         */
        public DeleteNextSubWordAction(ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(ST.DELETE_WORD_NEXT, viewer, doSubWordNavigation);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.NextSubWordAction#setCaretPosition(int)
         */
        protected void setCaretPosition(final int position) {
            // should check to see if the editor state is valid,
            // but for now we won't
            // if (!validateEditorInputState())
//          return;

            final int caret, length;
            Point selection = viewer.getSelectedRange();
            if (selection.y != 0) {
                caret = selection.x;
                length = selection.y;
            } else {
                caret = widgetOffset2ModelOffset(viewer, viewer.getTextWidget()
                        .getCaretOffset());
                length = position - caret;
            }

            try {
                viewer.getDocument().replace(caret, length, ""); //$NON-NLS-1$
            } catch (BadLocationException exception) {
                // Should not happen
            }
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.NextSubWordAction#findNextPosition(int)
         */
        protected int findNextPosition(int position) {
            return fIterator.following(position);
        }

        /*
         * @see org.eclipse.ui.texteditor.IUpdate#update()
         */
        public void update() {
            // should check to see if this is a read-only file
            setEnabled(true);
//          setEnabled(isEditorInputModifiable());
        }
    }

    /**
     * Text operation action to select the next sub-word.
     * 
     * @author Edward Lam
     */
    public static class SelectNextSubWordAction extends NextSubWordAction {

        /**
         * Creates a new select next sub-word action.
         * @param viewer source view
         * @param doSubWordNavigation true if should naviagate into partial words 
         */
        public SelectNextSubWordAction(ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(ST.SELECT_WORD_NEXT, viewer, doSubWordNavigation);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.NextSubWordAction#setCaretPosition(int)
         */
        protected void setCaretPosition(final int position) {

            final StyledText text = viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection = text.getSelection();
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(viewer, position);

                if (caret == selection.x)
                    text.setSelectionRange(selection.y, offset - selection.y);
                else
                    text.setSelectionRange(selection.x, offset - selection.x);
            }
        }
    }

    /**
     * Text navigation action to navigate to the previous sub-word.
     * 
     * @author Edward Lam
     */
    protected static abstract class PreviousSubWordAction extends TextNavigationAction {

        protected CALWordIterator fIterator = new CALWordIterator();

        private final boolean doSubWordNavigation;

        protected final ISourceViewer viewer;

        /**
         * Creates a new previous sub-word action.
         * 
         * @param code
         *          Action code for the default operation. Must be an action code
         *          from
         * @see org.eclipse.swt.custom.ST
         */
        protected PreviousSubWordAction(final int code, ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(viewer.getTextWidget(), code);
            this.doSubWordNavigation = doSubWordNavigation;
            this.viewer = viewer;
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
        public void run() {
            // Check whether we are in a java code partition and the preference is
            // enabled
            if (!doSubWordNavigation) {
                super.run();
                return;
            }

            final IDocument document = viewer.getDocument();
            if (document != null) {
                fIterator.setText((CharacterIterator) new DocumentCharacterIterator(
                        document));
                int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget()
                        .getCaretOffset());
                if (position == -1)
                    return;
    
                int previous = findPreviousPosition(position);
                if (previous != BreakIterator.DONE) {
                    setCaretPosition(previous);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            }
        }

        /**
         * Finds the previous position before the given position.
         * 
         * @param position
         *          the current position
         * @return the previous position
         */
        protected int findPreviousPosition(int position) {
            int widget = -1;
            while (position != BreakIterator.DONE && widget == -1) {
                position = fIterator.preceding(position);
                if (position != BreakIterator.DONE)
                    widget = modelOffset2WidgetOffset(viewer, position);
            }
            return position;
        }

        /**
         * Sets the caret position to the sub-word boundary given with
         * <code>position</code>.
         * 
         * @param position
         *          Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }

    /**
     * Text navigation action to navigate to the previous sub-word.
     * 
     * @author Edward Lam
     */
    public static class NavigatePreviousSubWordAction extends PreviousSubWordAction {

        /**
         * Creates a new navigate previous sub-word action.
         * @param viewer source view
         * @param doSubWordNavigation true if should naviagate into partial words 
         */
        public NavigatePreviousSubWordAction(ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(ST.WORD_PREVIOUS, viewer, doSubWordNavigation);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.PreviousSubWordAction#setCaretPosition(int)
         */
        protected void setCaretPosition(final int position) {
            getTextWidget()
            .setCaretOffset(modelOffset2WidgetOffset(viewer, position));
        }
    }

    /**
     * Text operation action to delete the previous sub-word.
     * 
     * @author Edward Lam
     */
    public static class DeletePreviousSubWordAction extends PreviousSubWordAction
    implements IUpdate {

        /**
         * Creates a new delete previous sub-word action.
         * @param viewer source view
         * @param doSubWordNavigation true if should naviagate into partial words 
         */
        public DeletePreviousSubWordAction(ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(ST.DELETE_WORD_PREVIOUS, viewer, doSubWordNavigation);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.PreviousSubWordAction#setCaretPosition(int)
         */
        protected void setCaretPosition(int position) {
//          if (!validateEditorInputState())
//          return;

            final int length;
            Point selection = viewer.getSelectedRange();
            if (selection.y != 0) {
                position = selection.x;
                length = selection.y;
            } else {
                length = widgetOffset2ModelOffset(viewer, viewer.getTextWidget()
                        .getCaretOffset())
                        - position;
            }

            try {
                viewer.getDocument().replace(position, length, ""); //$NON-NLS-1$
            } catch (BadLocationException exception) {
                // Should not happen
            }
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.PreviousSubWordAction#findPreviousPosition(int)
         */
        protected int findPreviousPosition(int position) {
            return fIterator.preceding(position);
        }

        /*
         * @see org.eclipse.ui.texteditor.IUpdate#update()
         */
        public void update() {
//          setEnabled(isEditorInputModifiable());
            setEnabled(true);
        }
    }

    /**
     * Text operation action to select the previous sub-word.
     * 
     * @author Edward Lam
     */
    public static class SelectPreviousSubWordAction extends PreviousSubWordAction {

        /**
         * Creates a new select previous sub-word action.
         * @param viewer source view
         * @param doSubWordNavigation true if should naviagate into partial words 
         */
        public SelectPreviousSubWordAction(ISourceViewer viewer,
                boolean doSubWordNavigation) {
            super(ST.SELECT_WORD_PREVIOUS, viewer, doSubWordNavigation);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.PreviousSubWordAction#setCaretPosition(int)
         */
        protected void setCaretPosition(final int position) {

            final StyledText text = viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection = text.getSelection();
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(viewer, position);

                if (caret == selection.x)
                    text.setSelectionRange(selection.y, offset - selection.y);
                else
                    text.setSelectionRange(selection.x, offset - selection.x);
            }
        }
    }
    
    /****************************************************************/
    /*******The following methods just delegate to the **************/
    /*******actoin's super class*************************************/
    /****************************************************************/

    /**
     * the action on the Java editor to use if the selection is not in a contained
     * editor
     */
    private final IAction javaEditorAction;

    /** used to determine the currently focused ContainedEditor */
    private final ControlManager controlManager;

    public SwitchableAction(IAction javaEditorAction,
            ControlManager controlManager) {
        this.javaEditorAction = javaEditorAction;
        this.controlManager = controlManager;
    }

    public void addPropertyChangeListener(IPropertyChangeListener listener) {
        getRelevantAction().addPropertyChangeListener(listener);
    }

    public int getAccelerator() {
        return getRelevantAction().getAccelerator();
    }

    public String getActionDefinitionId() {
        return getRelevantAction().getActionDefinitionId();
    }

    public String getDescription() {
        return getRelevantAction().getDescription();
    }

    public ImageDescriptor getDisabledImageDescriptor() {
        return getRelevantAction().getDisabledImageDescriptor();
    }

    public HelpListener getHelpListener() {
        return getRelevantAction().getHelpListener();
    }

    public ImageDescriptor getHoverImageDescriptor() {
        return getRelevantAction().getHoverImageDescriptor();
    }

    public String getId() {
        return getRelevantAction().getId();
    }

    public ImageDescriptor getImageDescriptor() {
        return getRelevantAction().getImageDescriptor();
    }

    public IMenuCreator getMenuCreator() {
        return getRelevantAction().getMenuCreator();
    }

    public int getStyle() {
        return getRelevantAction().getStyle();
    }

    public String getText() {
        return getRelevantAction().getText();
    }

    public String getToolTipText() {
        return getRelevantAction().getToolTipText();
    }

    public boolean isChecked() {
        return getRelevantAction().isChecked();
    }

    public boolean isEnabled() {
        return getRelevantAction().isEnabled();
    }

    public boolean isHandled() {
        return getRelevantAction().isHandled();
    }

    public void removePropertyChangeListener(IPropertyChangeListener listener) {
        getRelevantAction().removePropertyChangeListener(listener);
    }

    public void run() {
        getRelevantAction().run();
    }

    public void runWithEvent(Event event) {
        getRelevantAction().runWithEvent(event);
    }

    public void setAccelerator(int keycode) {
        getRelevantAction().setAccelerator(keycode);
    }

    public void setActionDefinitionId(String id) {
        getRelevantAction().setActionDefinitionId(id);
    }

    public void setChecked(boolean checked) {
        getRelevantAction().setChecked(checked);
    }

    public void setDescription(String text) {
        getRelevantAction().setDescription(text);
    }

    public void setDisabledImageDescriptor(ImageDescriptor newImage) {
        getRelevantAction().setDisabledImageDescriptor(newImage);
    }

    public void setEnabled(boolean enabled) {
        getRelevantAction().setEnabled(enabled);
    }

    public void setHelpListener(HelpListener listener) {
        getRelevantAction().setHelpListener(listener);
    }

    public void setHoverImageDescriptor(ImageDescriptor newImage) {
        getRelevantAction().setHoverImageDescriptor(newImage);
    }

    public void setId(String id) {
        getRelevantAction().setId(id);
    }

    public void setImageDescriptor(ImageDescriptor newImage) {
        getRelevantAction().setImageDescriptor(newImage);
    }

    public void setMenuCreator(IMenuCreator creator) {
        getRelevantAction().setMenuCreator(creator);
    }

    public void setText(String text) {
        getRelevantAction().setText(text);
    }

    public void setToolTipText(String text) {
        getRelevantAction().setToolTipText(text);
    }

    public void update() {
        IAction action = getRelevantAction();
        if (action instanceof IUpdate) {
            if (action instanceof TextOperationAction) {
                // this sets up the TextTarget
                ((TextOperationAction) action).setEditor(
                        controlManager.getContainingEditor());
            } else if (action instanceof TextEditorAction) {
                ((TextEditorAction) action).setEditor(
                        controlManager.getContainingEditor());
            }
            ((IUpdate) action).update();
        }
    }

    public IAction getJavaEditorAction() {
        return javaEditorAction;
    }

    /**
     * chooses either the JavaAction or one of the actions on a contained editor
     * depending on what is currently selected
     * 
     * @return either the Java editor action, or the contained editor action
     */
    private IAction getRelevantAction() {
        ContainedEditorManager editor = controlManager.getCurrentlyActiveEditor();
        if (editor == null) {
            return javaEditorAction;
        } else {
            IAction action = editor.getAction(javaEditorAction);
            return action != null ? action : javaEditorAction;
        }
    }

    /**
     * Utility method used by many of the actions
     * Maps from the projected (widget) offset to the model offset.  
     * Handles code folding.
     * 
     * @param viewer
     * @param widgetOffset
     * @return the model offset
     */
    protected final static int widgetOffset2ModelOffset(ISourceViewer viewer,
            int widgetOffset) {
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            return extension.widgetOffset2ModelOffset(widgetOffset);
        }
        return widgetOffset + viewer.getVisibleRegion().getOffset();
    }

    /**
     * Returns the offset of the given source viewer's text widget that
     * corresponds to the given model offset or <code>-1</code> if there is no
     * such offset.
     * 
     * @param viewer
     *          the source viewer
     * @param modelOffset
     *          the model offset
     * @return the corresponding offset in the source viewer's text widget or
     *         <code>-1</code>
     * @since 3.0
     */
    protected final static int modelOffset2WidgetOffset(ISourceViewer viewer,
            int modelOffset) {
        if (viewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
            return extension.modelOffset2WidgetOffset(modelOffset);
        }
        return modelOffset - viewer.getVisibleRegion().getOffset();
    }
}
