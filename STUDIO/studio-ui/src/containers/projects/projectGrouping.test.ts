import { describe, expect, it } from 'vitest'
import {
    activeLevels,
    buildGroupTree,
    GROUP_BY_REPOSITORY,
    groupKeys,
    pathToProject,
    searchTree,
    type GroupingLevels,
} from './projectGrouping'
import type { Project } from '../../types/projects'

const project = (id: string, name: string, repository: string, tags: Record<string, string> = {}): Project =>
    ({ id, name, repository, tags } as unknown as Project)

const repositoryName = (id: string) => (id === 'design' ? 'Design' : 'Git Flat')

const projects = [
    project('p1', 'Alpha', 'design', { Domain: 'Policy', LOB: 'Auto' }),
    project('p2', 'Beta', 'design', { Domain: 'Policy' }),
    project('p3', 'Gamma', 'flat', { Domain: 'Claims' }),
    project('p4', 'Delta', 'flat'),
]

describe('activeLevels', () => {
    it('stops at the first level that groups by nothing', () => {
        expect(activeLevels([GROUP_BY_REPOSITORY, '', 'LOB'] as GroupingLevels)).toEqual([GROUP_BY_REPOSITORY])
        expect(activeLevels(['Domain', 'LOB', ''] as GroupingLevels)).toEqual(['Domain', 'LOB'])
    })
})

describe('buildGroupTree', () => {
    it('lists the projects as they are when nothing groups them', () => {
        const tree = buildGroupTree(projects, [], repositoryName)

        expect(tree.map(node => node.title)).toEqual(['Alpha', 'Beta', 'Gamma', 'Delta'])
        expect(tree.every(node => node.project)).toBe(true)
    })

    it('groups by the repository, named the way the workspace names it', () => {
        const tree = buildGroupTree(projects, [GROUP_BY_REPOSITORY], repositoryName)

        expect(tree.map(node => node.title)).toEqual(['Design', 'Git Flat'])
        expect(tree[0]?.children.map(node => node.title)).toEqual(['Alpha', 'Beta'])
    })

    it('groups by a tag and keeps a project that does not carry it beside the groups', () => {
        const tree = buildGroupTree(projects, ['Domain'], repositoryName)

        // The groups come first, then the project with no Domain — never hidden by a tag it lacks.
        expect(tree.map(node => node.title)).toEqual(['Claims', 'Policy', 'Delta'])
        expect(tree[2]?.project?.id).toBe('p4')
    })

    it('nests the levels in the order they are configured', () => {
        const tree = buildGroupTree(projects, [GROUP_BY_REPOSITORY, 'Domain', 'LOB'], repositoryName)

        const design = tree.find(node => node.title === 'Design')
        const policy = design?.children.find(node => node.title === 'Policy')
        // Alpha carries LOB, Beta does not: one is grouped, the other stays at the level.
        expect(policy?.children.map(node => node.title)).toEqual(['Auto', 'Beta'])
        expect(policy?.children[0]?.children.map(node => node.title)).toEqual(['Alpha'])
    })

    it('groups only by the picked tag type, never by the other tags a project carries', () => {
        const tree = buildGroupTree(projects, ['LOB'], repositoryName)

        // Only Alpha carries LOB; the Domain values never become groups of this level.
        expect(tree.map(node => node.title)).toEqual(['Auto', 'Beta', 'Gamma', 'Delta'])
    })

    it('matches a tag type whatever case the project spelled it in', () => {
        const tree = buildGroupTree([project('p5', 'Eps', 'design', { domain: 'Policy' })], ['Domain'], repositoryName)

        expect(tree.map(node => node.title)).toEqual(['Policy'])
    })
})

describe('group filters', () => {
    it('carries the filters that show exactly the projects of the group', () => {
        const tree = buildGroupTree(projects, [GROUP_BY_REPOSITORY, 'Domain'], repositoryName)

        const design = tree.find(node => node.title === 'Design')
        expect(design?.filters).toEqual({ repositories: ['design'], tags: []})
        expect(design?.children[0]?.filters).toEqual({ repositories: ['design'], tags: ['Domain:Policy']})
    })
})

describe('pathToProject', () => {
    it('names the groups to open so a project shows', () => {
        const tree = buildGroupTree(projects, [GROUP_BY_REPOSITORY, 'Domain'], repositoryName)

        const path = pathToProject(tree, 'p1')

        expect(path).toHaveLength(2)
        expect(path?.[0]).toContain('design')
        expect(path?.[1]).toContain('Policy')
    })

    it('answers nothing for a project the tree does not hold', () => {
        expect(pathToProject(buildGroupTree(projects, [], repositoryName), 'nope')).toBeNull()
    })
})

describe('searchTree', () => {
    const tree = buildGroupTree(projects, [GROUP_BY_REPOSITORY, 'Domain'], repositoryName)

    it('keeps a group that matches by its own name whole, with everything under it', () => {
        const found = searchTree(tree, 'design')

        expect(found.map(node => node.title)).toEqual(['Design'])
        // The group answered the search, so its projects come along without matching themselves.
        expect(found[0]?.children.map(node => node.title)).toEqual(['Policy'])
        expect(found[0]?.children[0]?.children.map(node => node.title)).toEqual(['Alpha', 'Beta'])
    })

    it('keeps only the branch leading to a project that matches', () => {
        const found = searchTree(tree, 'gamma')

        expect(found.map(node => node.title)).toEqual(['Git Flat'])
        expect(found[0]?.children.map(node => node.title)).toEqual(['Claims'])
        expect(found[0]?.children[0]?.children.map(node => node.title)).toEqual(['Gamma'])
    })

    it('answers nothing when nothing matches, and everything without a search', () => {
        expect(searchTree(tree, 'nothing here')).toEqual([])
        expect(searchTree(tree, '   ')).toBe(tree)
    })
})

describe('groupKeys', () => {
    it('names every group so a search can show what it found', () => {
        const keys = groupKeys(buildGroupTree(projects, [GROUP_BY_REPOSITORY], repositoryName))

        expect(keys).toEqual(['grp/[Repository]=design', 'grp/[Repository]=flat'])
    })
})
