/**
 *
 */
package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author szyrianov
 *
 */
public class NewWebStudioProjectWizard extends NewProjectFromTemplateWizard {

    public NewWebStudioProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                "NewWebStudioProjectWizard") {
        });
    }

}
