package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

public class Tutorial10Wizard extends NewProjectFromTemplateWizard {

    public Tutorial10Wizard() {
        super(
                new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                        "Tutorial10Wizard") {
                });
    }

}
