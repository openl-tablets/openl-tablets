package com.exigen.eclipse.openl.model.editor.actions;

import com.exigen.eclipse.common.ui.property.page.ICommonPropertyPageContributor;
import com.exigen.eclipse.common.ui.property.page.ICommonPropertyPageContributorFactory;

public class OpenLPropertyPageContributorFactory implements
		ICommonPropertyPageContributorFactory {

	public ICommonPropertyPageContributor createContributor() {
		return new OpenLPropertyPageContributor();
	}
}
