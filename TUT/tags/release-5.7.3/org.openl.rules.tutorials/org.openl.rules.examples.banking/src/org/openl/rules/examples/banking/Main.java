/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules, Inc. 2003
 */

package org.openl.rules.examples.banking;

import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;



/**
 * @author snshor
 *
 */
public class Main
{
    public interface IExample {
        void resolveProblem(Response response);
        void upSell(Response response);
    }
    
    public static void main(String[] args)
    {
        System.out.println("*** Resolve Banking Problem ***");
        String fileName = "rules/Banking.xls";
        
        EngineFactory<IExample> engineFactory = new RuleEngineFactory<IExample>(fileName, IExample.class);
        IExample instance = engineFactory.makeInstance();

        System.out.println(
        "\n============================================\n" +
           fileName + "(resolveProblem)" + 
        "\n============================================\n");
        Response response1 = new Response();
        instance.resolveProblem(response1);
        System.out.println("Response:");
        System.out.println(response1);
        
        System.out.println("*** UpSell Banking Products ***");
        System.out.println(
        "\n============================================\n" +
           fileName + "(upSell)" + 
        "\n============================================\n");
        Response response2 = new Response();
        instance.upSell(response2);
        System.out.println("Response:");
        System.out.println(response2);      

    }
}
