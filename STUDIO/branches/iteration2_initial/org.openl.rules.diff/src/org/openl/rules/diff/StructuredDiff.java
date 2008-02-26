package org.openl.rules.diff;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
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


/**
 * Compares two project folders, returning a list of the additions, deletions  and
 * changes between them.
 *
 * @author Andrey Naumenko
 */
public class StructuredDiff {
    private static final Log log = LogFactory.getLog(StructuredDiff.class);
    private static final String SEPARATOR = "/";
    private static final String XLS_INNER_STRUCTURE_SEPARATOR = "//";
    private static final Comparator<String> pathComparator = new Comparator<String>() {
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

    /**
     * Returns map of {sheets => table names} for the given xls file. Path is
     * expected to have be given in the following format
     * "/tutorial1/rules/Tutorial_1.xls".
     *
     * @param project
     * @param xlsFilePath
     * @param project2 TODO
     * @param xlsFilePath2 TODO
     *
     * @return
     */
    private static Map<String, TreeSet<String>> getSheetsAndTables(Project project,
        String xlsFilePath, Project project2, String xlsFilePath2) {
        Map<String, TreeSet<String>> sheet2Tables = new TreeMap<String, TreeSet<String>>();

        ProjectArtefact a;
        try {
            a = project.getArtefactByPath(new ArtefactPathImpl(removeProjectName(
                            xlsFilePath)));
        } catch (ProjectException e) {
            if (project2 == null) {
                return sheet2Tables;
            }
            try {
                a = project2.getArtefactByPath(new ArtefactPathImpl(removeProjectName(
                                xlsFilePath2)));
            } catch (ProjectException e1) {
                return sheet2Tables;
            }
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

        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(),
                ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls",
                ucxt, tempFile.getPath());

        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        TableSyntaxNode[] nodes = xsn.getXlsTableSyntaxNodes();

        for (TableSyntaxNode node : nodes) {
            IOpenSourceCodeModule sheet = node.getModule();
            String sheetName = ((XlsSheetSourceCodeModule) sheet).getSheetName();

            if (sheet2Tables.get(sheetName) == null) {
                sheet2Tables.put(sheetName, new TreeSet<String>());
            }

            ILogicalTable table = node.getTable();

            String tableName = ((IGridTable) table.getGridTable()).getStringValue(0, 0);
            tableName = StringUtils.substring(tableName, 0, 20);
            tableName = tableName.replace("/", "");
            tableName += "...";

            sheet2Tables.get(sheetName).add(tableName);
        }

        tempFile.delete();
        return sheet2Tables;
    }

    private static boolean isSheetPath(String path) {
        return StringUtils.contains(path, XLS_INNER_STRUCTURE_SEPARATOR);
    }

    private static boolean isXlsFile(String path) {
        return org.springframework.util.StringUtils.endsWithIgnoreCase(path, ".xls");
    }

    private static boolean isFolder(String path) {
        return org.springframework.util.StringUtils.endsWithIgnoreCase(path, SEPARATOR);
    }

    /**
     * Returns table paths of the given sheet of xls file. Path is expected to
     * have the following format "/tutorial1/rules/Tutorial_1.xls//Intro".
     *
     * @param project
     * @param sheetPath
     * @param project2 TODO
     * @param sheetPath2 TODO
     *
     * @return
     */
    private static TreeSet<String> getTablePaths(Project project, final String sheetPath,
        Project project2, String sheetPath2) {
        int separatorPosition = sheetPath.indexOf(XLS_INNER_STRUCTURE_SEPARATOR);
        String xlsFilePath = sheetPath.substring(0, separatorPosition);
        String sheetName = sheetPath.substring(separatorPosition + 2);
        if (isFolder(sheetPath)) {
            sheetName = sheetName.substring(0, sheetName.length() - 1);
        }

        TreeSet<String> tableNames = getSheetsAndTables(project, xlsFilePath, project2,
                xlsFilePath).get(sheetName);

        if (tableNames == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        TreeSet<String> transformedTableNames = new TreeSet<String>(CollectionUtils
                    .collect(tableNames,
                        new Transformer() {
                        public Object transform(Object obj) {
                            String tableName = (String) obj;
                            return sheetPath + SEPARATOR + tableName;
                        }
                    }));
        return transformedTableNames;
    }

    /**
     * Returns sheet paths for the given xls file. Path is expected to have the
     * following format "/tutorial1/rules/Tutorial_1.xls".
     *
     * @param project
     * @param xlsFilePath
     * @param project2 TODO
     * @param xlsFilePath2 TODO
     *
     * @return
     */
    private static TreeSet<String> getSheetPaths(Project project,
        final String xlsFilePath, Project project2, String xlsFilePath2) {
        TreeSet<String> sheetNames = new TreeSet<String>(getSheetsAndTables(project,
                    xlsFilePath, project2, xlsFilePath2).keySet());

        @SuppressWarnings("unchecked")
        TreeSet<String> transformedSheetNames = new TreeSet<String>(CollectionUtils
                    .collect(sheetNames,
                        new Transformer() {
                        public Object transform(Object obj) {
                            String sheetName = (String) obj;
                            return xlsFilePath + XLS_INNER_STRUCTURE_SEPARATOR
                                + sheetName + SEPARATOR;
                        }
                    }));
        return transformedSheetNames;
    }

    private static String getArtefactPathForSorting(Object artefact) {
        String path = ((ProjectArtefact) artefact).getArtefactPath().getStringValue();
        if (artefact instanceof ProjectFolder) {
            path += SEPARATOR;
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    public static List<DiffElement> getDiff(Project p1, Project p2, String path1,
        String path2) throws ProjectException
    {
        TreeSet<String> items1;
        TreeSet<String> items2;

        if (isSheetPath(path1)) {
            items1 = getTablePaths(p1, path1, p2, path2);
            items2 = getTablePaths(p2, path2, p1, path1);
        } else if (isXlsFile(path1)) {
            items1 = getSheetPaths(p1, path1, p2, path2);
            items2 = getSheetPaths(p2, path2, p1, path1);
        } else {
            items1 = new TreeSet<String>(pathComparator);

            try {
                ProjectFolder f1 = getProjectFolder(p1, path1);
                items1.addAll(CollectionUtils.collect(f1.getArtefacts(),
                        new Transformer() {
                        public Object transform(Object artefact) {
                            return getArtefactPathForSorting(artefact);
                        }
                    }));
            } catch (ProjectException e) {}

            items2 = new TreeSet<String>(pathComparator);

            try {
                ProjectFolder f2 = getProjectFolder(p2, path2);
                items2.addAll(CollectionUtils.collect(f2.getArtefacts(),
                        new Transformer() {
                        public Object transform(Object artefact) {
                            return getArtefactPathForSorting(artefact);
                        }
                    }));
            } catch (ProjectException e) {}
        }

        List<DiffElement> elements = getDiffElements(p1, p2, items1, items2,
                pathComparator);
        return elements;
    }

    private static ProjectFolder getProjectFolder(Project project, String path)
        throws ProjectException
    {
        if (StringUtils.isEmpty(path)) {
            return project;
        }

        path = removeProjectName(path);
        ProjectFolder projectFolder = (ProjectFolder) project.getArtefactByPath(new ArtefactPathImpl(
                    path));

        return projectFolder;
    }

    private static String removeProjectName(String path) {
        path = path.substring(path.indexOf(SEPARATOR, 1) + 1);
        if (isFolder(path)) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Creates a list that contains differences and similarities between
     * f1.getArtefacts() and f2.getArtefacts(). Based on standard merging algorithm for
     * sorted arrays.
     *
     * @param project1
     * @param project2
     * @param items1
     * @param items2
     * @param itemsComparator
     *
     * @return
     *
     * @throws ProjectException
     */
    private static List<DiffElement> getDiffElements(Project project1, Project project2,
        TreeSet<String> items1, TreeSet<String> items2, Comparator<String> itemsComparator)
        throws ProjectException
    {
        List<DiffElement> result = new ArrayList<DiffElement>();
        List<String> list1 = new ArrayList<String>(items1);
        List<String> list2 = new ArrayList<String>(items2);
        int i1 = 0;
        int i2 = 0;
        int n1 = list1.size();
        int n2 = list2.size();

        while ((i1 < n1) || (i2 < n2)) {
            if (i2 == n2) {
                DiffElement de = buildDiffElement(list1.get(i1), DiffType.Addition);
                result.add(de);
                i1++;
            } else if (i1 == n1) {
                DiffElement de = buildDiffElement(list2.get(i2), DiffType.Deletion);
                result.add(de);
                i2++;
            } else {
                String path1 = list1.get(i1);
                String path2 = list2.get(i2);
                int v = itemsComparator.compare(path1, path2);
                if (v < 0) {
                    DiffElement de = buildDiffElement(path1, DiffType.Addition);
                    result.add(de);
                    i1++;
                } else if (v > 0) {
                    DiffElement de = buildDiffElement(path2, DiffType.Deletion);
                    result.add(de);
                    i2++;
                } else {
                    DiffElement de = buildDiffElement(path1, null);
                    if (!isFolder(path1) && !isFolder(path2)) {
                        DiffType diffType = compareFiles(project1, project2, path1, path2);
                        de.setDiffType(diffType);
                    } else {
                        DiffType diffType = compareFolders(project1, project2, path1,
                                path2);
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

    private static DiffElement buildDiffElement(String path, DiffType diffType) {
        DiffElement de = new DiffElement();
        de.setDiffType(diffType);

        if (isFolder(path)) {
            if (isSheetPath(path)) {
                de.setIsSheet(true);
            }
            de.setIsFolder(true);
            path = path.substring(0, path.length() - 1);
        } else if (isXlsFile(path)) {
            de.setIsXlsFile(true);
        }

        de.setPath(path);
        de.setName(path.substring(path.lastIndexOf(SEPARATOR) + 1));

        return de;
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
    private static DiffType compareFiles(Project project1, Project project2,
        String path1, String path2) {
        if (!isXlsFile(path1)) {
            return DiffType.Equal;
        }

        Map<String, TreeSet<String>> sheet2Tables1 = getSheetsAndTables(project1, path1,
                null, null);
        Map<String, TreeSet<String>> sheet2Tables2 = getSheetsAndTables(project2, path2,
                null, null);

        boolean equal = sheet2Tables1.equals(sheet2Tables2);
        return equal ? DiffType.Equal : DiffType.EqualWithDifferentChildren;
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
    private static DiffType compareFolders(Project project1, Project project2,
        String path1, String path2) throws ProjectException
    {
        if (isSheetPath(path1)) {
            TreeSet<String> tableNames1 = getTablePaths(project1, path1, null, null);
            TreeSet<String> tableNames2 = getTablePaths(project2, path2, null, null);

            if (tableNames1 == null) {
                return DiffType.Deletion;
            } else if (tableNames2 == null) {
                return DiffType.Addition;
            } else {
                boolean equal = tableNames1.equals(tableNames2);
                return equal ? DiffType.Equal : DiffType.EqualWithDifferentChildren;
            }
        }

        Set<ProjectArtefact> s1 = new TreeSet<ProjectArtefact>(ARTEFACT_COMPARATOR);
        Set<ProjectArtefact> s2 = new TreeSet<ProjectArtefact>(ARTEFACT_COMPARATOR);

        ProjectFolder f1 = getProjectFolder(project1, path1);
        ProjectFolder f2 = getProjectFolder(project2, path2);

        for (ProjectArtefact a : f1.getArtefacts()) {
            s1.add(a);
        }

        for (ProjectArtefact a : f2.getArtefacts()) {
            s2.add(a);
        }

        if (!s1.equals(s2)) {
            return DiffType.EqualWithDifferentChildren;
        }

        Iterator<ProjectArtefact> it1 = s1.iterator();
        Iterator<ProjectArtefact> it2 = s2.iterator();

        DiffType diffType = DiffType.Equal;
        while ((diffType == DiffType.Equal) && it1.hasNext()) {
            ProjectArtefact a1 = it1.next();
            ProjectArtefact a2 = it2.next();

            if (a1.isFolder()) {
                diffType = compareFolders(project1, project2,
                        a1.getArtefactPath().getStringValue(),
                        a2.getArtefactPath().getStringValue());
            } else {
                diffType = compareFiles(project1, project2,
                        a1.getArtefactPath().getStringValue(),
                        a2.getArtefactPath().getStringValue());
            }
        }

        return diffType;
    }
}
