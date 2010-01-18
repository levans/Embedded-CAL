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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class CALExpressionEditorManager extends ContainedEditorManager {

    public final static String EDITOR_KIND = "EXPRESSION_EDITOR";
    
    private Combo outputPolicyDropDown;
    
    private Button throwsExceptionCheck;
    
    private CALExpressionEditorProperties props;
    
    public CALExpressionEditorManager(CALExpressionEditorProperties props) {
        super(props);
        this.props = props;
    }
    @Override
    protected void createSubControls() {
        super.createSubControls();
        
        // throwsException check box
        throwsExceptionCheck = new Button(control, SWT.CHECK);
        throwsExceptionCheck.setSelection(props.isThrowsException());
        throwsExceptionCheck.setText("Explicit exception handling");
        FormData throwsData = new FormData();
        throwsData.bottom = new FormAttachment(100, -22);;
        throwsData.left = TOP_AND_LEFT;
        throwsData.height = 14;
        throwsData.width = 175;
        throwsExceptionCheck.setLayoutData(throwsData);

        
        // output policy drop down
        outputPolicyDropDown = new Combo(control, SWT.DROP_DOWN);
        outputPolicyDropDown.add("<<Infer output policy>>");
        outputPolicyDropDown.add("DEFAULT_OUTPUT_POLICY");
        outputPolicyDropDown.add("CAL_VALUE_OUTPUT_POLICY");
        outputPolicyDropDown.add("BYTE_OUTPUT_POLICY");
        outputPolicyDropDown.add("INT_OUTPUT_POLICY");
        outputPolicyDropDown.add("ITERATOR_OUTPUT_POLICY");
        outputPolicyDropDown.add("STRICT_CAL_VALUE_OUTPUT_POLICY");
        outputPolicyDropDown.add("BOOLEAN_OUTPUT_POLICY");
        outputPolicyDropDown.add("CHAR_OUTPUT_POLICY");
        outputPolicyDropDown.add("DOUBLE_OUTPUT_POLICY");
        outputPolicyDropDown.add("STRING_OUTPUT_POLICY");

        String outputPolicyReference = props.getOuputPolicyReference();
        if (outputPolicyReference == null) {
            outputPolicyDropDown.select(0);
        } else if (outputPolicyReference.endsWith("DEFAULT_OUTPUT_POLICY")) {
            outputPolicyDropDown.select(1);
        } else if (outputPolicyReference.endsWith("CAL_VALUE_OUTPUT_POLICY")) {
            outputPolicyDropDown.select(2);
        }
        outputPolicyDropDown.setText(outputPolicyReference==null ? "" : outputPolicyReference);
        outputPolicyDropDown.setToolTipText("Set the output policy");

        FormData comboData = new FormData();
        comboData.bottom = UNEXPANDED_BOTTOM;
        comboData.left = TOP_AND_LEFT;
        comboData.height = 14;
        comboData.width = 150;
        outputPolicyDropDown.setLayoutData(comboData);
        
        
        if (!props.isShowingAll()) {
            outputPolicyDropDown.setVisible(false);
            throwsExceptionCheck.setVisible(false);
        }

    }
    
    @Override
    protected void initListeners() {
        // add listener to the combo box that updates the properties on change
        outputPolicyDropDown.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                if (outputPolicyDropDown.getSelectionIndex() == 0) {
                    props.setOuputPolicyReference(null);
                } else {
                    props.setOuputPolicyReference(outputPolicyDropDown.getText());
                }
                // notify listeners of the change
                fireEditorChanged();
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
        outputPolicyDropDown.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                props.setOuputPolicyReference(outputPolicyDropDown.getText());
                // notify listeners of the change
                fireEditorChanged();
            }
        });
        
        
        throwsExceptionCheck.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                props.setThrowsException(throwsExceptionCheck.getSelection());
                props.setDirty(true);
                fireEditorChanged();
            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
        
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
                outputPolicyDropDown.setVisible(newSelection);
                throwsExceptionCheck.setVisible(newSelection);
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
        
        final MenuItem useCalValue = new MenuItem(m, SWT.CHECK);
        if (props.getOuputPolicyReference() != null && 
                props.getOuputPolicyReference().endsWith("CAL_VALUE_OUTPUT_POLICY")) {
            useCalValue.setSelection(true);
        } else {
            useCalValue.setSelection(false);
        }
        useCalValue.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                boolean newSelection = ! 
                    (props.getOuputPolicyReference() != null && 
                     props.getOuputPolicyReference().endsWith("CAL_VALUE_OUTPUT_POLICY"));
                if (newSelection) {
                    props.setOuputPolicyReference("CAL_VALUE_OUTPUT_POLICY");
                    outputPolicyDropDown.setText("CAL_VALUE_OUTPUT_POLICY");
                } else {
                    props.setOuputPolicyReference(null);
                    outputPolicyDropDown.setText("");
                }
                useCalValue.setSelection(newSelection);
                props.setDirty(true);
                fireEditorChanged();

            }
            public void widgetDefaultSelected(SelectionEvent e) { }
        });
        useCalValue.setText("Use CAL_VALUE_OUTPUT_POLICY");
        
        outputPolicyDropDown.setMenu(m);
        throwsExceptionCheck.setMenu(m);
        return m;
    }

    @Override
    protected Point getPreferredSize() {
        Point p = super.getPreferredSize();
        if (outputPolicyDropDown.isVisible()) {
            Point dropdownSize = outputPolicyDropDown.getSize();
            p.y += dropdownSize.y + 5;
            p.x = Math.max(dropdownSize.x, p.x);
        }
        if (throwsExceptionCheck.isVisible()) {
            Point throwsSize = throwsExceptionCheck.getSize();
            p.y += throwsSize.y + 5;
            p.x = Math.max(throwsSize.x, p.x);
        }
        
        
        return p;
    }
    @Override
    public String editorKind() {
        return EDITOR_KIND;
    }
}
