package org.openl.rules.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

/**
 * Abstract Entity.
 * It defines common properties and methods for OpenL Rules Project/Folder/File.
 *
 * @author Aleh Bykhavets
 *
 */
public interface REntity {
    /**
     * Gets name of the entity.
     *
     * @return name
     */
    public String getName();

    /**
     * Gets active version of the entity.
     *
     * @return active version
     */
    public RVersion getActiveVersion();

    /**
     * Gets version history of the entity.
     *
     * @return list of versions
     */
    public List<RVersion> getVersionHistory() throws RRepositoryException;

    /**
     * Deletes entity.
     * Also can delete other entities.
     * For example, deleting a folder will lead to deleting all its sub entities.
     *
     * @throws RRepositoryException if failed
     */
    public void delete() throws RRepositoryException;

    /**
     * Returns path of entity.
     *
     * @return path of entity
     * @throws RRepositoryException if failed
     */
    public String getPath() throws RRepositoryException;
    
    /**
     * Gets effective date for rules entity.
     * If effective date isn't set method returns <code>null</code>
     * 
     * @return effective date or <code>null</code>
     */
    public Date getEffectiveDate();

    /**
     * Gets expiration date for rules entity.
     * If expiration date isn't set method returns <code>null</code>
     * 
     * @return expiration date or <code>null</code>
     */
    public Date getExpirationDate();

    /**
     * Gets line of business for rules entity.
     * If line of business isn't set method returns <code>null</code>
     * 
     * @return line of business or <code>null</code>
     */
    public String getLineOfBusiness();

    /**
     * Gets attribute1 for rules entity.
     * If attribute1 isn't set method returns <code>null</code>
     * 
     * @return attribute1 or <code>null</code>
     */
    public String getAttribute1();
    
    /**
     * Gets attribute2 for rules entity.
     * If attribute2 isn't set method returns <code>null</code>
     * 
     * @return attribute2 or <code>null</code>
     */
    public String getAttribute2();
    
    /**
     * Gets attribute3 for rules entity.
     * If attribute3 isn't set method returns <code>null</code>
     * 
     * @return attribute3 or <code>null</code>
     */
    public String getAttribute3();
    
    /**
     * Gets attribute4 for rules entity.
     * If attribute4 isn't set method returns <code>null</code>
     * 
     * @return attribute4 or <code>null</code>
     */
    public String getAttribute4();
    
    /**
     * Gets attribute5 for rules entity.
     * If attribute5 isn't set method returns <code>null</code>
     * 
     * @return attribute5 or <code>null</code>
     */
    public String getAttribute5();
    
    /**
     * Gets attribute6 for rules entity.
     * If attribute6 isn't set method returns <code>null</code>
     * 
     * @return attribute6 or <code>null</code>
     */
    public Date getAttribute6();
    
    /**
     * Gets attribute7 for rules entity.
     * If attribute7 isn't set method returns <code>null</code>
     * 
     * @return attribute7 or <code>null</code>
     */
    public Date getAttribute7();
    
    /**
     * Gets attribute8 for rules entity.
     * If attribute8 isn't set method returns <code>null</code>
     * 
     * @return attribute8 or <code>null</code>
     */
    public Date getAttribute8();
    
    /**
     * Gets attribute9 for rules entity.
     * If attribute9 isn't set method returns <code>null</code>
     * 
     * @return attribute9 or <code>null</code>
     */
    public Date getAttribute9();
    
    /**
     * Gets attribute10 for rules entity.
     * If attribute10 isn't set method returns <code>null</code>
     * 
     * @return attribute10 or <code>null</code>
     */
    public Date getAttribute10();
    
    /**
     * Gets attribute11 for rules entity.
     * If attribute11 isn't set method returns <code>null</code>
     * 
     * @return attribute11 or <code>null</code>
     */
    public Double getAttribute11();
    
    /**
     * Gets attribute12 for rules entity.
     * If attribute12 isn't set method returns <code>null</code>
     * 
     * @return attribute12 or <code>null</code>
     */
    public Double getAttribute12();
    
    /**
     * Gets attribute13 for rules entity.
     * If attribute13 isn't set method returns <code>null</code>
     * 
     * @return attribute13 or <code>null</code>
     */
    public Double getAttribute13();
    
    /**
     * Gets attribute14 for rules entity.
     * If attribute14 isn't set method returns <code>null</code>
     * 
     * @return attribute14 or <code>null</code>
     */
    public Double getAttribute14();
    
    /**
     * Gets attribute15 for rules entity.
     * If attribute15 isn't set method returns <code>null</code>
     * 
     * @return attribute15 or <code>null</code>
     */
    public Double getAttribute15();
    
    /**
     * Sets effective date for rules entity.
     * Effective date can be disabled if <code>null</code> is passed.
     * 
     * @param date new effective date or <code>null</code>
     * @throws RRepositoryException if failed
     */
    public void setEffectiveDate(Date date) throws RRepositoryException;

    /**
     * Sets expiration date for rules entity.
     * expiration date can be disabled if <code>null</code> is passed.
     * 
     * @param date new expiration date or <code>null</code>
     * @throws RRepositoryException if failed
     */
    public void setExpirationDate(Date date) throws RRepositoryException;

    /**
     * Sets line of business for rules entity.
     * Line of business can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setLineOfBusiness(String lineOfBusiness) throws RRepositoryException;
    
    /**
     * Sets attribute1 for rules entity.
     * attribute1 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute1(String attribute1) throws RRepositoryException;
    
    /**
     * Sets attribute2 for rules entity.
     * attribute2 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute2(String attribute2) throws RRepositoryException;
    
    /**
     * Sets attribute3 for rules entity.
     * attribute3 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute3(String attribute3) throws RRepositoryException;
    
    /**
     * Sets attribute4 for rules entity.
     * attribute4 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute4(String attribute4) throws RRepositoryException;
    
    /**
     * Sets attribute5 for rules entity.
     * attribute5 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute5(String attribute5) throws RRepositoryException;
    
    /**
     * Sets attribute6 for rules entity.
     * attribute6 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute6(Date attribute6) throws RRepositoryException;
    
    /**
     * Sets attribute7 for rules entity.
     * attribute7 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute7(Date attribute7) throws RRepositoryException;
    
    /**
     * Sets attribute8 for rules entity.
     * attribute8 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute8(Date attribute8) throws RRepositoryException;
    
    /**
     * Sets attribute9 for rules entity.
     * attribute9 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute9(Date attribute9) throws RRepositoryException;
    
    /**
     * Sets attribute10 for rules entity.
     * attribute10 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute10(Date attribute10) throws RRepositoryException;
    
    /**
     * Sets attribute11 for rules entity.
     * attribute11 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute11(Double attribute11) throws RRepositoryException;
    
    /**
     * Sets attribute12 for rules entity.
     * attribute12 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute12(Double attribute12) throws RRepositoryException;
    
    /**
     * Sets attribute13 for rules entity.
     * attribute13 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute13(Double attribute13) throws RRepositoryException;
    
    /**
     * Sets attribute14 for rules entity.
     * attribute14 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute14(Double attribute14) throws RRepositoryException;
    
    /**
     * Sets attribute15 for rules entity.
     * attribute15 can be disabled if <code>null</code> is passed.
     * 
     * @throws RRepositoryException if failed
     */
    public void setAttribute15(Double attribute15) throws RRepositoryException;
    
    public Collection<RProperty> getProperties();
    public void addProperty(String name, RPropertyType type, Object value) throws RRepositoryException;
    public void removeProperty(String name) throws RRepositoryException;
    public boolean hasProperty(String name);
    public RProperty getProperty(String name) throws RRepositoryException;
}
