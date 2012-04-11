package org.openl.commons.web.jsf.facelets.acegi;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

import org.acegisecurity.Authentication;

import org.acegisecurity.acl.AclEntry;
import org.acegisecurity.acl.AclManager;
import org.acegisecurity.acl.basic.BasicAclEntry;

import org.acegisecurity.context.SecurityContextHolder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;

import org.springframework.context.ApplicationContext;

import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.el.ELException;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import javax.servlet.http.HttpServletRequest;

/**
 * Hangler similar to Acegi acl tag. See Acegi for details
 *
 * @author Andrey Naumenko
 */
public class AclHandler extends TagHandler {
    private static final Log log = LogFactory.getLog(AclHandler.class);
    private TagAttribute domainObject;
    private TagAttribute hasPermission;

    public AclHandler(TagConfig tagConfig) {
        super(tagConfig);

        domainObject = getAttribute("domainObject");
        hasPermission = getAttribute("hasPermission");
    }

    public void apply(FaceletContext faceletContext, UIComponent parent) throws IOException, FacesException,
            FaceletException, ELException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        ApplicationContext context = getAppCtx(faceletContext);
        String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, AclManager.class, false, false);

        if (beans.length == 0) {
            log.warn("No AclManager would found the application context: " + context.toString());
            log.warn("All secured resources will be allowed by default");
            nextHandler.apply(faceletContext, parent);
            return;
        }

        AclManager aclManager = (AclManager) context.getBean(beans[0]);

        // Obtain aclEntrys applying to the current Authentication object
        Object resolvedDomainObject = domainObject.getObject(faceletContext);
        AclEntry[] acls = aclManager.getAcls(resolvedDomainObject, auth);

        if (log.isDebugEnabled()) {
            log.debug("Authentication: '" + auth + "' has: " + ((acls == null) ? 0 : acls.length)
                    + " AclEntrys for domain object: '" + resolvedDomainObject + "' from AclManager: '"
                    + aclManager.toString() + "'");
        }

        if ((acls == null) || (acls.length == 0)) {
            log.debug("No alcs provided. Skip evaluation");
            return;
        }

        Integer[] requiredIntegers = parseIntegersString(hasPermission.getValue());
        for (int i = 0; i < acls.length; i++) {
            if (acls[i] instanceof BasicAclEntry) {
                BasicAclEntry processableAcl = (BasicAclEntry) acls[i];

                // See if principal has any of the required permissions
                for (int j = 0; j < requiredIntegers.length; j++) {
                    if (processableAcl.isPermitted(requiredIntegers[j].intValue())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Including tag body as found permission: " + requiredIntegers[j]
                                    + " due to AclEntry: '" + processableAcl + "'");
                        }
                        nextHandler.apply(faceletContext, parent);
                        return;
                    }
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("No permission, so skipping tag body");
        }
    }

    private ApplicationContext getAppCtx(FaceletContext ctx) {
        HttpServletRequest request = (HttpServletRequest) ctx.getFacesContext().getExternalContext().getRequest();
        return WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession().getServletContext());
    }

    private Integer[] parseIntegersString(String integersString) throws NumberFormatException {
        final Set<Integer> integers = new HashSet<Integer>();
        final StringTokenizer tokenizer;
        tokenizer = new StringTokenizer(integersString, ",", false);

        while (tokenizer.hasMoreTokens()) {
            String integer = tokenizer.nextToken();
            integers.add(new Integer(integer));
        }

        return integers.toArray(new Integer[] {});
    }
}
