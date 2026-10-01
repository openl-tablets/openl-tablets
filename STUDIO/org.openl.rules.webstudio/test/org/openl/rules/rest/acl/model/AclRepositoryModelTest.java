package org.openl.rules.rest.acl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;

class AclRepositoryModelTest {

    @Test
    void buildsTheRootOfRepositoriesWithoutName() {
        var id = AclRepositoryId.builder().type(AclRepositoryType.DESIGN).build();

        var model = AclRepositoryModel.rootRepositoryBuilder()
                .id(id)
                .type(AclRepositoryType.DESIGN)
                .role(AclRole.VIEWER)
                .build();

        assertEquals(id, model.getId());
        assertEquals(AclRepositoryType.DESIGN, model.getType());
        assertEquals(AclRole.VIEWER, model.getRole());
        assertNull(model.getName());
        assertNull(model.getSid());
    }
}
