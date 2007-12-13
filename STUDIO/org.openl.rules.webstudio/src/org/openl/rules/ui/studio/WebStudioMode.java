package org.openl.rules.ui.studio;

import org.openl.base.INamedThing;
import org.openl.base.NamedThing;

import org.openl.rules.dt.IDecisionTableConstants;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.ATableTreeSorter;
import org.openl.rules.ui.CategorySorter;
import org.openl.rules.ui.CategorySorterN;
import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.rules.ui.TableInstanceSorter;
import org.openl.rules.ui.WorkbookSorter;
import org.openl.rules.ui.WorksheetSorter;

import org.openl.util.StringTool;


/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public abstract class WebStudioMode extends NamedThing {
    public static final String DEVELOPER_MODE = "developer";
    public static final String BUSINESS_MODE = "business";
    public static final WebStudioMode DEVELOPER = new DeveloperMode();
    public static final WebStudioMode BUSINESS1 = new BusinessMode1();
    public static final WebStudioMode BUSINESS2 = new BusinessMode2();
    public static final WebStudioMode BUSINESS3 = new BusinessMode3();
    String name;
    String displayName;
    String description;

    public abstract boolean select(TableSyntaxNode tsn);

    public abstract ATableTreeSorter[][] getSorters();

    public abstract String[][] getFolders();

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

    public abstract String getTableMode();

    public abstract boolean showTableGrid();

    public abstract String getDisplayName(OpenLWrapperInfo wrapper);

    /**
     * DOCUMENT ME!
     *
     * @return mode type
     */
    public abstract Object getType();

    public static class DeveloperMode extends WebStudioMode {
        static ATableTreeSorter[][] sorters = {
                { ATableTreeSorter.TABLE_TYPE_SORTER, new TableInstanceSorter() },
                { new WorkbookSorter(), new WorksheetSorter(), new TableInstanceSorter() }
            };
        static String[][] folders = {
                { "By Type", "Organize Project by component type", "" },
                { "By File", "Organize project by physical location" }
            };

        DeveloperMode() {
            displayName = "Developer Mode. Provides all the technical details";
        }

        public boolean select(TableSyntaxNode tsn) {
            return true;
        }

        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }

        public String[][] getFolders() {
            return folders;
        }

        public String getTableMode() {
            return IDecisionTableConstants.VIEW_DEVELOPER;
        }

        public boolean showTableGrid() {
            return false;
        }

        /* (non-Javadoc)
         * @see org.openl.rules.ui.WebStudioMode#getDisplayName(org.openl.rules.ui.OpenLWrapperInfo)
         */
        public String getDisplayName(OpenLWrapperInfo wrapper) {
            String dname = wrapper.getDisplayName();
            if (dname.equals(wrapper.getWrapperClassName())) {
                dname = StringTool.lastToken(dname, ".");
            }
            return dname + " (" + wrapper.getWrapperClassName() + ")";
        }

        /* (non-Javadoc)
         * @see org.openl.rules.ui.WebStudioMode#getType()
         */
        public Object getType() {
            return DEVELOPER_MODE;
        }
    }

    public static class BusinessMode1 extends BusinessMode {
        static ATableTreeSorter[][] sorters = {
                { new CategorySorter(), new TableInstanceSorter() },
            };

        BusinessMode1() {
            setName(BUSINESS_MODE + ".1");
            displayName = "Business View 1. Provides categorized view";
        }

        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }
    }

    public static class BusinessMode2 extends BusinessMode {
        static ATableTreeSorter[][] sorters = {
                {
                    new CategorySorterN(0, "-"), new CategorySorterN(1, "-"),
                    new TableInstanceSorter()
                },
            };

        BusinessMode2() {
            setName(BUSINESS_MODE + ".2");
            displayName = "Business View 2. Provides more detailed categorized view";
        }

        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }
    }

    public static class BusinessMode3 extends BusinessMode {
        static ATableTreeSorter[][] sorters = {
                {
                    new CategorySorterN(1, "-"), new CategorySorterN(0, "-"),
                    new TableInstanceSorter()
                },
            };

        BusinessMode3() {
            setName(BUSINESS_MODE + ".3");
            displayName = "Business View 3. Provides inversed categorized view";
        }

        public ATableTreeSorter[][] getSorters() {
            return sorters;
        }
    }

    public static abstract class BusinessMode extends WebStudioMode {
//		static ATableTreeSorter[][] sorters = { {
//				ATableTreeSorter.TABLE_TYPE_SORTER, new TableInstanceSorter() }, };
        static String[][] folders = {
                { "By Type", "Organize Project by component type", "" },
            };

        public boolean select(TableSyntaxNode tsn) {
            String view = null;
            String name = null;
            TableProperties tp = tsn.getTableProperties();
            if (tp != null) {
                view = tp.getPropertyValue("view");
                name = tp.getPropertyValue("name");
            }

            return ((name != null)
                && ((view == null)
                || (view.indexOf(IDecisionTableConstants.VIEW_BUSINESS) >= 0)))
                || (tsn.getErrors() != null);
        }

        public String[][] getFolders() {
            return null;
        }

        public String getTableMode() {
            return IDecisionTableConstants.VIEW_BUSINESS;
        }

        public boolean showTableGrid() {
            return false;
        }

        public String getDisplayName(OpenLWrapperInfo wrapper) {
            String dname = wrapper.getDisplayName();
            if (dname.equals(wrapper.getWrapperClassName())) {
                dname = StringTool.lastToken(dname, ".");
            }
            return dname;
        }

        /* (non-Javadoc)
         * @see org.openl.rules.ui.WebStudioMode#getType()
         */
        public Object getType() {
            return BUSINESS_MODE;
        }
    }
}
