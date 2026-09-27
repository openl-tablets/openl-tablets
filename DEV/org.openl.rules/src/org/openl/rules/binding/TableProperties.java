package org.openl.rules.binding;

import java.util.Arrays;
import java.util.Objects;

import org.openl.rules.table.properties.ITableProperties;

public class TableProperties {
    private ITableProperties properties;

    public TableProperties(ITableProperties tableProperties) {
        this.properties = Objects.requireNonNull(tableProperties, "tableProperties cannot be null");
    }

    // <<< INSERT >>>
    public java.lang.String getName() {
        return properties.getName();
    }

    public java.lang.String getCategory() {
        return properties.getCategory();
    }

    public java.lang.String getCreatedBy() {
        return properties.getCreatedBy();
    }

    public java.util.Date getCreatedOn() {
        return properties.getCreatedOn();
    }

    public java.lang.String getModifiedBy() {
        return properties.getModifiedBy();
    }

    public java.util.Date getModifiedOn() {
        return properties.getModifiedOn();
    }

    public java.lang.String getDescription() {
        return properties.getDescription();
    }

    public java.lang.String[] getTags() {
        return properties.getTags();
    }

    public java.util.Date getEffectiveDate() {
        return properties.getEffectiveDate();
    }

    public java.util.Date getExpirationDate() {
        return properties.getExpirationDate();
    }

    public java.util.Date getStartRequestDate() {
        return properties.getStartRequestDate();
    }

    public java.util.Date getEndRequestDate() {
        return properties.getEndRequestDate();
    }

    public org.openl.rules.enumeration.CaRegionsEnum[] getCaRegions() {
        return properties.getCaRegions();
    }

    public org.openl.rules.enumeration.CaProvincesEnum[] getCaProvinces() {
        return properties.getCaProvinces();
    }

    public org.openl.rules.enumeration.CountriesEnum[] getCountry() {
        return properties.getCountry();
    }

    public org.openl.rules.enumeration.RegionsEnum[] getRegion() {
        return properties.getRegion();
    }

    public org.openl.rules.enumeration.CurrenciesEnum[] getCurrency() {
        return properties.getCurrency();
    }

    public org.openl.rules.enumeration.LanguagesEnum[] getLang() {
        return properties.getLang();
    }

    public java.lang.String[] getLob() {
        return properties.getLob();
    }

    public org.openl.rules.enumeration.OriginsEnum getOrigin() {
        return properties.getOrigin();
    }

    public org.openl.rules.enumeration.UsRegionsEnum[] getUsregion() {
        return properties.getUsregion();
    }

    public org.openl.rules.enumeration.UsStatesEnum[] getState() {
        return properties.getState();
    }

    public java.lang.String getVersion() {
        return properties.getVersion();
    }

    public java.lang.Boolean getActive() {
        return properties.getActive();
    }

    public java.lang.String getId() {
        return properties.getId();
    }

    public java.lang.String getBuildPhase() {
        return properties.getBuildPhase();
    }

    public org.openl.rules.enumeration.ValidateDTEnum getValidateDT() {
        return properties.getValidateDT();
    }

    public java.lang.Boolean getFailOnMiss() {
        return properties.getFailOnMiss();
    }

    public java.lang.String getScope() {
        return properties.getScope();
    }

    public java.lang.Integer getPriority() {
        return properties.getPriority();
    }

    public java.lang.String getDatatypePackage() {
        return properties.getDatatypePackage();
    }

    public java.lang.String getSpreadsheetResultPackage() {
        return properties.getSpreadsheetResultPackage();
    }

    public java.lang.Boolean getCacheable() {
        return properties.getCacheable();
    }

    public org.openl.rules.enumeration.RecalculateEnum getRecalculate() {
        return properties.getRecalculate();
    }

    public org.openl.rules.enumeration.DTEmptyResultProcessingEnum getEmptyResultProcessing() {
        return properties.getEmptyResultProcessing();
    }

    public java.lang.String getPrecision() {
        return properties.getPrecision();
    }

    public java.lang.Boolean getTableStructureDetails() {
        return properties.getTableStructureDetails();
    }

    public java.lang.Boolean getAutoType() {
        return properties.getAutoType();
    }

    public java.lang.Boolean getCalculateAllCells() {
        return properties.getCalculateAllCells();
    }

    public java.lang.Boolean getParallel() {
        return properties.getParallel();
    }

    public java.lang.String getNature() {
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

    private void appendProperty(StringBuilder sb, String name, Object value) {
        if (value != null) {
            sb.append(name).append(" = ").append(toString(value)).append("\r\n");
        }
    }

    private String toString(Object o) {
        if (o != null && o.getClass().isArray()) {
            return Arrays.deepToString((Object[]) o);
        }
        return o != null ? o.toString() : "null";
    }
}
