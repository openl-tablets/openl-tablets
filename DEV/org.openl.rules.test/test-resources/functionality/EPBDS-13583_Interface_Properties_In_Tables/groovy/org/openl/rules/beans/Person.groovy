package org.openl.rules.beans

interface Person extends Named {

    Integer getAge()

    Address getAddress()

    Address[] getFormerAddresses()

}
