package org.openl.rules.eclipse.wizard;

import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizard;
import org.openl.eclipse.wizard.base.NewProjectFromTemplateWizardCustomizer;

/**
 * @author smesh
 */
public class NewLEProjectWizard extends NewProjectFromTemplateWizard {

    public NewLEProjectWizard() {
        super(new NewProjectFromTemplateWizardCustomizer(LiveExcelWizardPlugin.getDefault().getBundle(),
                "NewLEProjectWizard", "org.openl.rules.eclipse.wizard.Messages_LE") {
        });
    }

}
