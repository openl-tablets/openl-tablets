/**
 * Created Jan 25, 2007
 */
package org.openl.rules.ui;

import org.openl.base.INamedThing;
import org.openl.base.NamedThing;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public abstract class WebStudioMode extends NamedThing {

    static public abstract class BusinessMode extends WebStudioMode {

        static String[][] folders = { { "By Type", "Organize Project by component type", "" }, };

        // static ATableTreeSorter[][] sorters = { {
        // ATableTreeSorter.TABLE_TYPE_SORTER, new TableInstanceSorter() }, };

        @Override
        public String getDisplayName(OpenLWrapperInfo wrapper) {
            String dname = wrapper.getDisplayName();
            if (dname.equals(wrapper.getWrapperClassName())) {
                dname = StringTool.lastToken(dname, ".");
            }
            return dname;
        }

        @Override
        public String[][] getFolders() {
            return null;
        }

        @Override
        public String getTableMode() {
            return IXlsTableNames.VIEW_BUSINESS;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.rules.ui.WebStudioMode#getType()
         */
        @Override
        public Object getType() {
            return BUSINESS_MODE;
        }

        @Override
        public boolean select(TableSyntaxNode tsn) {
            String view = null;
            String name = null;
            TableProperties tp = tsn.getTableProperties();
            if (tp != null) {
                view = tp.getPropertyValue("view");
                name = tp.getPropertyValue("name");
            }

            return name != null && (view == null || view.indexOf(IXlsTableNames.VIEW_BUSINESS) >= 0);
        }

        @Override
        public boolean showTableGrid() {
            return false;
        }

    }

    static public class BusinessMode1 extends BusinessMode {

        static ATableTreeSorter[][] sorters = { { new CategorySorter(), new TableInstanceSorter() }, };

        BusinessMode1() {
            setName(BUSINESS_MODE + ".1");
            displayName = "Business View 1. Provides categorized view";
        }

        @Override
        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }

    }

    static public class BusinessMode2 extends BusinessMode {

        static ATableTreeSorter[][] sorters = { { new CategorySorterN(0, "-"), new CategorySorterN(1, "-"),
                new TableInstanceSorter() }, };

        BusinessMode2() {
            setName(BUSINESS_MODE + ".2");
            displayName = "Business View 2. Provides more detailed categorized view";
        }

        @Override
        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }

    }

    static public class BusinessMode3 extends BusinessMode {

        static ATableTreeSorter[][] sorters = { { new CategorySorterN(1, "-"), new CategorySorterN(0, "-"),
                new TableInstanceSorter() }, };

        BusinessMode3() {
            setName(BUSINESS_MODE + ".3");
            displayName = "Business View 3. Provides inversed categorized view";
        }

        @Override
        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }

    }

    static public class DeveloperMode extends WebStudioMode {

        static ATableTreeSorter[][] sorters = { { ATableTreeSorter.TABLE_TYPE_SORTER, new TableInstanceSorter() },
                { new WorkbookSorter(), new WorksheetSorter(), new TableInstanceSorter() } };

        static String[][] folders = { { "By Type", "Organize Project by component type", "" },
                { "By File", "Organize project by physical location" } };

        DeveloperMode() {
            displayName = "Developer Mode. Provides all the technical details";
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.rules.ui.WebStudioMode#getDisplayName(org.openl.rules.ui.OpenLWrapperInfo)
         */
        @Override
        public String getDisplayName(OpenLWrapperInfo wrapper) {
            String dname = wrapper.getDisplayName();
            if (dname.equals(wrapper.getWrapperClassName())) {
                dname = StringTool.lastToken(dname, ".");
            }
            return dname + " (" + wrapper.getWrapperClassName() + ")";
        }

        @Override
        public String[][] getFolders() {
            return folders;
        }

        @Override
        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }

        @Override
        public String getTableMode() {
            return IXlsTableNames.VIEW_DEVELOPER;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.rules.ui.WebStudioMode#getType()
         */
        @Override
        public Object getType() {
            return DEVELOPER_MODE;
        }

        @Override
        public boolean select(TableSyntaxNode tsn) {
            return true;
        }

        @Override
        public boolean showTableGrid() {
            return false;
        }

    }

    static final public String DEVELOPER_MODE = "developer", BUSINESS_MODE = "business";

    static final public WebStudioMode DEVELOPER = new DeveloperMode();

    static final public WebStudioMode BUSINESS1 = new BusinessMode1();

    static final public WebStudioMode BUSINESS2 = new BusinessMode2();

    static final public WebStudioMode BUSINESS3 = new BusinessMode3();

    String name, displayName, description;

    @Override
    public String getDisplayName(int mode) {
        switch (mode) {
            case INamedThing.SHORT:
                return getName();
            case INamedThing.REGULAR:
                return displayName;
            case INamedThing.LONG:
                return description;
        }
        return null;
    }

    public abstract String getDisplayName(OpenLWrapperInfo wrapper);

    public abstract String[][] getFolders();

    public abstract ATableTreeSorter[][] getSorters();

    public abstract String getTableMode();

    /**
     * @return mode type
     */
    public abstract Object getType();
    public abstract boolean select(TableSyntaxNode tsn);

    public abstract boolean showTableGrid();

}
