package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class UserProfileBaseModel extends UserInfoModel {

    @Getter
    @Parameter(description = "Show table headers")
    private @Nullable Boolean showHeader;

    @Getter
    @Parameter(description = "Show formulas")
    private @Nullable Boolean showFormulas;

    @Getter
    @Parameter(description = "Test results per page, or -1 for all")
    private @Nullable Integer testsPerPage;

    @Getter
    @Parameter(description = "Test failures only")
    private @Nullable Boolean testsFailuresOnly;

    @Getter
    @Parameter(description = "Number of failures per test, or -1 for all")
    private @Nullable Integer testsFailuresPerTest;

    @Getter
    @Parameter(description = "Show complex result")
    private @Nullable Boolean showComplexResult;

    @Getter
    @Parameter(description = "trace.field.showRealNumbers")
    private @Nullable Boolean showRealNumbers;

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

    public UserProfileBaseModel setShowHeader(@Nullable Boolean showHeader) {
        this.showHeader = showHeader;
        return this;
    }

    public UserProfileBaseModel setShowFormulas(@Nullable Boolean showFormulas) {
        this.showFormulas = showFormulas;
        return this;
    }

    public UserProfileBaseModel setTestsPerPage(@Nullable Integer testsPerPage) {
        this.testsPerPage = testsPerPage;
        return this;
    }

    public UserProfileBaseModel setTestsFailuresOnly(@Nullable Boolean testsFailuresOnly) {
        this.testsFailuresOnly = testsFailuresOnly;
        return this;
    }

    public UserProfileBaseModel setTestsFailuresPerTest(@Nullable Integer testsFailuresPerTest) {
        this.testsFailuresPerTest = testsFailuresPerTest;
        return this;
    }

    public UserProfileBaseModel setShowComplexResult(@Nullable Boolean showComplexResult) {
        this.showComplexResult = showComplexResult;
        return this;
    }

    public UserProfileBaseModel setShowRealNumbers(@Nullable Boolean showRealNumbers) {
        this.showRealNumbers = showRealNumbers;
        return this;
    }
}
