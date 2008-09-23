package org.openl.rules.webstudio.web.tableeditor;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.EditorHelper;
import org.openl.rules.ui.AllTestsRunResult;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.webtools.WebTool;
import org.openl.rules.util.net.NetUtils;
import org.openl.rules.workspace.abstracts.ProjectException;
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

    public ShowTableBean() {
        String s_id = FacesUtils.getRequestParameter("elementID");
        WebStudio studio = WebStudioUtils.getWebStudio();

        if (s_id != null) {
            elementID = Integer.parseInt(s_id);
            switchParam = Boolean.valueOf(FacesUtils.getRequestParameter("switch"));

            Map sessionMap = FacesUtils.getSessionMap();
            EditorHelper editorHelper;
            synchronized (sessionMap) {
                editorHelper = (EditorHelper) sessionMap.get("editorHelper");
                if (editorHelper == null) {
                    sessionMap.put("editorHelper", editorHelper = new EditorHelper(studio));
                }
            }
            editorHelper.setTableID(elementID, studio.getModel(), getView(), !switchParam);
            studio.setTableID(elementID);
        } else {
            String s_uri = FacesUtils.getRequestParameter("elementURI");
            if (s_uri != null) {
                int index = studio.getModel().indexForNodeByURI(s_uri);
                if (index >= 0) studio.setTableID(index);
            }
            elementID = studio.getTableID();
        }
        url = studio.getModel().makeXlsUrl(elementID);
        uri = studio.getModel().getUri(elementID);
        text = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);
        name = studio.getModel().getDisplayNameFull(elementID);
        runnable = studio.getModel().isRunnable(elementID);
        testable = studio.getModel().isTestable(elementID);
        se = studio.getModel().getErrors(elementID);

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

    public boolean isEditable() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpSession session = request.getSession();
        try {
            return NetUtils.isLocalRequest(request)||(studio.getCurrentProject(session)!=null && (studio.getCurrentProject(session).isCheckedOut()||studio.getCurrentProject(session).isLocalOnly()));
        } catch (ProjectException e) {
            return false;
        } catch (WorkspaceException e) {
            return false;
        }
    }

    public boolean isCopyable() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        HttpSession session = request.getSession();
        try {
            return studio.getCurrentProject(session)!=null && (studio.getCurrentProject(session).isCheckedOut()||studio.getCurrentProject(session).isLocalOnly());
        } catch (ProjectException e) {
            return false;
        } catch (WorkspaceException e) {
            return false;
        }
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
