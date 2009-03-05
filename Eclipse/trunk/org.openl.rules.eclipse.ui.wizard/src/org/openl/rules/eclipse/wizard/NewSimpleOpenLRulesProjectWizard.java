package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author smesh
 */
public class NewSimpleOpenLRulesProjectWizard extends NewProjectFromTemplateWizard {

    public NewSimpleOpenLRulesProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                "NewSimpleOpenLRulesProjectWizard") {
        });
    }

}
