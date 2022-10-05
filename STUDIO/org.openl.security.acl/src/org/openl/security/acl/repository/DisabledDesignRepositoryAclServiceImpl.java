package org.openl.security.acl.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;

public class DisabledDesignRepositoryAclServiceImpl implements DesignRepositoryAclService {

    @Override
    public Map<Sid, List<Permission>> listPermissions(String repositoryId, String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Sid, List<Permission>> listPermissions(AProjectArtefact artefact) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Sid, List<Permission>> listRootPermissions() {
        return Collections.emptyMap();
    }

    @Override
    public void addPermissions(String repositoryId, String path, Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public void addPermissions(String repositoryId, String path, List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void addPermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void addPermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public void addRootPermissions(Map<Sid, List<Permission>> permissions) {
    }

    @Override
    public void addRootPermissions(List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void move(AProjectArtefact artefact, String newPath) {
    }

    @Override
    public void move(String repositoryId, String path, String newPath) {
    }

    @Override
    public void delete(AProjectArtefact artefact) {
    }

    @Override
    public void delete(String repositoryId, String path) {
    }

    @Override
    public void deleteRoot() {
    }

    @Override
    public void removePermissions(AProjectArtefact artefact) {
    }

    @Override
    public void removePermissions(String repositoryId, String path) {
    }

    @Override
    public void removePermissions(AProjectArtefact artefact, List<Sid> sids) {
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
    public void removePermissions(AProjectArtefact artefact, List<Permission> permissions, List<Sid> sids) {
    }

    @Override
    public void removePermissions(AProjectArtefact artefact, Map<Sid, List<Permission>> permissions) {
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
    public boolean isGranted(AProjectArtefact artefact, List<Permission> permissions) {
        return true;
    }

    @Override
    public void createAcl(String repositoryId, String path) {
    }

    @Override
    public void createAcl(AProjectArtefact artefact) {
    }
}
