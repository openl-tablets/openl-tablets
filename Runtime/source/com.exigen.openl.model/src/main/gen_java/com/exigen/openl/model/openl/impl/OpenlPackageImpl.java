/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.exigen.openl.model.openl.impl;

import com.exigen.common.model.components.ComponentsPackage;

import com.exigen.common.model.components.java.JavaComponentsPackage;

import com.exigen.common.model.library.LibraryPackage;

import com.exigen.common.model.mapping.MappingPackage;

import com.exigen.common.model.primitives.PrimitivesPackage;

import com.exigen.common.model.registry.RegistryPackage;

import com.exigen.common.model.security.SecurityPackage;

import com.exigen.common.model.spring.SpringPackage;

import com.exigen.openl.model.openl.OpenlFactory;
import com.exigen.openl.model.openl.OpenlPackage;
import com.exigen.openl.model.openl.RuleSet;
import com.exigen.openl.model.openl.RuleSetFile;

import com.exigen.openl.model.openl.RuleSetParameter;
import com.exigen.openl.model.openl.RuleSetReturn;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class OpenlPackageImpl extends EPackageImpl implements OpenlPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ruleSetFileEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ruleSetEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ruleSetParameterEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ruleSetReturnEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType excelResourceReferenceEDataType = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see com.exigen.openl.model.openl.OpenlPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private OpenlPackageImpl() {
		super(eNS_URI, OpenlFactory.eINSTANCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this
	 * model, and for any others upon which it depends.  Simple
	 * dependencies are satisfied by calling this method on all
	 * dependent packages before doing anything else.  This method drives
	 * initialization for interdependent packages directly, in parallel
	 * with this package, itself.
	 * <p>Of this package and its interdependencies, all packages which
	 * have not yet been registered by their URI values are first created
	 * and registered.  The packages are then initialized in two steps:
	 * meta-model objects for all of the packages are created before any
	 * are initialized, since one package's meta-model objects may refer to
	 * those of another.
	 * <p>Invocation of this method will not affect any packages that have
	 * already been initialized.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static OpenlPackage init() {
		if (isInited) return (OpenlPackage)EPackage.Registry.INSTANCE.getEPackage(OpenlPackage.eNS_URI);

		// Obtain or create and register package
		OpenlPackageImpl theOpenlPackage = (OpenlPackageImpl)(EPackage.Registry.INSTANCE.getEPackage(eNS_URI) instanceof OpenlPackageImpl ? EPackage.Registry.INSTANCE.getEPackage(eNS_URI) : new OpenlPackageImpl());

		isInited = true;

		// Initialize simple dependencies
		PrimitivesPackage.eINSTANCE.eClass();
		LibraryPackage.eINSTANCE.eClass();
		SecurityPackage.eINSTANCE.eClass();
		ComponentsPackage.eINSTANCE.eClass();
		MappingPackage.eINSTANCE.eClass();
		RegistryPackage.eINSTANCE.eClass();
		SpringPackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theOpenlPackage.createPackageContents();

		// Initialize created meta-data
		theOpenlPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theOpenlPackage.freeze();

		return theOpenlPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRuleSetFile() {
		return ruleSetFileEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRuleSetFile_ExcelResourceReference() {
		return (EAttribute)ruleSetFileEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRuleSetFile_RuleSets() {
		return (EReference)ruleSetFileEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRuleSetFile_InitParameters() {
		return (EReference)ruleSetFileEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRuleSet() {
		return ruleSetEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRuleSetParameter() {
		return ruleSetParameterEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRuleSetReturn() {
		return ruleSetReturnEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getExcelResourceReference() {
		return excelResourceReferenceEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OpenlFactory getOpenlFactory() {
		return (OpenlFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		ruleSetFileEClass = createEClass(RULE_SET_FILE);
		createEAttribute(ruleSetFileEClass, RULE_SET_FILE__EXCEL_RESOURCE_REFERENCE);
		createEReference(ruleSetFileEClass, RULE_SET_FILE__INIT_PARAMETERS);
		createEReference(ruleSetFileEClass, RULE_SET_FILE__RULE_SETS);

		ruleSetEClass = createEClass(RULE_SET);

		ruleSetParameterEClass = createEClass(RULE_SET_PARAMETER);

		ruleSetReturnEClass = createEClass(RULE_SET_RETURN);

		// Create data types
		excelResourceReferenceEDataType = createEDataType(EXCEL_RESOURCE_REFERENCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Obtain other dependent packages
		PrimitivesPackage thePrimitivesPackage = (PrimitivesPackage)EPackage.Registry.INSTANCE.getEPackage(PrimitivesPackage.eNS_URI);
		ComponentsPackage theComponentsPackage = (ComponentsPackage)EPackage.Registry.INSTANCE.getEPackage(ComponentsPackage.eNS_URI);
		JavaComponentsPackage theJavaComponentsPackage = (JavaComponentsPackage)EPackage.Registry.INSTANCE.getEPackage(JavaComponentsPackage.eNS_URI);

		// Add supertypes to classes
		ruleSetFileEClass.getESuperTypes().add(thePrimitivesPackage.getBaseRootModelElement());
		ruleSetFileEClass.getESuperTypes().add(theComponentsPackage.getComponentDefinition());
		ruleSetEClass.getESuperTypes().add(theJavaComponentsPackage.getJavaMethodOperationDefinition());
		ruleSetParameterEClass.getESuperTypes().add(theJavaComponentsPackage.getJavaMethodParameter());
		ruleSetReturnEClass.getESuperTypes().add(theJavaComponentsPackage.getJavaMethodReturn());

		// Initialize classes and features; add operations and parameters
		initEClass(ruleSetFileEClass, RuleSetFile.class, "RuleSetFile", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRuleSetFile_ExcelResourceReference(), this.getExcelResourceReference(), "excelResourceReference", null, 0, 1, RuleSetFile.class, IS_TRANSIENT, !IS_VOLATILE, !IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRuleSetFile_InitParameters(), theComponentsPackage.getInitParameter(), null, "initParameters", null, 0, -1, RuleSetFile.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRuleSetFile_RuleSets(), this.getRuleSet(), null, "ruleSets", null, 0, -1, RuleSetFile.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(ruleSetEClass, RuleSet.class, "RuleSet", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(ruleSetParameterEClass, RuleSetParameter.class, "RuleSetParameter", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(ruleSetReturnEClass, RuleSetReturn.class, "RuleSetReturn", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		// Initialize data types
		initEDataType(excelResourceReferenceEDataType, String.class, "ExcelResourceReference", IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);

		// Create resource
		createResource(eNS_URI);

		// Create annotations
		// http://www.exigengroup.com/common/Component
		createComponentAnnotations();
	}

	/**
	 * Initializes the annotations for <b>http://www.exigengroup.com/common/Component</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void createComponentAnnotations() {
		String source = "http://www.exigengroup.com/common/Component";		
		addAnnotation
		  (ruleSetFileEClass, 
		   source, 
		   new String[] {
			 "builder", "com.exigen.openl.component.RuleComponentBuilder"
		   });		
		addAnnotation
		  (ruleSetEClass, 
		   source, 
		   new String[] {
			 "executor", "com.exigen.openl.component.RuleSetExecutor"
		   });
	}

} //OpenlPackageImpl
