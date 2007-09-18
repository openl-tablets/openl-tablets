/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.exigen.openl.model.openl;

import com.exigen.common.model.components.java.JavaComponentsPackage;

import com.exigen.common.model.primitives.PrimitivesPackage;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see com.exigen.openl.model.openl.OpenlFactory
 * @model kind="package"
 * @generated
 */
public interface OpenlPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "openl";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://exigengroup.com/openl";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "openl";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	OpenlPackage eINSTANCE = com.exigen.openl.model.openl.impl.OpenlPackageImpl.init();

	/**
	 * The meta object id for the '{@link com.exigen.openl.model.openl.impl.RuleSetFileImpl <em>Rule Set File</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.exigen.openl.model.openl.impl.RuleSetFileImpl
	 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSetFile()
	 * @generated
	 */
	int RULE_SET_FILE = 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_FILE__DESCRIPTION = PrimitivesPackage.BASE_ROOT_MODEL_ELEMENT__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_FILE__NAME = PrimitivesPackage.BASE_ROOT_MODEL_ELEMENT__NAME;

	/**
	 * The feature id for the '<em><b>Excel Resource Reference</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_FILE__EXCEL_RESOURCE_REFERENCE = PrimitivesPackage.BASE_ROOT_MODEL_ELEMENT_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Init Parameters</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_FILE__INIT_PARAMETERS = PrimitivesPackage.BASE_ROOT_MODEL_ELEMENT_FEATURE_COUNT + 1;

	/**
	 * The feature id for the '<em><b>Rule Sets</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_FILE__RULE_SETS = PrimitivesPackage.BASE_ROOT_MODEL_ELEMENT_FEATURE_COUNT + 2;

	/**
	 * The number of structural features of the '<em>Rule Set File</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_FILE_FEATURE_COUNT = PrimitivesPackage.BASE_ROOT_MODEL_ELEMENT_FEATURE_COUNT + 3;

	/**
	 * The meta object id for the '{@link com.exigen.openl.model.openl.impl.RuleSetImpl <em>Rule Set</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.exigen.openl.model.openl.impl.RuleSetImpl
	 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSet()
	 * @generated
	 */
	int RULE_SET = 1;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET__DESCRIPTION = JavaComponentsPackage.JAVA_METHOD_OPERATION_DEFINITION__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET__NAME = JavaComponentsPackage.JAVA_METHOD_OPERATION_DEFINITION__NAME;

	/**
	 * The feature id for the '<em><b>Method Parameters</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET__METHOD_PARAMETERS = JavaComponentsPackage.JAVA_METHOD_OPERATION_DEFINITION__METHOD_PARAMETERS;

	/**
	 * The feature id for the '<em><b>Return</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET__RETURN = JavaComponentsPackage.JAVA_METHOD_OPERATION_DEFINITION__RETURN;

	/**
	 * The number of structural features of the '<em>Rule Set</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_FEATURE_COUNT = JavaComponentsPackage.JAVA_METHOD_OPERATION_DEFINITION_FEATURE_COUNT + 0;


	/**
	 * The meta object id for the '{@link com.exigen.openl.model.openl.impl.RuleSetParameterImpl <em>Rule Set Parameter</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.exigen.openl.model.openl.impl.RuleSetParameterImpl
	 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSetParameter()
	 * @generated
	 */
	int RULE_SET_PARAMETER = 2;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_PARAMETER__DESCRIPTION = JavaComponentsPackage.JAVA_METHOD_PARAMETER__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_PARAMETER__NAME = JavaComponentsPackage.JAVA_METHOD_PARAMETER__NAME;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_PARAMETER__TYPE = JavaComponentsPackage.JAVA_METHOD_PARAMETER__TYPE;

	/**
	 * The feature id for the '<em><b>Required</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_PARAMETER__REQUIRED = JavaComponentsPackage.JAVA_METHOD_PARAMETER__REQUIRED;

	/**
	 * The feature id for the '<em><b>Many</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_PARAMETER__MANY = JavaComponentsPackage.JAVA_METHOD_PARAMETER__MANY;

	/**
	 * The number of structural features of the '<em>Rule Set Parameter</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_PARAMETER_FEATURE_COUNT = JavaComponentsPackage.JAVA_METHOD_PARAMETER_FEATURE_COUNT + 0;

	/**
	 * The meta object id for the '{@link com.exigen.openl.model.openl.impl.RuleSetReturnImpl <em>Rule Set Return</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see com.exigen.openl.model.openl.impl.RuleSetReturnImpl
	 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSetReturn()
	 * @generated
	 */
	int RULE_SET_RETURN = 3;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_RETURN__DESCRIPTION = JavaComponentsPackage.JAVA_METHOD_RETURN__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_RETURN__TYPE = JavaComponentsPackage.JAVA_METHOD_RETURN__TYPE;

	/**
	 * The feature id for the '<em><b>Required</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_RETURN__REQUIRED = JavaComponentsPackage.JAVA_METHOD_RETURN__REQUIRED;

	/**
	 * The feature id for the '<em><b>Many</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_RETURN__MANY = JavaComponentsPackage.JAVA_METHOD_RETURN__MANY;

	/**
	 * The number of structural features of the '<em>Rule Set Return</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_SET_RETURN_FEATURE_COUNT = JavaComponentsPackage.JAVA_METHOD_RETURN_FEATURE_COUNT + 0;

	/**
	 * The meta object id for the '<em>Excel Resource Reference</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see java.lang.String
	 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getExcelResourceReference()
	 * @generated
	 */
	int EXCEL_RESOURCE_REFERENCE = 4;


	/**
	 * Returns the meta object for class '{@link com.exigen.openl.model.openl.RuleSetFile <em>Rule Set File</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Rule Set File</em>'.
	 * @see com.exigen.openl.model.openl.RuleSetFile
	 * @generated
	 */
	EClass getRuleSetFile();

	/**
	 * Returns the meta object for the attribute '{@link com.exigen.openl.model.openl.RuleSetFile#getExcelResourceReference <em>Excel Resource Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Excel Resource Reference</em>'.
	 * @see com.exigen.openl.model.openl.RuleSetFile#getExcelResourceReference()
	 * @see #getRuleSetFile()
	 * @generated
	 */
	EAttribute getRuleSetFile_ExcelResourceReference();

	/**
	 * Returns the meta object for the containment reference list '{@link com.exigen.openl.model.openl.RuleSetFile#getRuleSets <em>Rule Sets</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Rule Sets</em>'.
	 * @see com.exigen.openl.model.openl.RuleSetFile#getRuleSets()
	 * @see #getRuleSetFile()
	 * @generated
	 */
	EReference getRuleSetFile_RuleSets();

	/**
	 * Returns the meta object for the containment reference list '{@link com.exigen.openl.model.openl.RuleSetFile#getInitParameters <em>Init Parameters</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Init Parameters</em>'.
	 * @see com.exigen.openl.model.openl.RuleSetFile#getInitParameters()
	 * @see #getRuleSetFile()
	 * @generated
	 */
	EReference getRuleSetFile_InitParameters();

	/**
	 * Returns the meta object for class '{@link com.exigen.openl.model.openl.RuleSet <em>Rule Set</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Rule Set</em>'.
	 * @see com.exigen.openl.model.openl.RuleSet
	 * @generated
	 */
	EClass getRuleSet();

	/**
	 * Returns the meta object for class '{@link com.exigen.openl.model.openl.RuleSetParameter <em>Rule Set Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Rule Set Parameter</em>'.
	 * @see com.exigen.openl.model.openl.RuleSetParameter
	 * @generated
	 */
	EClass getRuleSetParameter();

	/**
	 * Returns the meta object for class '{@link com.exigen.openl.model.openl.RuleSetReturn <em>Rule Set Return</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Rule Set Return</em>'.
	 * @see com.exigen.openl.model.openl.RuleSetReturn
	 * @generated
	 */
	EClass getRuleSetReturn();

	/**
	 * Returns the meta object for data type '{@link java.lang.String <em>Excel Resource Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Excel Resource Reference</em>'.
	 * @see java.lang.String
	 * @model instanceClass="java.lang.String"
	 * @generated
	 */
	EDataType getExcelResourceReference();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	OpenlFactory getOpenlFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals  {
		/**
		 * The meta object literal for the '{@link com.exigen.openl.model.openl.impl.RuleSetFileImpl <em>Rule Set File</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.exigen.openl.model.openl.impl.RuleSetFileImpl
		 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSetFile()
		 * @generated
		 */
		EClass RULE_SET_FILE = eINSTANCE.getRuleSetFile();

		/**
		 * The meta object literal for the '<em><b>Excel Resource Reference</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RULE_SET_FILE__EXCEL_RESOURCE_REFERENCE = eINSTANCE.getRuleSetFile_ExcelResourceReference();

		/**
		 * The meta object literal for the '<em><b>Rule Sets</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RULE_SET_FILE__RULE_SETS = eINSTANCE.getRuleSetFile_RuleSets();

		/**
		 * The meta object literal for the '<em><b>Init Parameters</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference RULE_SET_FILE__INIT_PARAMETERS = eINSTANCE.getRuleSetFile_InitParameters();

		/**
		 * The meta object literal for the '{@link com.exigen.openl.model.openl.impl.RuleSetImpl <em>Rule Set</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.exigen.openl.model.openl.impl.RuleSetImpl
		 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSet()
		 * @generated
		 */
		EClass RULE_SET = eINSTANCE.getRuleSet();

			/**
		 * The meta object literal for the '{@link com.exigen.openl.model.openl.impl.RuleSetParameterImpl <em>Rule Set Parameter</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.exigen.openl.model.openl.impl.RuleSetParameterImpl
		 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSetParameter()
		 * @generated
		 */
		EClass RULE_SET_PARAMETER = eINSTANCE.getRuleSetParameter();

		/**
		 * The meta object literal for the '{@link com.exigen.openl.model.openl.impl.RuleSetReturnImpl <em>Rule Set Return</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see com.exigen.openl.model.openl.impl.RuleSetReturnImpl
		 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getRuleSetReturn()
		 * @generated
		 */
		EClass RULE_SET_RETURN = eINSTANCE.getRuleSetReturn();

		/**
		 * The meta object literal for the '<em>Excel Resource Reference</em>' data type.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see java.lang.String
		 * @see com.exigen.openl.model.openl.impl.OpenlPackageImpl#getExcelResourceReference()
		 * @generated
		 */
		EDataType EXCEL_RESOURCE_REFERENCE = eINSTANCE.getExcelResourceReference();

	}

} //OpenlPackage
