package org.openl.rules.webstudio.web.trace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.table.ui.filters.*;
import org.openl.rules.webstudio.web.trace.node.DTRuleTraceObject;
import org.openl.rules.webstudio.web.trace.node.DTRuleTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.DecisionTableTraceObject;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;

public class DecisionTableTraceFilterFactory {
    private static final int SELECTED_ITEM_INCREMENT_SIZE = 1;

    private final ITracerObject selectedTraceObject;
    private final IColorFilter defaultColorFilter;

    private List<IGridRegion> successfulChecks = new ArrayList<>();
    private List<IGridRegion> unsuccessfulChecks = new ArrayList<>();
    private List<IGridRegion> resultRegions = new ArrayList<>();
    private List<IGridRegion> allCheckedRegions = new ArrayList<>();
    private List<IGridRegion> successfulSelectedRegions = new ArrayList<>();
    private List<IGridRegion> unsuccessfulSelectedRegions = new ArrayList<>();
    private List<IGridRegion> indexedRegions = new ArrayList<>();

    public DecisionTableTraceFilterFactory(ITracerObject selectedTraceObject, IColorFilter defaultColorFilter) {
        this.selectedTraceObject = selectedTraceObject;
        this.defaultColorFilter = defaultColorFilter;
    }

    public IGridFilter[] createFilters() {
        ITracerObject rootTraceObject = getRoot();

        fillRegions(rootTraceObject);

        List<IGridFilter> filters = buildFilters();

        return filters.toArray(new IGridFilter[filters.size()]);
    }

    private ITracerObject getRoot() {
        ITracerObject rootTraceObject = selectedTraceObject;
        while (rootTraceObject.getParent() instanceof DecisionTableTraceObject) {
            rootTraceObject = rootTraceObject.getParent();
        }
        return rootTraceObject;
    }

    private void fillRegions(ITracerObject rootTraceObject) {
        Iterable<ITracerObject> children = rootTraceObject.getChildren();
        for (ITracerObject child : children) {
            // TODO: Remove class casting
            List<IGridRegion> regions = RegionsExtractor.getGridRegions(child);

            if (child instanceof DTRuleTraceObject) {
                DTRuleTraceObject conditionTrace = (DTRuleTraceObject) child;
                indexedRegions.addAll(regions);
                if (conditionTrace.isSuccessful()) {
                    successfulChecks.addAll(regions);
                    for (IGridRegion region : regions) {
                        if (unsuccessfulChecks.contains(region)) {
                            unsuccessfulChecks.remove(region);
                        }
                    }
                    allCheckedRegions.addAll(regions);
                } else {
                    unsuccessfulChecks.addAll(regions);
                    for (IGridRegion region : regions) {
                        if (successfulChecks.contains(region)) {
                            successfulChecks.remove(region);
                        }
                    }
                    allCheckedRegions.addAll(regions);
                }

                fillRegions(conditionTrace);
            } else if (child instanceof DTRuleTracerLeaf) {
                resultRegions.addAll(regions);
                allCheckedRegions.addAll(regions);

                successfulChecks.removeAll(regions);
                unsuccessfulChecks.removeAll(regions);
            }
        }

        List<IGridRegion> selectedRegions = RegionsExtractor.getGridRegions(selectedTraceObject);
        if (selectedRegions != null) {
            for (IGridRegion region : selectedRegions) {
                if (successfulChecks.contains(region) || resultRegions.contains(region)) {
                    successfulSelectedRegions.add(region);
                    if (unsuccessfulSelectedRegions.contains(region)) {
                        unsuccessfulSelectedRegions.remove(region);
                    }
                } else if (unsuccessfulChecks.contains(region)) {
                    unsuccessfulSelectedRegions.add(region);
                    if (successfulSelectedRegions.contains(region)) {
                        successfulSelectedRegions.remove(region);
                    }

                }
            }
        }
    }

    private List<IGridFilter> buildFilters() {
        short[] resultColor = new short[] { 0, 0xaa, 0 };

        FontGridFilter successfulFontFilter = new FontGridFilter.Builder()
            .setSelector(new RegionGridSelector(toArray(successfulChecks), false))
            .setFontColor(resultColor)
            .setItalic(true)
            .setBold(false)
            .build();
        FontGridFilter unsuccessfulFontFilter = new FontGridFilter.Builder()
            .setSelector(new RegionGridSelector(toArray(unsuccessfulChecks), false))
            .setFontColor(IColorFilter.RED)
            .setItalic(true)
            .setBold(false)
            .build();
        FontGridFilter resultFontFilter = new FontGridFilter.Builder()
            .setSelector(new RegionGridSelector(toArray(resultRegions), false))
            .setFontColor(resultColor)
            .setItalic(false)
            .setBold(true)
            .build();
        FontGridFilter indexedFontFilter = new FontGridFilter.Builder()
            .setSelector(new RegionGridSelector(toArray(indexedRegions), false))
            .setUnderlined(true)
            .build();

        CellStyleGridFilter notResultBorderFilter = new CellStyleGridFilter.Builder()
            .setSelector(new RegionGridSelector(toArray(resultRegions), true))
            .setBorderStyle(BorderStyle.DOTTED)
            .build();
        CellStyleGridFilter resultBorderFilter = new CellStyleGridFilter.Builder()
            .setSelector(new RegionGridSelector(toArray(resultRegions), false))
            .setBorderStyle(BorderStyle.THICK)
            .setBorderRGB(resultColor)
            .build();

        List<IGridFilter> filters = new ArrayList<>();
        filters.add(successfulFontFilter);
        filters.add(unsuccessfulFontFilter);
        filters.add(resultFontFilter);
        filters.add(indexedFontFilter);
        filters.add(notResultBorderFilter);
        filters.add(resultBorderFilter);
        filters.add(resultBorderFilter.createUpperRowBorderFilter());
        filters.add(resultBorderFilter.createLefterColumnBorderFilter());

        filters.add(createColorFilter(toArray(allCheckedRegions), IColorFilter.WHITE, ColorGridFilter.BACKGROUND));
        if (!successfulSelectedRegions.isEmpty()) {
            filters.add(createColorFilter(toArray(successfulSelectedRegions), resultColor, ColorGridFilter.BACKGROUND));
            FontGridFilter selectedFontFilter = new FontGridFilter.Builder()
                .setSelector(new RegionGridSelector(toArray(successfulSelectedRegions), false))
                .setIncrementSize(SELECTED_ITEM_INCREMENT_SIZE)
                .setFontColor(IColorFilter.WHITE)
                .build();
            filters.add(selectedFontFilter);
        }
        if (!unsuccessfulSelectedRegions.isEmpty()) {
            filters.add(
                createColorFilter(toArray(unsuccessfulSelectedRegions), IColorFilter.RED, ColorGridFilter.BACKGROUND));
            FontGridFilter selectedFontFilter = new FontGridFilter.Builder()
                .setSelector(new RegionGridSelector(toArray(unsuccessfulSelectedRegions), false))
                .setIncrementSize(SELECTED_ITEM_INCREMENT_SIZE)
                .setFontColor(IColorFilter.WHITE)
                .build();
            filters.add(selectedFontFilter);
        }
        filters.add(new ColorGridFilter(new RegionGridSelector(toArray(allCheckedRegions), true), defaultColorFilter));

        return filters;
    }

    private IGridRegion[] toArray(Collection<IGridRegion> regions) {
        return regions.toArray(new IGridRegion[regions.size()]);
    }

    private ColorGridFilter createColorFilter(IGridRegion[] region, final short[] rewriteColor, int scope) {
        return new ColorGridFilter(new RegionGridSelector(region, false), color -> rewriteColor, scope);
    }

}
