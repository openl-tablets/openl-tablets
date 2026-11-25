package org.openl.rules.binding;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.properties.ITableProperties;

/**
 * The properties of a rule that {@code $properties} and {@code $dispatchingProperties} return to rules.
 *
 * <p>The properties are read-only and always show the current values of the rule.
 *
 * <p>The class is public, so that a tool that reads the properties through the class of the object rather than
 * through {@link TableProperties}, as reflection does, can call its getters.
 */
@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class TablePropertiesImpl implements TableProperties {
    // @NonNull makes the constructor Lombok generates refuse null
    private final @NonNull ITableProperties properties;

    // <<< INSERT >>>
    @Override
    public java.lang.@Nullable String getName() {
        return properties.getName();
    }

    @Override
    public java.lang.@Nullable String getCategory() {
        return properties.getCategory();
    }

    @Override
    public java.lang.@Nullable String getCreatedBy() {
        return properties.getCreatedBy();
    }

    @Override
    public java.util.@Nullable Date getCreatedOn() {
        return properties.getCreatedOn();
    }

    @Override
    public java.lang.@Nullable String getModifiedBy() {
        return properties.getModifiedBy();
    }

    @Override
    public java.util.@Nullable Date getModifiedOn() {
        return properties.getModifiedOn();
    }

    @Override
    public java.lang.@Nullable String getDescription() {
        return properties.getDescription();
    }

    @Override
    public java.lang.String @Nullable [] getTags() {
        return properties.getTags();
    }

    @Override
    public java.util.@Nullable Date getEffectiveDate() {
        return properties.getEffectiveDate();
    }

    @Override
    public java.util.@Nullable Date getExpirationDate() {
        return properties.getExpirationDate();
    }

    @Override
    public java.util.@Nullable Date getStartRequestDate() {
        return properties.getStartRequestDate();
    }

    @Override
    public java.util.@Nullable Date getEndRequestDate() {
        return properties.getEndRequestDate();
    }

    @Override
    public org.openl.rules.enumeration.CaRegionsEnum @Nullable [] getCaRegions() {
        return properties.getCaRegions();
    }

    @Override
    public org.openl.rules.enumeration.CaProvincesEnum @Nullable [] getCaProvinces() {
        return properties.getCaProvinces();
    }

    @Override
    public org.openl.rules.enumeration.CountriesEnum @Nullable [] getCountry() {
        return properties.getCountry();
    }

    @Override
    public org.openl.rules.enumeration.RegionsEnum @Nullable [] getRegion() {
        return properties.getRegion();
    }

    @Override
    public org.openl.rules.enumeration.CurrenciesEnum @Nullable [] getCurrency() {
        return properties.getCurrency();
    }

    @Override
    public org.openl.rules.enumeration.LanguagesEnum @Nullable [] getLang() {
        return properties.getLang();
    }

    @Override
    public java.lang.String @Nullable [] getLob() {
        return properties.getLob();
    }

    @Override
    public org.openl.rules.enumeration.@Nullable OriginsEnum getOrigin() {
        return properties.getOrigin();
    }

    @Override
    public org.openl.rules.enumeration.UsRegionsEnum @Nullable [] getUsregion() {
        return properties.getUsregion();
    }

    @Override
    public org.openl.rules.enumeration.UsStatesEnum @Nullable [] getState() {
        return properties.getState();
    }

    @Override
    public java.lang.@Nullable String getVersion() {
        return properties.getVersion();
    }

    @Override
    public java.lang.@Nullable Boolean getActive() {
        return properties.getActive();
    }

    @Override
    public java.lang.@Nullable String getId() {
        return properties.getId();
    }

    @Override
    public java.lang.@Nullable String getBuildPhase() {
        return properties.getBuildPhase();
    }

    @Override
    public org.openl.rules.enumeration.@Nullable ValidateDTEnum getValidateDT() {
        return properties.getValidateDT();
    }

    @Override
    public java.lang.@Nullable Boolean getFailOnMiss() {
        return properties.getFailOnMiss();
    }

    @Override
    public java.lang.@Nullable String getScope() {
        return properties.getScope();
    }

    @Override
    public java.lang.@Nullable Integer getPriority() {
        return properties.getPriority();
    }

    @Override
    public java.lang.@Nullable String getDatatypePackage() {
        return properties.getDatatypePackage();
    }

    @Override
    public java.lang.@Nullable String getSpreadsheetResultPackage() {
        return properties.getSpreadsheetResultPackage();
    }

    @Override
    public java.lang.@Nullable Boolean getCacheable() {
        return properties.getCacheable();
    }

    @Override
    public org.openl.rules.enumeration.@Nullable RecalculateEnum getRecalculate() {
        return properties.getRecalculate();
    }

    @Override
    public org.openl.rules.enumeration.@Nullable DTEmptyResultProcessingEnum getEmptyResultProcessing() {
        return properties.getEmptyResultProcessing();
    }

    @Override
    public java.lang.@Nullable String getPrecision() {
        return properties.getPrecision();
    }

    @Override
    public java.lang.@Nullable Boolean getTableStructureDetails() {
        return properties.getTableStructureDetails();
    }

    @Override
    public java.lang.@Nullable Boolean getAutoType() {
        return properties.getAutoType();
    }

    @Override
    public java.lang.@Nullable Boolean getCalculateAllCells() {
        return properties.getCalculateAllCells();
    }

    @Override
    public java.lang.@Nullable Boolean getParallel() {
        return properties.getParallel();
    }

    @Override
    public java.lang.@Nullable String getNature() {
        return properties.getNature();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("{\r\n");
        appendProperty(sb, "Name", properties.getName());
        appendProperty(sb, "Category", properties.getCategory());
        appendProperty(sb, "CreatedBy", properties.getCreatedBy());
        appendProperty(sb, "CreatedOn", properties.getCreatedOn());
        appendProperty(sb, "ModifiedBy", properties.getModifiedBy());
        appendProperty(sb, "ModifiedOn", properties.getModifiedOn());
        appendProperty(sb, "Description", properties.getDescription());
        appendProperty(sb, "Tags", properties.getTags());
        appendProperty(sb, "EffectiveDate", properties.getEffectiveDate());
        appendProperty(sb, "ExpirationDate", properties.getExpirationDate());
        appendProperty(sb, "StartRequestDate", properties.getStartRequestDate());
        appendProperty(sb, "EndRequestDate", properties.getEndRequestDate());
        appendProperty(sb, "CaRegions", properties.getCaRegions());
        appendProperty(sb, "CaProvinces", properties.getCaProvinces());
        appendProperty(sb, "Country", properties.getCountry());
        appendProperty(sb, "Region", properties.getRegion());
        appendProperty(sb, "Currency", properties.getCurrency());
        appendProperty(sb, "Lang", properties.getLang());
        appendProperty(sb, "Lob", properties.getLob());
        appendProperty(sb, "Origin", properties.getOrigin());
        appendProperty(sb, "Usregion", properties.getUsregion());
        appendProperty(sb, "State", properties.getState());
        appendProperty(sb, "Version", properties.getVersion());
        appendProperty(sb, "Active", properties.getActive());
        appendProperty(sb, "Id", properties.getId());
        appendProperty(sb, "BuildPhase", properties.getBuildPhase());
        appendProperty(sb, "ValidateDT", properties.getValidateDT());
        appendProperty(sb, "FailOnMiss", properties.getFailOnMiss());
        appendProperty(sb, "Scope", properties.getScope());
        appendProperty(sb, "Priority", properties.getPriority());
        appendProperty(sb, "DatatypePackage", properties.getDatatypePackage());
        appendProperty(sb, "SpreadsheetResultPackage", properties.getSpreadsheetResultPackage());
        appendProperty(sb, "Cacheable", properties.getCacheable());
        appendProperty(sb, "Recalculate", properties.getRecalculate());
        appendProperty(sb, "EmptyResultProcessing", properties.getEmptyResultProcessing());
        appendProperty(sb, "Precision", properties.getPrecision());
        appendProperty(sb, "TableStructureDetails", properties.getTableStructureDetails());
        appendProperty(sb, "AutoType", properties.getAutoType());
        appendProperty(sb, "CalculateAllCells", properties.getCalculateAllCells());
        appendProperty(sb, "Parallel", properties.getParallel());
        appendProperty(sb, "Nature", properties.getNature());
        sb.append("}\r\n");
        return sb.toString();
    }
    // <<< END INSERT >>>

    private static void appendProperty(StringBuilder sb, String name, @Nullable Object value) {
        if (value != null) {
            sb.append(name).append(" = ").append(toString(value)).append("\r\n");
        }
    }

    private static String toString(Object value) {
        return value.getClass().isArray() ? Arrays.deepToString((Object[]) value) : value.toString();
    }
}
