package org.openl.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author smesh
 */
public class NewOpenlExamplesProjectWizard extends NewProjectFromTemplateWizard {

    public NewOpenlExamplesProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(OpenlWizardPlugin.getDefault().getBundle(),
                "NewOpenlExamplesProjectWizard") {
        });
    }

}
