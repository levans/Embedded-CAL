/*
 * Copyright (c) 2007 BUSINESS OBJECTS SOFTWARE LIMITED
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *  
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *  
 *     * Neither the name of Business Objects nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */


/*
 * IEmbeddedCalContents.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.exported;

import java.io.File;

/**
 * Some constants that are relevant for running the embedded CAL
 * 
 * clients are not expected to use these constants directly.
 * 
 * @author aeisenberg
 *
 */
public interface IEmbeddedCalConstants {
    public static final String CAL_ROOT = "CAL";
    public static final String CAL_PACKAGE = "Cal";
    public static final String CAL_EXTENSION = "cal";
    public static final String EMBEDDED_CAL_MODULE_NAME = "EmbeddedCal";
    public static final String EMBEDDED_CAL_MODULE = CAL_PACKAGE + "." + EMBEDDED_CAL_MODULE_NAME;
    public static final String WORKSPACE_NAME = "cal.samples.cws"; //"cal.platform.cws";
    public static final String WORKSPACE_DECLARATIONS = "Workspace Declarations";
    public static final String MODULE_PATH = 
        File.separator + CAL_ROOT + File.separator + 
        CAL_PACKAGE + File.separator +  
        EMBEDDED_CAL_MODULE_NAME + "." + 
        CAL_EXTENSION;
    public static final byte[] WORKSPACE_CONTENTS = 
        ("import StandardVault cal.platform.cws\n" +
        "StandardVault Cal.DefaultQuarkModule").getBytes();

    // note---break it up so that the strings do not display as
    // an embedded editor
    public static final String EMBEDDED_REGION_START = "/* " + "<-- */";
    public static final String EMBEDDED_REGION_END = "/* -" + "-> */";
    
    public static final String EXPRESSION_EDITOR_NAME_START = "RunQuark.";
    public static final String MODULE_EDITOR_NAME = "RunQuark.declareModule";

}
