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
        if (properties.getName() != null) {
            sb.append("Name").append(" = ").append(toString(properties.getName())).append("\r\n");
        }
        if (properties.getCategory() != null) {
            sb.append("Category").append(" = ").append(toString(properties.getCategory())).append("\r\n");
        }
        if (properties.getCreatedBy() != null) {
            sb.append("CreatedBy").append(" = ").append(toString(properties.getCreatedBy())).append("\r\n");
        }
        if (properties.getCreatedOn() != null) {
            sb.append("CreatedOn").append(" = ").append(toString(properties.getCreatedOn())).append("\r\n");
        }
        if (properties.getModifiedBy() != null) {
            sb.append("ModifiedBy").append(" = ").append(toString(properties.getModifiedBy())).append("\r\n");
        }
        if (properties.getModifiedOn() != null) {
            sb.append("ModifiedOn").append(" = ").append(toString(properties.getModifiedOn())).append("\r\n");
        }
        if (properties.getDescription() != null) {
            sb.append("Description").append(" = ").append(toString(properties.getDescription())).append("\r\n");
        }
        if (properties.getTags() != null) {
            sb.append("Tags").append(" = ").append(toString(properties.getTags())).append("\r\n");
        }
        if (properties.getEffectiveDate() != null) {
            sb.append("EffectiveDate")
                    .append(" = ")
                    .append(toString(properties.getEffectiveDate()))
                    .append("\r\n");
        }
        if (properties.getExpirationDate() != null) {
            sb.append("ExpirationDate")
                    .append(" = ")
                    .append(toString(properties.getExpirationDate()))
                    .append("\r\n");
        }
        if (properties.getStartRequestDate() != null) {
            sb.append("StartRequestDate")
                    .append(" = ")
                    .append(toString(properties.getStartRequestDate()))
                    .append("\r\n");
        }
        if (properties.getEndRequestDate() != null) {
            sb.append("EndRequestDate")
                    .append(" = ")
                    .append(toString(properties.getEndRequestDate()))
                    .append("\r\n");
        }
        if (properties.getCaRegions() != null) {
            sb.append("CaRegions").append(" = ").append(toString(properties.getCaRegions())).append("\r\n");
        }
        if (properties.getCaProvinces() != null) {
            sb.append("CaProvinces").append(" = ").append(toString(properties.getCaProvinces())).append("\r\n");
        }
        if (properties.getCountry() != null) {
            sb.append("Country").append(" = ").append(toString(properties.getCountry())).append("\r\n");
        }
        if (properties.getRegion() != null) {
            sb.append("Region").append(" = ").append(toString(properties.getRegion())).append("\r\n");
        }
        if (properties.getCurrency() != null) {
            sb.append("Currency").append(" = ").append(toString(properties.getCurrency())).append("\r\n");
        }
        if (properties.getLang() != null) {
            sb.append("Lang").append(" = ").append(toString(properties.getLang())).append("\r\n");
        }
        if (properties.getLob() != null) {
            sb.append("Lob").append(" = ").append(toString(properties.getLob())).append("\r\n");
        }
        if (properties.getOrigin() != null) {
            sb.append("Origin").append(" = ").append(toString(properties.getOrigin())).append("\r\n");
        }
        if (properties.getUsregion() != null) {
            sb.append("Usregion").append(" = ").append(toString(properties.getUsregion())).append("\r\n");
        }
        if (properties.getState() != null) {
            sb.append("State").append(" = ").append(toString(properties.getState())).append("\r\n");
        }
        if (properties.getVersion() != null) {
            sb.append("Version").append(" = ").append(toString(properties.getVersion())).append("\r\n");
        }
        if (properties.getActive() != null) {
            sb.append("Active").append(" = ").append(toString(properties.getActive())).append("\r\n");
        }
        if (properties.getId() != null) {
            sb.append("Id").append(" = ").append(toString(properties.getId())).append("\r\n");
        }
        if (properties.getBuildPhase() != null) {
            sb.append("BuildPhase").append(" = ").append(toString(properties.getBuildPhase())).append("\r\n");
        }
        if (properties.getValidateDT() != null) {
            sb.append("ValidateDT").append(" = ").append(toString(properties.getValidateDT())).append("\r\n");
        }
        if (properties.getFailOnMiss() != null) {
            sb.append("FailOnMiss").append(" = ").append(toString(properties.getFailOnMiss())).append("\r\n");
        }
        if (properties.getScope() != null) {
            sb.append("Scope").append(" = ").append(toString(properties.getScope())).append("\r\n");
        }
        if (properties.getPriority() != null) {
            sb.append("Priority").append(" = ").append(toString(properties.getPriority())).append("\r\n");
        }
        if (properties.getDatatypePackage() != null) {
            sb.append("DatatypePackage")
                    .append(" = ")
                    .append(toString(properties.getDatatypePackage()))
                    .append("\r\n");
        }
        if (properties.getSpreadsheetResultPackage() != null) {
            sb.append("SpreadsheetResultPackage")
                    .append(" = ")
                    .append(toString(properties.getSpreadsheetResultPackage()))
                    .append("\r\n");
        }
        if (properties.getCacheable() != null) {
            sb.append("Cacheable").append(" = ").append(toString(properties.getCacheable())).append("\r\n");
        }
        if (properties.getRecalculate() != null) {
            sb.append("Recalculate").append(" = ").append(toString(properties.getRecalculate())).append("\r\n");
        }
        if (properties.getEmptyResultProcessing() != null) {
            sb.append("EmptyResultProcessing")
                    .append(" = ")
                    .append(toString(properties.getEmptyResultProcessing()))
                    .append("\r\n");
        }
        if (properties.getPrecision() != null) {
            sb.append("Precision").append(" = ").append(toString(properties.getPrecision())).append("\r\n");
        }
        if (properties.getTableStructureDetails() != null) {
            sb.append("TableStructureDetails")
                    .append(" = ")
                    .append(toString(properties.getTableStructureDetails()))
                    .append("\r\n");
        }
        if (properties.getAutoType() != null) {
            sb.append("AutoType").append(" = ").append(toString(properties.getAutoType())).append("\r\n");
        }
        if (properties.getCalculateAllCells() != null) {
            sb.append("CalculateAllCells")
                    .append(" = ")
                    .append(toString(properties.getCalculateAllCells()))
                    .append("\r\n");
        }
        if (properties.getParallel() != null) {
            sb.append("Parallel").append(" = ").append(toString(properties.getParallel())).append("\r\n");
        }
        if (properties.getNature() != null) {
            sb.append("Nature").append(" = ").append(toString(properties.getNature())).append("\r\n");
        }
        sb.append("}\r\n");
        return sb.toString();
    }
    // <<< END INSERT >>>

    private String toString(Object o) {
        if (o != null && o.getClass().isArray()) {
            return Arrays.deepToString((Object[]) o);
        }
        return o != null ? o.toString() : "null";
    }
}
