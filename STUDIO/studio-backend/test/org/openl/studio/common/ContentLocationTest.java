package org.openl.studio.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;

class ContentLocationTest {

    @AfterEach
    void tearDown() {
        CurrentRequest.reset();
    }

    @Test
    void theOldIdIsReplacedInThePathAndKeepsTheQuery() {
        var response = CurrentRequest.bind("/rest/projects/old/tables", "kind=Rules&old=1");

        ContentLocation.servedAs("old", "new");

        assertEquals("/rest/projects/new/tables?kind=Rules&old=1", response.getHeader(HttpHeaders.CONTENT_LOCATION));
    }

    @Test
    void theOldIdIsReplacedInTheNamedQueryParameters() {
        var response = CurrentRequest.bind("/rest/projects/p/run", "tableId=old&other=old");

        ContentLocation.servedAs("old", "new", "tableId");

        assertEquals("/rest/projects/p/run?tableId=new&other=old", response.getHeader(HttpHeaders.CONTENT_LOCATION));
    }

    @Test
    void eachOldIdOfARequestIsReplacedInTheAddressTheOnesBeforeGave() {
        var response = CurrentRequest.bind("/rest/projects/oldProject/tables/oldTable", null);

        ContentLocation.servedAs("oldProject", "newProject");
        ContentLocation.servedAs("oldTable", "newTable", "tableId");

        assertEquals("/rest/projects/newProject/tables/newTable", response.getHeader(HttpHeaders.CONTENT_LOCATION));
    }

    @Test
    void anAddressThatNamesTheOldIdNowhereGetsNoHeader() {
        var response = CurrentRequest.bind("/rest/repos/design/projects/Copy/from-project", null);

        ContentLocation.servedAs("old", "new");

        assertNull(response.getHeader(HttpHeaders.CONTENT_LOCATION));
    }

    @Test
    void outsideOfARequestNothingIsNamed() {
        ContentLocation.servedAs("old", "new");

        assertNull(RequestContextHolder.getRequestAttributes());
    }
}
