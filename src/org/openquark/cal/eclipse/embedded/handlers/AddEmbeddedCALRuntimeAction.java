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
 * AddEmbeddedCALRuntimeAction.java
 * Created: Jul 19, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.handlers;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.openquark.cal.eclipse.embedded.EmbeddedClasspathVariableInitializer;

public class AddEmbeddedCALRuntimeAction implements IObjectActionDelegate {

    /** The currently selected element. */
    private ISelection selection;

    public AddEmbeddedCALRuntimeAction() { }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) { }

    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            for (Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }
                if (project != null) {
                    // Add the Embedded CAL runtime to the classpath
                    EmbeddedClasspathVariableInitializer.
                            addEmbeddedrtToBuildPath(project);
                    
                    // TODO ADE uncomment this when car jars are being shipped with the CAL Eclipse plugin
                    /*
                    EmbeddedClasspathVariableInitializer.
                            addQuarkLibraryToBuildPath(project);
                            */
                    
                    // This will add the Quark binaries project to the classpath
                    EmbeddedClasspathVariableInitializer.
                            addQuarkBinariesToClasspath(project);
                }
            }
        }        
    }

    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

}
