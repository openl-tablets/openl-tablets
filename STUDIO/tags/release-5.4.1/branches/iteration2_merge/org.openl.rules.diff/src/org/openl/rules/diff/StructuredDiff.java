package org.openl.rules.diff;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.IOpenSourceCodeModule;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares two project folders, returning a list of the additions, deletions,
 * equalities and changes between them.
 *
 * <p>Compared elements are: project, folder, file, sheet, table.</p>
 *
 * <p>Each element is identified by its path, which has the following format:</p>
 *
 * <pre>
 *  1. project /tutorial1/
 *  2. folder: /tutorial1/rules/
 *  3. file: /tutorial1/rules/Tutorial_1.xls
 *  4. excel sheet: /tutorial1/rules/Tutorial_1.xls//sheetname1/
 *  5. table: /tutorial1/rules/Tutorial_1.xls//sheetname1/tablename1
 * </pre>
 *
 * Projects, folders and excel sheets unlike files and tables have a '/' at the
 * end of their path.
 *
 * @author Andrey Naumenko
 */
public class StructuredDiff {
    private static final Log log = LogFactory.getLog(StructuredDiff.class);
    private static final char SEPARATOR = '/';
    private static final String XLS_FILE_FROM_SHEET_SEPARATOR = "//";
    private static final Pattern SHEET_PATH_PATTERN = Pattern.compile(XLS_FILE_FROM_SHEET_SEPARATOR + "[^" + SEPARATOR
            + "]+/$");
    private static final int NODE_NAME_MAXLENGTH = 23;

    private static final Comparator<String> PATH_COMPARATOR = new Comparator<String>() {
        public int compare(String path1, String path2) {
            if (isFolder(path1) == isFolder(path2)) {
                return path1.compareTo(path2);
            } else {
                return (isFolder(path1) ? (-1) : 1);
            }
        }
    };

    public static final Comparator<ProjectArtefact> ARTEFACT_COMPARATOR = new Comparator<ProjectArtefact>() {
        public int compare(ProjectArtefact a1, ProjectArtefact a2) {
            if (a1.isFolder() == a2.isFolder()) {
                return a1.getName().compareTo(a2.getName());
            } else {
                return (a1.isFolder() ? (-1) : 1);
            }
        }
    };

    private static boolean isRoot(String path) {
        return ("" + SEPARATOR).equals(path);
    }

    private static boolean isFolder(String path) {
        return org.springframework.util.StringUtils.endsWithIgnoreCase(path, "" + SEPARATOR);
    }

    private static boolean isXlsFile(String path) {
        return org.springframework.util.StringUtils.endsWithIgnoreCase(path, ".xls");
    }

    private static boolean isSheet(String path) {
        if (path == null) {
            return false;
        }
        Matcher m = SHEET_PATH_PATTERN.matcher(path);
        return m.find();
    }

    private static String truncateIfNecessary(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() > NODE_NAME_MAXLENGTH) {
            s = s.substring(0, NODE_NAME_MAXLENGTH - 3) + "...";
        }
        return s;
    }

    private static String filterSeparators(String tableName) {
        tableName = tableName.replace("" + SEPARATOR, "");
        return tableName;
    }

    /**
     * <p>Returns sorted map of {sheet names => table names} for xls file.</p>
     *
     * <p> Path for xls file is expected to have the following format:
     * "/tutorial1/rules/Tutorial_1.xls".</p>
     *
     * <p>'/' symbols are removed from table names to not interfere with path
     * separators.</p>
     *
     * <p>In case of any error, empty map is returned.</p>
     *
     * @param project
     * @param xlsFilePath
     * @return
     */
    private static Map<String, TreeSet<String>> getSheetsAndTableNames(Project project, String xlsFilePath) {
        Map<String, TreeSet<String>> sheet2Tables = new TreeMap<String, TreeSet<String>>();

        ProjectArtefact a;
        try {
            a = project.getArtefactByPath(new ArtefactPathImpl(removeProjectName(xlsFilePath)));
        } catch (ProjectException e) {
            return sheet2Tables;
        }

        InputStream is;
        try {
            is = ((ProjectResource) a).getContent();
        } catch (ProjectException e) {
            log.error("", e);
            return sheet2Tables;
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("diff", "xls");
            OutputStream tempOs = new FileOutputStream(tempFile);
            FileCopyUtils.copy(is, tempOs);
        } catch (IOException e) {
            log.error("", e);
            if (tempFile != null) {
                tempFile.delete();
            }
            return sheet2Tables;
        }

        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, tempFile.getPath());

        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();

        for (TableSyntaxNode node : nodes) {
            IOpenSourceCodeModule sheet = node.getModule();
            String sheetName = ((XlsSheetSourceCodeModule) sheet).getSheetName();
            // TODO: fix case when sheetName contains '/' properly
            sheetName = filterSeparators(sheetName);

            if (sheet2Tables.get(sheetName) == null) {
                sheet2Tables.put(sheetName, new TreeSet<String>());
            }

            ILogicalTable table = node.getTable();

            String tableName = ((IGridTable) table.getGridTable()).getStringValue(0, 0);
            tableName = filterSeparators(tableName);
            sheet2Tables.get(sheetName).add(tableName);
        }

        tempFile.delete();
        return sheet2Tables;
    }

    /**
     * Returns sorted set of table names of excel sheet. Sheet path is expected
     * to have the following format:
     * "/tutorial1/rules/Tutorial_1.xls//sheetname1/".
     *
     * @param project
     * @param sheetPath
     * @return
     */
    private static TreeSet<String> getTableNames(Project project, final String sheetPath) {
        int separatorPosition = sheetPath.indexOf(XLS_FILE_FROM_SHEET_SEPARATOR);
        String xlsFilePath = sheetPath.substring(0, separatorPosition);
        String sheetName = sheetPath.substring(separatorPosition + 2);
        sheetName = sheetName.substring(0, sheetName.length() - 1);

        TreeSet<String> tableNames = getSheetsAndTableNames(project, xlsFilePath).get(sheetName);

        if (tableNames != null) {
            return tableNames;
        }

        return new TreeSet<String>();
    }

    /**
     * Returns sorted set of sheet names of excel file. Xls file path is
     * expected to have the following format: "/tutorial1/rules/Tutorial_1.xls".
     *
     * Note: TreeSet<String>() conversion is unnecessary and is left for
     * clearness.
     *
     * @param project
     * @param xlsFilePath
     * @return
     */
    private static TreeSet<String> getSheetNames(Project project, final String xlsFilePath) {
        TreeSet<String> sheetNames = new TreeSet<String>(getSheetsAndTableNames(project, xlsFilePath).keySet());
        return sheetNames;
    }

    /**
     * Adjusts path before passing it to project.getArtefactByPath() by removing
     * project name from it.
     *
     * @param path
     * @return
     */
    private static String removeProjectName(String path) {
        // remove project name
        path = path.substring(path.indexOf(SEPARATOR, 1) + 1);
        if (isFolder(path)) {
            // remove slash in the end
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static ProjectFolder getProjectFolder(Project project, String path) throws ProjectException {
        path = removeProjectName(path);

        if (path.length() == 0) {
            return project;
        }

        ProjectFolder projectFolder = (ProjectFolder) project.getArtefactByPath(new ArtefactPathImpl(path));
        return projectFolder;
    }

    private static String appendFolderSeparator(Object artefact) {
        String path = ((ProjectArtefact) artefact).getName();
        if (artefact instanceof ProjectFolder) {
            path += SEPARATOR;
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    private static TreeSet<String> getChildItems(Project project, String path) {
        TreeSet<String> items;

        if (isRoot(path)) {
            items = new TreeSet<String>();
            items.add(project.getName() + SEPARATOR);
        } else if (isSheet(path)) {
            items = getTableNames(project, path);
        } else if (isXlsFile(path)) {
            items = new TreeSet<String>(PATH_COMPARATOR);
            items.addAll(CollectionUtils.collect(getSheetNames(project, path), new Transformer() {
                public Object transform(Object obj) {
                    String sheetName = (String) obj;
                    return sheetName + SEPARATOR;
                }
            }));
        } else if (isFolder(path)) {
            items = new TreeSet<String>(PATH_COMPARATOR);

            try {
                ProjectFolder f1 = getProjectFolder(project, path);
                items.addAll(CollectionUtils.collect(f1.getArtefacts(), new Transformer() {
                    public Object transform(Object artefact) {
                        return appendFolderSeparator(artefact);
                    }
                }));
            } catch (ProjectException e) {
            }
        } else {
            items = new TreeSet<String>(PATH_COMPARATOR);
        }
        return items;
    }

    /**
     * @param project1
     * @param project2
     * @param path1
     * @param path2
     * @return
     */
    public static List<DiffElement> getDiff(Project project1, Project project2, String path1, String path2)
            throws ProjectException {
        TreeSet<String> items1 = getChildItems(project1, path1);
        TreeSet<String> items2 = getChildItems(project2, path2);

        List<DiffElement> elements = getDiffElements(project1, project2, path1, path2, items1, items2);
        return elements;
    }

    private static String concatenatePathAndName(String pathPrefix, String name) {
        String path = pathPrefix;
        if (isXlsFile(pathPrefix)) {
            path += XLS_FILE_FROM_SHEET_SEPARATOR;
        }
        path += name;
        return path;
    }

    /**
     * Creates a list that contains additions, deletions, equalities and changes
     * between childItems1 and childItems2. Based on standard merging algorithm
     * for sorted arrays.
     *
     * @param project1
     * @param project2
     * @param path1
     * @param path2
     * @param childItems1
     * @param childItems2
     * @return
     *
     * @throws ProjectException
     */
    private static List<DiffElement> getDiffElements(Project project1, Project project2, String path1, String path2,
            TreeSet<String> childItems1, TreeSet<String> childItems2) throws ProjectException {
        List<DiffElement> result = new ArrayList<DiffElement>();
        List<String> list1 = new ArrayList<String>(childItems1);
        List<String> list2 = new ArrayList<String>(childItems2);
        int i1 = 0;
        int i2 = 0;
        int n1 = list1.size();
        int n2 = list2.size();

        while ((i1 < n1) || (i2 < n2)) {
            if (i2 == n2) {
                DiffElement de = buildDiffElement(path1, list1.get(i1), DiffType.Addition);
                result.add(de);
                i1++;
            } else if (i1 == n1) {
                DiffElement de = buildDiffElement(path1, list2.get(i2), DiffType.Deletion);
                result.add(de);
                i2++;
            } else {
                String name1 = list1.get(i1);
                String name2 = list2.get(i2);
                int v = PATH_COMPARATOR.compare(name1, name2);

                String fullPath1 = concatenatePathAndName(path1, name1);
                String fullPath2 = concatenatePathAndName(path2, name2);
                if (v < 0) { // name1 exists in list1, but not in list2
                    DiffElement de = buildDiffElement(path1, name1, DiffType.Addition);
                    result.add(de);
                    i1++;
                } else if (v > 0) { // name2 exists in list2, but not in list1
                    DiffElement de = buildDiffElement(path1, name2, DiffType.Deletion);
                    result.add(de);
                    i2++;
                } else { // name1 equals to name2
                    DiffElement de = buildDiffElement(path1, name1, DiffType.Equal);
                    if (isSheet(fullPath1)) {
                        DiffType diffType = compareSheets(project1, project2, fullPath1, fullPath2);
                        de.setDiffType(diffType);
                    } else if (isXlsFile(fullPath1)) {
                        DiffType diffType = compareXlsFiles(project1, project2, fullPath1, fullPath2);
                        de.setDiffType(diffType);
                    } else if (isFolder(fullPath1)) {
                        DiffType diffType = compareFolders(project1, project2, fullPath1, fullPath2);
                        de.setDiffType(diffType);
                    }
                    result.add(de);
                    i1++;
                    i2++;
                }
            }
        }
        return result;
    }

    /**
     * Compares two XLS files.
     *
     * @param project1
     * @param project2
     * @param path1
     * @param path2
     *
     * @return
     */
    private static DiffType compareXlsFiles(Project project1, Project project2, String filePath1, String filePath2) {
        Map<String, TreeSet<String>> sheet2Tables1 = getSheetsAndTableNames(project1, filePath1);
        Map<String, TreeSet<String>> sheet2Tables2 = getSheetsAndTableNames(project2, filePath2);

        boolean equal = sheet2Tables1.equals(sheet2Tables2);
        return equal ? DiffType.Equal : DiffType.EqualWithDifferentChildren;
    }

    private static DiffType compareSheets(Project project1, Project project2, String sheetPath1, String sheetPath2) {
        TreeSet<String> tableNames1 = getTableNames(project1, sheetPath1);
        TreeSet<String> tableNames2 = getTableNames(project2, sheetPath2);

        boolean equal = tableNames1.equals(tableNames2);
        return equal ? DiffType.Equal : DiffType.EqualWithDifferentChildren;
    }

    private static DiffType compareFiles(Project project1, Project project2, String filePath1, String filePath2) {
        if (isXlsFile(filePath1)) {
            return compareXlsFiles(project1, project2, filePath1, filePath2);
        }
        // TODO: implement file comparison by content
        return DiffType.Equal;
    }

    private static TreeSet<ProjectArtefact> getChildArtefacts(Project project, String path) throws ProjectException {
        TreeSet<ProjectArtefact> artefacts = new TreeSet<ProjectArtefact>(ARTEFACT_COMPARATOR);
        if (isRoot(path)) {
            artefacts.add(project);
        } else {
            ProjectFolder folder = getProjectFolder(project, path);

            for (ProjectArtefact a : folder.getArtefacts()) {
                artefacts.add(a);
            }
        }
        return artefacts;
    }

    /**
     * Compares two folders.
     *
     * @param project1
     * @param project2
     * @param path1
     * @param path2
     *
     * @return
     *
     * @throws ProjectException
     */
    private static DiffType compareFolders(Project project1, Project project2, String path1, String path2)
            throws ProjectException {

        Set<ProjectArtefact> artefacts1 = getChildArtefacts(project1, path1);
        Set<ProjectArtefact> artefacts2 = getChildArtefacts(project2, path2);

        if (!artefacts1.equals(artefacts2)) {
            return DiffType.EqualWithDifferentChildren;
        }

        // artefacts1 and artefacts2 are equal relative to ARTEFACT_COMPARATOR

        Iterator<ProjectArtefact> it1 = artefacts1.iterator();
        Iterator<ProjectArtefact> it2 = artefacts2.iterator();

        DiffType diffType = DiffType.Equal;
        while ((diffType == DiffType.Equal) && it1.hasNext()) {
            ProjectArtefact a1 = it1.next();
            ProjectArtefact a2 = it2.next();

            String artefactPath1 = a1.getArtefactPath().getStringValue();
            String artefactPath2 = a2.getArtefactPath().getStringValue();

            if (a1.isFolder()) {
                diffType = compareFolders(project1, project2, artefactPath1 + SEPARATOR, artefactPath2 + SEPARATOR);
            } else {
                diffType = compareFiles(project1, project2, artefactPath1, artefactPath2);
            }
        }

        return diffType;
    }

    private static DiffElement buildDiffElement(String pathPrefix, String name, DiffType diffType) {
        DiffElement de = new DiffElement();
        de.setPath(concatenatePathAndName(pathPrefix, name));
        de.setDiffType(diffType);

        if (isFolder(name)) {
            if (isXlsFile(pathPrefix)) {
                de.setIsSheet(true);
            }
            de.setIsFolder(true);
        } else if (isXlsFile(name)) {
            de.setIsXlsFile(true);
        }

        if (isFolder(name)) {
            name = name.substring(0, name.length() - 1);
        }
        name = name.substring(name.lastIndexOf(SEPARATOR) + 1);

        de.setTooltip(name);
        de.setName(truncateIfNecessary(name));
        return de;
    }
}
