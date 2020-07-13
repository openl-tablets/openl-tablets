package org.openl.rules.webstudio.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

public class LdapToOpenLUserDetailsMapper implements UserDetailsContextMapper {
    private final Logger log = LoggerFactory.getLogger(LdapToOpenLUserDetailsMapper.class);
    private final UserDetailsContextMapper delegate;

    private final String groupFilter;

    // Fields below are needed to make additional requests to AD:
    private final String domain;
    private final String url;
    private final String rootDn;
    private final String searchFilter;

    public LdapToOpenLUserDetailsMapper(UserDetailsContextMapper delegate, PropertyResolver propertyResolver) {
        this.delegate = delegate;
        String domainProperty = propertyResolver.getProperty("security.ad.domain");
        this.domain = StringUtils.isNotBlank(domainProperty) ? domainProperty.toLowerCase() : null;
        this.url = propertyResolver.getProperty("security.ad.server-url");
        this.searchFilter = propertyResolver.getProperty("security.ad.search-filter");
        this.groupFilter = propertyResolver.getProperty("security.ad.group-filter");

        rootDn = this.domain == null ? null : rootDnFromDomain(this.domain);
    }

    
    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx,
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        UserDetails userDetails = delegate.mapUserFromContext(ctx, username, authorities);

        String firstName = ctx.getStringAttribute("givenName");
        String lastName = ctx.getStringAttribute("sn");

        Collection<? extends GrantedAuthority> userAuthorities = getAuthorities(ctx,
            username,
            userDetails.getAuthorities());

        Collection<Privilege> privileges = new ArrayList<>(userAuthorities.size());
        for (GrantedAuthority authority : userAuthorities) {
            if (authority instanceof Privilege) {
                privileges.add((Privilege) authority);
            } else {
                privileges.add(new SimplePrivilege(authority.getAuthority(), authority.getAuthority()));
            }
        }
        return new SimpleUser(firstName, lastName, userDetails.getUsername(), null, privileges);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        delegate.mapUserToContext(user, ctx);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(DirContextOperations ctx,
            String username,
            Collection<? extends GrantedAuthority> fallbackAuthorities) {
        Collection<? extends GrantedAuthority> userAuthorities = null;

        Authentication authentication = AuthenticationHolder.getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            // Try to load nested groups and primary group of a user
            userAuthorities = loadUserAuthorities(ctx, username, (String) authentication.getCredentials());
        }

        if (userAuthorities == null) {
            // Fallback to default implementation
            userAuthorities = fallbackAuthorities;
        }
        return userAuthorities;
    }

    /**
     * Load nested groups and primary group. If error is occurred return null.
     *
     * @return Not null list if successful and null if cannot load user authorities because of an error.
     */
    private Collection<? extends GrantedAuthority> loadUserAuthorities(DirContextOperations userData,
            String username,
            String password) {
        try {
            String bindPrincipal = createBindPrincipal(username);
            String searchRoot = rootDn != null ? rootDn : searchRootFromPrincipal(bindPrincipal);

            DirContext context = bindAsUser(bindPrincipal, password);

            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DistinguishedName searchBaseDn = new DistinguishedName(searchRoot);

            // This search must be done using DirContext with java.naming.ldap.attributes.binary attribute is set to
            // "objectSid" because in current implementation of ActiveDirectoryLdapAuthenticationProvider the object
            // "userData"
            // contains objectSid attribute with String type and is broken.
            NamingEnumeration<SearchResult> userSearch = context
                .search(searchBaseDn, searchFilter, new Object[] { bindPrincipal, username }, searchControls);
            if (!userSearch.hasMoreElements()) {
                log.warn("Cannot find account '" + username + "'. Skip nested groups and primary group search.");
                return null;
            }
            userSearch.close();

            // Find all groups
            NamingEnumeration<SearchResult> groupsSearch = context
                    .search(searchBaseDn, groupFilter, new Object[] { bindPrincipal, username, userData.getDn() }, searchControls);

            // Fill authorities using search result
            ArrayList<GrantedAuthority> authorities = new ArrayList<>();
            try {
                while (groupsSearch.hasMore()) {
                    SearchResult searchResult = groupsSearch.next();
                    DistinguishedName dn = new DistinguishedName(new CompositeName(searchResult.getName()));

                    if (!searchRoot.isEmpty()) {
                        dn.prepend(searchBaseDn);
                    }

                    authorities.add(new SimpleGrantedAuthority(dn.removeLast().getValue()));
                }
            } catch (PartialResultException e) {
                groupsSearch.close();
                log.info("Ignoring PartialResultException with message: " + e.getMessage());
            }

            return authorities;
        } catch (NamingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    private DirContext bindAsUser(String bindPrincipal, String password) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, bindPrincipal);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.OBJECT_FACTORIES, DefaultDirObjectFactory.class.getName());
        // To handle "Unprocessed Continuation Reference(s)" errors
        env.put(Context.REFERRAL, "follow");

        return new InitialLdapContext(env, null);
    }

    private String searchRootFromPrincipal(String bindPrincipal) throws NamingException {
        int atChar = bindPrincipal.lastIndexOf('@');

        if (atChar < 0) {
            String message = "User principal '" + bindPrincipal + "' does not contain the domain, and no domain has been configured";
            log.error(message);
            throw new ConfigurationException(message);
        }

        return rootDnFromDomain(bindPrincipal.substring(atChar + 1));
    }

    private String rootDnFromDomain(String domain) {
        String[] tokens = org.springframework.util.StringUtils.tokenizeToStringArray(domain, ".");
        StringBuilder root = new StringBuilder();

        for (String token : tokens) {
            if (root.length() > 0) {
                root.append(',');
            }
            root.append("dc=").append(token);
        }

        return root.toString();
    }

    private String createBindPrincipal(String username) {
        if (domain == null || username.toLowerCase().endsWith(domain)) {
            return username;
        }

        return username + "@" + domain;
    }
}
