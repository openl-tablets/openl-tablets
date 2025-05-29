package org.openl.rules.rest.acl.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.transaction.support.TransactionTemplate;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.model.AclResourceAccess;
import org.openl.rules.rest.acl.model.BulkAclOverwriteRequest;
import org.openl.rules.webstudio.security.SecureDeploymentRepositoryService;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.webstudio.service.GroupManagementService;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.security.acl.JdbcMutableAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;

public class BulkAclOverwriteServiceImpl implements BulkAclOverwriteService {

    private static final Logger LOG = LoggerFactory.getLogger(BulkAclOverwriteServiceImpl.class);

    private static final String LOCK_NAME = "bulk-acl-overwrite-lock";

    private final UserManagementService userManagementService;
    private final GroupManagementService groupManagementService;
    private final RepositoryAclServiceProvider aclServiceProvider;
    private final LockRegistry lockRegistry;
    private final TransactionTemplate txTemplate;
    private final SecureDesignTimeRepository designTimeRepository;
    private final SecureDeploymentRepositoryService deploymentRepositoryService;
    private final JdbcMutableAclService aclService;

    public BulkAclOverwriteServiceImpl(UserManagementService userManagementService,
                                       GroupManagementService groupManagementService,
                                       RepositoryAclServiceProvider aclServiceProvider,
                                       LockRegistry lockRegistry,
                                       TransactionTemplate txTemplate,
                                       SecureDesignTimeRepository designTimeRepository,
                                       SecureDeploymentRepositoryService deploymentRepositoryService,
                                       JdbcMutableAclService aclService) {
        this.userManagementService = userManagementService;
        this.groupManagementService = groupManagementService;
        this.aclServiceProvider = aclServiceProvider;
        this.lockRegistry = lockRegistry;
        this.txTemplate = txTemplate;
        this.designTimeRepository = designTimeRepository;
        this.deploymentRepositoryService = deploymentRepositoryService;
        this.aclService = aclService;
    }

    @Override
    public void process(BulkAclOverwriteRequest request) {
        var lock = lockRegistry.obtain(LOCK_NAME);
        boolean lockAcquired = false;
        try {
            lockAcquired = lock.tryLock(30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new IllegalStateException("Cannot acquire lock for bulk ACL overwrite");
            }
            // transaction must be started after lock is acquired
            processInTransaction(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Bulk ACL overwrite interrupted", e);
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
        }
    }

    private void processInTransaction(BulkAclOverwriteRequest request) {
        txTemplate.execute(status -> {
            syncGroups(request.getGroups());
            syncUsers(request.getUsers());
            syncRepositoryRootsPermissions(request.getResources());
            syncRepositoriesPermissions(request.getResources());
            syncProjectPermissions(request.getResources());
            return null;
        });
    }

    private void syncGroups(Set<String> groupNames) {
        var existingGroupNames = groupManagementService.getGroupNames();

        // Create a new group for each name in groupNames that does not already exist
        groupNames.stream()
                .filter(groupName -> !existingGroupNames.contains(groupName))
                .forEach(groupName -> {
                    groupManagementService.addGroup(groupName, null);
                });

        // Delete sids from ACL that are in existingGroupNames but not in groupNames
        existingGroupNames.stream()
                .filter(groupName -> !groupNames.contains(groupName))
                .forEach(toDelete -> {
                    aclService.deleteSid(new GrantedAuthoritySid(toDelete));
                });
    }

    private void syncUsers(Set<String> userNames) {
        var existingUserNames = userManagementService.getUserNames();

        // Delete users that are in the database but not in the provided set
        existingUserNames.stream()
                .filter(user -> !userNames.contains(user))
                .map(PrincipalSid::new)
                .forEach(aclService::deleteSid);
    }

    private void syncRepositoryRootsPermissions(List<AclResourceAccess> resourceAccesses) {
        // Delete all permissions first
        aclServiceProvider.getDesignRepoAclService().removeRootPermissions();
        aclServiceProvider.getProdRepoAclService().removeRootPermissions();

        // Add permissions for repository roots
        for (var resourceAccess : resourceAccesses) {
            var ref = resourceAccess.getResourceRef();
            if (!ref.isRepositoryRoot()) {
                continue;
            }
            var aclService = aclServiceProvider.getAclService(ref.getRepositoryType().getType());
            Stream.ofNullable(resourceAccess.getAces())
                    .flatMap(Collection::stream)
                    .forEach(ace -> {
                        var sid = ace.getSub().toSid();
                        aclService.addRootPermissions(sid, ace.getRole().getCumulativePermission());
                    });
        }
    }

    private void syncRepositoriesPermissions(List<AclResourceAccess> resourceAccesses) {
        // Delete all permissions first
        designTimeRepository.getRepositories().stream()
                .map(Repository::getId)
                .forEach(repoId -> {
                    var aclService = aclServiceProvider.getDesignRepoAclService();
                    aclService.removePermissions(repoId, null);
                });

        deploymentRepositoryService.getRepositories().stream()
                .map(RepositoryConfiguration::getId)
                .forEach(repoId -> {
                    var aclService = aclServiceProvider.getProdRepoAclService();
                    aclService.removePermissions(repoId, null);
                });

        // Add permissions for repositories
        for (var resourceAccess : resourceAccesses) {
            var ref = resourceAccess.getResourceRef();
            if (!ref.isRepository()) {
                continue;
            }
            var aclService = aclServiceProvider.getAclService(ref.getRepositoryType().getType());
            Stream.ofNullable(resourceAccess.getAces())
                    .flatMap(Collection::stream)
                    .forEach(ace -> {
                        var sid = ace.getSub().toSid();
                        aclService.addPermissions(ref.getRepositoryId(), null, sid, ace.getRole().getCumulativePermission());
                    });
        }
    }

    private void syncProjectPermissions(List<AclResourceAccess> resourceAccesses) {
        // Delete all permissions first
        var aclService = aclServiceProvider.getDesignRepoAclService();
        designTimeRepository.getProjects().forEach(aclService::removePermissions);

        // Add permissions for repositories
        for (var resourceAccess : resourceAccesses) {
            var ref = resourceAccess.getResourceRef();
            if (!ref.isProject()) {
                continue;
            }
            try {
                var project = designTimeRepository.getProject(ref.getRepositoryId(), ref.getProjectName());
                Stream.ofNullable(resourceAccess.getAces())
                        .flatMap(Collection::stream)
                        .forEach(ace -> {
                            var sid = ace.getSub().toSid();
                            aclService.addPermissions(project, sid, ace.getRole().getCumulativePermission());
                        });
            } catch (ProjectException ignored) {

            }
        }
    }

}
