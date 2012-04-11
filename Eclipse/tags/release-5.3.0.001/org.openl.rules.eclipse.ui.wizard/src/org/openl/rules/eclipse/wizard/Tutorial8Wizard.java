package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

public class Tutorial8Wizard extends NewProjectFromTemplateWizard {

    public Tutorial8Wizard() {
        super(
                new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                        "Tutorial8Wizard") {
                });
    }

}
