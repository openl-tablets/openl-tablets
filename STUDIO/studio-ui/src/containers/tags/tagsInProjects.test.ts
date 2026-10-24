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

        expect(found.values).toEqual(new Map([[1, [{ value: 'Banking', projects: 2 }]]]))
        expect(found.types).toEqual([{ type: 'Region', values: [{ value: 'EU', projects: 3 }]}])
    })

    it('matches tag types and values regardless of case', () => {
        const found = tagsInProjects([tagType(1, 'Region', ['EU'])], [counts('region', { eu: 4 })])

        expect(found.values.size).toBe(0)
        expect(found.types).toEqual([])
    })

    it('takes a tag type with no values', () => {
        const found = tagsInProjects([{ ...tagType(1, 'LOB', []), tags: undefined as never }], [counts('LOB', { Auto: 1 })])

        expect(found.values.get(1)).toEqual([{ value: 'Auto', projects: 1 }])
    })
})

describe('projectsWithTag', () => {
    it('opens the projects list filtered by the tag', () => {
        expect(projectsWithTag('Insurance Type', 'P&C')).toBe('/projects?tags=Insurance+Type%3AP%26C')
    })
})
