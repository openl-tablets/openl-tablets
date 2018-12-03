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
            log("    Total Memory : {} bytes", runtime.totalMemory());
            log("     Free Memory : {} bytes", runtime.freeMemory());
            log("      Max Memory : {} bytes", runtime.maxMemory());
            log("      Processors : {}", runtime.availableProcessors());
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
}
