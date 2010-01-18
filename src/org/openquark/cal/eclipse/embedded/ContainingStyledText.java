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
 * ContainingStyledText.java
 * Created: Jun 29, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Warning! Not for the faint of heart!
 * 
 * Not supposed to extend styled text, but need to in order to control scrolling
 * behavior.
 * 
 * This class removes the flicker of embedded controls when there is a newline
 * entered in the containing editor.
 * 
 * 
 * @author aeisenberg
 * 
 */
public class ContainingStyledText extends StyledText {

    public ContainingStyledText(Composite parent, int style) {
        super(parent, style);
    }

    @Override
    public void scroll(int destX, int destY, int x, int y, int width,
            int height, boolean all) {
        super.scroll(destX, destY, x, y, width, height, false);

        boolean normalScroll = false; // normal scroll or line feed
        try {
            throw new Exception();
        } catch (Exception e) {
            e.fillInStackTrace();
            normalScroll = !e.getStackTrace()[1].getMethodName().equals(
                    "scrollText");
        }

        if (all) {
            int caretPosition = this.getCaret().getLocation().y;
            int deltaX = destX - x, deltaY = destY - y;
            Control[] children = getChildren();
            for (int i = 0; i < children.length; i++) {
                Control child = children[i];
                Rectangle rect = child.getBounds();

                if (normalScroll || rect.y >= caretPosition) {
                    child.setLocation(rect.x + deltaX, rect.y + deltaY);
                }
            }
        }
    }

    /**
     * the super definition of this method always tries to give focus to the first child.
     * this is not the behavior we want.
     * 
     * focus should go to this StyledText and only afterwards decide if it should be propagated to a child
     */
    @Override
    public boolean setFocus() {
        checkWidget ();
        if ((getStyle() & SWT.NO_FOCUS) != 0) return false;
        
        // check to see if a child already has focus
        if (searchForFocusChild(this)) {
            return true;
        }
        
        return forceFocus ();
    }
    
    private boolean searchForFocusChild(Composite control) {
        // check to see if a child already has focus
        for (Control child : control.getChildren()) {
            if (child.isFocusControl()) {
                return true;
            }
            if (child instanceof Composite) {
                Composite childComposite = (Composite) child;
                if (searchForFocusChild(childComposite)) {
                    return true;
                }
            }
        }
        return false;
    }
}
