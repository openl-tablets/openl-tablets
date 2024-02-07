package org.openl.info;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
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
        log("System info:");
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
            log("     ENV : {} CPU    Memory : Max={}   Committed={}   Used={}",
                    Integer.toString(runtime.availableProcessors()),
                    toMiB(runtime.maxMemory()),
                    toMiB(runtime.totalMemory()),
                    toMiB(runtime.totalMemory() - runtime.freeMemory()));
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

    public void memStat() {
        try {
            System.gc();
            Thread.sleep(100); // Wait GC performing
            var runtime = Runtime.getRuntime();
            log("Memory : Max={}   Committed={}   Used={}",
                    toMiB(runtime.maxMemory()),
                    toMiB(runtime.totalMemory()),
                    toMiB(runtime.totalMemory() - runtime.freeMemory()));

        } catch (Exception ignored) {
            log("##### Cannot access to the Runtime environment");
        }

        try {
            var memPools = ManagementFactory.getMemoryPoolMXBeans();
            memPools.sort(Comparator.comparing(MemoryPoolMXBean::getName));
            log("----- Memory Usage in MiB -----     Init        Used    peak     Committed  peak         Max  ----- Type -----");
            for (var pool : memPools) {
                var usage = pool.getUsage();
                if (usage == null) {
                    continue;
                }
                var peak = pool.getPeakUsage();
                peak = peak == null ? new MemoryUsage(0, 0, 0, 0) : peak;
                log("{} {}     {} {}     {} {}     {}   {}",
                        String.format("%-32s", pool.getName()),
                        toMiBAlign(usage.getInit()),
                        toMiBAlign(usage.getUsed()),
                        toMiBAlign(peak.getUsed()),
                        toMiBAlign(usage.getCommitted()),
                        toMiBAlign(peak.getCommitted()),
                        toMiBAlign(usage.getMax()),
                        String.format("%15s", pool.getType()));
            }
        } catch (Exception ignored) {
            log("##### Cannot access to the Runtime environment");
        }
    }

    private String toMiB(long bytes) {
        return String.format("%.1f MiB", bytes / 1024.0 / 1024.0);
    }

    private String toMiBAlign(long bytes) {
        if (bytes < 0) {
            return "       ";  // 7 spaces according to the bellow format
        } else if (bytes == 0) {
            return "     0 ";
        }
        return String.format("%7.1f", bytes / 1024.0 / 1024.0);
    }
}
