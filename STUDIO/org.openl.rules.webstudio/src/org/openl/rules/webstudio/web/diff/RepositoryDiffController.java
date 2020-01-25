package org.openl.rules.webstudio.web.diff;

import java.io.*;
import java.util.*;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.model.SelectItem;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls2.XlsDiff2;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supplies repository structured diff UI tree with data.
 *
 * @author Andrey Naumenko
 */
@ManagedBean
@SessionScoped
public class RepositoryDiffController extends AbstractDiffController {
    private final Logger log = LoggerFactory.getLogger(RepositoryDiffController.class);

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    @ManagedProperty(value = "#{designTimeRepository}")
    private DesignTimeRepository designTimeRepository;

    private AProject projectUW; // User Workspace project
    private List<AProjectArtefact> excelArtefactsUW;
    private List<AProjectArtefact> excelArtefactsRepo;

    private String selectedExcelFileUW;
    private String selectedExcelFileRepo;
    private String selectedVersionRepo;

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
        Collection<ProjectVersion> versions = projectUW.getVersions();
        SelectItem[] selectItems = new SelectItem[versions.size()];

        int i = 0;
        for (ProjectVersion version : versions) {
            selectItems[i] = new SelectItem(version.getVersionName());
            i++;
        }

        return selectItems;
    }

    public List<SelectItem> getExcelFilesUW() {
        init();
        List<SelectItem> excelItems = new ArrayList<>();
        for (AProjectArtefact excelArtefact : excelArtefactsUW) {
            excelItems.add(new SelectItem(excelArtefact.getArtefactPath().getStringValue(), excelArtefact.getName()));
        }
        return excelItems;
    }

    public List<SelectItem> getExcelFilesRepo() {
        List<SelectItem> excelItems = new ArrayList<>();
        if (excelArtefactsRepo == null) {
            return excelItems;
        }
        for (AProjectArtefact excelArtefact : excelArtefactsRepo) {
            excelItems.add(new SelectItem(excelArtefact.getArtefactPath().getStringValue(), excelArtefact.getName()));
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
        return null;
    }

    public void initProjectUW() {
        try {
            UserWorkspaceProject selectedProject = repositoryTreeState.getSelectedProject();
            if (projectUW != selectedProject) {
                projectUW = selectedProject;
                selectedVersionRepo = projectUW.getVersion().getVersionName();
                setDiffTree(null);
            }
            excelArtefactsUW = getExcelArtefacts(projectUW, "");
        } catch (Exception e) {
            log.warn("Failed to init Diff controller", e);
        }
    }

    public void initProjectRepo() {
        try {
            CommonVersionImpl version = new CommonVersionImpl(selectedVersionRepo);
            // Repository project
            AProject projectRepo;
            try {
                projectRepo = designTimeRepository.getProject(projectUW.getName(), version);
            } catch (Exception e) {
                log.warn("Could not get project'{}' of version '{}'", projectUW.getName(), version.getVersionName(), e);
                projectRepo = designTimeRepository.getProject(projectUW.getName());
            }
            excelArtefactsRepo = getExcelArtefacts(projectRepo, "");
        } catch (Exception e) {
            log.warn("Failed to retrieve repository project info for Diff controller", e);
        }
    }

    private List<AProjectArtefact> getExcelArtefacts(AProject project, String rootPath) {
        Collection<? extends AProjectArtefact> projectArtefacts;
        if (rootPath != null) {
            try {
                projectArtefacts = getProjectFolder(project, rootPath).getArtefacts();
            } catch (Exception e) {
                return Collections.emptyList();
            }
        } else {
            projectArtefacts = project.getArtefacts();
        }
        List<AProjectArtefact> excelArtefacts = new ArrayList<>();
        for (AProjectArtefact projectArtefact : projectArtefacts) {
            String artefactPath = projectArtefact.getArtefactPath().getStringValue();
            if (projectArtefact.isFolder()) {
                excelArtefacts.addAll(getExcelArtefacts(project, projectArtefact.getArtefactPath().getStringValue()));
            } else if (FileTypeHelper.isExcelFile(artefactPath)) {
                excelArtefacts.add(projectArtefact);
            }
        }
        Collections.sort(excelArtefacts, new Comparator<AProjectArtefact>() {
            @Override
            public int compare(AProjectArtefact o1, AProjectArtefact o2) {
                String s1 = o1.getName();
                int t = s1.lastIndexOf(".");
                if (t > 0) {
                    s1 = s1.substring(0, t);
                }
                String s2 = o2.getName();
                t = s2.lastIndexOf(".");
                if (t > 0) {
                    s2 = s2.substring(0, t);
                }
                return s1.compareTo(s2);
            }
        });
        return excelArtefacts;
    }

    private AProjectFolder getProjectFolder(AProject project, String path) throws ProjectException {
        path = removeProjectName(path);

        if (path.length() == 0) {
            return project;
        }

        return (AProjectFolder) project.getArtefactByPath(new ArtefactPathImpl(path));
    }

    private File downloadExcelFile(AProjectArtefact excelArtefact) {
        if (excelArtefact == null) {
            return null;
        }
        InputStream in = null;
        try {
            in = ((AProjectResource) excelArtefact).getContent();
        } catch (ProjectException e) {
            log.error(e.getMessage(), e);
        }

        File tempFile = null;
        OutputStream out = null;
        try {
            tempFile = File.createTempFile("openl-cmp", excelArtefact.getName());
            out = new FileOutputStream(tempFile);
            IOUtils.copy(in, out);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
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

    private AProjectArtefact getExcelArtefactByPath(List<AProjectArtefact> excelArtefacts, String path) {
        for (AProjectArtefact excelArtefact : excelArtefacts) {
            if (excelArtefact.getArtefactPath().getStringValue().equals(path)) {
                return excelArtefact;
            }
        }
        return null;
    }

    @Override
    public String compare() {
        if (StringUtils.isEmpty(selectedExcelFileUW) || StringUtils.isEmpty(selectedExcelFileRepo)) {
            return null;
        }
        // Files can be reloaded lazily later. We cannot delete them immediately. Instead delete them when Bean
        // is destroyed (on session timeout) or before next comparison.
        deleteTempFiles();

        AProjectArtefact excelArtefact1 = getExcelArtefactByPath(excelArtefactsUW, selectedExcelFileUW);
        File excelFile1 = downloadExcelFile(excelArtefact1);
        addTempFile(excelFile1);

        AProjectArtefact excelArtefact2 = getExcelArtefactByPath(excelArtefactsRepo, selectedExcelFileRepo);
        File excelFile2 = downloadExcelFile(excelArtefact2);
        addTempFile(excelFile2);

        try {
            if (excelFile1 == null) {
                WebStudioUtils.addErrorMessage("Cannot open the file " + selectedExcelFileUW);
                return null;
            }
            if (excelFile2 == null) {
                WebStudioUtils.addErrorMessage("Cannot open the file " + selectedExcelFileRepo);
                return null;
            }
            // The Diff Tree can be huge. As far as we don't need the
            // previous instance anymore, we should clear it before any
            // further calculations.
            setDiffTree(null);
            XlsDiff2 x = new XlsDiff2();
            DiffTreeNode diffTree = x.diffFiles(excelFile1, excelFile2);
            setDiffTree(diffTree);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e;
            }
            String message = "Cannot compare the files '" + FileUtils
                .getName(selectedExcelFileUW) + "' and '" + FileUtils
                    .getName(selectedExcelFileRepo) + "'. Cause: " + cause.getMessage();
            log.error(message, e);
            WebStudioUtils.addErrorMessage(message);
        }

        return null;
    }

    /**
     * Reset current selection in UITree to prevent NPE while rendering a new tree
     */
    public void resetTreeSelection(String componentId) {
        UIComponent treeComponent = RichFunction.findComponent(componentId);
        if (treeComponent instanceof UITree) {
            ((UITree) treeComponent).setSelection(Collections.emptyList());
        }
    }

    @PreDestroy
    public void destroy() {
        deleteTempFiles();
    }
}
