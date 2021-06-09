package org.openl.info;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryManagerMXBean;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SysInfo {
    public static Map<String, Object> get() {
        LinkedHashMap<String, Object> fn = new LinkedHashMap<>();
        fn.put("locale", Locale.getDefault());
        fn.put("time.now", ZonedDateTime.now().toString());
        fn.put("time.milli", Instant.now().toEpochMilli());
        fn.put("cpu", Runtime.getRuntime().availableProcessors());
        fn.put("maxMemory", Runtime.getRuntime().maxMemory());
        fn.put("totalMemory", Runtime.getRuntime().totalMemory());
        fn.put("freeMemory", Runtime.getRuntime().freeMemory());
        fn.put("os.load", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        fn.put("os.name", ManagementFactory.getOperatingSystemMXBean().getName());
        fn.put("os.version", ManagementFactory.getOperatingSystemMXBean().getVersion());
        fn.put("os.arch", ManagementFactory.getOperatingSystemMXBean().getArch());
        fn.put("vm.name", ManagementFactory.getRuntimeMXBean().getVmName());
        fn.put("vm.vendor", ManagementFactory.getRuntimeMXBean().getVmVendor());
        fn.put("vm.version", ManagementFactory.getRuntimeMXBean().getVmVersion());
        fn.put("vm.uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        fn.put("vm.startTime", ManagementFactory.getRuntimeMXBean().getStartTime());
        fn.put("thread.count", ManagementFactory.getThreadMXBean().getThreadCount());
        fn.put("thread.daemon", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
        fn.put("thread.peakCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        fn.put("thread.total", ManagementFactory.getThreadMXBean().getTotalStartedThreadCount());
        fn.put("class.loaded", ManagementFactory.getClassLoadingMXBean().getLoadedClassCount());
        fn.put("class.unloaded", ManagementFactory.getClassLoadingMXBean().getUnloadedClassCount());
        fn.put("class.total", ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
        fn.put("heapMem.init", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit());
        fn.put("heapMem.max", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax());
        fn.put("heapMem.used", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        fn.put("heapMem.committed", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted());
        fn.put("nonHeapMem.init", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getInit());
        fn.put("nonHeapMem.max", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getMax());
        fn.put("nonHeapMem.used", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
        fn.put("nonHeapMem.committed", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted());
        Supplier<Stream<GarbageCollectorMXBean>> activeGCs = () -> ManagementFactory.getGarbageCollectorMXBeans()
            .stream()
            .filter(MemoryManagerMXBean::isValid);
        fn.put("gc.count", activeGCs.get().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum());
        fn.put("gc.time", activeGCs.get().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum());

        return fn;
    }
}
