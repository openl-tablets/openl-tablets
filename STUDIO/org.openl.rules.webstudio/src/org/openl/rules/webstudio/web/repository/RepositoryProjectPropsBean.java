package org.openl.rules.webstudio.web.repository;

import org.openl.rules.common.ArtefactType;
import org.openl.rules.common.InheritedProperty;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.RulesRepositoryArtefact;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.webstudio.web.tableeditor.PropertyRow;
import org.openl.rules.webstudio.web.tableeditor.PropertyRowType;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean
@ViewScoped
public class RepositoryProjectPropsBean {
    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    private final static String DBP_GROUP_NAME = "Business Dimension";
    private final static String OTHER_PROP_GROUP_NAME = "Custom Properties";

    private final Logger log = LoggerFactory.getLogger(RepositoryProjectPropsBean.class);
    private List<TablePropertyDefinition> bussinedDimensionProps;
    private String propertyToAdd;
    private List<PropertyRow> propsStore;
    private String storeProjName;
    private String storeProjVersion;
    private Map<String, String> attribs;
    private RepositoryAttributeUtils repoAttrsUtils;

    public RepositoryProjectPropsBean() {
        bussinedDimensionProps = TablePropertyDefinitionUtils.getDimensionalTableProperties();

        RepositoryArtefactPropsHolder rap = new RepositoryArtefactPropsHolder();
        attribs = rap.getProps();

        if (attribs == null) {
            attribs = new HashMap<String, String>();
        }

        if (repositoryTreeState != null && repositoryTreeState.getSelectedNode() != null
                && storeProjName == null) {
            storeProjName = repositoryTreeState.getSelectedNode().getData().getName();
            storeProjVersion = repositoryTreeState.getSelectedNode().getData().getVersion().getVersionName();
        }

        repoAttrsUtils = new RepositoryAttributeUtils();
    }

    /**
     * Sets property to rules repository.
     *
     * @param propName  name of property
     * @param propValue value of property
     */
    private void setProperty(String propName, Object propValue) {
        try {
            Map<String, Object> props = getProps();
            if (props == null) {
                props = new HashMap<String, Object>();
            } else {
                props = new HashMap<String, Object>(props);
            }

            props.put(propName, propValue);
            repositoryTreeState.getSelectedNode().getData().setProps(props);
        } catch (PropertyException e) {
            log.warn("Failed to set {} !", propName, e);
        }
    }

    private void removeProperty(String propName) {
        try {
            Map<String, Object> props = getProps();
            if (props == null) {
                props = new HashMap<String, Object>();
            } else {
                props = new HashMap<String, Object>(props);
            }

            props.remove(propName);
            repositoryTreeState.getSelectedNode().getData().setProps(props);
        } catch (PropertyException e) {
            log.warn("Failed to set {} !", propName, e);
        }
    }

    /**
     * Gets all properties from a rules repository.
     *
     * @return map of properties
     */
    private Map<String, Object> getProps() {
        RulesRepositoryArtefact dataBean = repositoryTreeState.getSelectedNode().getData();

        if (dataBean != null) {
            Map<String, Object> returnProps = dataBean.getProps();

            if (returnProps != null) {
                return returnProps;
            } else {
                return new HashMap<String, Object>();
            }
        }
        return new HashMap<String, Object>();
    }

    public List<TablePropertyDefinition> getBussinedDimensionProps() {
        return bussinedDimensionProps;
    }

    public void setBussinedDimensionProps(List<TablePropertyDefinition> bussinedDimensionProps) {
        this.bussinedDimensionProps = bussinedDimensionProps;
    }

    public List<SelectItem> getPropsForSelect() {
        List<SelectItem> props = new ArrayList<SelectItem>();

        //Set bussines dimension 
        for (TablePropertyDefinition prop : getBussinedDimensionProps()) {
            boolean presents = false;

            for (PropertyRow row : propsStore) {
                if (row.getType().equals(PropertyRowType.PROPERTY)) {
                    TableProperty tp = (TableProperty) row.getData();

                    if (tp.getName().equals(prop.getName())) {
                        presents = true;
                    }
                }
            }

            if (!presents) {
                props.add(new SelectItem(prop.getName(), prop.getDisplayName()));
            }
        }

        //set attribs
        Map<String, String> attribs = repoAttrsUtils.getActiveAttribs();

        if (attribs != null) {
            for (Map.Entry<String, String> entry: attribs.entrySet()) {
                boolean presents = false;

                String key = entry.getKey();
                for (PropertyRow row : propsStore) {
                    if (row.getType().equals(PropertyRowType.PROPERTY)) {
                        TableProperty tp = (TableProperty) row.getData();

                        if (tp.getName().equals(key)) {
                            presents = true;
                        }
                    }
                }

                if (!presents) {
                    props.add(new SelectItem(key, entry.getValue()));
                }
            }
        }

        return props;
    }

    public String getPropertyToAdd() {
        return propertyToAdd;
    }

    public void setPropertyToAdd(String propertyToAdd) {
        this.propertyToAdd = propertyToAdd;
    }

    public void addNew() {
        PropertyRow selectProp = getEmptyPropByName(propertyToAdd);

        if (selectProp == null) {
            return;
        }

        if (attribs != null) {
            if (attribs.containsKey(propertyToAdd)) {
                int groupHeaderId = getGroupFirstPosition(OTHER_PROP_GROUP_NAME);
                
                /*Add group header if needed*/
                if (propsStore.isEmpty() || groupHeaderId == propsStore.size()) {
                    //add other props group header
                    propsStore.add(groupHeaderId, new PropertyRow(PropertyRowType.GROUP, OTHER_PROP_GROUP_NAME));
                }

                propsStore.add(propsStore.size(), selectProp);
                return;
            }
        }


        int groupHeaderId = getGroupFirstPosition(DBP_GROUP_NAME);
        
        /*Add group header if needed*/
        if (propsStore.isEmpty() || groupHeaderId == propsStore.size()) {
            //add other props group header
            propsStore.add(0, new PropertyRow(PropertyRowType.GROUP, DBP_GROUP_NAME));
        }

        propsStore.add(getGroupFirstPosition(OTHER_PROP_GROUP_NAME), selectProp);
    }

    private int getGroupFirstPosition(String groupName) {
        int i = 0;

        for (PropertyRow row : propsStore) {
            if (row.getType().equals(PropertyRowType.GROUP)) {
                if (row.getData().toString().equals(groupName)) {
                    return i;
                }
            }

            i++;
        }

        return propsStore.size();
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public List<PropertyRow> getPropsStore() {
        /*
         * propsStore = initSettedProps(); return propsStore;
         */
        if (propsStore == null || propsStore.isEmpty() || storeProjName == null
                || !isTheSameBean(repositoryTreeState.getSelectedNode().getData())) {

            if (repositoryTreeState.getSelectedNode().getData() != null) {
                storeProjName = repositoryTreeState.getSelectedNode().getData().getName();
                storeProjVersion = repositoryTreeState.getSelectedNode().getData().getVersion().getVersionName();
            }

            propsStore = initSettedProps();
        }

        return propsStore;
    }

    private void refresh() {
        propsStore = initSettedProps();
    }

    private boolean isTheSameBean(RulesRepositoryArtefact obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof AProjectArtefact) {
            if (storeProjName != null && storeProjVersion != null) {
                if (((AProjectArtefact) obj).getName().equals(storeProjName) && ((AProjectArtefact) obj).getVersion().getVersionName().equals(storeProjVersion)) {
                    return true;
                }
            }

            return false;
        }

        return false;
    }

    public void setPropsStore(List<PropertyRow> propsStore) {
        this.propsStore = propsStore;
    }

    public List<PropertyRow> initSettedProps() {
        Map<String, InheritedProperty> inheritedProp = getInheritedProps();
        Map<String, Object> settedPropsList = getProps();

        return makeTableProps(inheritedProp, settedPropsList, false);
    }

    private PropertyRow getEmptyPropByName(String propertyToAdd) {
        for (TablePropertyDefinition tpd : bussinedDimensionProps) {
            if (tpd.getName().equals(propertyToAdd)) {
                TableProperty tp = new TableProperty(tpd);

                return new PropertyRow(PropertyRowType.PROPERTY, tp);
            }
        }

        if (StringUtils.isNotBlank(propertyToAdd) && attribs.containsKey(propertyToAdd)) {
            return repoAttrsUtils.getPropertyRowByAttrName(propertyToAdd);
        }

        return null;
    }

    public void remove(TableProperty selectTProp) {
        for (PropertyRow row : propsStore) {
            if (row.getType().equals(PropertyRowType.PROPERTY)) {
                TableProperty tProp = (TableProperty) row.getData();
                if (tProp.getName().equals(selectTProp.getName())) {
                    propsStore.remove(row);

                    removeProperty(selectTProp.getName());
                    refresh();
                    return;
                }
            }
        }
    }

    public void save(TableProperty selectTProp) {
        if (StringUtils.isNotBlank(selectTProp.getDisplayValue())) {
            for (PropertyRow row : propsStore) {
                if (row.getType().equals(PropertyRowType.PROPERTY)) {
                    TableProperty tProp = (TableProperty) row.getData();
                    if (tProp.getName().equals(selectTProp.getName())) {
                        tProp.setValue(selectTProp.getDisplayValue());

                        setProperty(selectTProp.getName(), selectTProp.getDisplayValue());
                        refresh();
                        return;
                    }
                }
            }
        }
    }

    public static List<PropertyRow> getProjectPropsToolTip(RulesRepositoryArtefact dataBean) {
        Map<String, InheritedProperty> inheritedProp = dataBean.getInheritedProps();
        Map<String, Object> settedPropsList = dataBean.getProps();

        return makeTableProps(inheritedProp, settedPropsList, true);
    }

    public static List<TableProperty> getVersionPropToolTip(Map<String, Object> props) {
        List<TablePropertyDefinition> bussinedDimensionProps = TablePropertyDefinitionUtils
                .getDimensionalTableProperties();

        List<TableProperty> ptList = new ArrayList<TableProperty>();

        for (TablePropertyDefinition propDefinition : bussinedDimensionProps) {
            if (props.containsKey(propDefinition.getName())) {
                TableProperty tProp = new TableProperty(propDefinition);

                try {
                    tProp.setValue(props.get(propDefinition.getName()));
                } catch (Exception e) {
                    tProp.setValue("");
                }

                ptList.add(tProp);
            }
        }

        return ptList;
    }

    private static TablePropertyDefinition getPropDefByName(String name) {
        List<TablePropertyDefinition> bussinedDimensionProps = TablePropertyDefinitionUtils
                .getDimensionalTableProperties();

        for (TablePropertyDefinition tpd : bussinedDimensionProps) {
            if (tpd.getName().equals(name)) {
                return tpd;
            }
        }

        return null;
    }

    private Map<String, InheritedProperty> getInheritedProps() {
        AProjectArtefact dataBean = repositoryTreeState.getSelectedNode().getData();

        if (dataBean != null) {
            Map<String, InheritedProperty> returnProps = dataBean.getInheritedProps();

            if (returnProps != null) {
                return returnProps;
            } else {
                return new HashMap<String, InheritedProperty>();
            }
        }

        return new HashMap<String, InheritedProperty>();
    }

    private static PropertyRow addGroupHeaderRow(String headerText) {
        return new PropertyRow(PropertyRowType.GROUP, headerText);
    }

    private static List<PropertyRow> makeTableProps(Map<String, InheritedProperty> inheritedProp, Map<String, Object> settedPropsList, boolean onlyBDProps) {
        RepositoryAttributeUtils repoAttrsUtils = new RepositoryAttributeUtils();
        List<PropertyRow> propsStore = new ArrayList<PropertyRow>();
        List<TablePropertyDefinition> bussinesDimensionProps = TablePropertyDefinitionUtils
                .getDimensionalTableProperties();

        Map<String, String> customAttrs = repoAttrsUtils.getActiveAttribs();

        /*Add inherited Props*/
        boolean firstBDRow = true;

        if (settedPropsList != null) {
            for (TablePropertyDefinition propDefinition : bussinesDimensionProps) {
                if (inheritedProp.containsKey(propDefinition.getName())) {
                    if (!settedPropsList.containsKey(propDefinition.getName())) {
                        if (firstBDRow && !onlyBDProps) {
                            propsStore.add(addGroupHeaderRow(DBP_GROUP_NAME));
                            firstBDRow = false;
                        }

                        TableProperty prop = new TableProperty(propDefinition);
                        try {
                            InheritedProperty inhProp = inheritedProp.get(propDefinition.getName());

                            prop.setValue(inhProp.getValue());

                            if (inhProp.getTypeOfNode().equals(ArtefactType.FOLDER)) {
                                prop.setInheritanceLevel(InheritanceLevel.FOLDER);
                            } else {
                                prop.setInheritanceLevel(InheritanceLevel.PROJECT);
                            }

                            prop.setInheritedTableName(inhProp.getNameOfNode());
                        } catch (Exception e) {

                        }

                        PropertyRow row = new PropertyRow(PropertyRowType.PROPERTY, prop);
                        propsStore.add(row);
                    }
                }
            }
        }
        
        /*Add bd project bd properties*/
        if (bussinesDimensionProps != null && settedPropsList != null) {
            for (TablePropertyDefinition propDefinition : bussinesDimensionProps) {
                if (settedPropsList.containsKey(propDefinition.getName())) {
                    if (firstBDRow && !onlyBDProps) {
                        propsStore.add(addGroupHeaderRow(DBP_GROUP_NAME));
                        firstBDRow = false;
                    }

                    TableProperty prop = new TableProperty(propDefinition);
                    try {
                        prop.setValue(settedPropsList.get(propDefinition.getName()));
                    } catch (Exception e) {

                    }

                    PropertyRow row = new PropertyRow(PropertyRowType.PROPERTY, prop);
                    propsStore.add(row);
                }
            }
        }

        if (!onlyBDProps && customAttrs != null) {
            boolean firstRow = true;

            for (String key : customAttrs.keySet()) {
                if (settedPropsList.containsKey(key)) {
                    if (firstRow) {
                        propsStore.add(addGroupHeaderRow(OTHER_PROP_GROUP_NAME));
                        firstRow = false;
                    }

                    try {
                        PropertyRow row = repoAttrsUtils.getPropertyRowByAttrName(key);
                        ((TableProperty) row.getData()).setValue(settedPropsList.get(key));

                        propsStore.add(row);
                    } catch (Exception e) {

                    }
                }
            }
        }

        return propsStore;
    }

}
