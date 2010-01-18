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
 * ColorManager.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager {
    private Display d = Display.getCurrent();

    protected Map<RGB,Color> fColorTable = new HashMap<RGB,Color>(10);

    public void dispose() {
        Iterator<Color> e = fColorTable.values().iterator();
        while (e.hasNext())
            e.next().dispose();
    }
    public Color getColor(RGB rgb) {
        Color color = fColorTable.get(rgb);
        if (color == null) {
            color = new Color(d, rgb);
            fColorTable.put(rgb, color);
        }
        return color;
    }
}
