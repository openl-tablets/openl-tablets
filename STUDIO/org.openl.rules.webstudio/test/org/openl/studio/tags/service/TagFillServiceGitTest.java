package org.openl.studio.tags.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.abstraction.ProjectTags;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.impl.local.LockEngineImpl;
import org.openl.rules.project.impl.local.MetainfoRegistry;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.git.GitRepositoryFactory;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.security.standalone.persistence.Tag;
import org.openl.rules.security.standalone.persistence.TagType;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.service.files.ProjectFileLookupService;
import org.openl.studio.projects.service.files.ProjectFileRootFactory;
import org.openl.studio.projects.service.protection.ProtectedBranchBypassService;
import org.openl.studio.projects.validator.ProjectStateValidatorImpl;
import org.openl.studio.tags.model.TagFillBlocker;
import org.openl.studio.tags.model.TagFillResult;
import org.openl.util.IOUtils;

/**
 * Filling tags from the project name templates into the projects of a Git design repository.
 *
 * <p>A closed project gets its tags file in a commit of its own, made on behalf of the current user. An
 * opened project gets the file in its working copy, where it waits to be saved.
 *
 * @author Yury Molchan
 */
class TagFillServiceGitTest {

    private static final String PROJECT = "Policy-Auto-Rating";
    private static final String TAGS_PATH = PROJECT + "/" + ProjectTags.TAGS_FILE_NAME;

    @TempDir
    File remoteRoot;
    @TempDir
    File localRepositoriesFolder;
    @TempDir
    Path userDir;
    @TempDir
    File workspacesRoot;

    private Repository design;
    private LocalRepository localRepository;
    private LockEngine lockEngine;
    private UserWorkspace workspace;
    private TagFillService service;

    @BeforeEach
    void setUp() throws Exception {
        try (var git = Git.init().setDirectory(remoteRoot).call()) {
            var descriptor = new File(remoteRoot, PROJECT + "/rules.xml");
            Files.createDirectories(descriptor.getParentFile().toPath());
            Files.writeString(descriptor.toPath(), "<project/>");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Add the project").setCommitter("Previous Author", "previous@example.com").call();
        }
        design = new GitRepositoryFactory().create(key -> switch (key) {
            case "id" -> "design";
            case "uri" -> remoteRoot.toURI().toString();
            case "local-repositories-folder" -> localRepositoriesFolder.getAbsolutePath();
            default -> null;
        });
        localRepository = new LocalRepository(userDir, MetainfoRegistry.open(userDir));
        localRepository.setId("design");
        localRepository.initialize();
        lockEngine = LockEngineImpl.create(workspacesRoot, "rules");
        workspace = mock(UserWorkspace.class);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a"));
        service = fillService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        IOUtils.closeQuietly(design);
    }

    /** The service filling the Domain tag with the configured value Policy, as the admin who may write. */
    private TagFillService fillService() {
        var domain = new TagType();
        domain.setName("Domain");
        var policy = new Tag();
        policy.setType(domain);
        policy.setName("Policy");
        var tagTypeService = mock(TagTypeService.class);
        when(tagTypeService.getAllTagTypes()).thenReturn(List.of(domain));
        var tagService = mock(TagService.class);
        when(tagService.getAll()).thenReturn(List.of(policy));
        var tagTemplateService = mock(TagTemplateService.class);
        when(tagTemplateService.getTags(PROJECT)).thenReturn(List.of(policy));
        var aclProjectsHelper = mock(AclProjectsHelper.class);
        when(aclProjectsHelper.hasPermission(any(RulesProject.class), eq(BasePermission.WRITE))).thenReturn(true);
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.refreshBranch(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        var validator = new ProjectStateValidatorImpl(mock(ProtectedBranchBypassService.class));
        var files = new ProjectFileRootFactory(aclProjectsHelper, validator, mock(ProjectFileLookupService.class),
                mock(UserManagementService.class), designTimeRepository);
        var provider = new TagCatalogProvider(tagTypeService, tagService);
        return new TagFillService(tagTemplateService, provider, new TagAssignmentValidator(provider, tagService),
                files) {
            @Override
            public UserWorkspace getUserWorkspace() {
                return workspace;
            }
        };
    }

    /** The project as the given user sees it: closed, read straight from the design repository. */
    private RulesProject project(String userName) throws IOException {
        return new RulesProject(new WorkspaceUserImpl(userName, UserInfo::new), localRepository, null, design,
                design.check(PROJECT), lockEngine);
    }

    private void workspaceHolds(RulesProject project) {
        when(workspace.getProjects()).thenReturn(List.of(project));
    }

    private void commit(String path, String content, UserInfo author) throws IOException {
        var data = new FileData();
        data.setName(path);
        data.setAuthor(author);
        data.setComment("Tag the project");
        design.save(data, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static TagFillResult filled() {
        return TagFillResult.updated(PROJECT, Map.of("Domain", "Policy"), Map.of());
    }

    private static TagFillResult leftAlone(TagFillBlocker blocker) {
        return TagFillResult.notModifiable(PROJECT, blocker);
    }

    /** The tags file was last committed on behalf of the admin, with the comment of the fill. */
    private void assertFilledByTheAdmin() throws IOException {
        var commit = design.check(TAGS_PATH);
        assertEquals("admin", commit.getAuthor().getName());
        assertEquals(TagFillService.FILL_COMMENT, commit.getComment());
    }

    private String read(String path) throws IOException {
        try (var item = design.read(path)) {
            return new String(item.getStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void closedProjectGetsItsTagsInACommitOfTheCurrentUser() throws Exception {
        var project = project("admin");
        workspaceHolds(project);

        assertTrue(service.preview().getFirst().modifiable());
        assertEquals(List.of(filled()), service.fill(null));

        assertEquals("Domain=Policy\n", read(TAGS_PATH));
        assertFilledByTheAdmin();
        // Nothing is left behind: the project stays closed, and nobody holds it.
        assertFalse(project.isOpened());
        assertFalse(project.isLocked());
    }

    @Test
    void closedProjectKeepsTheTagsItCarries() throws Exception {
        commit(TAGS_PATH, "Team=Payroll\n", new UserInfo("previous", "previous@example.com", "Previous Author"));
        var project = project("admin");
        workspaceHolds(project);

        assertEquals(List.of(filled()), service.fill(null));

        assertEquals("Team=Payroll\nDomain=Policy\n", read(TAGS_PATH));
        // The commit is the current user's, not a repetition of the one that wrote the file before.
        assertFilledByTheAdmin();
        assertFalse(project.isLocked());
    }

    @Test
    void closedProjectAnotherUserIsEditingIsLeftAlone() throws Exception {
        assertTrue(project("jdoe").tryLock());
        var project = project("admin");
        workspaceHolds(project);

        // The admin learns who holds the project.
        var locked = TagFillBlocker.locked("jdoe");
        assertEquals(locked, service.preview().getFirst().blocker());
        assertEquals(List.of(leftAlone(locked)), service.fill(null));

        assertNull(design.check(TAGS_PATH));
        assertEquals("jdoe", project.getLockInfo().getLockedBy());
    }

    @Test
    void closedProjectTheAdminStillHoldsALockOnIsLeftAloneForTheAdminToRelease() throws Exception {
        // A lock that outlived an earlier change of the admin, as a failed write used to leave behind.
        assertTrue(project("admin").tryLock());
        var project = project("admin");
        workspaceHolds(project);

        // The admin is told the lock is theirs to release, not that somebody is editing the project.
        var ownLock = TagFillBlocker.of(TagFillBlocker.Reason.LOCKED_BY_YOU);
        assertEquals(ownLock, service.preview().getFirst().blocker());
        assertEquals(List.of(leftAlone(ownLock)), service.fill(null));

        assertNull(design.check(TAGS_PATH));
        assertTrue(project.isLockedByMe());
    }

    @Test
    void olderRevisionOpenedToBeReadIsLeftAlone() throws Exception {
        var firstRevision = design.check(PROJECT).getVersion();
        commit(PROJECT + "/notes.txt", "The second revision\n", new UserInfo("jdoe", "jdoe@example.com", "John Doe"));
        var project = project("admin");
        project.openVersion(firstRevision);
        workspaceHolds(project);

        var olderRevision = TagFillBlocker.of(TagFillBlocker.Reason.OLDER_REVISION);
        assertEquals(olderRevision, service.preview().getFirst().blocker());
        assertEquals(List.of(leftAlone(olderRevision)), service.fill(null));

        // The revision stays as it was read: nothing is written into it or into the repository.
        assertFalse(Files.exists(userDir.resolve(TAGS_PATH)));
        assertNull(design.check(TAGS_PATH));
        assertTrue(project.isReadingOtherVersion());
    }

    @Test
    void openedProjectGetsItsTagsInTheWorkingCopy() throws Exception {
        var project = project("admin");
        project.open();
        workspaceHolds(project);

        assertEquals(List.of(filled()), service.fill(null));

        // Nothing reaches the design repository until the project is saved.
        assertNull(design.check(TAGS_PATH));
        assertEquals("Domain=Policy\n", Files.readString(userDir.resolve(TAGS_PATH)));
        assertTrue(project.isModified());
        assertTrue(project.isLockedByMe());
    }
}
