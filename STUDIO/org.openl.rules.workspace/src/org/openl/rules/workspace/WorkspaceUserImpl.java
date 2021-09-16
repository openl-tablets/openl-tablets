package org.openl.rules.workspace;

import java.util.Optional;
import java.util.function.Function;

import org.openl.rules.repository.api.UserInfo;

/**
 *
 * @author Aleh Bykhavets
 *
 */
public class WorkspaceUserImpl implements WorkspaceUser {

    private final String userId;
    private final String userName;
    private final Function<String, UserInfo> userInfoCollector;

    public WorkspaceUserImpl(String userName, Function<String, UserInfo> userInfoCollector) {
        userId = generateUserId(userName);
        this.userName = userName;
        this.userInfoCollector = userInfoCollector;
    }

    /**
     * Compare two users.
     * <p/>
     * Note: comparison is based on name of users, not IDs.
     */
    @Override
    public int compareTo(WorkspaceUser o) {
        return userName.compareTo(o.getUserName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof WorkspaceUser) {
            return 0 == compareTo((WorkspaceUser) obj);
        } else {
            return false;
        }
    }

    /**
     * Generates system safe user id.
     */
    protected String generateUserId(String s) {
        StringBuilder sb = new StringBuilder(32);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            } else {
                // replace non letter or digit char with "(<hex>)"
                sb.append('(');
                sb.append(Integer.toHexString(c));
                sb.append(')');
            }
        }

        return sb.toString();
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public UserInfo getUserInfo() {
        return Optional.ofNullable(userInfoCollector.apply(userName)).orElse(new UserInfo(userName));
    }

    // --- protected

    @Override
    public int hashCode() {
        return userName.hashCode();
    }

}
