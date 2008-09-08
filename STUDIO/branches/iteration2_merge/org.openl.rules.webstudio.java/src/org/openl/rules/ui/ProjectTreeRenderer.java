/**
 * Created Jan 26, 2007
 */
package org.openl.rules.ui;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.util.ITreeElement;

/**
 * @author snshor
 */
public class ProjectTreeRenderer extends DTreeRenderer implements IProjectTypes, ITableNodeTypes {

    static String[][] icons = {
            { "workbook", "webresource/images/excel-workbook.png", "webresource/images/excel-workbook.png",
                    "webresource/images/excel-workbook-error.png", "webresource/images/excel-workbook-error.png" },
            { "worksheet", "webresource/images/worksheet.gif", "webresource/images/worksheet.gif",
                    "webresource/images/worksheet-error.png", "webresource/images/worksheet-error.png" }
            //		,{PT_TABLE + "." + XLS_DT, "webresource/images/ruleset.gif","webresource/images/ruleset-h.gif","webresource/images/ruleset-error.png","webresource/images/ruleset-error.png"}
            ,
            { PT_TABLE + "." + XLS_DT, "webresource/images/dt3.png", "webresource/mages/dt3.png",
                    "webresource/images/dt3-error.png", "webresource/images/dt3-error.png",
                    "webresource/images/dt3-check.png", "webresource/images/dt3-check.png", },
            { PT_TABLE + "." + XLS_DATA, "webresource/images/data.gif", "webresource/images/data.gif",
                    "webresource/images/data-error.png", "webresource/images/data-error.png" },
            { PT_TABLE + "." + XLS_DATATYPE, "webresource/images/dataobject.gif", "webresource/images/dataobject.gif",
                    "webresource/images/dataobject-error.png", "images/dataobject-error.png" },
            { PT_TABLE + "." + XLS_ENVIRONMENT, "webresource/images/config_obj.gif",
                    "webresource/images/config_obj.gif", "webresource/images/config_obj-error.png",
                    "webresource/images/config_obj-error.png" },
            { PT_TABLE + "." + XLS_METHOD, "webresource/images/method.gif", "webresource/images/method.gif",
                    "webresource/images/method-error.png", "webresource/images/method-error.png" },
            { PT_TABLE + "." + XLS_TEST_METHOD, "webresource/images/test_ok.gif", "webresource/images/test_ok.gif",
                    "webresource/images/test_ok-error.gif", "webresource/images/test_ok-error.gif" },
            { PT_TABLE + "." + XLS_RUN_METHOD, "webresource/images/test.gif", "webresource/images/test.gif",
                    "webresource/images/test-error.png", "webresource/images/test-error.png" } };

    ProjectModel project;

    /**
     * @param jsp
     * @param frame
     * @param icons
     */
    public ProjectTreeRenderer(ProjectModel project, String jsp, String frame) {
        super(jsp, frame, icons);
        this.project = project;
    }

    protected String makeURL(ITreeElement element) {

        if (element.getType().startsWith(PT_TABLE + "."))
            return targetJsp + "?elementID=" + map.getID(element);

        if (element.getType().startsWith(PT_PROBLEM))
            return "jsp/showError.jsp" + "?elementID=" + map.getID(element);

        return null;
    }

    public ProjectTreeElement getElement(int id) {
        return (ProjectTreeElement) map.getObject(id);
    }

    protected int getState(ITreeElement element) {
        ProjectTreeElement pte = (ProjectTreeElement) element;
        if (pte.hasProblem())
            return 1;
        if (pte.tsn != null && project.isTestable(pte.tsn))
            return 2;

        return 0;
    }

}
