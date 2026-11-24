import { ProjectStatus } from '../../constants/project'
import { mavenProjectOf } from '../../services/mavenDependencies'
import type { Project } from '../../types/projects'
import { qualifiersOf } from './projectQualifiers'

const project = (id: string, name: string, repository: string, path = name): Project => ({
    id,
    name,
    repository,
    path,
    branch: '',
    comment: '',
    modifiedAt: '',
    modifiedBy: '',
    revision: '',
    status: ProjectStatus.Opened,
})

const maven = (coordinates: string, name: string) => {
    const [groupId = '', artifactId = '', version = ''] = coordinates.split(':')
    return mavenProjectOf({ groupId, artifactId, version, name })
}

const names: Record<string, string> = { design: 'Design', second: 'Second' }
const repositoryName = (id: string) => names[id] ?? id

describe('qualifiersOf', () => {
    it('tells the version of a downloaded project, and nothing of a project alone with its name', () => {
        const qualifiers = qualifiersOf([project('A', 'Rules', 'design'), maven('com.example:domain:1.0.0', 'Domain')],
            repositoryName)

        expect(qualifiers.get('maven:com.example:domain:1.0.0')).toBe('1.0.0')
        expect(qualifiers.has('A')).toBe(false)
    })

    it('tells apart the projects of a name: the repository, the version, the coordinates', () => {
        const qualifiers = qualifiersOf([
            project('A', 'Domain', 'design'),
            maven('com.example:domain:1.0.0', 'Domain'),
            maven('com.example:domain:2.0.0', 'Domain'),
            maven('org.other:domain:2.0.0', 'Domain'),
        ], repositoryName)

        expect(qualifiers.get('A')).toBe('Design')
        expect(qualifiers.get('maven:com.example:domain:1.0.0')).toBe('1.0.0')
        expect(qualifiers.get('maven:com.example:domain:2.0.0')).toBe('com.example:domain:2.0.0')
        expect(qualifiers.get('maven:org.other:domain:2.0.0')).toBe('org.other:domain:2.0.0')
    })

    it('tells the path of projects of a name in one repository', () => {
        const qualifiers = qualifiersOf([
            project('A', 'Domain', 'design', 'teams/a/Domain'),
            project('B', 'Domain', 'design', 'teams/b/Domain'),
            project('C', 'Domain', 'second'),
        ], repositoryName)

        expect(qualifiers.get('A')).toBe('teams/a/Domain')
        expect(qualifiers.get('B')).toBe('teams/b/Domain')
        expect(qualifiers.get('C')).toBe('Second')
    })
})
