package org.openl.info;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class SysInfoLogger extends OpenLLogger {
    @Override
    protected String getName() {
        return "sys";
    }

    @Override
    protected void discover() {
        log("System properties:");
        try {
            log("    Java : {} v{} ({})",
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                System.getProperty("java.class.version"));
            log("      OS : {} v{} ({})",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
        } catch (Exception ignored) {
            log("##### Cannot access to the System properties");
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            log("     ENV : {} CPU / -Xmx={}M / Allocated={} MiB / Free={} MiB ",
                Integer.toString(runtime.availableProcessors()),
                toMiB(runtime.maxMemory()),
                toMiB(runtime.totalMemory()),
                toMiB(runtime.freeMemory()));
        } catch (Exception ignored) {
            log("##### Cannot access to the Runtime environment");
        }
        try {
            log("    Time : {} ({} - {})",
                new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss.SSS XXX (z)").format(new Date()),
                TimeZone.getDefault().getID(),
                TimeZone.getDefault().getDisplayName());
            log("  Locale : {}", Locale.getDefault());
        } catch (Exception ignored) {
            log("##### Cannot access to the TimeZone or Locale");
        }
        try {
            log("Work dir : {}", Paths.get("").toAbsolutePath());
        } catch (Exception ignored) {
            log("##### Cannot access to the FileSystem");
        }
        try {
            log("App path : {}", OpenLVersion.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        } catch (Exception ignored) {
            log("##### Cannot access to the Application location");
        }
    }

    private String toMiB(long bytes) {
        return String.valueOf(bytes / 262144 / 4.0);
    }
}
