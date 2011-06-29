package org.openl.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author smesh
 */
public class NewConfigProjectWizard extends NewProjectFromTemplateWizard {

    public NewConfigProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(OpenlWizardPlugin.getDefault().getBundle(),
                "NewConfigProjectWizard") {
        });
    }

}
