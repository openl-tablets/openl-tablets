package org.openl.rules.webstudio.web.servlet;

import org.openl.rules.webstudio.application.ThreadLocalUserHolder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.lw.impl.LocalWorkspaceManagerImpl;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public class SessionFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain filterChain) throws IOException, ServletException
    {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String remoteAddr = request.getRemoteAddr();
            HttpSession session = httpRequest.getSession(true);

            boolean isLocalUser = WebStudioUtils.isLocalRequest(httpRequest);
            String username;
            if (isLocalUser) {
                username = LocalWorkspaceManagerImpl.USER_LOCAL;
            } else {
                username = remoteAddr;
            }
            WorkspaceUser user = new WorkspaceUserImpl(username);

            ThreadLocalUserHolder.setUser(user);
        }

        filterChain.doFilter(request, response);
    }

    public void destroy() {}

    public void init(FilterConfig arg0) throws ServletException {}
}
