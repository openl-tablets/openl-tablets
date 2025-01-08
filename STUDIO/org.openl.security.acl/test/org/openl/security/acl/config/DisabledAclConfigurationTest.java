package org.openl.security.acl.config;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService;

public class DisabledAclConfigurationTest {

    private DisabledAclConfiguration disabledAclConfiguration;

    @BeforeEach
    public void setUp() {
        disabledAclConfiguration = new DisabledAclConfiguration();
    }

    @Test
    public void test_disabled_designRepositoryAclService() {
        var aclService = disabledAclConfiguration.designRepositoryAclService();
        test_DisabledSimpleRepositoryAclService_stubs(aclService);
        test_DisabledRepositoryAclService_stubs(aclService);
    }

    @Test
    public void test_disabled_productionRepositoryAclService() {
        var aclService = disabledAclConfiguration.productionRepositoryAclService();
        test_DisabledSimpleRepositoryAclService_stubs(aclService);
    }

    private void test_DisabledRepositoryAclService_stubs(RepositoryAclService service) {
        AProjectArtefact stubArtefact = mock(AProjectArtefact.class);
        service.move(stubArtefact, null);
        service.deleteAcl(stubArtefact);
        assertTrue(service.isGranted(stubArtefact, List.of()));
        assertTrue(service.createAcl(stubArtefact, List.of(), false));
        assertTrue(service.hasAcl(stubArtefact));
        assertNull(service.getPath(stubArtefact));
        assertNull(service.getFullPath(stubArtefact));
    }

    private void test_DisabledSimpleRepositoryAclService_stubs(SimpleRepositoryAclService service) {
        assertTrue(service.listPermissions(null, null).isEmpty());
        assertTrue(service.listRootPermissions().isEmpty());
        assertTrue(service.listRootPermissions(null).isEmpty());

        service.addPermissions(null, null, List.of(), List.of());
        service.addRootPermissions(List.of(), List.of());
        service.move(null, null, null);
        service.deleteAcl(null, null);
        service.removePermissions(null, null);
        service.removePermissions(null, null, List.of());
        service.removePermissions(null, null, List.of(), List.of());
        service.removeRootPermissions(List.of(), List.of());
        service.removeRootPermissions(null);
        service.removeRootPermissions();

        assertTrue(service.isGranted(null, null, List.of()));
        assertTrue(service.createAcl(null, null, List.of(), false));

        assertNull(service.getOwner(null, null));

        assertTrue(service.updateOwner(null, null, null));

        assertNotNull(service.hashCode());
        assertNotEquals(new Object(), service);
        assertTrue(service.toString().startsWith("Disabled"));
    }

}
