import type { ProjectProperty } from 'types/tables'

/**
 * A property as the project metadata endpoint describes it, with only what a test cares about spelled out.
 *
 * <p>Every field the endpoint answers with has a default here, so a test names the one or two that matter to it and
 * a new field on {@link ProjectProperty} is added in one place rather than in every test that builds one.
 */
export const property = (definition: Partial<ProjectProperty> & { name: string }): ProjectProperty => ({
    displayName: definition.name,
    group: 'Dev',
    type: 'text',
    multiple: false,
    dimensional: false,
    defaultValue: null,
    pattern: null,
    values: [],
    ...definition,
})
