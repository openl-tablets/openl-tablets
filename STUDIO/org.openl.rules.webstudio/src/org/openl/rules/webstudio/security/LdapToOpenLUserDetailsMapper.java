package org.openl.rules.webstudio.security;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimplePrivilege;
import org.openl.rules.security.SimpleUser;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import javax.naming.CompositeName;
import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

public class LdapToOpenLUserDetailsMapper implements UserDetailsContextMapper {
    private final Logger log = LoggerFactory.getLogger(LdapToOpenLUserDetailsMapper.class);
    private final UserDetailsContextMapper delegate;

    private final String primaryGroupFilter;
    private final String groupFilter;

    // Fields below are needed to make additional requests to AD:
    private final String domain;
    private final String url;
    private final String rootDn;
    private final String searchFilter;

    public LdapToOpenLUserDetailsMapper(UserDetailsContextMapper delegate, String domain, String url, String searchFilter, String primaryGroupFilter, String groupFilter) {
        this.delegate = delegate;
        this.domain = StringUtils.isNotBlank(domain) ? domain.toLowerCase() : null;
        this.url = url;
        this.searchFilter = searchFilter;
        this.primaryGroupFilter = primaryGroupFilter;
        this.groupFilter = groupFilter;

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
            NamingEnumeration<SearchResult> userSearch = context.search(searchBaseDn,
                    searchFilter,
                    new Object[]{bindPrincipal},
                    searchControls);
            if (!userSearch.hasMoreElements()) {
                log.warn("Cannot find account '" + username + "'. Skip nested groups and primary group search.");
                return null;
            }
            String primaryGroupSid = getPrimaryGroupSid(userSearch.next().getAttributes());
            LdapUtils.closeEnumeration(userSearch);

            // Find all groups
            NamingEnumeration<SearchResult> groupsSearch;
            if (primaryGroupSid != null) {
                // Find nested groups + primary group
                groupsSearch = context.search(searchBaseDn,
                        primaryGroupFilter,
                        new Object[]{userData.getDn(), primaryGroupSid},
                        searchControls);
            } else {
                // Find nested groups without primary group
                groupsSearch = context.search(searchBaseDn,
                        groupFilter,
                        new Object[]{userData.getDn()},
                        searchControls);
            }

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
                LdapUtils.closeEnumeration(groupsSearch);
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
        // Needed to get objectSid as binary array and then find primary group
        env.put("java.naming.ldap.attributes.binary", "objectSid");
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

        return rootDnFromDomain(bindPrincipal.substring(atChar + 1, bindPrincipal.length()));
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

    /**
     * Get SID of a primary group based on primaryGroupId and objectSid of current user.
     *
     * @see <a href=
     * "https://support.microsoft.com/en-us/help/297951/how-to-use-the-primarygroupid-attribute-to-find-the-primary-group-for">How
     * to use the PrimaryGroupID attribute to find the primary group for a user</a>
     */
    private String getPrimaryGroupSid(Attributes attributes) throws NamingException {
        Attribute attrPrimaryGroupId = attributes.get("primaryGroupId");
        Attribute attrObjectSid = attributes.get("objectSid");

        if (attrPrimaryGroupId != null && attrObjectSid != null) {
            String primaryGroupId = attrPrimaryGroupId.get().toString();
            String objectSid = decodeSid((byte[]) attrObjectSid.get());

            return objectSid.substring(0, objectSid.lastIndexOf('-') + 1) + primaryGroupId;
        }

        return null;
    }

    /**
     * The binary data is in the form: byte[0] - revision level byte[1] - count of sub-authorities byte[2-7] - 48 bit
     * identifier authority (big-endian) and then count x 32 bit sub authorities (little-endian)
     * <p>
     * The String value is: S-Revision-Authority-SubAuthority[n]...
     * <p>
     *
     * @see <a href="https://technet.microsoft.com/en-us/library/cc962011.aspx">Security Identifier Structure</a>
     * @see <a href="https://blogs.msdn.microsoft.com/oldnewthing/20040315-00/?p=40253">How do I convert a SID between
     * binary and string forms?</a>
     */
    private static String decodeSid(byte[] sid) {
        final StringBuilder strSid = new StringBuilder("S-");

        // Revision
        strSid.append(Integer.toString(sid[0]));

        // The count of sub-authorities
        final int subAuthoritiesCount = sid[1] & 0xFF;

        // 6 bytes of identifier authority (big-endian)
        long authority = 0;
        final int firstByteIndex = 2;
        final int lastByteIndex = 7;
        for (int i = firstByteIndex; i <= lastByteIndex; i++) {
            authority |= (long) sid[i] << 8 * (lastByteIndex - i);
        }
        strSid.append("-");
        strSid.append(authority);

        // Sub authorities (little-endian)
        int offset = 8;
        final int authSize = 4; // 4 bytes for each sub auth
        for (int j = 0; j < subAuthoritiesCount; j++) {
            long subAuthority = 0;
            for (int k = 0; k < authSize; k++) {
                subAuthority |= (long) (sid[offset + k] & 0xFF) << 8 * k;
            }

            strSid.append("-");
            strSid.append(subAuthority);

            offset += authSize;
        }

        return strSid.toString();
    }
}
