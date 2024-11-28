package org.openl.rules.ruleservice.publish;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StaticRmiHandler extends Remote {
    String baseHello(int hour) throws RemoteException;
}
