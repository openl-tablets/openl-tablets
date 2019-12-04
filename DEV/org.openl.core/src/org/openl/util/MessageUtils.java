package org.openl.util;

public class MessageUtils {

    public static String getTypeNotFoundMessage(String typeName) {
        return String.format("Type '%s' is not found.", typeName);
    }

    public static String getConstructorNotFoundMessage(String constructorSignature) {
        return String.format("Constructor '%s' is not found.", constructorSignature);
    }

    public static String getTypeDefinedErrorMessage(String typeName) {
        return String.format("Type '%s' is defined with errors.", typeName);
    }

}
