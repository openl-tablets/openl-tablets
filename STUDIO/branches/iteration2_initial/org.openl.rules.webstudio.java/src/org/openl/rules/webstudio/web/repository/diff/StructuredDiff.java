package org.openl.rules.webstudio.web.repository.diff;

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
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
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
 * Compares two projects, returning a list of the additions, deletions  and
 * changes between them.
 *
 * @author Andrey Naumenko
 */
public class StructuredDiff {
    private static final String SEPARATOR = "/";
    private static final String XLS_INNER_STRUCTURE_SEPARATOR = "//";
    private static final Log log = LogFactory.getLog(StructuredDiff.class);
    private static final Comparator<String> pathComparator = new Comparator<String>() {
            public int compare(String path1, String path2) {
                if (isFolder(path1) == isFolder(path2)) {
                    return path1.compareTo(path2);
                } else {
                    return (isFolder(path1) ? (-1) : 1);
                }
            }
        };

    private static Map<String, TreeSet<String>> getSheetsAndTables(Project project,
        String xlsFilePath) {
        Map<String, TreeSet<String>> sheet2Tables = new TreeMap<String, TreeSet<String>>();

        ProjectArtefact a;
        try {
            a = project.getArtefactByPath(new ArtefactPathImpl(removeProjectName(
                            xlsFilePath)));
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

    private static TreeSet<String> getTableNames(Project project, final String path) {
        int sheetNameStartPosition = path.indexOf(XLS_INNER_STRUCTURE_SEPARATOR);
        String xlsFilePath = path.substring(0, sheetNameStartPosition);
        String sheetName = path.substring(sheetNameStartPosition + 2);

        TreeSet<String> tableNames = getSheetsAndTables(project, xlsFilePath)
                .get(sheetName);
        TreeSet<String> transformedTableNames = new TreeSet<String>(CollectionUtils
                    .collect(tableNames,
                        new Transformer() {
                        public Object transform(Object obj) {
                            String s = (String) obj;
                            return path + SEPARATOR + s;
                        }
                    }));
        return transformedTableNames;
    }

    private static TreeSet<String> getSheetNames(Project project, final String xlsFilePath) {
        TreeSet<String> sheetNames = new TreeSet<String>(getSheetsAndTables(project,
                    xlsFilePath).keySet());

        TreeSet<String> transformedSheetNames = new TreeSet<String>(CollectionUtils
                    .collect(sheetNames,
                        new Transformer() {
                        public Object transform(Object obj) {
                            String s = (String) obj;
                            return xlsFilePath + XLS_INNER_STRUCTURE_SEPARATOR + s
                                + SEPARATOR;
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
            items1 = getTableNames(p1, path1);
            items2 = getTableNames(p2, path2);
        } else if (isXlsFile(path1)) {
            items1 = getSheetNames(p1, path1);
            items2 = getSheetNames(p2, path2);
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

        List<DiffElement> elements = getDiffElements(items1, items2, pathComparator, p1,
                p2);
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
     * f1.getArtefacts() and f2.getArtefacts(). Standard merging algorithm for sorted
     * arrays is used.
     *
     * @param items1
     * @param items2
     * @param comparator DOCUMENT ME!
     * @param p1 DOCUMENT ME!
     * @param p2 DOCUMENT ME!
     *
     * @return
     *
     * @throws ProjectException
     */
    private static List<DiffElement> getDiffElements(TreeSet<String> items1,
        TreeSet<String> items2, Comparator<String> comparator, Project p1, Project p2)
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
                DiffElement de = getDiffElement(list1.get(i1), DiffType.Addition);
                result.add(de);
                i1++;
            } else if (i1 == n1) {
                DiffElement de = getDiffElement(list2.get(i2), DiffType.Deletion);
                result.add(de);
                i2++;
            } else {
                String path1 = list1.get(i1);
                String path2 = list2.get(i2);
                int v = comparator.compare(path1, path2);
                if (v < 0) {
                    DiffElement de = getDiffElement(path1, DiffType.Addition);
                    result.add(de);
                    i1++;
                } else if (v > 0) {
                    DiffElement de = getDiffElement(path2, DiffType.Deletion);
                    result.add(de);
                    i2++;
                } else {
                    DiffElement de = getDiffElement(path1, null);
                    if (!isFolder(path1) && !isFolder(path2)) {
                        boolean equal = compareFiles(path1, path2, p1, p2);
                        de.setDiffType(equal ? DiffType.Equal
                            : DiffType.EqualWithDifferentChildren);
                    } else {
                        boolean equal = compareFolders(path1, path2, p1, p2);
                        de.setDiffType(equal ? DiffType.Equal
                            : DiffType.EqualWithDifferentChildren);
                    }
                    result.add(de);
                    i1++;
                    i2++;
                }
            }
        }
        return result;
    }

    private static DiffElement getDiffElement(String path, DiffType diffType) {
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
     * @param path1
     * @param path2
     * @param p1 DOCUMENT ME!
     * @param p2 DOCUMENT ME!
     *
     * @return
     */
    private static boolean compareFiles(String path1, String path2, Project p1, Project p2) {
        if (!isXlsFile(path1)) {
            return true;
        }

        Map<String, TreeSet<String>> sheet2Tables1 = getSheetsAndTables(p1, path1);
        Map<String, TreeSet<String>> sheet2Tables2 = getSheetsAndTables(p2, path2);

        boolean equal = sheet2Tables1.equals(sheet2Tables2);
        return equal;
    }

    /**
     * Compares two folders.
     *
     * @param path1
     * @param path2
     * @param p1 DOCUMENT ME!
     * @param p2 DOCUMENT ME!
     *
     * @return
     *
     * @throws ProjectException
     */
    private static boolean compareFolders(String path1, String path2, Project p1,
        Project p2) throws ProjectException
    {
        if (isSheetPath(path1)) {
            Map<String, TreeSet<String>> sheet2Tables1 = getSheetsAndTables(p1, path1);
            Map<String, TreeSet<String>> sheet2Tables2 = getSheetsAndTables(p2, path2);

            boolean equal = sheet2Tables1.keySet().equals(sheet2Tables2.keySet());
            return equal;
        }

        Set<ProjectArtefact> s1 = new TreeSet<ProjectArtefact>(RepositoryUtils.ARTEFACT_COMPARATOR);
        Set<ProjectArtefact> s2 = new TreeSet<ProjectArtefact>(RepositoryUtils.ARTEFACT_COMPARATOR);

        ProjectFolder f1 = getProjectFolder(p1, path1);
        ProjectFolder f2 = getProjectFolder(p2, path2);

        for (ProjectArtefact a : f1.getArtefacts()) {
            s1.add(a);
        }

        for (ProjectArtefact a : f2.getArtefacts()) {
            s2.add(a);
        }

        if (!s1.equals(s2)) {
            return false;
        }

        Iterator<ProjectArtefact> it1 = s1.iterator();
        Iterator<ProjectArtefact> it2 = s2.iterator();

        boolean isEqual = true;
        while (isEqual && it1.hasNext()) {
            ProjectArtefact a1 = it1.next();
            ProjectArtefact a2 = it2.next();

            if (a1.isFolder()) {
                isEqual = compareFolders(a1.getArtefactPath().getStringValue(),
                        a2.getArtefactPath().getStringValue(), p1, p2);
            } else {
                isEqual = compareFiles(a1.getArtefactPath().getStringValue(),
                        a2.getArtefactPath().getStringValue(), p1, p2);
            }
        }

        return isEqual;
    }
}
