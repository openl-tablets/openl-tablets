package org.openl.rules.repository.zip;

import java.lang.reflect.Field;
import java.net.URLConnection;

/**
 * @author Vladyslav Pikus
 */
class VfsURLConnection {

    private static final Field FILE_FIELD;

    private final URLConnection urlConnection;

    public VfsURLConnection(URLConnection urlConnection) {
        this.urlConnection = urlConnection;
    }

    public VfsFile getContent() {
        try {
            return new VfsFile(FILE_FIELD.get(urlConnection));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to get field value of VFS URL Connection", e);
        }
    }

    static {
        ClassLoader classLoader = VfsFile.class.getClassLoader();
        try {
            Class<?> virtualFile = classLoader.loadClass("org.jboss.vfs.protocol.VirtualFileURLConnection");
            FILE_FIELD = virtualFile.getDeclaredField("file");
            FILE_FIELD.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new IllegalStateException("Could not detect JBoss VFS infrastructure", e);
        }
    }

}
