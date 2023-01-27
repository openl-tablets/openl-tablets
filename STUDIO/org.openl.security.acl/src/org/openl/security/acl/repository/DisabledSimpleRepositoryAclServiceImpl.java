package org.openl.security.acl.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public class DisabledSimpleRepositoryAclServiceImpl implements SimpleRepositoryAclService {
    @Override
    public Map<Sid, List<Permission>> listPermissions(String repositoryId, String path) {
        return Collections.emptyMap();
    }

    @Override
    public Map<Sid, List<Permission>> listPermissions(String repositoryId, String path, List<Sid> sids) {
        return Collections.emptyMap();
    }

    @Override
    public Map<Sid, List<Permission>> listRootPermissions() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Sid, List<Permission>> listRootPermissions(List<Sid> sids) {
        return Collections.emptyMap();
    }

    @Override
    public void addPermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void addRootPermissions(Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public void addRootPermissions(List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void move(String repositoryId, String path, String newPath) {
    }

    @Override
    public void deleteAcl(String repositoryId, String path) {
    }

    @Override
    public void deleteAclRoot() {
    }

    @Override
    public void removePermissions(String repositoryId, String path) {
    }

    @Override
    public void removePermissions(String repositoryId, String path, List<Sid> sids) {
    }

    @Override
    public void removePermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void removePermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public void removeRootPermissions(List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void removeRootPermissions(List<Sid> sids) {
    }

    @Override
    public void removeRootPermissions() {
    }

    @Override
    public boolean isGranted(String repositoryId, String path, List<Permission> permissions) {
        return true;
    }

    @Override
    public boolean createAcl(String repositoryId, String path, List<Permission> permissions) {
        return true;
    }

    @Override
    public Sid getOwner(String repositoryId, String path) {
        return null;
    }

    @Override
    public boolean updateOwner(String repositoryId, String path, Sid sid) {
        return true;
    }
}
