package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;

public class UserProfileBaseModel extends UserInfoModel {

    @Parameter(description = "Show table headers")
    private boolean showHeader;

    @Parameter(description = "Show formulas")
    private boolean showFormulas;

    @Parameter(description = "Test results per page")
    private int testsPerPage;

    @Parameter(description = "Test failures only")
    private boolean testsFailuresOnly;

    @Parameter(description = "Number of failures per test")
    private int testsFailuresPerTest;

    @Parameter(description = "Show complex result")
    private boolean showComplexResult;

    @Parameter(description = "trace.field.showRealNumbers")
    private boolean showRealNumbers;

    @Parameter(description = "Default order")
    private String treeView;

    @Override
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserProfileBaseModel setEmail(String email) {
        return (UserProfileBaseModel) super.setEmail(email);
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
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
    public String getLastName() {
        return super.getLastName();
    }

    @Override
    public UserProfileBaseModel setLastName(String lastName) {
        return (UserProfileBaseModel) super.setLastName(lastName);
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public UserProfileBaseModel setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
        return this;
    }

    public boolean isShowFormulas() {
        return showFormulas;
    }

    public UserProfileBaseModel setShowFormulas(boolean showFormulas) {
        this.showFormulas = showFormulas;
        return this;
    }

    public int getTestsPerPage() {
        return testsPerPage;
    }

    public UserProfileBaseModel setTestsPerPage(int testsPerPage) {
        this.testsPerPage = testsPerPage;
        return this;
    }

    public boolean isTestsFailuresOnly() {
        return testsFailuresOnly;
    }

    public UserProfileBaseModel setTestsFailuresOnly(boolean testsFailuresOnly) {
        this.testsFailuresOnly = testsFailuresOnly;
        return this;
    }

    public int getTestsFailuresPerTest() {
        return testsFailuresPerTest;
    }

    public UserProfileBaseModel setTestsFailuresPerTest(int testsFailuresPerTest) {
        this.testsFailuresPerTest = testsFailuresPerTest;
        return this;
    }

    public boolean isShowComplexResult() {
        return showComplexResult;
    }

    public UserProfileBaseModel setShowComplexResult(boolean showComplexResult) {
        this.showComplexResult = showComplexResult;
        return this;
    }

    public boolean isShowRealNumbers() {
        return showRealNumbers;
    }

    public UserProfileBaseModel setShowRealNumbers(boolean showRealNumbers) {
        this.showRealNumbers = showRealNumbers;
        return this;
    }

    public String getTreeView() {
        return treeView;
    }

    public UserProfileBaseModel setTreeView(String treeView) {
        this.treeView = treeView;
        return this;
    }
}
