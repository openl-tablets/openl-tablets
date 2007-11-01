package org.openl.rules.webstudio;

import org.openl.rules.MultiUserWorkspaceManager;
import org.openl.rules.WorkspaceException;
import org.openl.rules.WorkspaceUser;
import org.openl.rules.WorkspaceUserImpl;
import org.openl.rules.commons.logs.CLog;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class SessionFilter implements Filter {
    private MultiUserWorkspaceManager workspaceManager;

    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            workspaceManager = new MultiUserWorkspaceManager();
        } catch (WorkspaceException e) {
            CLog.log(CLog.FATAL, "Failed to init Workspace manager!", e);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

//            String remoteHost = request.getRemoteHost();
            String remoteAddr = request.getRemoteAddr();
//            String remoteUser = httpRequest.getRemoteUser();
            HttpSession session = httpRequest.getSession(false);

            if (session == null) {
                WorkspaceUser user = new WorkspaceUserImpl(remoteAddr);
                RulesUserSession rulesUserSession = new RulesUserSession(user, workspaceManager);

                session = httpRequest.getSession(true);
                session.setAttribute("rulesUserSession", rulesUserSession);
//                session.setMaxInactiveInterval(15);
            }
        }

        filterChain.doFilter(request, response);
    }

    public void destroy() {
        //
    }
}
