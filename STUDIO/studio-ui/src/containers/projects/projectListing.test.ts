import { describe, expect, it } from 'vitest'
import { countFacets, refineProjects, searchProjects, sortProjects } from './projectListing'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

const project = (over: Partial<Project>): Project => ({
    id: over.id ?? 'p',
    name: over.name ?? 'Project',
    repository: over.repository ?? 'design',
    status: over.status ?? ProjectStatus.Closed,
    ...over,
} as Project)

const alpha = project({ id: 'p1', name: 'Alpha', modifiedBy: 'jane', branch: 'main', modifiedAt: '2026-01-02T00:00:00Z', tags: { Domain: 'Policy', LOB: 'Auto' } })
const beta = project({ id: 'p2', name: 'Beta', repository: 'flat', modifiedBy: 'john', branch: 'feature/rates', modifiedAt: '2026-03-01T00:00:00Z', status: ProjectStatus.Opened, tags: { Domain: 'Claims' } })
const local = project({ id: 'p3', name: 'Gamma', repository: 'design', status: ProjectStatus.Local })
const removed = project({ id: 'p4', name: 'Delta', status: ProjectStatus.Deleted })
const all = [alpha, beta, local, removed]

const noFilters = { statuses: new Set<string>(), repositories: new Set<string>(), tags: new Set<string>() }

describe('searchProjects', () => {
    it('matches the name, the author, the branch and the tags at once, ignoring case', () => {
        expect(searchProjects(all, 'ph').map(p => p.id)).toEqual(['p1'])
        expect(searchProjects(all, 'JOHN').map(p => p.id)).toEqual(['p2'])
        expect(searchProjects(all, 'feature').map(p => p.id)).toEqual(['p2'])
        // A tag matches by its type as well as its value.
        expect(searchProjects(all, 'claims').map(p => p.id)).toEqual(['p2'])
        expect(searchProjects(all, 'domain').map(p => p.id)).toEqual(['p1', 'p2'])
        // One word found in different fields of different projects lists them all — the fields are OR'ed.
        expect(searchProjects(all, 'a').map(p => p.id)).toEqual(['p1', 'p2', 'p3', 'p4'])
        expect(searchProjects(all, '  ').map(p => p.id)).toEqual(['p1', 'p2', 'p3', 'p4'])
        expect(searchProjects(all, 'nowhere')).toEqual([])
    })
})

describe('refineProjects', () => {
    it('leaves a deleted project out until its status is asked for', () => {
        expect(refineProjects(all, noFilters).map(p => p.id)).toEqual(['p1', 'p2', 'p3'])
        expect(refineProjects(all, { ...noFilters, statuses: new Set([ProjectStatus.Deleted]) }).map(p => p.id))
            .toEqual(['p4'])
    })

    it('matches the local facet by status, not by repository', () => {
        expect(refineProjects(all, { ...noFilters, repositories: new Set(['__local__']) }).map(p => p.id))
            .toEqual(['p3'])
        expect(refineProjects(all, { ...noFilters, repositories: new Set(['flat']) }).map(p => p.id))
            .toEqual(['p2'])
        // A local project stays out of the repository it was checked out from, matching how it is counted.
        expect(refineProjects(all, { ...noFilters, repositories: new Set(['design']) }).map(p => p.id))
            .toEqual(['p1'])
    })

    it('reads several values of one tag type as either of them, and different types as all of them', () => {
        expect(refineProjects(all, { ...noFilters, tags: new Set(['Domain:Policy', 'Domain:Claims']) }).map(p => p.id))
            .toEqual(['p1', 'p2'])
        expect(refineProjects(all, { ...noFilters, tags: new Set(['Domain:Policy', 'LOB:Auto']) }).map(p => p.id))
            .toEqual(['p1'])
        expect(refineProjects(all, { ...noFilters, tags: new Set(['Domain:Policy', 'LOB:Home']) })).toEqual([])
    })
})

describe('sortProjects', () => {
    it('sorts by name, branch or date, in either direction', () => {
        expect(sortProjects([beta, alpha], 'name').map(p => p.id)).toEqual(['p1', 'p2'])
        expect(sortProjects([beta, alpha], 'name', 'desc').map(p => p.id)).toEqual(['p2', 'p1'])
        expect(sortProjects([beta, alpha], 'branch').map(p => p.id)).toEqual(['p2', 'p1'])
        expect(sortProjects([alpha, beta], 'updated').map(p => p.id)).toEqual(['p1', 'p2'])
        expect(sortProjects([alpha, beta], 'updated', 'desc').map(p => p.id)).toEqual(['p2', 'p1'])
    })

    it('keeps a project without a timestamp last in either direction of the date sort', () => {
        expect(sortProjects([local, alpha], 'updated').map(p => p.id)).toEqual(['p1', 'p3'])
        expect(sortProjects([local, alpha], 'updated', 'desc').map(p => p.id)).toEqual(['p1', 'p3'])
    })
})

describe('countFacets', () => {
    it('counts the statuses, the repositories and every tag value of the scope', () => {
        const counts = countFacets(all, id => (id === 'design' ? 'Design' : 'Git Flat'))

        expect(counts.statusCounts).toEqual({ local: 1, opened: 1, editing: 0, viewingVersion: 0, closed: 1, deleted: 1 })
        // A local project is counted under the local facet rather than its repository.
        expect(counts.repositoryCounts).toEqual([
            { id: 'design', name: 'Design', count: 2 },
            { id: 'flat', name: 'Git Flat', count: 1 },
            { id: '__local__', name: 'Local', count: 1 },
        ])
        expect(counts.tagCounts).toEqual([
            { type: 'Domain', values: [
                { id: 'Claims', name: 'Claims', count: 1 },
                { id: 'Policy', name: 'Policy', count: 1 },
            ]},
            { type: 'LOB', values: [{ id: 'Auto', name: 'Auto', count: 1 }]},
        ])
    })
})
