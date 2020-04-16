package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.binding.impl.FieldUsageSearcher;
import org.openl.binding.impl.MethodUsagesSearcher;
import org.openl.binding.impl.MethodUsagesSearcher.MethodUsage;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.NodeUsageComparator;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.CompositeSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;

/**
 * Compiles OpenL expressions from the cells and sets meta info about used methods.
 */
public class MetaInfoReaderUtils {

    public static List<CellMetaInfo> getMetaInfo(IOpenSourceCodeModule source, CompositeMethod method) {
        int startIndex = 0;
        if (source instanceof CompositeSourceCodeModule) {
            return getMetaInfoForCompositeSource(method, (CompositeSourceCodeModule) source, startIndex);
        } else {
            IOpenSourceCodeModule src = source;
            // extract original cell source
            while (src instanceof SubTextSourceCodeModule) {
                startIndex += src.getStartPosition();
                src = ((SubTextSourceCodeModule) src).getBaseModule();
            }
            if (src instanceof GridCellSourceCodeModule) {
                List<NodeUsage> nodeUsages = getNodeUsages(method, source.getCode(), startIndex);
                return Collections.singletonList(getCellMetaInfoOrNull(nodeUsages));
            }
        }

        return Collections.singletonList(null);
    }

    public static List<NodeUsage> getNodeUsages(CompositeMethod method, String sourceString, int startIndex) {
        if (method == null) {
            // Table contains errors
            return Collections.emptyList();
        }

        List<NodeUsage> nodeUsages = new ArrayList<>(
            MethodUsagesSearcher.findAllMethods(method.getMethodBodyBoundNode(), sourceString, startIndex));
        FieldUsageSearcher.findAllFields(nodeUsages, method.getMethodBodyBoundNode(), sourceString, startIndex);
        Collections.sort(nodeUsages, new NodeUsageComparator());
        return nodeUsages;
    }

    private static List<CellMetaInfo> getMetaInfoForCompositeSource(CompositeMethod method,
            CompositeSourceCodeModule source,
            int startIndex) {
        List<NodeUsage> nodeUsages = getNodeUsages(method, source.getCode(), startIndex);

        IOpenSourceCodeModule[] modules = source.getModules();
        int moduleStart = 0;
        List<CellMetaInfo> metaInfoList = new ArrayList<>();
        for (IOpenSourceCodeModule module : modules) {
            int moduleEnd = moduleStart + module.getCode().length();
            if (module instanceof GridCellSourceCodeModule) {
                // find all methods used in current cell
                List<NodeUsage> currentCellMethodUsages = new ArrayList<>();
                for (NodeUsage usage : nodeUsages) {
                    if (usage.getStart() >= moduleStart && usage.getEnd() <= moduleEnd) {
                        if (usage instanceof MethodUsage) {
                            currentCellMethodUsages.add(new MethodUsage(usage.getStart() - moduleStart,
                                usage.getEnd() - moduleStart,
                                ((MethodUsage) usage).getMethod()));
                        } else {
                            currentCellMethodUsages.add(new SimpleNodeUsage(usage.getStart() - moduleStart,
                                usage.getEnd() - moduleStart,
                                usage.getDescription(),
                                usage.getUri(),
                                usage.getNodeType()));
                        }
                    }
                }
                metaInfoList.add(getCellMetaInfoOrNull(currentCellMethodUsages));
            } else {
                metaInfoList.add(null);
            }
            moduleStart = moduleEnd + 1;
        }

        return metaInfoList;
    }

    private static CellMetaInfo getCellMetaInfoOrNull(List<NodeUsage> methodUsages) {
        if (CollectionUtils.isNotEmpty(methodUsages)) {
            return new CellMetaInfo(JavaOpenClass.STRING, false, methodUsages);
        }

        return null;
    }

}
