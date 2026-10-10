package org.openl.rules.rest.model;

import java.util.Optional;
import java.util.function.BiConsumer;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.webstudio.service.UserSettings;

/**
 * The profile of a user with the settings the user keeps for themselves.
 *
 * <p>Each setting is stored for the user under a key of its own, with its default in the configuration. The profile
 * reads the settings with {@link #load(UserSettings)} and saves them with {@link #store(BiConsumer)}.
 */
public class UserProfileBaseModel extends UserInfoModel {

    private static final String TABLE_VIEW = "table.view";
    private static final String TABLE_FORMULAS_SHOW = "table.formulas.show";
    private static final String TABLE_EXCEL_FORMATTING_SHOW = "table.excelFormatting.show";
    private static final String TEST_TESTS_PERPAGE = "test.tests.perpage";
    private static final String TEST_FAILURES_ONLY = "test.failures.only";
    private static final String TEST_FAILURES_PERTEST = "test.failures.pertest";
    private static final String TEST_RESULT_COMPLEX_SHOW = "test.result.complex.show";
    private static final String TRACE_REALNUMBERS_SHOW = "trace.realNumbers.show";

    @Getter
    @Parameter(description = "Show table headers")
    private @Nullable Boolean showHeader;

    @Getter
    @Parameter(description = "Show formulas")
    private @Nullable Boolean showFormulas;

    @Getter
    @Parameter(description = "Show the tables in the formatting and the colours of their Excel files, as Excel shows "
            + "them, rather than formatted with the table theme in the colours of the OpenL Studio theme. A view only: "
            + "the workbook is not changed")
    private @Nullable Boolean showExcelFormatting;

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

    /**
     * Reads the settings of the user into the profile.
     *
     * <p>A setting the user has not changed reads as its default. A setting with neither a value nor a default is
     * left out, but for the header, which is then hidden.
     */
    public void load(UserSettings settings) {
        showHeader = IXlsTableNames.VIEW_DEVELOPER.equals(settings.getString(TABLE_VIEW));
        showFormulas = settings.getBoolean(TABLE_FORMULAS_SHOW);
        showExcelFormatting = settings.getBoolean(TABLE_EXCEL_FORMATTING_SHOW);
        testsPerPage = settings.getInteger(TEST_TESTS_PERPAGE);
        testsFailuresOnly = settings.getBoolean(TEST_FAILURES_ONLY);
        testsFailuresPerTest = settings.getInteger(TEST_FAILURES_PERTEST);
        showComplexResult = settings.getBoolean(TEST_RESULT_COMPLEX_SHOW);
        showRealNumbers = settings.getBoolean(TRACE_REALNUMBERS_SHOW);
    }

    /**
     * Saves the settings the profile carries, each as the text of its value under its key.
     *
     * <p>A setting the profile leaves out keeps the value stored for the user.
     *
     * @param settings saves the value of a setting under its key
     */
    public void store(BiConsumer<String, String> settings) {
        Optional.ofNullable(showHeader)
                .map(shown -> shown ? IXlsTableNames.VIEW_DEVELOPER : IXlsTableNames.VIEW_BUSINESS)
                .ifPresent(view -> settings.accept(TABLE_VIEW, view));
        store(settings, TABLE_FORMULAS_SHOW, showFormulas);
        store(settings, TABLE_EXCEL_FORMATTING_SHOW, showExcelFormatting);
        store(settings, TEST_TESTS_PERPAGE, testsPerPage);
        store(settings, TEST_FAILURES_ONLY, testsFailuresOnly);
        store(settings, TEST_FAILURES_PERTEST, testsFailuresPerTest);
        store(settings, TEST_RESULT_COMPLEX_SHOW, showComplexResult);
        store(settings, TRACE_REALNUMBERS_SHOW, showRealNumbers);
    }

    private static void store(BiConsumer<String, String> settings, String key, @Nullable Object value) {
        Optional.ofNullable(value).ifPresent(set -> settings.accept(key, set.toString()));
    }

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

    public UserProfileBaseModel setShowExcelFormatting(@Nullable Boolean showExcelFormatting) {
        this.showExcelFormatting = showExcelFormatting;
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
