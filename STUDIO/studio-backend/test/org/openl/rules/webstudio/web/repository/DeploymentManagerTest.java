package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.ProjectDescriptorImpl;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

/**
 * A deployment repository that keeps each project as an archive gets the libraries inside the archive of the
 * project, and every downloaded project as an archive of its own.
 */
class DeploymentManagerTest {

    @TempDir
    private Path temp;

    @Test
    void savesTheLibrariesInTheArchiveOfTheProjectAndTheDownloadedProjectsBesideIt() throws Exception {
        var saved = new LinkedHashMap<String, Map<String, String>>();
        var manager = manager(saved);
        var library = Files.writeString(temp.resolve("lib-1.0.0.jar"), "library");
        var domain = Files.createDirectories(temp.resolve("domain-1.0.0-1"));
        Files.writeString(domain.resolve("rules.xml"), "<project><name>Domain</name></project>");
        var user = mock(CommonUser.class);
        when(user.getUserName()).thenReturn("admin");
        var request = DeploymentRequest.builder()
                .productionRepositoryId("prod")
                .name("Rules")
                .projectDescriptors(List.of(ProjectDescriptorImpl.builder()
                        .repositoryId("design")
                        .projectName("Rules")
                        .path("Rules")
                        .projectVersion(new CommonVersionImpl("1"))
                        .build()))
                .currentUser(user)
                .extras(new DeploymentExtras(Map.of("Rules", List.of(library)), Map.of("Domain", domain)))
                .build();

        manager.deploy(request);

        assertEquals(List.of("deploy/Rules/Rules", "deploy/Rules/Domain"), new ArrayList<>(saved.keySet()));
        var rules = saved.get("deploy/Rules/Rules");
        assertEquals("<project><name>Rules</name></project>", rules.get("rules.xml"));
        assertEquals("library", rules.get("lib/lib-1.0.0.jar"));
        assertEquals(Map.of("rules.xml", "<project><name>Domain</name></project>"), saved.get("deploy/Rules/Domain"));
    }

    @Test
    void namesTheDeploymentOfADownloadedProjectAfterItsApiVersionAndKeepsWhatItDeploysAgain() throws Exception {
        var saved = new LinkedHashMap<String, Map<String, String>>();
        var deleted = new ArrayList<String>();
        var manager = manager(saved, deleted, List.of("deploy/Domain_Vv2/Domain", "deploy/Domain_Vv2/Gone"));
        var domain = Files.createDirectories(temp.resolve("domain-1.0.0-1"));
        Files.writeString(domain.resolve("rules.xml"), "<project><name>Domain</name></project>");
        Files.writeString(domain.resolve("rules-deploy.xml"), "<rules-deploy><version>v2</version></rules-deploy>");
        var user = mock(CommonUser.class);
        when(user.getUserName()).thenReturn("admin");
        var request = DeploymentRequest.builder()
                .productionRepositoryId("prod")
                .name("Domain")
                .projectDescriptors(List.of())
                .currentUser(user)
                .extras(new DeploymentExtras(Map.of(), Map.of("Domain", domain)))
                .downloadedFolder(domain)
                .build();

        assertEquals("Domain_Vv2", manager.deploy(request).getName());

        assertEquals(List.of("deploy/Domain_Vv2/Domain"), new ArrayList<>(saved.keySet()));
        assertEquals(List.of("deploy/Domain_Vv2/Gone"), deleted, "the downloaded project is saved over, not deleted");
    }

    private static DeploymentManager manager(Map<String, Map<String, String>> saved) throws IOException {
        return manager(saved, new ArrayList<>(), List.of());
    }

    /** A manager deploying to {@code prod}, an archive repository whose saved archives go to {@code saved}. */
    private static DeploymentManager manager(Map<String, Map<String, String>> saved,
                                             List<String> deleted,
                                             List<String> existing) throws IOException {
        var design = mock(Repository.class);
        when(design.supports()).thenReturn(new FeaturesBuilder(design).setFolders(false).build());
        var history = new FileData();
        history.setModifiedAt(new Date(0));
        when(design.checkHistory("rules/Rules", "1")).thenReturn(history);
        when(design.readHistory("rules/Rules", "1")).thenAnswer(call -> new FileItem("rules/Rules",
                new ByteArrayInputStream(zip(Map.of("rules.xml", "<project><name>Rules</name></project>")))));
        var designRepository = mock(DesignTimeRepository.class);
        when(designRepository.getRulesLocation()).thenReturn("rules/");
        when(designRepository.getRepository("design")).thenReturn(design);

        var deployRepository = mock(Repository.class);
        when(deployRepository.supports()).thenReturn(new FeaturesBuilder(deployRepository).setFolders(false).build());
        when(deployRepository.list(any())).thenReturn(existing.stream().map(name -> {
            var file = new FileData();
            file.setName(name);
            return file;
        }).toList());
        when(deployRepository.delete(any(FileData.class))).thenAnswer(call -> {
            deleted.add(call.<FileData>getArgument(0).getName());
            return true;
        });
        when(deployRepository.save(any(FileData.class), any(InputStream.class))).thenAnswer(call -> {
            saved.put(call.<FileData>getArgument(0).getName(), unzip(call.getArgument(1)));
            return call.getArgument(0);
        });
        var proxy = mock(RepositoryFactoryProxy.class);
        when(proxy.getRepositoryInstance("prod")).thenReturn(deployRepository);
        when(proxy.getBasePath("prod")).thenReturn("deploy/");

        var manager = new DeploymentManager();
        manager.addRepository("prod");
        manager.setDesignRepository(designRepository);
        manager.setRepositoryFactoryProxy(proxy);
        return manager;
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static Map<String, String> unzip(InputStream archive) throws IOException {
        var entries = new LinkedHashMap<String, String>();
        try (var zip = new ZipInputStream(archive)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && !entry.getName().startsWith("META-INF/")) {
                    entries.put(entry.getName(), new String(zip.readAllBytes()));
                }
            }
        }
        return entries;
    }
}
