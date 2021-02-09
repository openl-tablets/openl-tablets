package org.openl.rules.repository.zip;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Decorator for JBoss VFS infrastructure
 *
 * @author Vladyslav Pikus
 */
class VfsFile {

    private static final Method GET_NAME_METHOD;
    private static final Method GET_PHYSICAL_FILE_METHOD;

    private final Object vfsFile;

    public VfsFile(Object vfsFile) {
        this.vfsFile = Objects.requireNonNull(vfsFile);
    }

    /**
     * Get original file name
     *
     * @return file name
     * @throws IOException
     */
    public String getName() throws IOException {
        return (String) invokeMethod(GET_NAME_METHOD);
    }

    /**
     * Get file location in file system
     *
     * @return file
     * @throws IOException
     */
    public File getFile() throws IOException {
        return (File) invokeMethod(GET_PHYSICAL_FILE_METHOD);
    }

    private Object invokeMethod(Method method, Object... arg) throws IOException {
        try {
            return method.invoke(vfsFile, arg);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Must be never thrown", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IllegalStateException("Failed to invoke VFS file method", e);
        }
    }

    static {
        ClassLoader classLoader = VfsFile.class.getClassLoader();
        try {
            Class<?> virtualFile = classLoader.loadClass("org.jboss.vfs.VirtualFile");
            GET_NAME_METHOD = virtualFile.getMethod("getName");
            GET_PHYSICAL_FILE_METHOD = virtualFile.getMethod("getPhysicalFile");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException("Could not detect JBoss VFS infrastructure", e);
        }
    }


}
