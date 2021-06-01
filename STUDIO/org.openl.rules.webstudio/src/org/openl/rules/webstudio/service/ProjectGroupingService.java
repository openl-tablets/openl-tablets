package org.openl.rules.webstudio.service;

import org.openl.rules.security.standalone.persistence.ProjectGrouping;

public class ProjectGroupingService {
    private static final String LEVEL_PREFIX = "project.grouping.level.";
    private UserSettingManagementService userSettingManagementService;

    public ProjectGrouping getProjectGrouping(String login) {
        final ProjectGrouping grouping = new ProjectGrouping();
        grouping.setLoginName(login);
        grouping.setGroup1(userSettingManagementService.getStringProperty(login, LEVEL_PREFIX + 1));
        grouping.setGroup2(userSettingManagementService.getStringProperty(login, LEVEL_PREFIX + 2));
        grouping.setGroup3(userSettingManagementService.getStringProperty(login, LEVEL_PREFIX + 3));
        return grouping;
    }

    public void save(ProjectGrouping projectGrouping) {
        final String login = projectGrouping.getLoginName();
        userSettingManagementService.setProperty(login, LEVEL_PREFIX + 1, projectGrouping.getGroup1());
        userSettingManagementService.setProperty(login, LEVEL_PREFIX + 2, projectGrouping.getGroup2());
        userSettingManagementService.setProperty(login, LEVEL_PREFIX + 3, projectGrouping.getGroup3());
    }

    public void setUserSettingManagementService(UserSettingManagementService userSettingManagementService) {
        this.userSettingManagementService = userSettingManagementService;
    }
}
