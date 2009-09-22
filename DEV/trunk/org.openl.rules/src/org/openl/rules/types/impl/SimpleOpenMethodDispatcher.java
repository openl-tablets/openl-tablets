package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.context.properties.ConstraintDefinition;
import org.openl.rules.context.properties.DefaultConstraintDefinitions;
import org.openl.rules.context.properties.IMatcher;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.DefaultTableProperties;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.runtime.IContext;
import org.openl.types.IOpenMethod;

/**
 * 
 * Class that provides simple implementation of {@link OpenMethodDispather}.
 * 
 * @author Alexey Gamanovich
 * 
 */
public class SimpleOpenMethodDispatcher extends OpenMethodDispatcher {
	
	/**
	 * Map of table property name => table property definition pairs that
	 * contains dimensional table properties definitions. Used as internal
	 * dictionary.
	 */
	private Map<String, TablePropertyDefinition> dimensionalPropertiesDefinitions;
	
	/**
	 * Map of table property name => appropriate constraint definition pairs
	 * that contains dimensional constraints definitions (expressions what are
	 * used to resolve best matching method). Used as internal dictionary.
	 */
	private Map<String, ConstraintDefinition> dimensionalConstraints;
	
	/**
	 * Creates new instance of class.
	 * 
	 * @param delegate
	 *            delegate method
	 */
	public SimpleOpenMethodDispatcher(IOpenMethod delegate) {
		
		super();
		
		decorate(delegate);
		
		this.dimensionalPropertiesDefinitions = getDimensionalPropertiesMap(DefaultPropertyDefinitions
		        .getDefaultDefinitions());
		this.dimensionalConstraints = getDimensionalConstraintsMap(DefaultConstraintDefinitions
		        .getConstraintDefinitions());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IContext context) {
		
		IRulesContext rulesContext = (IRulesContext) context;
		
		List<IOpenMethod> filteredMethods = new ArrayList<IOpenMethod>();
		
		for (IOpenMethod candidate : candidates) {
			
			if (candidate.getInfo() == null || candidate.getInfo().getSyntaxNode() == null) {
				continue;
			}
			
			TableSyntaxNode syntaxNode = (TableSyntaxNode) candidate.getInfo().getSyntaxNode();
			TableProperties tableNodeProperties = syntaxNode.getTableProperties();
			
			DefaultTableProperties properties = new DefaultTableProperties();
			
			for (TableProperties.Property property : tableNodeProperties.getProperties()) {
				properties.put(property.getKey().getValue(), property.getValue().getValue());
			}
			
			List<String> tableDimensionProperties = getTableDimensionPropertiesNames(tableNodeProperties);
			
			boolean result = true;
			
			for (String tableDimensionProperty : tableDimensionProperties) {
				ConstraintDefinition constraint = dimensionalConstraints.get(tableDimensionProperty);
				
				if (constraint == null) {
					result = false;
				} else {
					
					IMatcher matcher = constraint.getMatcher();
					result = result && matcher.isMatch(rulesContext, properties);
				}
			}
			
			if (result) {
				filteredMethods.add(candidate);
			}
		}
		
		if (filteredMethods.size() == 1) {
			return filteredMethods.get(0);
		}
		
		if (filteredMethods.size() > 1) {
			throw new RuntimeException("Ambiguous method parameters");
		}
		
		return null;
	}
	
	/**
	 * Gets the dimensional properties map. Provides map of table properties
	 * what are used to resolve best matching method.
	 * 
	 * @param definitions
	 *            list of available definitions
	 * @return map of table property name => table property definition pairs
	 * 
	 */
	private Map<String, TablePropertyDefinition> getDimensionalPropertiesMap(TablePropertyDefinition[] definitions) {
		
		Map<String, TablePropertyDefinition> dimensionProperties = new HashMap<String, TablePropertyDefinition>();
		
		for (TablePropertyDefinition definition : definitions) {
			if (definition.isDimensional()) {
				dimensionProperties.put(definition.getName(), definition);
			}
		}
		
		return dimensionProperties;
	}
	
	/**
	 * Gets list of dimensional properties names what used in table properties
	 * of concrete method.
	 * 
	 * @param tableProperties
	 *            table properties of concrete method
	 * @return list of dimensional properties names
	 */
	private List<String> getTableDimensionPropertiesNames(TableProperties tableProperties) {
		
		List<String> tableDimensionProperties = new ArrayList<String>();
		
		TableProperties.Property[] properties = tableProperties.getProperties();
		
		for (TableProperties.Property property : properties) {
			if (isDimensionProperty(property)) {
				tableDimensionProperties.add(property.getKey().getValue());
			}
		}
		
		return tableDimensionProperties;
	}
	
	/**
	 * Checks table property that its a dimensional.
	 * 
	 * @param property
	 *            table property to check
	 * @return <code>true</code> if is a dimensional property;
	 *         <code>false</code> - otherwise
	 */
	private boolean isDimensionProperty(TableProperties.Property property) {
		
		return this.dimensionalPropertiesDefinitions.containsKey(property.getKey().getValue());
	}
	
	/**
	 * Gets the constraint definitions map. Provides map of constraint
	 * definitions what are used to resolve best matching method.
	 * 
	 * @param constraintDefinitions
	 * @return map of table property name => appropriate constraint definition
	 *         pairs
	 */
	private Map<String, ConstraintDefinition> getDimensionalConstraintsMap(ConstraintDefinition[] constraintDefinitions) {
		
		Map<String, ConstraintDefinition> dimensionConstraints = new HashMap<String, ConstraintDefinition>();
		
		for (ConstraintDefinition constraintDefinition : constraintDefinitions) {
			dimensionConstraints.put(constraintDefinition.getTablePropertyDefinition().getName(), constraintDefinition);
		}
		
		return dimensionConstraints;
	}
}
