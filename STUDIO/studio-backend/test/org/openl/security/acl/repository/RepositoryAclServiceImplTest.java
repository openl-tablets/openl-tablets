package org.openl.security.acl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.SidRetrievalStrategy;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.security.acl.SidManagingAclService;
import org.openl.security.acl.oid.AclObjectIdentityProvider;

/**
 * A permission is asked of the database once per ACL identity, whatever the number of artefacts that
 * address it. Every branch of a project addresses the same identity, so a project of a many-branch
 * repository would otherwise pay one read per branch.
 */
class RepositoryAclServiceImplTest {

    private SidManagingAclService aclService;
    private AclObjectIdentityProvider oidProvider;
    private RepositoryAclServiceImpl service;

    @BeforeEach
    void setUp() {
        aclService = mock(SidManagingAclService.class);
        oidProvider = mock(AclObjectIdentityProvider.class);
        var sidRetrievalStrategy = mock(SidRetrievalStrategy.class);
        when(sidRetrievalStrategy.getSids(any())).thenReturn(List.of(new GrantedAuthoritySid("DEVELOPERS")));
        var missingAclCache = mock(Cache.class);
        service = new RepositoryAclServiceImpl(mock(AclCache.class),
                missingAclCache,
                aclService,
                new GrantedAuthoritySid("ADMIN"),
                sidRetrievalStrategy,
                oidProvider);
    }

    private AProjectArtefact artefactAt(ObjectIdentity identity) {
        var artefact = mock(AProjectArtefact.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(artefact.getRepository().getId()).thenReturn("design");
        when(oidProvider.getArtifactOid(artefact)).thenReturn(identity);
        return artefact;
    }

    private void granted(ObjectIdentity identity) {
        var acl = mock(MutableAcl.class);
        when(acl.isGranted(any(), any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(true);
        when(aclService.readAclById(identity)).thenReturn(acl);
    }

    @Test
    void oneIdentityIsReadOnceHoweverManyArtefactsAddressIt() {
        var identity = new ObjectIdentityImpl(ProjectArtifact.class, "design:/rules/Rates");
        granted(identity);
        var branches = List.of(artefactAt(identity), artefactAt(identity), artefactAt(identity));

        var result = service.filterGranted(branches, List.of(BasePermission.READ));

        assertEquals(3, result.size());
        assertTrue(result.containsAll(branches));
        verify(aclService, times(1)).readAclById(identity);
    }

    @Test
    void anArtefactOfADesignRepositoryIsAskedOfItsAcl() {
        var identity = new ObjectIdentityImpl(ProjectArtifact.class, "design:/rules/Rates");
        granted(identity);

        assertTrue(service.isGranted(artefactAt(identity), List.of(BasePermission.WRITE)));
        verify(aclService).readAclById(identity);
    }

    @Test
    void eachDistinctIdentityIsReadOnItsOwn() {
        var rates = new ObjectIdentityImpl(ProjectArtifact.class, "design:/rules/Rates");
        var pricing = new ObjectIdentityImpl(ProjectArtifact.class, "design:/rules/Pricing");
        granted(rates);
        granted(pricing);

        var result = service.filterGranted(List.of(artefactAt(rates), artefactAt(pricing)),
                List.of(BasePermission.READ));

        assertEquals(2, result.size());
        verify(aclService, times(1)).readAclById(rates);
        verify(aclService, times(1)).readAclById(pricing);
    }

    /** An artefact of a project downloaded from the Maven repository, opened in a read-only repository. */
    private static AProjectArtefact readOnlyArtefact() {
        var repository = mock(LocalRepository.class);
        when(repository.isReadOnly()).thenReturn(true);
        var artefact = mock(AProjectArtefact.class);
        when(artefact.getRepository()).thenReturn(repository);
        return artefact;
    }

    @Test
    void aReadOnlyProjectIsReadByEveryoneAndChangedByNobody() {
        var artefact = readOnlyArtefact();

        assertTrue(service.isGranted(artefact, List.of(BasePermission.READ)));
        assertTrue(service.isGranted(artefact, true, BasePermission.READ));
        assertFalse(service.isGranted(artefact, List.of(BasePermission.READ, BasePermission.WRITE)));
        assertFalse(service.isGranted(artefact, false, BasePermission.WRITE));
        assertFalse(service.isGranted(artefact, true, BasePermission.DELETE));
        verify(aclService, never()).readAclById(any());
    }

    @Test
    void aReadOnlyProjectIsListedForAReadOnly() {
        var artefact = readOnlyArtefact();

        assertEquals(1, service.filterGranted(List.of(artefact), List.of(BasePermission.READ)).size());
        assertTrue(service.filterGranted(List.of(artefact), List.of(BasePermission.WRITE)).isEmpty());
        verify(aclService, never()).readAclById(any());
    }

    @Test
    void anArtefactOfAnEditableLocalRepositoryKeepsItsAcl() {
        var identity = new ObjectIdentityImpl(ProjectArtifact.class, "design:/rules/Rates");
        granted(identity);
        var repository = mock(LocalRepository.class);
        when(repository.getId()).thenReturn("design");
        var artefact = mock(AProjectArtefact.class);
        when(artefact.getRepository()).thenReturn(repository);
        when(oidProvider.getArtifactOid(artefact)).thenReturn(identity);

        assertEquals(1, service.filterGranted(List.of(artefact), List.of(BasePermission.WRITE)).size());
        assertTrue(service.isGranted(artefact, List.of(BasePermission.WRITE)));
    }
}
