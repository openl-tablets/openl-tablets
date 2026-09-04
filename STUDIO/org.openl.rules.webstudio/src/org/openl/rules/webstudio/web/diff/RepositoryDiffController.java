package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.PreDestroy;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls2.XlsDiff2;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.util.Utils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Supplies the repository revision diff screen with data.
 *
 * <p>Compares the working copy of a project against a chosen branch and revision from the design
 * repository. The project is addressed by the {@code projectName} and {@code repoId} request
 * parameters, so the screen can be opened stand-alone (in a popup window) from the projects UI.
 */
@Service
@SessionScope
@Slf4j
@RequiredArgsConstructor
public class RepositoryDiffController extends AbstractDiffController {

    private static final char SEPARATOR = '/';

    private final Utils utils;
    // The scratch directory for the compared spreadsheets lives under the Studio home rather than in the
    // world-writable system temp directory, so other local users can never read the downloaded files.
    @Value("${openl.home}")
    private final String openlHome;

    @Getter
    private String projectName;
    private String repositoryId;

    // Identifies the project the screen is currently bound to; a change of parameters rebinds it.
    private String loadedKey;

    @Getter
    @Setter
    private String branch;
    @Getter
    @Setter
    private String selectedVersionRepo;
    @Getter
    @Setter
    private String selectedExcelFileUW;
    @Getter
    @Setter
    private String selectedExcelFileRepo;

    private RulesProject projectUW; // User Workspace project
    private List<AProjectArtefact> excelArtefactsUW = Collections.emptyList();
    private List<AProjectArtefact> excelArtefactsRepo = Collections.emptyList();

    private static UserWorkspace getUserWorkspace() {
        var rulesUserSession = WebStudioUtils.getRulesUserSession();
        if (rulesUserSession == null) {
            // The diff screen only opens for an authenticated user, so a missing session is exceptional.
            throw new IllegalStateException("No user session");
        }
        return rulesUserSession.getUserWorkspace();
    }

    /**
     * Binds the screen to the project named by the request parameters and loads both compared sides.
     * Runs on the initial page render only; a postback keeps the selection made by the user.
     */
    public void init() {
        if (FacesContext.getCurrentInstance().isPostback()) {
            return;
        }
        repositoryId = WebStudioUtils.getRequestParameter("repoId");
        projectName = WebStudioUtils.getRequestParameter("projectName");
        resolveProject();
        initProjectUW();
        initProjectRepo();
        if (projectUW == null) {
            WebStudioUtils.addErrorMessage("Cannot find the project '" + projectName + "' to compare.");
        }
    }

    private void resolveProject() {
        String key = repositoryId + "\u0000" + projectName;
        if (key.equals(loadedKey)) {
            return;
        }
        loadedKey = key;
        branch = null;
        selectedVersionRepo = null;
        selectedExcelFileUW = null;
        selectedExcelFileRepo = null;
        setDiffTree(null);
        projectUW = lookupProject();
        if (projectUW != null && projectUW.isSupportsBranches()) {
            branch = projectUW.getBranch();
        }
    }

    /**
     * Finds the project by the name the projects UI shows, which is its business name, in the repository
     * the caller came from. A repository is given only to tell apart projects sharing a business name.
     */
    private RulesProject lookupProject() {
        if (StringUtils.isBlank(projectName)) {
            return null;
        }
        try {
            return pickProject(getUserWorkspace().getProjectsByName(projectName), repositoryId).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to resolve project '{}' for comparison", projectName, e);
            return null;
        }
    }

    /**
     * The project of the given repository among the projects sharing a business name. Without a repository
     * the first one is taken, which is the only choice when the name is unique.
     */
    static Optional<RulesProject> pickProject(Collection<RulesProject> projects, String repositoryId) {
        return projects.stream()
                .filter(project -> StringUtils.isBlank(repositoryId)
                        || repositoryId.equals(project.getDesignRepository().getId()))
                .findFirst();
    }

    public boolean isSupportsBranches() {
        return projectUW != null && projectUW.isSupportsBranches();
    }

    /**
     * Branches the compared project exists in, for the right-hand revision picker.
     */
    public List<SelectItem> getBranches() {
        if (!isSupportsBranches()) {
            return Collections.emptyList();
        }
        try {
            var designTimeRepository = getUserWorkspace().getDesignTimeRepository();
            List<String> branches = designTimeRepository
                    .getBranchedProject(projectUW.getDesignRepository().getId(), projectUW.getDesignProjectName())
                    .map(project -> new ArrayList<>(project.entries().keySet()))
                    .orElseGet(ArrayList::new);
            String projectBranch = projectUW.getBranch();
            if (projectBranch != null && !branches.contains(projectBranch)) {
                branches.add(projectBranch);
            }
            branches.sort(String.CASE_INSENSITIVE_ORDER);
            return branches.stream().map(b -> new SelectItem(b, b)).toList();
        } catch (Exception e) {
            log.warn("Failed to list branches of project '{}'", projectName, e);
            return Collections.emptyList();
        }
    }

    private List<ProjectVersion> getVersionsRepo() {
        if (projectUW == null) {
            return Collections.emptyList();
        }
        try {
            List<ProjectVersion> versions;
            DesignTimeRepository designTimeRepository = getUserWorkspace().getDesignTimeRepository();
            Repository designRepository = projectUW.getDesignRepository();
            if (designRepository.supports().branches()) {
                Repository repository = ((BranchRepository) designRepository).forBranch(branch);
                String folderPath = designTimeRepository.getProject(designRepository.getId(), projectUW.getName())
                        .getFolderPath();
                versions = new AProject(repository, folderPath).getVersions();
            } else {
                versions = projectUW.getVersions();
            }
            Collections.reverse(versions);
            return versions;
        } catch (Exception e) {
            log.warn("Failed to list revisions of project '{}'", projectName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Revisions available for comparison on the selected branch, most recent first.
     */
    public List<SelectItem> getVersionSelectItems() {
        List<SelectItem> selectItems = new ArrayList<>();
        for (ProjectVersion version : getVersionsRepo()) {
            if (!version.isDeleted()) {
                selectItems.add(new SelectItem(version.getVersionName(), utils.getDescriptiveVersion(version)));
            }
        }
        return selectItems;
    }

    public List<SelectItem> getExcelFilesUW() {
        return toExcelItems(excelArtefactsUW);
    }

    public List<SelectItem> getExcelFilesRepo() {
        return toExcelItems(excelArtefactsRepo);
    }

    private static List<SelectItem> toExcelItems(List<AProjectArtefact> excelArtefacts) {
        List<SelectItem> excelItems = new ArrayList<>();
        for (AProjectArtefact excelArtefact : excelArtefacts) {
            excelItems.add(new SelectItem(excelArtefact.getArtefactPath().getStringValue(), excelArtefact.getName()));
        }
        return excelItems;
    }

    private void initProjectUW() {
        if (projectUW == null) {
            excelArtefactsUW = Collections.emptyList();
            return;
        }
        try {
            excelArtefactsUW = getExcelArtefacts(projectUW, "");
        } catch (Exception e) {
            log.warn("Failed to list working-copy Excel files of project '{}'", projectName, e);
            excelArtefactsUW = Collections.emptyList();
        }
    }

    public void initProjectRepo() {
        if (projectUW == null) {
            excelArtefactsRepo = Collections.emptyList();
            return;
        }
        try {
            AProject projectRepo;
            DesignTimeRepository designTimeRepository = getUserWorkspace().getDesignTimeRepository();
            Repository designRepository = projectUW.getDesignRepository();
            if (designRepository.supports().branches()) {
                List<ProjectVersion> versions = new AProject(((BranchRepository) designRepository).forBranch(branch),
                        projectUW.getRealPath()).getVersions();
                if (versions.stream().noneMatch(v -> v.getVersionName().equals(selectedVersionRepo))) {
                    selectedVersionRepo = null;
                }
                projectRepo = designTimeRepository
                        .getProjectByPath(designRepository.getId(), branch, projectUW.getRealPath(), selectedVersionRepo);
            } else {
                projectRepo = getProjectRepoNoBranch(designTimeRepository, designRepository);
            }
            excelArtefactsRepo = getExcelArtefacts(projectRepo, "");
        } catch (Exception e) {
            log.warn("Failed to load repository revision of project '{}'", projectName, e);
            excelArtefactsRepo = Collections.emptyList();
        }
    }

    private AProject getProjectRepoNoBranch(DesignTimeRepository designTimeRepository,
                                            Repository designRepository) throws ProjectException {
        try {
            return designTimeRepository
                    .getProjectByPath(designRepository.getId(), null, projectUW.getRealPath(), selectedVersionRepo);
        } catch (Exception e) {
            log.warn("Could not get project '{}' of revision '{}'", projectUW.getName(), selectedVersionRepo, e);
            return designTimeRepository.getProject(designRepository.getId(), projectUW.getName());
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
                excelArtefacts.addAll(getExcelArtefacts(project, artefactPath));
            } else if (FileTypeHelper.isExcelFile(artefactPath)) {
                excelArtefacts.add(projectArtefact);
            }
        }
        excelArtefacts.sort((o1, o2) -> baseName(o1.getName()).compareTo(baseName(o2.getName())));
        return excelArtefacts;
    }

    private static String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private AProjectFolder getProjectFolder(AProject project, String path) throws ProjectException {
        path = removeProjectName(path);
        if (path.isEmpty()) {
            return project;
        }
        return (AProjectFolder) project.getArtefactByPath(new ArtefactPathImpl(path));
    }

    private static String removeProjectName(String path) {
        // The artefact path is prefixed with the project name; drop it to get the in-project path.
        return path.substring(path.indexOf(SEPARATOR, 1) + 1);
    }

    private File downloadExcelFile(AProjectArtefact excelArtefact) {
        if (excelArtefact == null) {
            return null;
        }
        File tempFile = null;
        try (InputStream in = ((AProjectResource) excelArtefact).getContent()) {
            Path scratch = Files.createDirectories(Path.of(openlHome, "tmp", "diff"));
            tempFile = Files.createTempFile(scratch, "openl-cmp", excelArtefact.getName()).toFile();
            try (OutputStream out = new FileOutputStream(tempFile)) {
                in.transferTo(out);
            }
        } catch (ProjectException | IOException e) {
            log.error(e.getMessage(), e);
        }
        return tempFile;
    }

    private static AProjectArtefact getExcelArtefactByPath(List<AProjectArtefact> excelArtefacts, String path) {
        return excelArtefacts.stream()
                .filter(excelArtefact -> excelArtefact.getArtefactPath().getStringValue().equals(path))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String compare() {
        if (StringUtils.isEmpty(selectedExcelFileUW) || StringUtils.isEmpty(selectedExcelFileRepo)) {
            return null;
        }
        // Files can be reloaded lazily later, so they cannot be deleted immediately. Delete them when the bean
        // is destroyed (on session timeout) or before the next comparison.
        deleteTempFiles();

        File excelFile1 = downloadExcelFile(getExcelArtefactByPath(excelArtefactsUW, selectedExcelFileUW));
        addTempFile(excelFile1);
        File excelFile2 = downloadExcelFile(getExcelArtefactByPath(excelArtefactsRepo, selectedExcelFileRepo));
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
            // The diff tree can be huge. As the previous instance is no longer needed, clear it before any
            // further calculation.
            setDiffTree(null);
            DiffTreeNode diffTree = new XlsDiff2().diffFiles(excelFile1, excelFile2);
            setDiffTree(diffTree);
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            String message = "Cannot compare the files '" + FileUtils.getName(selectedExcelFileUW) + "' and '"
                    + FileUtils.getName(selectedExcelFileRepo) + "'. Cause: " + cause.getMessage();
            log.error(message, e);
            WebStudioUtils.addErrorMessage(message);
        }
        return null;
    }

    /**
     * Resets the current selection in the tree to prevent an NPE while rendering a new tree.
     */
    public void resetTreeSelection(String componentId) {
        UIComponent treeComponent = RichFunction.findComponent(componentId);
        if (treeComponent instanceof UITree tree) {
            tree.setSelection(Collections.emptyList());
        }
    }

    @PreDestroy
    public void destroy() {
        deleteTempFiles();
    }
}
