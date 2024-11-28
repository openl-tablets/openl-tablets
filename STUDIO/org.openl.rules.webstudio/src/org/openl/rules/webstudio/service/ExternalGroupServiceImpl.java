package org.openl.rules.webstudio.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.openl.rules.security.Group;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.dao.ExternalGroupDao;
import org.openl.rules.security.standalone.persistence.ExternalGroup;

/**
 * External groups service implementation
 *
 * @author Vladyslav Pikus
 */
public class ExternalGroupServiceImpl implements ExternalGroupService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalGroupServiceImpl.class);

    private final ExternalGroupDao externalGroupDao;
    private final LockRegistry lockRegistry;
    private final TransactionTemplate txTemplate;

    public ExternalGroupServiceImpl(ExternalGroupDao externalGroupDao,
                                    LockRegistry lockRegistry,
                                    TransactionTemplate txTemplate) {
        this.externalGroupDao = externalGroupDao;
        this.lockRegistry = lockRegistry;
        this.txTemplate = txTemplate;
    }

    @Override
    @Transactional
    public void deleteAll() {
        externalGroupDao.deleteAll();
    }

    @Override
    public void mergeAllForUser(String loginName, Collection<? extends GrantedAuthority> externalGroups) {
        var lock = lockRegistry.obtain("externalGroupMergeLock_" + loginName);
        boolean lockAcquired = false;
        try {
            lockAcquired = lock.tryLock(30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new DataAccessResourceFailureException("Cannot acquire lock for user: " + loginName);
            }
            // transaction must be started after lock is acquired
            txTemplate.execute(status -> {
                externalGroupDao.deleteAllForUser(loginName);
                externalGroupDao.save(new BatchCreateExternalGroupCursor(loginName, externalGroups));
                return null;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Thread interrupted", e);
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public List<Group> findAllForUser(String loginName) {
        return externalGroupDao.findAllForUser(loginName)
                .stream()
                .map(ext -> new SimpleGroup(ext.getGroupName(), ext.getGroupName(), Collections.emptySet()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public long countAllForUser(String loginName) {
        return externalGroupDao.countAllForUser(loginName);
    }

    @Override
    @Transactional
    public List<Group> findMatchedForUser(String loginName) {
        return externalGroupDao.findMatchedForUser(loginName)
                .stream()
                .map(PrivilegesEvaluator::wrap)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public long countMatchedForUser(String loginName) {
        return externalGroupDao.countMatchedForUser(loginName);
    }

    @Override
    @Transactional
    public List<Group> findNotMatchedForUser(String loginName) {
        return externalGroupDao.findNotMatchedForUser(loginName)
                .stream()
                .map(ext -> new SimpleGroup(ext.getGroupName(), ext.getGroupName(), Collections.emptySet()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public long countNotMatchedForUser(String loginName) {
        return externalGroupDao.countNotMatchedForUser(loginName);
    }

    @Override
    @Transactional
    public List<Group> findAllByName(String groupName, int limit) {
        return externalGroupDao.findAllByName(groupName, limit)
                .stream()
                .map(ext -> new SimpleGroup(ext, ext, Collections.emptySet()))
                .collect(Collectors.toList());
    }

    private static class BatchCreateExternalGroupCursor implements Iterable<ExternalGroup> {

        private final String loginName;
        private final Collection<? extends GrantedAuthority> externalGroups;

        public BatchCreateExternalGroupCursor(String loginName, Collection<? extends GrantedAuthority> externalGroups) {
            this.externalGroups = externalGroups;
            this.loginName = loginName;
        }

        @Override
        public Iterator<ExternalGroup> iterator() {
            final Iterator<? extends GrantedAuthority> it = externalGroups.iterator();
            return new Iterator<ExternalGroup>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public ExternalGroup next() {
                    GrantedAuthority group = it.next();
                    ExternalGroup externalGroup = new ExternalGroup();
                    externalGroup.setLoginName(loginName);
                    externalGroup.setGroupName(group.getAuthority());
                    return externalGroup;
                }
            };
        }

    }

}
