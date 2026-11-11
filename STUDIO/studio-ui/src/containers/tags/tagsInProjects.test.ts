import { describe, expect, it } from 'vitest'
import { projectsWithTag, tagsInProjects } from './tagsInProjects'
import type { TagType } from './TagTable'

const tagType = (id: number, name: string, values: string[]): TagType => ({
    id, name, extensible: false, nullable: true, tags: values.map((value, index) => ({ id: index, name: value, tagTypeId: id })),
})

const counts = (type: string, values: Record<string, number>) => ({
    type, values: Object.entries(values).map(([name, count]) => ({ id: name, name, count })),
})

describe('tagsInProjects', () => {
    it('reports the values a configured tag type does not have and the tag types that are not configured', () => {
        const found = tagsInProjects(
            [tagType(1, 'BLOB', ['Benefit']), tagType(2, 'Domain', ['Policy'])],
            [counts('BLOB', { Banking: 2, Benefit: 5 }), counts('Domain', { Policy: 1 }), counts('Region', { EU: 3 })]
        )

        expect(found).toEqual([
            { type: 'BLOB', defined: true, values: [{ value: 'Banking', projects: 2 }]},
            { type: 'Region', defined: false, values: [{ value: 'EU', projects: 3 }]},
        ])
    })

    it('matches tag types and values regardless of case', () => {
        expect(tagsInProjects([tagType(1, 'Region', ['EU'])], [counts('region', { eu: 4 })])).toEqual([])
    })

    it('names a configured tag type as it is configured, whatever case the projects spell it in', () => {
        const found = tagsInProjects([tagType(1, 'Region', ['EU'])], [counts('REGION', { Asia: 1 })])

        expect(found).toEqual([{ type: 'Region', defined: true, values: [{ value: 'Asia', projects: 1 }]}])
    })

    it('takes a tag type with no values', () => {
        const found = tagsInProjects([{ ...tagType(1, 'LOB', []), tags: undefined as never }], [counts('LOB', { Auto: 1 })])

        expect(found).toEqual([{ type: 'LOB', defined: true, values: [{ value: 'Auto', projects: 1 }]}])
    })
})

describe('projectsWithTag', () => {
    it('opens the projects list filtered by the tag', () => {
        expect(projectsWithTag('Insurance Type', 'P&C')).toBe('/projects?tags=Insurance+Type%3AP%26C')
    })
})
