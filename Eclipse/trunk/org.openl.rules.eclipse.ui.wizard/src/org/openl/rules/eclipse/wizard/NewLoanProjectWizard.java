package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author smesh
 */
public class NewLoanProjectWizard extends NewProjectFromTemplateWizard {

    public NewLoanProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                "NewLoanProjectWizard") {
        });
    }

}
