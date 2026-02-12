package org.openl.studio.security;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.util.StringUtils;

/**
 * Interceptor that checks if the authenticated user has email and display name configured.
 *
 * @see CommitInfoRequired
 * @see CommitInfoPostProcessor
 */
@Component
public class CommitInfoInterceptor implements MethodInterceptor {

    private final CurrentUserInfo currentUserInfo;
    private final UserManagementService userManagementService;

    public CommitInfoInterceptor(@Lazy CurrentUserInfo currentUserInfo,
                                 @Lazy UserManagementService userManagementService) {
        this.currentUserInfo = currentUserInfo;
        this.userManagementService = userManagementService;
    }

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        var username = currentUserInfo.getUserName();
        if (username != null) {
            var user = userManagementService.getUser(username);
            if (user == null || StringUtils.isBlank(user.getEmail()) || StringUtils.isBlank(user.getDisplayName())) {
                throw new BadRequestException("commit-info.required.message");
            }
        }
        return invocation.proceed();
    }
}
