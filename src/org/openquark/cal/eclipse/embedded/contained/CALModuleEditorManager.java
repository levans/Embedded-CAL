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
 * CALExpressionEditorManager.java
 * Created: Jul 10, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.contained;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class CALModuleEditorManager extends ContainedEditorManager {

    public final static String EDITOR_KIND = "MODULE_EDITOR";
    
//    private Text moduleNameText;
    
    private CALModuleEditorProperties props;
    
    private static final Color green = 
        new Color(Display.getDefault(), new RGB(0, 255, 0));
    
    public CALModuleEditorManager(CALModuleEditorProperties props) {
        super(props);
        this.props = props;
    }
    @Override
    protected void createSubControls() {
        super.createSubControls();
        
        control.setBackground(green);

//        moduleNameText = new Text(control, SWT.SINGLE);
//        String moduleName = 
//            props.getModuleName();
//        moduleNameText.setText(moduleName == null ? "null" : moduleName);
//
//        FormData comboData = new FormData();
//        comboData.bottom = UNEXPANDED_BOTTOM;
//        comboData.left = TOP_AND_LEFT;
//        comboData.height = 14;
//        comboData.width = 150;
//        moduleNameText.setLayoutData(comboData);
//        moduleNameText.setToolTipText("Set the CAL module associated with this compilation unit");
//        if (!props.isShowingAll()) {
//            moduleNameText.setVisible(false);
//        }

    }
    
    @Override
    protected void initListeners() {
//        moduleNameText.addModifyListener(new ModifyListener() {
//            public void modifyText(ModifyEvent e) {
//                // notify listeners of the change
//                fireEditorChanged();
//            }
//        });
//        
        super.initListeners();
    }
    
    @Override
    protected Menu createContextMenu() {
        Menu m = super.createContextMenu();
        final MenuItem showAll = new MenuItem(m, SWT.CHECK);
        if (props.isShowingAll()) {
            showAll.setSelection(true);
        } else {
            showAll.setSelection(false);
        }
        showAll.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                boolean newSelection = showAll.getSelection();
                props.setShowAll(newSelection);
//                moduleNameText.setVisible(newSelection);
                if (newSelection) {
                    ((FormData) styledText.getLayoutData()).bottom = EXPANDED_BOTTOM;
                } else {
                    ((FormData) styledText.getLayoutData()).bottom = UNEXPANDED_BOTTOM;
                }
                autoresize();
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
        showAll.setText("Show expanded view");
//        moduleNameText.setMenu(m);
        return m;
    }

    @Override
    protected Point getPreferredSize() {
        Point p = super.getPreferredSize();
//        if (moduleNameText.isVisible()) {
//            Point dropdownSize = moduleNameText.getSize();
//            p.y += dropdownSize.y + 5;
//            p.x = Math.max(dropdownSize.x, p.x);
//        }
        return p;
    }
    
    public String getModuleName() {
        return props.getModuleName();
    }
    @Override
    public String editorKind() {
        return EDITOR_KIND;
    }
}
