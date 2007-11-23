package org.openl.rules.workspace.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class IOUtil {
    private IOUtil() {}

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1 << 16];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        out.flush();
        out.close();
    }

    public static void copy(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        out.flush();
        out.close();
    }
}
