/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.exigen.openl.model.openl.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import com.exigen.common.model.components.InitParameter;
import com.exigen.common.model.primitives.impl.BaseRootModelElementImpl;
import com.exigen.openl.model.openl.OpenlPackage;
import com.exigen.openl.model.openl.RuleSet;
import com.exigen.openl.model.openl.RuleSetFile;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Rule Set File</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.exigen.openl.model.openl.impl.RuleSetFileImpl#getExcelResourceReference <em>Excel Resource Reference</em>}</li>
 *   <li>{@link com.exigen.openl.model.openl.impl.RuleSetFileImpl#getInitParameters <em>Init Parameters</em>}</li>
 *   <li>{@link com.exigen.openl.model.openl.impl.RuleSetFileImpl#getRuleSets <em>Rule Sets</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class RuleSetFileImpl extends BaseRootModelElementImpl implements RuleSetFile {
	/**
	 * The default value of the '{@link #getExcelResourceReference() <em>Excel Resource Reference</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExcelResourceReference()
	 * @generated
	 * @ordered
	 */
	protected static final String EXCEL_RESOURCE_REFERENCE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getExcelResourceReference() <em>Excel Resource Reference</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExcelResourceReference()
	 * @generated
	 * @ordered
	 */
	protected String excelResourceReference = EXCEL_RESOURCE_REFERENCE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getInitParameters() <em>Init Parameters</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInitParameters()
	 * @generated
	 * @ordered
	 */
	protected EList initParameters = null;

	/**
	 * The cached value of the '{@link #getRuleSets() <em>Rule Sets</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRuleSets()
	 * @generated
	 * @ordered
	 */
	protected EList ruleSets = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected RuleSetFileImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EClass eStaticClass() {
		return OpenlPackage.Literals.RULE_SET_FILE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 */
	public String getExcelResourceReference() {
		String modelPath = eResource().getURI().trimFileExtension().path();
		if (modelPath.startsWith("/"))
			modelPath = modelPath.substring(1);
		
		return modelPath+(".xls");
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getExcelResourceReferenceGen() {
		return excelResourceReference;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getRuleSets() {
		if (ruleSets == null) {
			ruleSets = new EObjectContainmentEList(RuleSet.class, this, OpenlPackage.RULE_SET_FILE__RULE_SETS);
		}
		return ruleSets;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated not
	 */
	public EList getOperations() {
		return getRuleSets();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList getInitParameters() {
		if (initParameters == null) {
			initParameters = new EObjectContainmentEList(InitParameter.class, this, OpenlPackage.RULE_SET_FILE__INIT_PARAMETERS);
		}
		return initParameters;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OpenlPackage.RULE_SET_FILE__INIT_PARAMETERS:
				return ((InternalEList)getInitParameters()).basicRemove(otherEnd, msgs);
			case OpenlPackage.RULE_SET_FILE__RULE_SETS:
				return ((InternalEList)getRuleSets()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OpenlPackage.RULE_SET_FILE__EXCEL_RESOURCE_REFERENCE:
				return getExcelResourceReference();
			case OpenlPackage.RULE_SET_FILE__INIT_PARAMETERS:
				return getInitParameters();
			case OpenlPackage.RULE_SET_FILE__RULE_SETS:
				return getRuleSets();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OpenlPackage.RULE_SET_FILE__INIT_PARAMETERS:
				getInitParameters().clear();
				getInitParameters().addAll((Collection)newValue);
				return;
			case OpenlPackage.RULE_SET_FILE__RULE_SETS:
				getRuleSets().clear();
				getRuleSets().addAll((Collection)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void eUnset(int featureID) {
		switch (featureID) {
			case OpenlPackage.RULE_SET_FILE__INIT_PARAMETERS:
				getInitParameters().clear();
				return;
			case OpenlPackage.RULE_SET_FILE__RULE_SETS:
				getRuleSets().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OpenlPackage.RULE_SET_FILE__EXCEL_RESOURCE_REFERENCE:
				return EXCEL_RESOURCE_REFERENCE_EDEFAULT == null ? excelResourceReference != null : !EXCEL_RESOURCE_REFERENCE_EDEFAULT.equals(excelResourceReference);
			case OpenlPackage.RULE_SET_FILE__INIT_PARAMETERS:
				return initParameters != null && !initParameters.isEmpty();
			case OpenlPackage.RULE_SET_FILE__RULE_SETS:
				return ruleSets != null && !ruleSets.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (excelResourceReference: ");
		result.append(excelResourceReference);
		result.append(')');
		return result.toString();
	}

} //RuleSetFileImpl