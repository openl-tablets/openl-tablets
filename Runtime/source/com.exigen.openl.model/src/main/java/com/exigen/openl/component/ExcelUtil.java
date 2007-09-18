package com.exigen.openl.component;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import com.exigen.common.repository.emf.FileManagerURIConverter;
import com.exigen.common.util.runtime.Assert;
import com.exigen.openl.model.openl.RuleSetFile;

public class ExcelUtil {
	public static String getFileName(RuleSetFile ruleSetFile) {
		Resource resource = ruleSetFile.eResource();
		Assert
				.isNotNull(resource,
						"Unable to determine Excel file name for model, not in resource set");

		URI uri = resource.getURI();
		String fileName = FileManagerURIConverter.getFileName(uri);
		int i = fileName.lastIndexOf(".");
		if (i > 0) {
			fileName = fileName.substring(0, i);
			return fileName + ".xls";
		}
		Assert.isTrue(false,
				"Unable to determine Excel file name for Rule Set File with URI "
						+ uri);
		return null;

	}

}
