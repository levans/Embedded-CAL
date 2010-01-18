/*******************************************************************************
 * Copyright (c) 2007 Business Objects Software Limited and others.
 * All rights reserved. 
 * This file is made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Business Objects Software Limited - initial API and implementation based on Eclipse 3.1.2 code for
 *                             /org.eclipse.jdt.ui/org/eclipse/jdt/internal/ui/text/java/ContentAssistProcessor.java
 *                           Eclipse source is available at: http://www.eclipse.org/downloads/
 *******************************************************************************/
package org.openquark.cal.eclipse.embedded.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;
import org.openquark.cal.eclipse.core.formatter.DefaultCodeFormatterConstants;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer;
import org.openquark.cal.eclipse.embedded.analyzer.CodeAnalyzer.AnalysisResults;
import org.openquark.cal.eclipse.embedded.contained.ContainedEditorManager;
import org.openquark.cal.eclipse.embedded.containing.ContainingEditor;
import org.openquark.cal.eclipse.ui.CALEclipseUIPlugin;
import org.openquark.cal.eclipse.ui.templates.CALTemplateContextType;

/**
 * The class that implements auto-complete in the CAL Editor.
 * 
 * It proposes both source code and template completions 
 * 
 * @author Andrew Eisenberg
 */
public class CompletionProcessor extends TemplateCompletionProcessor implements
        IContentAssistProcessor {

    
    


    /**
     * the image that is next to all template proposals 
     */
    private final static Image templateImage = CALEclipseUIPlugin.getImageDescriptor("icons/template.gif").createImage();
    private final static Image image_nav_namespace = CALEclipseUIPlugin.getImageDescriptor("/icons/nav_namespace.png").createImage();
    @SuppressWarnings("restriction")
    private final static Image javaFieldImage = 
        org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider.getFieldImageDescriptor(false, 0).createImage();

    final ContainedEditorManager editorManager;
    final ContainingEditor containingEditor;

    private final IPreferenceStore preferenceStore;
    
    /**
     * Constructor for a CompletionProcessor. Call getCompletionProcessor()
     * to obtain instances.
     * @param editorManager 
     * @param containingEditor 
     * @param preferenceStore
     */
    public CompletionProcessor(ContainedEditorManager editorManager, 
            ContainingEditor containingEditor, IPreferenceStore preferenceStore) {
        this.editorManager = editorManager;
        this.containingEditor = containingEditor;
        this.preferenceStore = preferenceStore;
    }

    

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

        try {
            MatchInformation matchInformation;
                matchInformation = new MatchInformation(viewer.getDocument(), offset);
            String matchThis = matchInformation.prefix;
            if (matchInformation.scopingComponents != null){
                if (matchInformation.scopingComponents.length == 1 && matchInformation.partialHierarchicalName){
                    matchThis = matchInformation.scopingComponents[0];
                }
            }
            
            // get all imported entities
            List<ICompletionProposal> proposals = 
                computeApplicableImportedProposals(matchThis, viewer, offset);
            
            // add the template proposals to the mix
            List<ICompletionProposal> templateProposals = 
                computeApplicableTemplates(matchThis, viewer, offset);
            proposals.addAll(templateProposals);
            
            // add java proposals
            List<ICompletionProposal> javaProposals = computeApplicableJavaProposals(matchThis, offset);
            proposals.addAll(javaProposals);
            
            // sort
            Collections.sort(proposals, new Comparator<ICompletionProposal> () {
                public int compare(ICompletionProposal prop1, ICompletionProposal prop2) {
                    return prop1.getDisplayString().compareToIgnoreCase(prop2.getDisplayString());
                }
            });

            
            return proposals.toArray(new ICompletionProposal[proposals.size()]);
        } catch (BadLocationException e) {
            return new ICompletionProposal[0];
        }
        
    }

    private List<ICompletionProposal> computeApplicableImportedProposals(
            String matchThis, ITextViewer viewer, int offset) {
        
        CodeAnalyzer analyzer = containingEditor.getAnalyzer();
        AnalysisResults results = analyzer.findAllIdentifiers(viewer.getDocument().get());
        List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
        if (results.importedIds != null) {
            for (final String result : results.importedIds.keySet()) {
                if (matchesPrefix(result, matchThis)) {
                    String insert = result.substring(matchThis.length());
                    proposals.add(new CompletionProposal(insert, offset, 
                            0, insert.length(), image_nav_namespace, result, null, null));
                }
            }
        }
        if (results.locallyBoundIds != null) {
            for (final String result : results.locallyBoundIds.keySet()) {
                if (matchesPrefix(result, matchThis)) {
                    String insert = result.substring(matchThis.length());
                    proposals.add(new CompletionProposal(insert, offset, 
                            0, insert.length(), image_nav_namespace, result, null, null));
                }
            }
        }
        return proposals;
    }

    @SuppressWarnings("restriction")
    private List<ICompletionProposal> computeApplicableJavaProposals(
            String matchThis, int offset) {
        // ICompilationUnitDocumentProvider is restricted
        ICompilationUnit unit = ((org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider) containingEditor.getDocumentProvider()).
                getWorkingCopy(containingEditor.getEditorInput());
        
        int containingOffset = containingEditor.getContainedEditorPosition(editorManager).offset;
        
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        ASTNode node = parser.createAST(null);
        
        if (node instanceof CompilationUnit) {
            // ScopeAnalyzer is restricted
            org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer scopeAnalyzer = 
                new org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer((CompilationUnit) node);
            IBinding[] bindings = scopeAnalyzer.getDeclarationsInScope(containingOffset, 
                    org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer.VARIABLES);
            List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();
            for (final IBinding binding : bindings) {
                if (binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding varBinding = (IVariableBinding) binding;
                    String name = varBinding.getName();
                    if (matchesPrefix(name, matchThis)) {
                        String insert = name.substring(matchThis.length());
                        CompletionProposal proposal = new CompletionProposal(insert, offset, 
                                0, insert.length(), javaFieldImage, name, null, null);
                        proposals.add(proposal);
                    }
                }
            }
            return proposals;
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    @Override
    public IContextInformation[] computeContextInformation(
            ITextViewer viewer, int offset) {
        return new IContextInformation[0];
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        // if the property is not set this counts as on.
        if (!preferenceStore.contains(DefaultCodeFormatterConstants.ENABLE_AUTO_COMPLETION)){
            return autoActivationCharacters;
        }
        
        if (preferenceStore.getBoolean(DefaultCodeFormatterConstants.ENABLE_AUTO_COMPLETION)) {
            final String configuredAutoActivationCharacters = preferenceStore.getString(DefaultCodeFormatterConstants.AUTO_COMPLETION_TRIGGERS);
            if (configuredAutoActivationCharacters == null) {
                return autoActivationCharacters;
            } else {
                return configuredAutoActivationCharacters.toCharArray();
            }
        } else {
            return no_chars;
        }
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }
    
    @Override
    protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
        return CALEclipseUIPlugin.getDefault().getTemplateContextRegistry().getContextType(CALTemplateContextType.ID);
    }

    /**
     * Shows the template image next to all template proposals
     */
    @Override
    protected Image getImage(Template template) {
        return templateImage;
    }
    
    /**
     * CAL only has one context for now.  Therefore, all possible templates
     * are available at all possible locations.  But in the future we may want to add a
     * CalDoc context.  
     * 
     * @param contextTypeId
     *            the template context ID
     * @return the list of templates available in this context
     */
    // ADE the templates are distracting for the demo, so we remove them
    @Override
    protected Template[] getTemplates(String contextTypeId) {
        return new Template[0];
//        return CALEclipseUIPlugin.getDefault().getTemplateStore().getTemplates(
//                contextTypeId);
    }
    
    
    protected List<ICompletionProposal> computeApplicableTemplates(String prefix, 
            ITextViewer viewer, int offset) {
        
        ICompletionProposal[] unfiltered = super.computeCompletionProposals(viewer, offset);
        List filtered = new ArrayList(unfiltered.length);
        for (int cnt = 0; cnt < unfiltered.length; cnt++) {
            if (matchesPrefix(unfiltered[cnt].getDisplayString(), prefix)) {
                filtered.add(unfiltered[cnt]);
            }
        }
        return filtered;
    }
    
    private boolean matchesPrefix(String display, String prefixToMatch) {
        return display.startsWith(prefixToMatch);
    }

    private final char[] autoActivationCharacters = { '.' };
    private final char[] no_chars = {};
    
}