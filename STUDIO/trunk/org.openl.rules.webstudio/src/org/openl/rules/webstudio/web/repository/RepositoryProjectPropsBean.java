package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ArtefactType;
import org.openl.rules.common.InheritedProperty;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.RulesRepositoryArtefact;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.tableeditor.renderkit.TableProperty;

@ManagedBean
@ViewScoped
public class RepositoryProjectPropsBean {
    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    private final Log log = LogFactory.getLog(RepositoryProjectPropsBean.class);
    private List<TablePropertyDefinition> bussinedDimensionProps;
    private String propertyToAdd;
    private List<TableProperty> propsStore;
    private String storeProjName;
    private String storeProjVersion;

    public RepositoryProjectPropsBean() {
        bussinedDimensionProps = TablePropertyDefinitionUtils.getDimensionalTableProperties();

        if (repositoryTreeState != null && repositoryTreeState.getSelectedNode() != null
                && storeProjName == null) {
            storeProjName = repositoryTreeState.getSelectedNode().getData().getName();
            storeProjVersion = repositoryTreeState.getSelectedNode().getData().getVersion().getVersionName();
        }
    }

    /**
     * Sets property to rules repository.
     * 
     * @param propName name of property
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
            /*
             * String propUIName = getPropUIName(propName);
             * log.error("Failed to set " + propUIName + "!", e);
             * FacesUtils.addErrorMessage("Can not set " + propUIName + ".",
             * e.getMessage());
             */
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
            /*
             * String propUIName = getPropUIName(propName);
             * log.error("Failed to set " + propUIName + "!", e);
             * FacesUtils.addErrorMessage("Can not set " + propUIName + ".",
             * e.getMessage());
             */
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

        for (TablePropertyDefinition prop : getBussinedDimensionProps()) {
            boolean presents = false;

            for (TableProperty tp : propsStore) {
                if (tp.getName().equals(prop.getName())) {
                    presents = true;
                }
            }

            if (!presents) {
                props.add(new SelectItem(prop.getName(), prop.getDisplayName()));
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
        TableProperty selectProp = getEmptyPropByName(propertyToAdd);

        if (selectProp != null) {
            propsStore.add(selectProp);

            //setProperty(selectProp.getName(),null);
        }
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public List<TableProperty> getPropsStore() {
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

    private boolean isTheSameBean(RulesRepositoryArtefact obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof AProjectArtefact) {
            if (storeProjName != null && storeProjVersion != null) {
                if ( ((AProjectArtefact) obj).getName().equals(storeProjName) && ((AProjectArtefact) obj).getVersion().getVersionName().equals(storeProjVersion) ) {
                    return true;
                }
            }
            
            return false;
        }

        return false;
    }

    public void setPropsStore(List<TableProperty> propsStore) {
        this.propsStore = propsStore;
    }

    public List<TableProperty> initSettedProps() {
        Map<String, InheritedProperty> inheritedProp = getInheritedProps();
        Map<String, Object> settedPropsList = getProps();
        
        return makeTableProps(inheritedProp, settedPropsList);
    }

    private TableProperty getEmptyPropByName(String propertyToAdd) {
        for (TablePropertyDefinition tpd : bussinedDimensionProps) {
            if (tpd.getName().equals(propertyToAdd)) {
                return new TableProperty(tpd);
            }
        }

        return null;
    }

    public void remove(TableProperty selectTProp) {
        for (TableProperty tProp : propsStore) {
            if (tProp.getName().equals(selectTProp.getName())) {
                propsStore.remove(tProp);

                removeProperty(selectTProp.getName());
                return;
            }
        }
    }

    public void save(TableProperty selectTProp) {
        for (TableProperty tProp : propsStore) {
            if (tProp.getName().equals(selectTProp.getName())) {
                tProp.setValue(selectTProp.getDisplayValue());

                setProperty(selectTProp.getName(), selectTProp.getDisplayValue());
                return;
            }
        }
    }

    public static List<TableProperty> getProjectPropsToolTip(RulesRepositoryArtefact dataBean) {
        Map<String, InheritedProperty> inheritedProp = dataBean.getInheritedProps();
        Map<String, Object> settedPropsList = dataBean.getProps();
       
        return makeTableProps(inheritedProp, settedPropsList);
    }

    public static List<TableProperty> getVersionPropToolTip(java.util.Map objList) {
        Map<String, Object> props = (Map<String, Object>) objList;
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

    private static List<TableProperty> makeTableProps(Map<String, InheritedProperty> inheritedProp, Map<String, Object> settedPropsList) {
        List<TableProperty> propsStore = new ArrayList<TableProperty>();
        List<TablePropertyDefinition> bussinedDimensionProps = TablePropertyDefinitionUtils
                .getDimensionalTableProperties();

        /*Add inherited Props*/
        if (settedPropsList != null) {
            for (TablePropertyDefinition propDefinition : bussinedDimensionProps) {
                if (inheritedProp.containsKey(propDefinition.getName())) {
                    if (!settedPropsList.containsKey(propDefinition.getName())) {
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
    
                        propsStore.add(prop);
                    }
                }
            }
        }

        if (bussinedDimensionProps != null && settedPropsList != null) {
            for (TablePropertyDefinition propDefinition : bussinedDimensionProps) {
                if (settedPropsList.containsKey(propDefinition.getName())) {
                    TableProperty prop = new TableProperty(propDefinition);
                    try {
                        prop.setValue(settedPropsList.get(propDefinition.getName()));
                    } catch (Exception e) {
                        
                    }
                    propsStore.add(prop);
                }
            }
        }

        return propsStore;
    }

}
