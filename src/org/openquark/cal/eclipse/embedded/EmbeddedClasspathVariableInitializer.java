/*******************************************************************************
 * Copyright (c) 2007 Business Objects Software Limited and others.
 * All rights reserved. 
 * This file is made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Business Objects Software Limited - initial API and implementation based on
 *     AJDT 1.5 code for  
 *         org\eclipse\ajdt\internal\core\AspectJRTContainerInitializer.java and 
 *         org\eclipse\ajdt\internal\core\AspectJRTInitializer.java
 *         
 *         Eclipse source is available at: http://www.eclipse.org/ajdt/downloads/
 *******************************************************************************/

/*
 * EmbeddedClasspathVariableInitializer.java
 * Created: Jul 19, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.openquark.cal.eclipse.core.CALModelManager;
import org.openquark.cal.module.Cal.Core.CAL_Prelude;
import org.osgi.framework.Bundle;

public class EmbeddedClasspathVariableInitializer extends
        ClasspathVariableInitializer {

    public static final String EMBEDDED_CAL_RT_LIB = "EMBEDDED_CAL_RT_LIB";
    
    // this will be used when the CAL Eclipse plugin includes the car jars
    public static final String QUARK_LIB = "org.eclipse.jdt.USER_LIBRARY/Quark";

    
    private static final String EMBEDDED_JAR_NAME = "embeddedCAL_RT.jar";
    private static String embeddedCALRTPath = null;

    
    
    
    public EmbeddedClasspathVariableInitializer() {    }

    /**
     * Initializes the Embedded CAL variable if necessary
     * Borrowed from: AspectJRTInitializer.initialize()
     */
    @Override
    public void initialize(String variable) {
        if (variable.equals(EMBEDDED_CAL_RT_LIB)) { //$NON-NLS-1$
            // define it to point to aspectjrt.jar in ajde project.
            String embeddedPath = getEmbeddedRTClasspath();            
            try {
                JavaCore.setClasspathVariable(EMBEDDED_CAL_RT_LIB, //$NON-NLS-1$
                        new Path(embeddedPath),null);
            } catch (JavaModelException e) {
            }
        }
    }
    
    /**
     * Get the embeddedCAL_RT.jar classpath entry. This is usually in
     * plugins/org.openquark.cal.eclipse.embedded_ <VERSION>/embeddedCAL_RT.jar
     * @return a string representing the class path
     * 
     * borrowed from: CoreUtils.getAspectjrtClasspath()
     */
    public static String getEmbeddedRTClasspath() {

        if (embeddedCALRTPath == null) {
            StringBuffer cpath = new StringBuffer();

            // This returns the bundle with the highest version or null if none
            // found
            // - for Eclipse 3.0 compatibility
            Bundle embeddedBundle = Platform
                    .getBundle(EmbeddedCALPlugin.PLUGIN_ID);

            String pluginLoc = null;
            // 3.0 using bundles instead of plugin descriptors
            if (embeddedBundle != null) {
                URL installLoc = embeddedBundle.getEntry("/"); //$NON-NLS-1$
                URL resolved = null;
                try {
                    resolved = FileLocator.resolve(installLoc);
                    pluginLoc = resolved.toExternalForm();
                } catch (IOException e) {
                }
            }
            if (pluginLoc != null) {
                if (pluginLoc.startsWith("file:")) { //$NON-NLS-1$
                    cpath.append(pluginLoc.substring("file:".length())); //$NON-NLS-1$
                    cpath.append(EMBEDDED_JAR_NAME); //$NON-NLS-1$
                }
            }

            // Verify that the file actually exists at the plugins location
            // derived above. If not then it might be because we are inside
            // a runtime workbench. Check under the workspace directory.
            if (new File(cpath.toString()).exists()) {
                // File does exist under the plugins directory
                embeddedCALRTPath = cpath.toString();
            } else {
                // File does *not* exist under plugins. Try under workspace...
                IPath rootPath = EmbeddedCALPlugin.getWorkspace().getRoot()
                        .getLocation();
                IPath installPath = rootPath.removeLastSegments(1);
                cpath = new StringBuffer().append(installPath.toOSString());
                cpath.append(File.separator);
                // TODO: what if the workspace isn't called workspace!!!
                cpath.append("workspace"); //$NON-NLS-1$
                cpath.append(File.separator);
                cpath.append(EmbeddedCALPlugin.PLUGIN_ID);
                cpath.append(File.separator);
                cpath.append(EMBEDDED_JAR_NAME); //$NON-NLS-1$

                // Only set the embeddedCALRTPath if the jar file exists here.
                if (new File(cpath.toString()).exists())
                    embeddedCALRTPath = cpath.toString();
            }
        }
        return embeddedCALRTPath;
    }
    
    
    /**
     * Attempt to update the project's build classpath with the EmbeddedCal runtime
     * library.
     * 
     * Borrowed from AspectJUIPlugin.addAjrtToBuildPath()
     * 
     * @param project
     */
    public static void addEmbeddedrtToBuildPath(IProject project) {
        IJavaProject javaProject = JavaCore.create(project);
        try {
            IClasspathEntry[] originalCP = javaProject.getRawClasspath();
            
            // check to see if classpath entry already exists
            for (final IClasspathEntry classpathEntry : originalCP) {
                if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                    if (classpathEntry.getPath().toString().equals(EMBEDDED_CAL_RT_LIB)) {
                        // found
                        return;
                    }
                }
            }
            
            IClasspathEntry embeddedrtVAR = JavaCore.newVariableEntry(
                    new Path(EMBEDDED_CAL_RT_LIB), null, null);
            
            // Update the raw classpath with the new embeddedrtCP entry.
            int originalCPLength = originalCP.length;
            IClasspathEntry[] newCP = new IClasspathEntry[originalCPLength + 1];
            System.arraycopy(originalCP, 0, newCP, 0, originalCPLength);
            newCP[originalCPLength] = embeddedrtVAR;
            javaProject.setRawClasspath(newCP, new NullProgressMonitor());
        } catch (JavaModelException e) {
        }
    }

    /**
     * Attempt to update the project's build classpath with all of the Quark Binaries
     * library.
     * 
     * Borrowed from AspectJUIPlugin.addAjrtToBuildPath()
     * 
     * TODO ADE A warning:  this method will only work if you already have the Quark user 
     * library set up.  Setting it up requires the proper installation of Quark 
     * (including car-jars).  Quark is not currently installed this way by default,
     * but it should be at some point.
     * 
     * I created this method to help with the creation of the videos.
     * 
     * In order for this method to work, the QUARK_LIB user library needs to be created.  It 
     * needs to contain the following references:
     * 
     *  (External jars)
     *  xmlParserAPIs.jar
     *  asm-all-3.0.jar
     *  commons-collections-3.1.jar
     *  xercesImpl.jar
     *  antlr.jar
     *  icu4j.jar
     *  log4j.jar
     *
     *  (plugins)
     *  org.openquark.util plugin
     *  org.openquark.cal.platform pluign
     *  com.businessobjects.lang.cal.libraries plugin
     *  
     *  (Car-jars)
     *  cal.libraries.test.car.jar
     *  cal.platform.car.jar
     *  cal.platform.test.car.jar
     *  cal.libraries.car.jar
     * 
     * 
     * @param project
     */
    public static void addQuarkLibraryToBuildPath(IProject project) {
        IJavaProject javaProject = JavaCore.create(project);
        try {
            IClasspathEntry[] originalCP = javaProject.getRawClasspath();
            
            // check to see if classpath entry already exists
            for (final IClasspathEntry classpathEntry : originalCP) {
                if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    if (classpathEntry.getPath().toString().equals(QUARK_LIB)) {
                        // found
                        return;
                    }
                }
            }
            
            IClasspathEntry quarkLIB = JavaCore.newContainerEntry(
                    new Path(QUARK_LIB));
            
            // Update the raw classpath with the new embeddedrtCP entry.
            int originalCPLength = originalCP.length;
            IClasspathEntry[] newCP = new IClasspathEntry[originalCPLength + 1];
            System.arraycopy(originalCP, 0, newCP, 0, originalCPLength);
            newCP[originalCPLength] = quarkLIB;
            javaProject.setRawClasspath(newCP, new NullProgressMonitor());
        } catch (JavaModelException e) {
        }
    }
    
    public static void addQuarkBinariesToClasspath(IProject project) {
        IJavaProject javaProject = JavaCore.create(project);
        try {
            IClasspathEntry[] originalCP = javaProject.getRawClasspath();
            
            // find the quark binaries project
            IJavaProject quarkBinaries = getQuarkBinaries();
            
            if (quarkBinaries == null) {
                // could not find quark binaries project
                return;
            }
            
            // check to see if classpath entry already exists
            for (final IClasspathEntry classpathEntry : originalCP) {
                if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                    if (classpathEntry.getPath().toString().equals(quarkBinaries.getPath())) {
                        // found
                        return;
                    }
                }
            }
            
            // add it
            IClasspathEntry quarkBinariesEntry = JavaCore.newProjectEntry(quarkBinaries.getPath());
            int originalCPLength = originalCP.length;
            IClasspathEntry[] newCP = new IClasspathEntry[originalCPLength + 1];
            System.arraycopy(originalCP, 0, newCP, 0, originalCPLength);
            newCP[originalCPLength] = quarkBinariesEntry;
            javaProject.setRawClasspath(newCP, new NullProgressMonitor());
            
        } catch (JavaModelException e) {
        }
    }
    
    private static IJavaProject getQuarkBinaries() {
        final CALModelManager cmm = CALModelManager.getCALModelManager();
        
        // TODO ADE need some other way of finding the libraries
        // this will not work for the classpath variable
        final IStorage thisStorage = cmm.getInputSourceFile(CAL_Prelude.MODULE_NAME);
        if (! (thisStorage instanceof IFile)) {
            // No quark binaries project
            return null;
        }
        
        IFile thisFile = (IFile) thisStorage;
        return JavaCore.create(thisFile.getProject());
    }
}
