package org.openl.rules.workspace;

import java.util.Optional;
import java.util.function.Function;

import org.openl.rules.repository.api.UserInfo;

/**
 * @author Aleh Bykhavets
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

        if (obj instanceof WorkspaceUser user) {
            return 0 == compareTo(user);
        } else {
            return false;
        }
    }

    /**
     * Generates system safe user id.
     */
    protected String generateUserId(String s) {
        var sb = new StringBuilder(32);

        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);

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

    /**
     * Restores the user name a system-safe user id was generated from — the inverse of
     * {@link #generateUserId(String)}. The id doubles as the user's workspace folder name, so this is
     * how a folder observed on disk leads back to its user.
     *
     * <p>An input this class could not have generated — an unmatched or empty {@code (hex)} escape —
     * is returned unchanged: callers feed arbitrary folder names, and a malformed one simply is not
     * an encoded user id.
     */
    public static String decodeUserId(String userId) {
        var sb = new StringBuilder(userId.length());
        var i = 0;
        while (i < userId.length()) {
            var c = userId.charAt(i);
            if (c == '(') {
                var end = userId.indexOf(')', i);
                if (end <= i + 1) {
                    return userId;
                }
                try {
                    sb.append((char) Integer.parseInt(userId, i + 1, end, 16));
                } catch (NumberFormatException e) {
                    return userId;
                }
                i = end + 1;
            } else {
                sb.append(c);
                i++;
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

    @Override
    public UserInfo getUserInfo() {
        return Optional.ofNullable(userInfoCollector.apply(userName)).orElse(new UserInfo(userName));
    }

    // --- protected

    @Override
    public int hashCode() {
        return userName.hashCode();
    }

}
