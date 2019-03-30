package org.openl.rules.types;

public final class UriMemberHelper {

    private UriMemberHelper() {
    }

    public static boolean isTheSame(IUriMember uriMember1, IUriMember uriMember2) {
        return uriMember1.getUri().equals(uriMember2.getUri());
    }
}
