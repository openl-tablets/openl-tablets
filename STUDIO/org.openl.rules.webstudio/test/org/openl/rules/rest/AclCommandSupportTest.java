package org.openl.rules.rest;

import org.junit.Assert;
import org.junit.Test;

public class AclCommandSupportTest {
    @Test
    public void invalidFormats() {
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(AclCommandSupport.MSG1, e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add1:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(String.format(AclCommandSupport.MSG5, "add1"), e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:x:username:VIEW", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(String.format(AclCommandSupport.MSG6, "x"), e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN1/design1:/DESIGN/rules/Project1:user:username:VIEW", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(String.format(AclCommandSupport.MSG7, "DESIGN1"), e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(AclCommandSupport.MSG2, e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:ADD1", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(
                String.format(AclCommandSupport.MSG3, "ADD1", AclCommandSupport.RepoType.DESIGN.getName()),
                e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW,ADD1", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(
                String.format(AclCommandSupport.MSG3, "ADD1", AclCommandSupport.RepoType.DESIGN.getName()),
                e.getMessage());
        }
        try {
            AclCommandSupport.toCommand("add:DESIGN:/DESIGN/rules/Project1:user:username:VIEW,ADD1", null);
            Assert.fail();
        } catch (CommandFormatException e) {
            Assert.assertEquals(AclCommandSupport.MSG4, e.getMessage());
        }
    }

    @Test
    public void ok() throws CommandFormatException {
        AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW,ADD", null);
        AclCommandSupport.toCommand("AdD:DeSiGn/design1:/DESIGN/rules/Project1:user:username:ViEw,AdD", null);
        AclCommandSupport.toCommand("rEmOvE:PrOd/design1:/DESIGN/rules/Project1:user:username:EdIt", null);
        AclCommandSupport.toCommand("rEmOvE:DePlOyCoNfIg/deploy-config:/DESIGN/rules/Project1:user:username:ViEw,AdD",
            null);
        AclCommandSupport.toCommand("add:DESIGN/design1:/DESIGN/rules/Project1:user:username: VIEW, ADD", null);
        AclCommandSupport.toCommand("add:DESIGN::user:username:VIEW,ADD", null);

        AclCommandSupport.toCommand("set:DESIGN/design1:/DESIGN/rules/Project1:user:username:VIEW,ADD", null);
        AclCommandSupport.toCommand("list:DESIGN/design1:", null);
        AclCommandSupport.toCommand("list:DESIGN/design1:/DESIGN/rules/Project1", null);
        AclCommandSupport.toCommand("listAll:DESIGN/design1:", null);
        AclCommandSupport.toCommand("listAll:DESIGN/design1:/DESIGN/rules/Project1", null);

    }
}
