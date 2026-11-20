import { encodeProjectPath, mavenArtifactOf, mavenIdOf, mavenProjectId, moduleRoute, projectCoordinatesOf, projectFileRoute, projectRoute, projectTopic } from './projectId'

describe('moduleRoute', () => {
    it('puts the project id in the address as the server issued it', () => {
        expect(moduleRoute('2QK5V769WQEQ8', 'Main Module', 'tbl-1', 'D101'))
            .toBe('/projects/2QK5V769WQEQ8/modules/Main%20Module?table=tbl-1&errorCell=D101')
    })
})

describe('a project downloaded from the Maven repository', () => {
    const domain = mavenProjectId('com.example:domain:1.0.0')
    const coordinates = { groupId: 'com.example', artifactId: 'domain', version: '1.0.0' }

    it('is named by the coordinates of a project archive, and of nothing else', () => {
        expect(projectCoordinatesOf('com.example:domain:1.0.0')).toEqual(coordinates)
        expect(projectCoordinatesOf(' com.example:domain:zip:1.0.0 ')).toEqual(coordinates)
        expect(projectCoordinatesOf('com.example:lib:jar:1.0.0')).toBeUndefined()
        expect(projectCoordinatesOf('com.example:domain:zip:tests:1.0.0')).toBeUndefined()
        expect(projectCoordinatesOf('com.example::1.0.0')).toBeUndefined()
        expect(projectCoordinatesOf(undefined)).toBeUndefined()
        expect(mavenIdOf(coordinates)).toBe(domain)
    })

    it('is addressed by its coordinates', () => {
        expect(domain).toBe('maven:com.example:domain:1.0.0')
        expect(mavenArtifactOf(domain)).toBe('com.example:domain:1.0.0')
        expect(mavenArtifactOf('ABC123')).toBeUndefined()
        expect(projectRoute(domain)).toBe('/maven/com.example/domain/1.0.0')
        expect(projectRoute('ABC123')).toBe('/projects/ABC123')
        expect(projectRoute(mavenProjectId('com.example:domain:zip:1.0 b'))).toBe('/maven/com.example/domain/1.0%20b')
    })

    it('opens a file of it on its page', () => {
        expect(projectFileRoute(domain, 'rules.xml')).toBe('/maven/com.example/domain/1.0.0?file=rules.xml')
    })

    it('has topics that bear its id as it is', () => {
        expect(projectTopic(domain)).toBe('/user/topic/projects/maven:com.example:domain:1.0.0')
        expect(projectTopic('ABC123')).toBe('/user/topic/projects/ABC123')
    })

    it('opens its modules under its own address', () => {
        expect(moduleRoute(domain, 'Main Rules', 'T1')).toBe('/maven/com.example/domain/1.0.0/modules/Main%20Rules?table=T1')
    })
})

describe('projectFileRoute', () => {
    it('opens the project on the file', () => {
        expect(projectFileRoute('2QK5V769WQEQ8', 'rules/Main.xlsx'))
            .toBe('/projects/2QK5V769WQEQ8?file=rules%2FMain.xlsx')
    })

    it('opens the file on a line of it when one is given', () => {
        expect(projectFileRoute('FeZdnMnl3XRa', 'rules/A b.md', 3))
            .toBe('/projects/FeZdnMnl3XRa?file=rules%2FA%20b.md&line=3')
    })
})

describe('encodeProjectPath', () => {
    it('keeps the separators and encodes each segment', () => {
        expect(encodeProjectPath('rules/My Module#1.xlsx')).toBe('rules/My%20Module%231.xlsx')
    })
})
