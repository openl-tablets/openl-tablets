package org.openl.rules.webstudio.web.tableeditor;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openl.rules.table.IGridTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.AllTestsRunResult;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.syntax.ISyntaxError;

/**
 * Request scope managed bean for showTable facelet.
 */
public class ShowTableBean {
    private String url;
    private String text;
    private String name;
    private boolean runnable;
    private boolean testable;
    private ISyntaxError[] se;
    private String uri;
    private int elementID = -1;
    private String notViewParams;
    private boolean switchParam;

    @SuppressWarnings("unchecked")
    public ShowTableBean() {
        String s_id = FacesUtils.getRequestParameter("elementID");
        WebStudio studio = WebStudioUtils.getWebStudio();

        if (s_id != null) {
            elementID = Integer.parseInt(s_id);
            switchParam = Boolean.valueOf(FacesUtils.getRequestParameter("switch"));
            studio.setTableID(elementID);
        } else {
            String s_uri = FacesUtils.getRequestParameter("elementURI");
            if (s_uri != null) {
                int index = studio.getModel().indexForNodeByURI(s_uri);
                if (index >= 0) studio.setTableID(index);
            }
            elementID = studio.getTableID();
        }
        final ProjectModel model = studio.getModel();
        url = model.makeXlsUrl(elementID);
        uri = model.getUri(elementID);
        text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);
        name = model.getDisplayNameFull(elementID);
        runnable = model.isRunnable(elementID);
        testable = model.isTestable(elementID);
        se = model.getErrors(elementID);

        Map paramMap = new HashMap(FacesUtils.getRequestParameterMap());
        for (Map.Entry entry : (Set<Map.Entry>) paramMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                entry.setValue(new String[]{(String) entry.getValue()});
            }
        }
        notViewParams = WebTool.listParamsExcept(new String[]{"transparency", "filterType", "view"}, paramMap);
    }

    public String getName() {
        return name;
    }

    public boolean isRunnable() {
        return runnable;
    }

    public ISyntaxError[] getSe() {
        return se;
    }

    public boolean isTestable() {
        return testable;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public String getUri() {
        return uri;
    }

    public boolean isHasErrors() {
        return se != null && se.length > 0;
    }

    public boolean isTsnHasErrors() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio != null && webStudio.getModel().hasErrors(elementID);
    }

    public String getErrorString() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio.getModel().showErrors(elementID);
    }

    public int getElementID() {
        return elementID;
    }

    public String getNotViewParams() {
        return notViewParams;
    }

    UserWorkspaceProject getCurrentProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpSession session = request.getSession();
        
        try {
            return studio.getCurrentProject(session);
        } catch (ProjectException e) {
            return null;
        } catch (WorkspaceException e) {
            return null;
        }
    }

    boolean canModifyCurrentProject() {
        UserWorkspaceProject currentProject = getCurrentProject();

        if (currentProject != null) {
            return (currentProject.isCheckedOut() || currentProject.isLocalOnly());
        } else {
            return false;
        }
    }

    public boolean isEditable() {
        return canModifyCurrentProject();
    }

    public boolean isCopyable() {
        return canModifyCurrentProject();
    }

    public String getView() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        return studio.getModel().getTableView(FacesUtils.getRequestParameter("view"));
    }

    public String getEditCell() {
        if (switchParam)
            return "";
        return FacesUtils.getRequestParameter("cell");
    }

    public TestRunsResultBean getTestRunResults() {
        AllTestsRunResult atr = WebStudioUtils.getWebStudio().getModel().getRunMethods(elementID);
        AllTestsRunResult.Test[] tests = null;
        if (atr != null)
            tests = atr.getTests();
        return new TestRunsResultBean(tests);
    }

    public IGridTable getTable() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        IGridTable table = studio.getModel().getTableWithMode(
                elementID == -1 ? studio.getTableID() : elementID, getView());
        return table;
    }

    public static class TestRunsResultBean {
        private AllTestsRunResult.Test[] tests;
        private TestProxy[] proxies;

        public class TestProxy {
            int index;

            public TestProxy(int index) {
                this.index = index;
            }

            public String[] getDescriptions() {
                AllTestsRunResult.Test test = getTest();
                String[] descriptions = new String[test.ntests()];
                for (int i = 0; i < descriptions.length; i++) {
                    descriptions[i] = test.getTestDescription(i);
                }
                return descriptions;
            }
            public String getTestName() {return WebTool.encodeURL(getTest().getTestName());}

            private AllTestsRunResult.Test getTest() {return tests[index];}
        }

        public TestRunsResultBean(AllTestsRunResult.Test[] tests) {
            this.tests = tests;
            if (tests == null)
                proxies = new TestProxy[0];
            else
                proxies = new TestProxy[tests.length];

            for (int i = 0; i < proxies.length; i++)
                proxies[i] = new TestProxy(i);
        }

        public TestProxy[] getTests() {
            return proxies;
        }

        public boolean isNotEmpty() {
            return tests != null && tests.length > 0;
        }
    }
}
