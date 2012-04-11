package org.openl.rules.webstudio.web.repository.diff;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.openl.rules.diff.test.AbstractProjection;
import org.openl.rules.diff.test.DiffTreeBuilderImpl;
import org.openl.rules.diff.test.ProjectionDifferImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjectionBuilder;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.webstudio.web.diff.AbstractDiffController;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.abstracts.ProjectResource;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.impl.ArtefactPathImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.util.FileTypeHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.SelectItem;

/**
 * Supplies repository structured diff UI tree with data.
 *
 * @author Andrey Naumenko
 */
public class RepositoryDiffController extends AbstractDiffController {
    //private static Log log = LogFactory.getLog(StructuredDiffController.class);
    private DesignTimeRepository designTimeRepository;
    private RepositoryTreeState repositoryTreeState;
    private Project projectUW; // User Workspace project
    private Project projectRepo; // Repository project
    private List<ProjectArtefact> excelArtefactsUW;
    private List<ProjectArtefact> excelArtefactsRepo;

    private String selectedExcelFileUW;
    private String selectedExcelFileRepo;
    private String selectedVersionRepo = "0.0.0";

    public void setSelectedExcelFileUW(String selectedExcelFileUW) {
        this.selectedExcelFileUW = selectedExcelFileUW;
    }

    public String getSelectedExcelFileUW() {
        return selectedExcelFileUW;
    }

    public void setSelectedExcelFileRepo(String selectedExcelFileRepo) {
        this.selectedExcelFileRepo = selectedExcelFileRepo;
    }

    public String getSelectedExcelFileRepo() {
        return selectedExcelFileRepo;
    }

    public void setSelectedVersionRepo(String selectedVersionRepo) {
        this.selectedVersionRepo = selectedVersionRepo;
    }

    public String getSelectedVersionRepo() {
        return selectedVersionRepo;
    }

    public SelectItem[] getVersionsRepo() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) projectUW;
        Collection<ProjectVersion> versions = projectArtefact.getVersions();
        SelectItem[] selectItems = new SelectItem[versions.size()];

        int i = 0;
        for (ProjectVersion version : versions) {
            selectItems[i] = new SelectItem(version.getMajor() + "." + version.getMinor() + "." + version.getRevision());
            i++;
        }

        return selectItems;
    }

    public List<SelectItem> getExcelFilesUW() {
        init();
        List<SelectItem> excelItems = new ArrayList<SelectItem>();
        for (ProjectArtefact excelArtefact : excelArtefactsUW) {
            excelItems.add(new SelectItem(excelArtefact.getArtefactPath().getStringValue(),
                    excelArtefact.getName()));
        }
        return excelItems;
    }

    public List<SelectItem> getExcelFilesRepo() {
        List<SelectItem> excelItems = new ArrayList<SelectItem>();
        for (ProjectArtefact excelArtefact : excelArtefactsRepo) {
            excelItems.add(new SelectItem(excelArtefact.getArtefactPath().getStringValue(),
                    excelArtefact.getName()));
        }
        return excelItems;
    }

    public void setDesignTimeRepository(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public String init() {
        initProjectUW();
        initProjectRepo();
        //setDiffTree(null);
        return null;
    }

    public void initProjectUW() {
        try {
            projectUW = repositoryTreeState.getSelectedProject();
            excelArtefactsUW = getExcelArtefacts(projectUW, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initProjectRepo() {
        try {
            projectRepo = designTimeRepository.getProject(projectUW.getName(),
                    new CommonVersionImpl(selectedVersionRepo));
            excelArtefactsRepo = getExcelArtefacts(projectRepo, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ProjectArtefact> getExcelArtefacts(Project project, String rootPath) throws Exception {
        List<ProjectArtefact> excelArtefacts = new ArrayList<ProjectArtefact>();
        Collection<? extends ProjectArtefact> projectArtefacts = null;
        if (rootPath != null) {
            try {
                projectArtefacts = getProjectFolder(project, rootPath).getArtefacts();
            } catch (Exception e) {
                return excelArtefacts;
            }
        } else {
            projectArtefacts = project.getArtefacts();
        }
        for (ProjectArtefact projectArtefact : projectArtefacts) {
            String artefactPath = projectArtefact.getArtefactPath().getStringValue();
            if (projectArtefact.isFolder()) {
                excelArtefacts.addAll(getExcelArtefacts(project,
                        projectArtefact.getArtefactPath().getStringValue()));
            } else if (FileTypeHelper.isExcelFile(artefactPath)) {
                excelArtefacts.add(projectArtefact);
            }
        }
        return excelArtefacts;
    }

    private ProjectFolder getProjectFolder(Project project, String path) throws ProjectException {
        path = removeProjectName(path);

        if (path.length() == 0) {
            return project;
        }

        ProjectFolder projectFolder = (ProjectFolder) project.getArtefactByPath(new ArtefactPathImpl(path));
        return projectFolder;
    }

    private File downloadExelFile(ProjectArtefact excelArtefact) {
        if (excelArtefact == null) {
            return null;
        }
        InputStream in = null;
        try {
            in = ((ProjectResource) excelArtefact).getContent();
        } catch (ProjectException e) {
            e.printStackTrace();
        }

        File tempFile = null;
        OutputStream out = null;
        String filePrefix = ((excelArtefact instanceof LocalProjectArtefact) ? "uw" : selectedVersionRepo) + "_";
        try {
            tempFile = new File(filePrefix + excelArtefact.getName());
            out = new FileOutputStream(tempFile);
            IOUtils.copy(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
        return tempFile;
    }

    private String removeProjectName(String path) {
        // remove project name
        path = path.substring(path.indexOf(SEPARATOR, 1) + 1);
        return path;
    }

    private static final char SEPARATOR = '/';

    private ProjectArtefact getExcelArtefactByPath(List<ProjectArtefact> excelArtefacts, String path) {
        for (ProjectArtefact excelArtefact : excelArtefacts) {
            if (excelArtefact.getArtefactPath().getStringValue().equals(path)) {
                return excelArtefact;
            }
        }
        return null;
    }

    public String compare() {
        if (StringUtils.isEmpty(selectedExcelFileUW) || StringUtils.isEmpty(selectedExcelFileRepo)) {
            System.err.println("exit");
            return null;
        }
        ProjectArtefact excelArtefact1 = getExcelArtefactByPath(excelArtefactsUW, selectedExcelFileUW);
        File excelFile1 = downloadExelFile(excelArtefact1);
        XlsMetaInfo xmi1 = XlsHelper.getXlsMetaInfo(excelFile1.getName());
        AbstractProjection p1 = XlsProjectionBuilder.build(xmi1, "xls1");
        excelFile1.delete();

        ProjectArtefact excelArtefact2 = getExcelArtefactByPath(excelArtefactsRepo, selectedExcelFileRepo);
        File excelFile2 = downloadExelFile(excelArtefact2);
        XlsMetaInfo xmi2 = XlsHelper.getXlsMetaInfo(excelFile2.getName());
        AbstractProjection p2 = XlsProjectionBuilder.build(xmi2, "xls2");
        excelFile2.delete();

        DiffTreeBuilderImpl builder = new DiffTreeBuilderImpl();
        builder.setProjectionDiffer(new ProjectionDifferImpl());
        DiffTreeNode diffTree = builder.compare(p1, p2);
        setDiffTree(diffTree);

        return null;
    }

}
