import { RepositoryType, Role } from '../constants'

export interface Repository {
    aclId: string
    id: string
    name: string
}

export interface RepositoryRole {
    id: string
    name: string
    role: Role
    type: RepositoryType
}

export interface RepositoryRootRole {
    id: string
    role: Role
    type: RepositoryType
}
