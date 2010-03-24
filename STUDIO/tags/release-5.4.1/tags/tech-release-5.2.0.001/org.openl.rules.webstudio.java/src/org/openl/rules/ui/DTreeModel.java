package org.openl.rules.ui;

import java.util.HashMap;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.ITableNodeTypes;

public class DTreeModel implements IProjectTypes, ITableNodeTypes {
	public void renderElement(ProjectTreeElement parent,
			ProjectTreeElement element, String targetJsp, StringBuffer buf) {
		// d.add(id, parentId, name, url, title, target, icon, iconOpen, open) {

		buf.append("d.add(");
		int parentId = getID(parent);
		int id = getID(element);
		String sfx = (element.getNameCount() < 2 ? "" : "("
				+ element.getNameCount() + ")");
		String name = element.getDisplayName(INamedThing.SHORT) + sfx;
		String url = makeURL(element, targetJsp);
		String title = element.getDisplayName(INamedThing.REGULAR);
		String target = "mainFrame";
		String icon = getIcon(element);
		String iconOpen = getIconOpen(element);
		String open = null;

		buf.append(id).append(',');
		buf.append(parentId).append(',');
		buf.append(jsStrName(name)).append(',');
		buf.append(jsStr(url)).append(',');
		buf.append(jsStrName(title)).append(',');
		buf.append(jsStr(target)).append(',');
		buf.append(jsStr(icon)).append(',');
		buf.append(jsStr(iconOpen)).append(',');
		buf.append(jsStr(open));

		buf.append(");");
		buf.append("\n");
	}

	public static String jsStr(String string) {
		if (string == null)
			return "''";

		return "'" + string + "'";
	}

	public static String jsStrName(String string) {
		if (string == null)
			return "''";

		int idx = string.indexOf('\n');
		if (idx > 0)
			string = string.substring(0, idx) + " ...";

		idx = string.indexOf('\'');
		if (idx > 0)
			string = string.substring(0, idx) + " ...";

		// if (string.length() > 30)
		// string = string.substring(0, 29) + " ...";

		return "'" + string + "'";
	}

	static final int errIndex(ProjectTreeElement element) {
		return element.hasProblem() ? 1 : 0;
	}

	static final int ERR_SHIFT = 2;

	private String getIconOpen(ProjectTreeElement element) {
		int err = errIndex(element);

		for (int i = 0; i < icons.length; i++) {
			if (icons[i][0].equals(element.getType()))
				return icons[i][2 + ERR_SHIFT * err];
		}

		return element.isLeaf() ? DEFAULT_ICON_LEAF[err]
				: DEFAULT_ICON_OPEN[err];
	}

	static final String[] DEFAULT_ICON_OPEN = {
			"webresource/images/dtree/folder-o-n.gif",
			"webresource/images/folder-o-error.png" }, DEFAULT_ICON_CLOSED = {
			"webresource/images/dtree/folder-c-n.gif",
			"webresource/images/folder-c-error.png" }, DEFAULT_ICON_LEAF = {
			"webresource/images/categoryset.gif",
			"webresource/images/categoryset.gif" };

	static String[][] icons = {
			{ "workbook", "webresource/images/excel-workbook.png",
					"webresource/images/excel-workbook.png",
					"webresource/images/excel-workbook-error.png",
					"webresource/images/excel-workbook-error.png" },
			{ "worksheet", "webresource/images/worksheet.gif",
					"webresource/images/worksheet.gif",
					"webresource/images/worksheet-error.png",
					"webresource/images/worksheet-error.png" },
			{ PT_TABLE + "." + XLS_DT, "webresource/images/ruleset.gif",
					"webresource/images/ruleset-h.gif",
					"webresource/images/ruleset-error.png",
					"webresource/images/ruleset-error.png" },
			{ PT_TABLE + "." + XLS_SPREADSHEET, "webresource/images/spreadsheet.gif",
					"webresource/images/spreadsheet.gif",
					"webresource/images/spreadsheet-error.gif",
					"webresource/images/spreadsheet-error.gif" },
			{ PT_TABLE + "." + XLS_DATA, "webresource/images/data.gif",
					"webresource/images/data.gif",
					"webresource/images/data-error.png",
					"webresource/images/data-error.png" },
			{ PT_TABLE + "." + XLS_DATATYPE,
					"webresource/images/dataobject.gif",
					"webresource/images/dataobject.gif",
					"webresource/images/dataobject-error.png",
					"webresource/images/dataobject-error.png" },
			{ PT_TABLE + "." + XLS_ENVIRONMENT,
					"webresource/images/config_obj.gif",
					"webresource/images/config_obj.gif",
					"webresource/images/config_obj-error.png",
					"webresource/images/config_obj-error.png" },
			{ PT_TABLE + "." + XLS_METHOD, "webresource/images/method.gif",
					"webresource/images/method.gif",
					"webresource/images/method-error.png",
					"webresource/images/method-error.png" },
			{ PT_TABLE + "." + XLS_TEST_METHOD, "webresource/images/test.gif",
					"webresource/images/test.gif",
					"webresource/images/test-error.png",
					"webresource/images/test-error.png" },
			{ PT_TABLE + "." + XLS_RUN_METHOD, "webresource/images/test.gif",
					"webresource/images/test.gif",
					"webresource/images/test-error.png",
					"webresource/images/test-error.png" } };

	private String getIcon(ProjectTreeElement element) {
		int err = errIndex(element);
		for (int i = 0; i < icons.length; i++) {
			if (icons[i][0].equals(element.getType()))
				return icons[i][1 + ERR_SHIFT * err];
		}

		return element.isLeaf() ? DEFAULT_ICON_LEAF[err]
				: DEFAULT_ICON_CLOSED[err];
	}

	private String makeURL(ProjectTreeElement element, String targetJsp) {
		if (element.getType().startsWith(PT_TABLE + "."))
			return targetJsp + "?elementID=" + getID(element);

		return null;
	}

	synchronized static int getID(ProjectTreeElement element) {
		if (element == null)
			return -1;
		Integer id = (Integer) elementID.get(element);
		if (id == null) {
			id = new Integer(++uniqueID);
			elementID.put(element, id);
			idElement.put(id, element);
		}
		return id.intValue();
	}

	static HashMap<ProjectTreeElement, Integer> elementID = new HashMap<ProjectTreeElement, Integer>();
	static HashMap<Integer, ProjectTreeElement> idElement = new HashMap<Integer, ProjectTreeElement>();

	static int uniqueID = 0;

	public static ProjectTreeElement getElement(int elementID) {
		return (ProjectTreeElement) idElement.get(new Integer(elementID));
	}
}
