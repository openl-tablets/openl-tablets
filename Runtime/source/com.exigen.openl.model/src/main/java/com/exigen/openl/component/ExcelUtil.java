package com.exigen.openl.component;

import com.exigen.openl.model.openl.RuleSetFile;

public class ExcelUtil {
	public static String getFileName(RuleSetFile ruleSetFile) {
		String path = ruleSetFile.getExcelResourceReference();
		
		if (path==null)
			return null;

		int j = path.lastIndexOf("/");
		String packageName = "";
		String resourceName = path;
		if (j >= 0) {
			packageName = path.substring(0, j);
			resourceName = path.substring(j + 1);
		}
		packageName = packageName.replace("/", ".");
		if (resourceName.length() == 0)
			return null;
		
		return packageName.replace(".", "/")+"/"+resourceName;

	}

}
