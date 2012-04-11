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
public class ApplicationDemoProjectWizard extends NewProjectFromTemplateWizard {

    public ApplicationDemoProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(RulesWizardPlugin.getDefault().getBundle(),
                "ApplicationDemoProjectWizard") {
        });
    }

}
