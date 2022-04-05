package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class UserProfileBaseModel extends UserInfoModel {

    @Schema(description = "Show table headers")
    private boolean showHeader;

    @Schema(description = "Show formulas")
    private boolean showFormulas;

    @Schema(description = "Test results per page")
    private int testsPerPage;

    @Schema(description = "Test failures only")
    private boolean testsFailuresOnly;

    @Schema(description = "Number of failures per test")
    private int testsFailuresPerTest;

    @Schema(description = "Show complex result")
    private boolean showComplexResult;

    @Schema(description = "trace.field.showRealNumbers")
    private boolean showRealNumbers;

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
}
