package com.exigen.eclipse.openl.model;

import java.util.Map;

import com.exigen.eclipse.common.model.DefaultModelType;
import com.exigen.openl.model.openl.OpenlFactory;
import com.exigen.openl.model.openl.OpenlPackage;

public class OpenLModelType extends DefaultModelType {
	private static final long serialVersionUID = 1L;

	public org.eclipse.emf.ecore.EObject createRootElement(Map arg0) {
		return OpenlFactory.eINSTANCE.createRuleSetFile();
	}

	public org.eclipse.emf.ecore.EClass getRootElementType() {
		return OpenlPackage.eINSTANCE.getRuleSetFile();
	}
}
