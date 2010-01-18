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
 * VariableHeightCaret.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Caret;


/**
 * 
 * @author Andrew Eisenberg
 * @created May 4, 2007
 *
 * adjusts the caret's position based on the line its on
 */
public class VariableHeightCaret extends Caret {

    private final StyledText text;

    public VariableHeightCaret(Canvas parent, int style, StyledText text) {
        super(parent, style);
        this.text = text;
    }

    @Override
    public void setLocation(int x, int y) {
        int line = text.getLineAtOffset(text.getOffsetAtLocation(new Point(x, y)));
        int lineHeight = text.getLineHeight(line);
        int newY = lineHeight/2*3 + y;

        super.setLocation(x, newY);
    }

}
