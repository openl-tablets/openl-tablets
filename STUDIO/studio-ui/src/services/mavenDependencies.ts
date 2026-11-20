import type { Project } from 'types/projects'
import { ProjectStatus } from '../constants/project'
import apiCall, { asArray, LOCAL_LOAD_API_OPTIONS } from './apiCall'
import { coordinatesText, type MavenCoordinates, mavenIdOf, projectRoute } from './projectId'

/** A project of the Maven repository the workspace depends on, as the list of them names it. */
export interface MavenProjectSummary extends MavenCoordinates {
    name: string
}

/** The repository the projects of the Maven repository are listed under, beside the design repositories. */
export const MAVEN_REPOSITORY = { id: 'maven', name: 'Maven', type: 'maven' } as const

/** The parameters of an address, of which those of a downloaded project name its coordinates. */
type AddressParams = Partial<Record<keyof MavenCoordinates | 'projectId', string | undefined>>

/**
 * The id of the downloaded project the parameters of an address name, `/maven/:groupId/:artifactId/:version`, or
 * undefined for an address that names none.
 */
export const mavenIdOfParams = (params: AddressParams | undefined): string | undefined => {
    const { groupId, artifactId = '', version = '' } = params ?? {}
    return groupId === undefined ? undefined : mavenIdOf({ groupId, artifactId, version })
}

/**
 * The id of the project the parameters of an address name: `/projects/:projectId`, or the coordinates of a downloaded
 * project, which opens read-only.
 */
export const projectIdOfParams = (params: AddressParams | undefined): string | undefined =>
    params?.projectId ?? mavenIdOfParams(params)

/** The screen of the project, which shows it read-only: `/maven/groupId/artifactId/version`. */
export const mavenProjectPage = (coordinates: MavenCoordinates): string => projectRoute(mavenIdOf(coordinates))

/**
 * A downloaded project as the screens of projects list one: under the Maven repository, by the id a screen addresses
 * it by. It is compiled as a dependency and read like a project opened, and nobody may write it.
 */
export const mavenProjectOf = (project: MavenProjectSummary): Project => ({
    id: mavenIdOf(project),
    name: project.name,
    branch: '',
    comment: '',
    modifiedAt: '',
    modifiedBy: '',
    repository: MAVEN_REPOSITORY.id,
    repositoryInfo: { ...MAVEN_REPOSITORY },
    revision: '',
    path: coordinatesText(project),
    status: ProjectStatus.Opened,
    readOnly: true,
})

/** Lists the projects of the Maven repository the workspace depends on; none while no Maven repository is set. */
export const getMavenProjects = async (): Promise<MavenProjectSummary[]> =>
    asArray(await apiCall('/maven', undefined, LOCAL_LOAD_API_OPTIONS) as MavenProjectSummary[] | null)
