package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

public class Tutorial3Wizard extends NewProjectFromTemplateWizard {

    public Tutorial3Wizard() {
        super(
                new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                        "Tutorial3Wizard") {
                });
    }

}
