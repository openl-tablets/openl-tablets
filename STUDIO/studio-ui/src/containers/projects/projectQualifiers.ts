import type { Project } from '../../types/projects'
import { mavenArtifactOf } from '../../services/projectId'

/**
 * What tells each project apart from the others of its name, by the id of the project; a project alone with its name
 * needs nothing.
 *
 * A project downloaded from the Maven repository always tells its version, since several versions of one may be
 * required, or its coordinates when another one of its name has the same version. A project of a design repository
 * tells its repository when another project has its name, or its path when that one is in the same repository too.
 */
export const qualifiersOf = (projects: Project[], repositoryName: (id: string) => string): Map<string, string> => {
    const byName = Map.groupBy(projects, project => project.name)
    const qualifiers = new Map<string, string>()
    for (const project of projects) {
        const namesakes = (byName.get(project.name) ?? []).filter(other => other.id !== project.id)
        const qualifier = qualifierOf(project, namesakes, repositoryName)
        if (qualifier) {
            qualifiers.set(project.id, qualifier)
        }
    }
    return qualifiers
}

const qualifierOf = (
    project: Project,
    namesakes: Project[],
    repositoryName: (id: string) => string
): string | undefined => {
    const coordinates = mavenArtifactOf(project.id)
    if (coordinates !== undefined) {
        const version = coordinates.slice(coordinates.lastIndexOf(':') + 1)
        const sameVersion = namesakes.some(other => mavenArtifactOf(other.id)?.endsWith(`:${version}`))
        return sameVersion ? coordinates : version
    }
    if (namesakes.length === 0) {
        return undefined
    }
    const sameRepository = namesakes.some(other => other.repository === project.repository)
    return sameRepository && project.path ? project.path : repositoryName(project.repository)
}
