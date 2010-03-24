package org.openl.rules.ui;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.webtools.XlsUrlParser;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author Aliaksandr Antonik.
 */
public class NavigationBean {

    private static class FoundResult {
        OpenLWrapperInfo wrapperInfo;
        TableSyntaxNode syntaxNode;

        private FoundResult(TableSyntaxNode syntaxNode, OpenLWrapperInfo wrapperInfo) {
            this.syntaxNode = syntaxNode;
            this.wrapperInfo = wrapperInfo;
        }
    }

    public boolean navigate(HttpServletRequest request)  {
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
            webStudio.setCurrentWrapper(res.wrapperInfo);
        } catch (Exception e) {
            return false;
        }
        
        request.setAttribute("url", res.syntaxNode.getUri());
        return true;
    }

    private FoundResult findMatchingWrapper(String url, WebStudio webStudio) {
        XlsUrlParser parser = new XlsUrlParser();
        parser.parse(url);

        if (parser.wsName == null && parser.range == null) {
            return findMatchingWrapperByFileOnly(parser, webStudio);
        }

        try {
            for (OpenLWrapperInfo w : webStudio.getWrappers()) {
                try {
                    ProjectModel model = new ProjectModel(webStudio);
                    model.setWrapperInfo(w);
                    TableSyntaxNode syntaxNode = model.findNode(parser);
                    if (syntaxNode != null) {
                        return new FoundResult(syntaxNode, w);
                    }
                } catch (Exception e) {}
            }
        } catch (IOException e) {}

        return null;
    }

    private FoundResult findMatchingWrapperByFileOnly(XlsUrlParser parser, WebStudio webStudio) {
        if (parser.wbPath == null || parser.wbName == null)
            return null;


        try {
            for (OpenLWrapperInfo w : webStudio.getWrappers()) {
                try {
                    ProjectModel model = new ProjectModel(webStudio);
                    model.setWrapperInfo(w);
                    TableSyntaxNode syntaxNode = model.findAnyTableNodeByLocation(parser);
                    if (syntaxNode != null) {
                        return new FoundResult(syntaxNode, w);
                    }
                } catch (Exception e) {}
            }
        } catch (IOException e) {}

        return null;
    }
}
