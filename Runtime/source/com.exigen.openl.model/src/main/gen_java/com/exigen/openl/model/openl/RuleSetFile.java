/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.exigen.openl.model.openl;

import com.exigen.common.model.components.ComponentDefinition;

import com.exigen.common.model.primitives.BaseRootModelElement;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Rule Set File</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.exigen.openl.model.openl.RuleSetFile#getExcelResourceReference <em>Excel Resource Reference</em>}</li>
 *   <li>{@link com.exigen.openl.model.openl.RuleSetFile#getInitParameters <em>Init Parameters</em>}</li>
 *   <li>{@link com.exigen.openl.model.openl.RuleSetFile#getRuleSets <em>Rule Sets</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.exigen.openl.model.openl.OpenlPackage#getRuleSetFile()
 * @model annotation="http://www.exigengroup.com/common/Component builder='com.exigen.openl.component.RuleComponentBuilder'"
 * @generated
 */
public interface RuleSetFile extends BaseRootModelElement, ComponentDefinition {
	/**
	 * Returns the value of the '<em><b>Excel Resource Reference</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Excel Resource Reference</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Excel Resource Reference</em>' attribute.
	 * @see com.exigen.openl.model.openl.OpenlPackage#getRuleSetFile_ExcelResourceReference()
	 * @model dataType="com.exigen.openl.model.openl.ExcelResourceReference" transient="true" changeable="false"
	 * @generated
	 */
	String getExcelResourceReference();

	/**
	 * Returns the value of the '<em><b>Rule Sets</b></em>' containment reference list.
	 * The list contents are of type {@link com.exigen.openl.model.openl.RuleSet}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Rule Sets</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rule Sets</em>' containment reference list.
	 * @see com.exigen.openl.model.openl.OpenlPackage#getRuleSetFile_RuleSets()
	 * @model type="com.exigen.openl.model.openl.RuleSet" containment="true"
	 * @generated
	 */
	EList getRuleSets();

	/**
	 * Returns the value of the '<em><b>Init Parameters</b></em>' containment reference list.
	 * The list contents are of type {@link com.exigen.common.model.components.InitParameter}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Init Parameters</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Init Parameters</em>' containment reference list.
	 * @see com.exigen.openl.model.openl.OpenlPackage#getRuleSetFile_InitParameters()
	 * @model type="com.exigen.common.model.components.InitParameter" containment="true"
	 * @generated
	 */
	EList getInitParameters();

} // RuleSetFile