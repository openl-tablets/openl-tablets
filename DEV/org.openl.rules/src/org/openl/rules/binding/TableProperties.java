package org.openl.rules.binding;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The properties of a rule, as rules read them through {@code $properties} and {@code $dispatchingProperties}.
 *
 * <p>Rules only read the properties. A Data or a Test table still sets them on the object it builds, so a test checks
 * the properties a rule returns, such as {@code _res_.state}.
 */
@NullMarked
public interface TableProperties {

    // <<< INSERT >>>

    java.lang.@Nullable String getName();

    java.lang.@Nullable String getCategory();

    java.lang.@Nullable String getCreatedBy();

    java.util.@Nullable Date getCreatedOn();

    java.lang.@Nullable String getModifiedBy();

    java.util.@Nullable Date getModifiedOn();

    java.lang.@Nullable String getDescription();

    java.lang.String @Nullable [] getTags();

    java.util.@Nullable Date getEffectiveDate();

    java.util.@Nullable Date getExpirationDate();

    java.util.@Nullable Date getStartRequestDate();

    java.util.@Nullable Date getEndRequestDate();

    org.openl.rules.enumeration.CaRegionsEnum @Nullable [] getCaRegions();

    org.openl.rules.enumeration.CaProvincesEnum @Nullable [] getCaProvinces();

    org.openl.rules.enumeration.CountriesEnum @Nullable [] getCountry();

    org.openl.rules.enumeration.RegionsEnum @Nullable [] getRegion();

    org.openl.rules.enumeration.CurrenciesEnum @Nullable [] getCurrency();

    org.openl.rules.enumeration.LanguagesEnum @Nullable [] getLang();

    java.lang.String @Nullable [] getLob();

    org.openl.rules.enumeration.@Nullable OriginsEnum getOrigin();

    org.openl.rules.enumeration.UsRegionsEnum @Nullable [] getUsregion();

    org.openl.rules.enumeration.UsStatesEnum @Nullable [] getState();

    java.lang.@Nullable String getVersion();

    java.lang.@Nullable Boolean getActive();

    java.lang.@Nullable String getId();

    java.lang.@Nullable String getBuildPhase();

    org.openl.rules.enumeration.@Nullable ValidateDTEnum getValidateDT();

    java.lang.@Nullable Boolean getFailOnMiss();

    java.lang.@Nullable String getScope();

    java.lang.@Nullable Integer getPriority();

    java.lang.@Nullable String getDatatypePackage();

    java.lang.@Nullable String getSpreadsheetResultPackage();

    java.lang.@Nullable Boolean getCacheable();

    org.openl.rules.enumeration.@Nullable RecalculateEnum getRecalculate();

    org.openl.rules.enumeration.@Nullable DTEmptyResultProcessingEnum getEmptyResultProcessing();

    java.lang.@Nullable String getPrecision();

    java.lang.@Nullable Boolean getTableStructureDetails();

    java.lang.@Nullable Boolean getAutoType();

    java.lang.@Nullable Boolean getCalculateAllCells();

    java.lang.@Nullable Boolean getParallel();

    java.lang.@Nullable String getNature();
    // <<< END INSERT >>>
}
