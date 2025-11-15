export interface Table {
    id: string
    name: string
    url: string
    dependencies: string[]
}

export interface DependencyGraphData {
    tables: Table[]
    dependentsMap: Map<string, Set<string>>
}

export type DependencyDirection = 'dependsOn' | 'dependedBy'
