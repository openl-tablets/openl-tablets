import { getMavenProjects, mavenIdOf, mavenIdOfParams, mavenProjectOf, mavenProjectPage, projectCoordinatesOf } from './mavenDependencies'
import apiCall from './apiCall'

vi.mock('./apiCall', async importOriginal => ({
    ...await importOriginal<typeof import('./apiCall')>(),
    default: vi.fn(),
}))

const DOMAIN = { groupId: 'com.example', artifactId: 'domain', version: '1.0.0' }

describe('mavenDependencies', () => {
    it('reads the coordinates of a project archive, and of nothing else', () => {
        expect(projectCoordinatesOf('com.example:domain:1.0.0')).toEqual(DOMAIN)
        expect(projectCoordinatesOf(' com.example:domain:zip:1.0.0 ')).toEqual(DOMAIN)
        expect(projectCoordinatesOf('com.example:lib:jar:1.0.0')).toBeUndefined()
        expect(projectCoordinatesOf('com.example:domain:zip:tests:1.0.0')).toBeUndefined()
        expect(projectCoordinatesOf('com.example::1.0.0')).toBeUndefined()
        expect(projectCoordinatesOf(undefined)).toBeUndefined()
    })

    it('addresses the project by its coordinates', () => {
        expect(mavenProjectPage(DOMAIN)).toBe('/maven/com.example/domain/1.0.0')
        expect(mavenProjectPage({ ...DOMAIN, version: '1.0 beta' })).toBe('/maven/com.example/domain/1.0%20beta')
        expect(mavenIdOf(DOMAIN)).toBe('maven:com.example:domain:1.0.0')
    })

    it('lists a downloaded project as one that can only be read', () => {
        expect(mavenProjectOf({ ...DOMAIN, name: 'Domain' })).toMatchObject({
            id: 'maven:com.example:domain:1.0.0',
            readOnly: true,
        })
    })

    it('reads the id of the project the parameters of an address name', () => {
        expect(mavenIdOfParams(DOMAIN)).toBe('maven:com.example:domain:1.0.0')
        expect(mavenIdOfParams({ groupId: 'com.example' })).toBe('maven:com.example::')
        // An address of a project of the workspace names no coordinates.
        expect(mavenIdOfParams({})).toBeUndefined()
        expect(mavenIdOfParams(undefined)).toBeUndefined()
    })

    it('lists the downloaded projects the workspace depends on, none while no Maven repository is set', async () => {
        vi.mocked(apiCall).mockResolvedValueOnce([{ ...DOMAIN, name: 'Domain' }])
        expect(await getMavenProjects()).toEqual([{ ...DOMAIN, name: 'Domain' }])
        expect(apiCall).toHaveBeenLastCalledWith('/maven', undefined, expect.anything())

        vi.mocked(apiCall).mockResolvedValueOnce(null)
        expect(await getMavenProjects()).toEqual([])
    })
})
