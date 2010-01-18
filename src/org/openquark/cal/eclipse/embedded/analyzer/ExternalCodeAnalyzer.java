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
 * ExternalCodeAnalyzer.java
 * Created: Jul 10, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.analyzer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.openquark.cal.eclipse.embedded.exported.IEmbeddedCalConstants;

/**
 * Code analyzer that analyzes a module defined externally from 
 * the compilation unit that uses it
 * 
 * @author aeisenberg
 */
public class ExternalCodeAnalyzer extends CodeAnalyzer {

    /**
     * reference to the quark module
     */
    private final IFile quarkModule;


    public ExternalCodeAnalyzer(ICompilationUnit unit) {
        quarkModule = findModuleFile(unit);
        installListener();
        setStale();
    }
    
    @SuppressWarnings("restriction")
    public String getModuleText() throws JavaModelException {
        String moduleText;
        if (quarkModule != null) {
            moduleText = new String(
                    org.eclipse.jdt.internal.core.util.Util.getResourceContentsAsCharArray(
                            quarkModule));
            if (moduleText == null) moduleText = "module EmbeddedCAL; import Cal.Core.Prelude;";  
        } else {
            moduleText = "module EmbeddedCAL; import Cal.Core.Prelude;";
        }
        
        return moduleText;
    }

    /**
     * find the file that is the DefaultQuarkModule and contains all of the import 
     * statements for this embedded editor
     * <p>
     * We have to be careful here.  There are a lot of places where things can go wrong.
     * @param unit the compilation unit of the contained editor
     * @return
     */
    private IFile findModuleFile(ICompilationUnit unit) {
        IFile file = null;
        try {
            IProject project = unit.getJavaProject().getProject();
            IPackageFragmentRoot[] roots = unit.getJavaProject().getPackageFragmentRoots();
            for (int cnt = 0; cnt < roots.length; cnt++) {
                IResource rootResource = roots[cnt].getResource();
                if (rootResource != null) {
                    IResource candidate = project.findMember(rootResource.getProjectRelativePath().toOSString() + 
                            IEmbeddedCalConstants.MODULE_PATH);
                    if (candidate != null && candidate.exists() && candidate instanceof IFile) {
                        file = (IFile) candidate;
                        break;
                    }
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }

        return file;
    }

    protected void installListener() {
        IResourceChangeListener listener = new IResourceChangeListener() {
            
            // ADE this isn't quite right...we should be searching through the
            // resource delta and looking for the file that we care about.
            // as it is, the stale flag is getting set after every single
            // resource change event.
            // 
            // probably doesn't matter much since this class is not really being used any more
            public void resourceChanged(IResourceChangeEvent event) {
                setStale();
            }
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(listener,
                IResourceChangeEvent.POST_CHANGE);

    }

    

}
