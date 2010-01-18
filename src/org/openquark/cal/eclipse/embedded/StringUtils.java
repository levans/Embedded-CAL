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
 * StringUtils.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded;

/**
 * This utility class performs string operations
 * @author Andrew Eisenberg
 *
 */
public class StringUtils {
    // uninstantiable
    private StringUtils() { }

    public static String escape(String str) {
        StringBuffer sb = new StringBuffer(str.length());

        char[] array = str.toCharArray();
        for (int cnt = 0; cnt < array.length; cnt++) {
            switch (array[cnt]) {
            case '\n':
                sb.append("\\n");
                break;

            case '"':
                sb.append("\\\"");
                break;

            case '\r':
                sb.append("\\r");
                break;

            case '\\':
                sb.append("\\\\");
                break;

            case '\t':
                // convert to 2 spaced-tabs
                sb.append("  ");
                break;
            default:
                sb.append(array[cnt]);
            break;
            }
        }

        return sb.toString();
    }

    public static String unescape(String str) {
        StringBuffer sb = new StringBuffer(str.length());
        char[] array = str.toCharArray();
        for (int cnt = 0; cnt < array.length; cnt++) {
            if (array[cnt] != '\\') {
                sb.append(array[cnt]);
            } else {
                switch (array[++cnt]) {
                case 'n':
                    sb.append('\n');
                    break;

                case '\\':
                    sb.append('\\');
                    break;

                case 'r':
                    sb.append('\r');
                    break;

                default:
                    break;
                }
            }
        }

        return sb.toString();
    }
}
