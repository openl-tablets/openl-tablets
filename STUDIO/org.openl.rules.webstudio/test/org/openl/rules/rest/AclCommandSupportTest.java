package org.openl.rules.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class AclCommandSupportTest {
    @Test
    public void invalidFormats() {
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(AclCommandSupport.MSG1, e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add1:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(String.format(AclCommandSupport.MSG5, "add1"), e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:x:username:VIEW");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(String.format(AclCommandSupport.MSG6, "x"), e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN1/design1:/DESIGN/rules/Project1:user:username:VIEW");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(String.format(AclCommandSupport.MSG7, "DESIGN1"), e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(AclCommandSupport.MSG2, e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:ADD1");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(
                    String.format(AclCommandSupport.MSG3, "ADD1", AclCommandSupport.RepoType.DESIGN.getName()),
                    e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW,ADD1");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(
                    String.format(AclCommandSupport.MSG3, "ADD1", AclCommandSupport.RepoType.DESIGN.getName()),
                    e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN:/DESIGN/rules/Project1:user:username:VIEW,ADD1");
            fail();
        } catch (CommandFormatException e) {
            assertEquals(AclCommandSupport.MSG4, e.getMessage());
        }
    }

    @Test
    public void ok() throws CommandFormatException {
        AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW,CREATE");
        AclCommandSupport.toCommand("AdD:DeSiGn/design1:/DESIGN/rules/Project1:user:username:ViEw,CrEaTe");
        AclCommandSupport.toCommand("rEmOvE:PrOd/design1:/DESIGN/rules/Project1:user:username:EdIt");
        AclCommandSupport.toCommand("rEmOvE:DePlOyCoNfIg/deploy-config:/DESIGN/rules/Project1:user:username:ViEw,CrEaTe");
        AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username: VIEW, CREATE");
        AclCommandSupport.toCommand("add:DESIGN::user:username:VIEW,CREATE");

        AclCommandSupport.toCommand("set:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW,CREATE");
        AclCommandSupport.toCommand("list:DESIGN/design1:");
        AclCommandSupport.toCommand("list:DESIGN/design1:/DESIGN/rules/Project1");
        AclCommandSupport.toCommand("listAll:DESIGN/design1:");
        AclCommandSupport.toCommand("listAll:DESIGN/design1:/DESIGN/rules/Project1");

    }
}
