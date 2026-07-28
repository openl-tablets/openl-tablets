package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;

public class UserProfileBaseModel extends UserInfoModel {

    @Getter
    @Parameter(description = "Show table headers")
    private boolean showHeader;

    @Getter
    @Parameter(description = "Show formulas")
    private boolean showFormulas;

    @Getter
    @Parameter(description = "Test results per page")
    private int testsPerPage;

    @Getter
    @Parameter(description = "Test failures only")
    private boolean testsFailuresOnly;

    @Getter
    @Parameter(description = "Number of failures per test")
    private int testsFailuresPerTest;

    @Getter
    @Parameter(description = "Show complex result")
    private boolean showComplexResult;

    @Getter
    @Parameter(description = "trace.field.showRealNumbers")
    private boolean showRealNumbers;

    @Getter
    @Parameter(description = "Default order")
    private String treeView;

    @Override
    public UserProfileBaseModel setEmail(String email) {
        return (UserProfileBaseModel) super.setEmail(email);
    }

    @Override
    public UserProfileBaseModel setDisplayName(String displayName) {
        return (UserProfileBaseModel) super.setDisplayName(displayName);
    }

    @Override
    public UserProfileBaseModel setFirstName(String firstName) {
        return (UserProfileBaseModel) super.setFirstName(firstName);
    }

    @Override
    public UserProfileBaseModel setLastName(String lastName) {
        return (UserProfileBaseModel) super.setLastName(lastName);
    }

    public UserProfileBaseModel setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
        return this;
    }

    public UserProfileBaseModel setShowFormulas(boolean showFormulas) {
        this.showFormulas = showFormulas;
        return this;
    }

    public UserProfileBaseModel setTestsPerPage(int testsPerPage) {
        this.testsPerPage = testsPerPage;
        return this;
    }

    public UserProfileBaseModel setTestsFailuresOnly(boolean testsFailuresOnly) {
        this.testsFailuresOnly = testsFailuresOnly;
        return this;
    }

    public UserProfileBaseModel setTestsFailuresPerTest(int testsFailuresPerTest) {
        this.testsFailuresPerTest = testsFailuresPerTest;
        return this;
    }

    public UserProfileBaseModel setShowComplexResult(boolean showComplexResult) {
        this.showComplexResult = showComplexResult;
        return this;
    }

    public UserProfileBaseModel setShowRealNumbers(boolean showRealNumbers) {
        this.showRealNumbers = showRealNumbers;
        return this;
    }

    public UserProfileBaseModel setTreeView(String treeView) {
        this.treeView = treeView;
        return this;
    }
}
