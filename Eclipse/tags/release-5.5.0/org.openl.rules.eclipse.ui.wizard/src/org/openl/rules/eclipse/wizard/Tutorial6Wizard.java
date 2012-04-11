package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

public class Tutorial6Wizard extends NewProjectFromTemplateWizard {

    public Tutorial6Wizard() {
        super(
                new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                        "Tutorial6Wizard") {
                });
    }

}
