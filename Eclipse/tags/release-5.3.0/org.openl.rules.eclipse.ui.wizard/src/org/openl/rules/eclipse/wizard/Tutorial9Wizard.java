package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

public class Tutorial9Wizard extends NewProjectFromTemplateWizard {

    public Tutorial9Wizard() {
        super(
                new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                        "Tutorial9Wizard") {
                });
    }

}
