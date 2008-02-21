package org.openl.rules.webstudio.services;

/**
 * Service interface.
 *
 * @author Andrey Naumenko
 */
public interface Service {
    /**
     * Execute service.
     *
     * @param serviceParams parameters for service
     *
     * @return result of service execution
     *
     * @throws ServiceException if error occurs during executing service
     */
    ServiceResult execute(ServiceParams serviceParams) throws ServiceException;
}
