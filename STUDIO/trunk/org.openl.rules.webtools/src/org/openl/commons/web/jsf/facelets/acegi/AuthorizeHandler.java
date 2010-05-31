package org.openl.commons.web.jsf.facelets.acegi;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

import org.acegisecurity.taglibs.velocity.Authz;
import org.acegisecurity.taglibs.velocity.AuthzImpl;

import org.apache.commons.lang.StringUtils;

import org.springframework.context.ApplicationContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;

import javax.el.ELException;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import javax.servlet.http.HttpServletRequest;

/**
 * See javadocs for AuthorizeTag from Acegi for more details.
 *
 * @author Andrey Naumenko
 */
public class AuthorizeHandler extends TagHandler {
    private TagAttribute ifAllGranted;
    private TagAttribute ifAnyGranted;
    private TagAttribute ifNotGranted;

    public AuthorizeHandler(TagConfig config) {
        super(config);

        ifAnyGranted = getAttribute("ifAnyGranted");
        ifAllGranted = getAttribute("ifAllGranted");
        ifNotGranted = getAttribute("ifNotGranted");
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException {
        Authz authz = new AuthzImpl();
        authz.setAppCtx(getAppCtx(ctx));
        String roles = null;
        boolean apply = false;

        if (ifNotGranted != null) {
            roles = ifNotGranted.getValue(ctx);
            if (StringUtils.isNotEmpty(roles)) {
                apply = authz.noneGranted(roles);
            }
        }

        if (ifAllGranted != null) {
            roles = ifAllGranted.getValue(ctx);
            if (StringUtils.isNotEmpty(roles)) {
                apply = authz.allGranted(roles);
            }
        }

        if (ifAnyGranted != null) {
            roles = ifAnyGranted.getValue(ctx);
            if (StringUtils.isNotEmpty(roles)) {
                apply = authz.anyGranted(roles);
            }
        }

        if (apply) {
            nextHandler.apply(ctx, parent);
        }
    }

    private ApplicationContext getAppCtx(FaceletContext ctx) {
        HttpServletRequest request = (HttpServletRequest) ctx.getFacesContext().getExternalContext().getRequest();
        return WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession().getServletContext());
    }
}
