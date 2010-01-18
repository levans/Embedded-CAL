/*******************************************************************************
 * Copyright (c) 2006 Business Objects Software Limited and others.
 * All rights reserved. 
 * This file is made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Business Objects Software Limited - initial implementation.
 *******************************************************************************/

/*
 * ShowTooltipDescriptionAction.java
 * Created: January 11, 2007
 * By: Greg McClement
 */
package org.openquark.cal.eclipse.embedded.handlers;

import java.util.ResourceBundle;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.openquark.cal.eclipse.ui.util.CoreUtility;


/**
 * Shows the CAL doc for the symbols near the cursor if any. This work for both the F2 press from the
 * top level and F2 pressed with a hover window is open.
 * 
 * @author Greg McClement
 */
public class ShowTooltipDescriptionAction extends TextEditorAction {
    
    private TextOperationAction textOperationAction;
    
    public ShowTooltipDescriptionAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
        super(bundle, prefix, editor);
        textOperationAction = new TextOperationAction(bundle, prefix, editor, ISourceViewer.INFORMATION, true);
    }

    public void update() {
        super.update();
        setEnabled(true);
        
    }

    /*
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        // update has been called by the framework
        if (!isEnabled()) {
            return;
        }
        textOperationAction.setEditor(null);
        textOperationAction.setEditor(getTextEditor());
        textOperationAction.update();
        
        CoreUtility.initializeCALBuilder(null, 100, 100);
        textOperationAction.run();
    }
}
