package org.openl.rules.search;

import org.openl.util.export.IExportRow;
import org.openl.util.export.IExportSection;
import org.openl.util.export.IExportable;
import org.openl.util.export.ExportRow;
import org.openl.util.export.ExportSectionSingleRow;
import org.openl.util.export.IImporter;
import org.openl.util.export.IImportedSection;
import org.apache.commons.lang.StringUtils;

/**
 * Represents a persistable search. It is intended that instances of
 * <code>OpenLAdvancedSearch</code> are converted to instances of this class
 * to be saved say to an Excel sheet, and in the reverse case after being loaded
 * from persistent storage instances of this class are converted into
 * <code>OpenLAdvancedSearch</code> objects.
 *
 * @author Aliaksandr Antonik.
 */
public class OpenLSavedSearch implements IExportable<OpenLSavedSearch>, IExportSection<OpenLSavedSearch> {
    private static class SearchElementSection implements IExportSection<SearchConditionElement> {
        String name;
        SearchConditionElement[] searchElements;

        private SearchElementSection(String name, SearchConditionElement[] searchElements) {
            this.name = name;
            this.searchElements = searchElements;
        }

        public Class<SearchConditionElement> getExportedClass() {
            return SearchConditionElement.class;
        }

        public String getName() {
            return name;
        }

        /**
         * Return array of rows - section data. Can be <code>null</code>.
         *
         * @return section rows.
         */
        public IExportRow[] getRows() {
            IExportRow[] exportRows = new IExportRow[searchElements.length];

            for (int i = 0; i < searchElements.length; ++i) {
                SearchConditionElement searchElement = searchElements[i];
                exportRows[i] = new ExportRow(searchElement.getType(), String.valueOf(searchElement.isNotFlag()),
                        searchElement.getGroupOperator().getName(), searchElement.getOpType1(), searchElement.getElementValueName(),
                        searchElement.getOpType2(), searchElement.getElementValue());
            }

            return exportRows;
        }

        public IExportSection<SearchConditionElement>[] getSubSections() {
            return null;
        }
    }
    private SearchConditionElement[] tableElements;
    private SearchConditionElement[] columnElements;
    private String tableTypes;

    /**
     * A logical name of the saved search.
     */
    private String name;

    private static String join(String[] strings) {
        if (strings == null || strings.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(strings[0]);
        for (int i = 1; i < strings.length; ++i) {
            sb.append(",").append(strings[i]);
        }

        return sb.toString();
    }

    private static SearchConditionElement[] readSearchElementSection(IImporter importer, IImportedSection section) {
        String[][] rows = importer.readRows(section.getId());
        SearchConditionElement[] searchElements = new SearchConditionElement[rows.length];
        for (int i = 0; i < searchElements.length; i++) {
            String[] row = rows[i];
            SearchConditionElement searchElement = searchElements[i] = new SearchConditionElement(row[0]);
            searchElement.setNotFlag(Boolean.valueOf(row[1]));
            searchElement.setGroupOperator(GroupOperator.find(row[2]));
            searchElement.setOpType1(row[3]);
            searchElement.setElementValueName(row[4]);
            searchElement.setOpType2(row[5]);
            searchElement.setElementValue(row[6]);
        }
        return searchElements;
    }

    public OpenLSavedSearch() {
    }

    public OpenLSavedSearch(SearchConditionElement[] columnElements, SearchConditionElement[] tableElements, String[] tableType) {
        this.columnElements = columnElements;
        this.tableElements = tableElements;
        tableTypes = join(tableType);
    }

    public SearchConditionElement[] getColumnElements() {
        return columnElements;
    }

    /**
     * Returns the java class this section represents.
     *
     * @return a class
     */
    public Class<OpenLSavedSearch> getExportedClass() {
        return OpenLSavedSearch.class;
    }

    /**
     * Returns section name.
     *
     * @return name of this search
     */
    public String getName() {
        return StringUtils.isEmpty(name) ? "OpenL Search" : name;
    }

    /**
     * {@inheritDoc}
     */
    public IExportRow[] getRows() {
        return null;
    }

    /**
     * Returns array of subsections of this section. Can be <code>null</code>.
     *
     * @return child sections
     */
    @SuppressWarnings("unchecked")
    public IExportSection[] getSubSections() {
        return new IExportSection[] { new ExportSectionSingleRow("Table Type", new String[] { tableTypes }),
                new SearchElementSection("Table elements", tableElements),
                new SearchElementSection("Column elements", columnElements), };
    }

    public SearchConditionElement[] getTableElements() {
        return tableElements;
    }

    public String getTableTypes() {
        return tableTypes;
    }

    /**
     * {@inheritDoc}
     */
    public IExportSection<OpenLSavedSearch> mainSection() {
        return this;
    }

    public OpenLSavedSearch restore(IImporter importer) {
        IImportedSection mainSection = importer.readSections(null)[0];
        setName(mainSection.getName());
        IImportedSection[] subSections = importer.readSections(mainSection.getId());

        IImportedSection tableTypeSection = subSections[0];
        tableTypes = importer.readRows(tableTypeSection.getId())[0][0];

        tableElements = readSearchElementSection(importer, subSections[1]);
        columnElements = readSearchElementSection(importer, subSections[2]);

        return this;
    }

    public void setColumnElements(SearchConditionElement[] columnElements) {
        this.columnElements = columnElements;
    }

    /**
     * Sets the name of the search.
     *
     * @param name any string
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setTableElements(SearchConditionElement[] tableElements) {
        this.tableElements = tableElements;
    }

}
