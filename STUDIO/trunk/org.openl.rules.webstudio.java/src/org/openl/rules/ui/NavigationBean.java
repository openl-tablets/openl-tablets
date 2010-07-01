package org.openl.rules.ui;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author Aliaksandr Antonik.
 */
public class NavigationBean {

    private static class FoundResult {
        Module moduleInfo;
        TableSyntaxNode syntaxNode;

        private FoundResult(TableSyntaxNode syntaxNode, Module moduleInfo) {
            this.syntaxNode = syntaxNode;
            this.moduleInfo = moduleInfo;
        }
    }

    private FoundResult findMatchingWrapper(String url, WebStudio webStudio) {
        XlsUrlParser parser = new XlsUrlParser();
        parser.parse(url);

        if (parser.wsName == null && parser.range == null) {
            return findMatchingWrapperByFileOnly(parser, webStudio);
        }

        for (ProjectDescriptor project : webStudio.getAllProjects()) {
            for (Module m : project.getModules()) {
                try {
                    ProjectModel model = new ProjectModel(webStudio);
                    model.setModuleInfo(m);
                    TableSyntaxNode syntaxNode = model.findNode(parser);
                    if (syntaxNode != null) {
                        return new FoundResult(syntaxNode, m);
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private FoundResult findMatchingWrapperByFileOnly(XlsUrlParser parser, WebStudio webStudio) {
        if (parser.wbPath == null || parser.wbName == null) {
            return null;
        }

        for (ProjectDescriptor project : webStudio.getAllProjects()) {
            for (Module m : project.getModules()) {
                try {
                    ProjectModel model = new ProjectModel(webStudio);
                    model.setModuleInfo(m);
                    TableSyntaxNode syntaxNode = model.findAnyTableNodeByLocation(parser);
                    if (syntaxNode != null) {
                        return new FoundResult(syntaxNode, m);
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    public boolean navigate(HttpServletRequest request) {
        String url = request.getParameter("url");
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        if (request.getParameter("range") != null) {
            url += "&range=" + request.getParameter("range");
        }

        WebStudio webStudio = WebStudioUtils.getWebStudio(request.getSession(false));
        if (webStudio == null) {
            return false;
        }

        FoundResult res = findMatchingWrapper(url, webStudio);
        if (res == null) {
            return false;
        }

        try {
            webStudio.setCurrentModule(res.moduleInfo);
        } catch (Exception e) {
            return false;
        }

        request.setAttribute("url", res.syntaxNode.getUri());
        return true;
    }
}
