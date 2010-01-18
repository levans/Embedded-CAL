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
 * ContainingEditorReconciler.java
 * Created: Jun 19, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.containing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.custom.StyleRange;

/**
 * XXX DELETEME!
 * 
 * Well, keep this around for now, since I will look into it to help with 
 * flicker reduction.
 * 
 * @author aeisenberg
 * Not using this class, but it may be helpful use parts of it to reduce flicker
 */
@SuppressWarnings("restriction")
public class ContainingEditorReconciler extends 
org.eclipse.jdt.internal.ui.text.JavaPresentationReconciler {



    @Override
    protected TextPresentation createPresentation(IRegion damage,
            IDocument document) {
        TextPresentation presentation = super.createPresentation(damage, document);
        if (presentation != null) {
            Iterator<StyleRange> rangeIter = presentation.getAllStyleRangeIterator();
            List<StyleRange> applicableRanges = new LinkedList<StyleRange>();

            Position[] untouchableRegion = getPositions(document);

            while (rangeIter.hasNext()) {
                StyleRange range = rangeIter.next();
                // ranges with glyph metrics are stuff that we added.
                // these ranges should not be repaired, so don't put them back
                // into the new ranges list.


                if (containsRegion(untouchableRegion, range.start, range.length)) {
                    applicableRanges.add(range);
                }
            }

            TextPresentation newPresentation = new TextPresentation(damage, applicableRanges.size()+1);
            for (final StyleRange range : applicableRanges) {
                newPresentation.addStyleRange(range);
            }
            newPresentation.setDefaultStyleRange(new StyleRange(damage.getOffset(), damage.getLength(), null, null));
            return newPresentation;
        } else {
            return null;
        }
    }


    Position[] getPositions(IDocument doc) {
        String[] categories =  doc.getPositionCategories();
        for (int cnt = 0; cnt < categories.length; cnt++) {
            if (categories[cnt].startsWith("org.openquark")) {
                try {
                    return doc.getPositions(categories[cnt]);
                } catch (BadPositionCategoryException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    boolean containsRegion(Position[] positions, int start, int length) {
        for (int cnt = 0; cnt < positions.length; cnt++) {
            Position position = positions[cnt];
            if (position.overlapsWith(start, length)) {
                return true;
            }
        }
        return false;
    }

}
