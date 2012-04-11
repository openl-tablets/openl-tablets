package org.openl.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author smesh
 */
public class NewSimpleOpenlProjectWizard extends NewProjectFromTemplateWizard {

    public NewSimpleOpenlProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(OpenlWizardPlugin.getDefault().getBundle(),
                "NewSimpleOpenlProjectWizard") {
        });
    }

}
