# A module that declares no name in rules.xml

The project's `rules.xml` declares its module by path alone:

```xml
<module><rules-root path="rules/Main.xlsx"/></module>
```

Such a module is still known by a name — the base name of its path — and this suite pins the endpoints that have
to answer with it.

- `010-list-modules` — the module list names it `Main`
- `020-module-sheets` — the worksheets of the module are reachable under that same name
- `030-project-descriptor` — the project descriptor the details screen reads names it the same way

> [!Note]
> These endpoints already answered `Main` before [EPBDS-16430](https://jira.eisgroup.com/browse/EPBDS-16430), so
> this suite pins a contract rather than guarding that fix. The fix itself is in the editor, which reads the
> descriptor as rules.xml writes it; it is covered by `WebStudioGetModuleTest`, and the rename it belongs to is
> submitted by a JSF form that this framework cannot drive.
