package org.openl.rules.types;

public class UriMemberHelper {
    public static boolean isTheSame(IUriMember uriMember1, IUriMember uriMember2) {
        if (!uriMember1.getUri().equals(uriMember2.getUri())) {
            return false;
        }
        return true;
    }
}
