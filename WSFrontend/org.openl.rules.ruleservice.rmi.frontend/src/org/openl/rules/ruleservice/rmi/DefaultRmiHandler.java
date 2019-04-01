package org.openl.rules.ruleservice.rmi;

/*
 * #%L
 * OpenL - RuleService - RuleService - RMI
 * %%
 * Copyright (C) 2015 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This RMI handler is used for deployed services with generated interfaces.
 *
 * @author Marat Kamalov
 *
 */
public interface DefaultRmiHandler extends Remote {
    /**
     * Executes method with specified parameters.
     *
     * @param serviceNmae Name of deployed service
     * @param ruleName Technical name of the rule to execute
     * @param inputParamsTypes Types of method input parameters to discover method
     * @param params Parameters for method execution
     * @return Result of execution
     */
    Object execute(String ruleName, Class<?>[] inputParamsTypes, Object[] params) throws RemoteException;

    /**
     * Executes method with specified parameters. Method discovery is done based on parameters types.
     *
     * @param serviceNmae Name of deployed service
     * @param ruleName Technical name of the rule to execute
     * @param params Parameters for method execution
     * @return Result of execution
     */
    Object execute(String ruleName, Object... params) throws RemoteException;
}
