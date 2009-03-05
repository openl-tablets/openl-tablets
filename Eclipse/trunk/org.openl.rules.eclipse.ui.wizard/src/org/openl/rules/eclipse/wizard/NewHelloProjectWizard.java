package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author smesh
 */
public class NewHelloProjectWizard extends NewProjectFromTemplateWizard {

    public NewHelloProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                "NewHelloProjectWizard") {
        });
    }

}
