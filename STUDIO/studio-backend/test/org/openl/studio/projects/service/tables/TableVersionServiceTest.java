package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IOpenMember;

class TableVersionServiceTest {

    private final TableVersionService service = new TableVersionService();

    @Test
    void offersTheNextVariantAfterTheCurrentVersion() {
        assertEquals("0.0.2", TableVersionService.next("0.0.1", Set.of()));
        assertEquals("1.4.8", TableVersionService.next("1.4.7", Set.of()));
        // The offered version steps over the ones already taken.
        assertEquals("0.0.4", TableVersionService.next("0.0.1", Set.of("0.0.1", "0.0.2", "0.0.3")));
        // A version the engine cannot read counts as none at all, so a readable one is offered.
        assertEquals("0.0.1", TableVersionService.next("1.2", Set.of()));
        // A number longer than the engine orders by is one of those, rather than an arithmetic failure.
        assertEquals("0.0.1", TableVersionService.next("2147483648.0.0", Set.of()));
        assertEquals("999999999.0.1", TableVersionService.next("999999999.0.0", Set.of()));
    }

    @Test
    void listsTheVersionsTheTablesGroupAlreadyCarries() {
        var nodes = new TableSyntaxNode[]{
                node("BankLimitIndex", Map.of(), "0.0.1"),
                node("BankLimitIndex", Map.of(), "0.0.2"),
                // Another name, and the same name answering other requests: neither is a version of this table.
                node("Other", Map.of(), "0.0.5"),
                node("BankLimitIndex", Map.of("state", new String[]{"AL"}), "0.0.9"),
        };

        assertEquals(List.of("0.0.1", "0.0.2"),
                List.copyOf(service.taken("BankLimitIndex", Map.of(), nodes)));
    }

    @Test
    void countsATableDeclaringNoVersionAsTheInitialOne() {
        var nodes = new TableSyntaxNode[]{node("BankLimitIndex", Map.of(), null)};

        assertEquals(List.of("0.0.1"), List.copyOf(service.taken("BankLimitIndex", Map.of(), nodes)));
    }

    @Test
    void describesNothingForATableThatCarriesNoVersions() {
        assertNull(service.describe(table(false, null), new TableSyntaxNode[0]));
    }

    @Test
    void describesTheCurrentVersionTheNextOneAndTheTakenOnes() {
        var table = table(true, "0.0.2");
        var nodes = new TableSyntaxNode[]{
                node("BankLimitIndex", Map.of(), "0.0.1"),
                node("BankLimitIndex", Map.of(), "0.0.2"),
        };

        var versions = service.describe(table, nodes);

        assertEquals("0.0.2", versions.current());
        assertEquals("0.0.3", versions.next());
        assertEquals(List.of("0.0.1", "0.0.2"), versions.taken());
    }

    @Test
    void picksTheDimensionValuesOutOfTheOnesARequestDeclares() {
        var states = new UsStatesEnum[]{UsStatesEnum.AL, UsStatesEnum.CA};
        var declared = service.declaredDimensions(Map.of("state", states, "version", "0.0.2"));

        // The version is not a dimension: the engine does not dispatch on it.
        assertFalse(declared.containsKey("version"));
        assertEquals(List.of(UsStatesEnum.AL, UsStatesEnum.CA), List.of((UsStatesEnum[]) declared.get("state")));

        // A table declaring those same states answers the same requests; one declaring none does not.
        assertTrue(service.sameGroup(properties(Map.of("state", states), null), declared));
        assertFalse(service.sameGroup(properties(Map.of(), null), declared));
    }

    @Test
    void keepsADeclaredValueTheWayTheEngineDoes() {
        // A table's own values are the ones the engine kept, so a declared one is kept the same way before the two
        // are compared: several values ordered, and a date closing a period moved to the end of its day.
        var declared = service.declaredDimensions(Map.of(
                "state", new UsStatesEnum[]{UsStatesEnum.CA, UsStatesEnum.AL},
                "expirationDate", day(2009, 12, 31)));

        assertEquals(List.of(UsStatesEnum.AL, UsStatesEnum.CA), List.of((UsStatesEnum[]) declared.get("state")));
        assertEquals(endOfDay(2009, 12, 31), declared.get("expirationDate"));
        // A table declaring the day the request names answers the same requests, whichever moment of it was sent.
        assertTrue(service.sameGroup(properties(Map.of("expirationDate", endOfDay(2009, 12, 31)), null),
                service.declaredDimensions(Map.of("expirationDate", day(2009, 12, 31)))));
    }

    private static Date day(int year, int month, int dayOfMonth) {
        return Date.from(LocalDate.of(year, month, dayOfMonth).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date endOfDay(int year, int month, int dayOfMonth) {
        return Date.from(LocalDate.of(year, month, dayOfMonth).atTime(23, 59, 59, 999_000_000)
                .atZone(ZoneId.systemDefault()).toInstant());
    }

    private static IOpenLTable table(boolean versionable, String version) {
        // Every mock is finished before it is handed to another when(), which Mockito reads as unfinished stubbing.
        var properties = properties(Map.of(), version);
        var table = mock(IOpenLTable.class);
        when(table.isVersionable()).thenReturn(versionable);
        when(table.getName()).thenReturn("BankLimitIndex");
        when(table.getProperties()).thenReturn(properties);
        return table;
    }

    private static TableSyntaxNode node(String name, Map<String, Object> own, String version) {
        var properties = properties(own, version);
        var member = mock(IOpenMember.class);
        when(member.getName()).thenReturn(name);
        var node = mock(TableSyntaxNode.class);
        when(node.getMember()).thenReturn(member);
        when(node.getTableProperties()).thenReturn(properties);
        return node;
    }

    private static ITableProperties properties(Map<String, Object> own, String version) {
        var properties = mock(ITableProperties.class);
        when(properties.getTableProperties()).thenReturn(own);
        when(properties.getVersion()).thenReturn(version);
        return properties;
    }
}
