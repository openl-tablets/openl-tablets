import { ProjectStatus } from '../constants/project'
import { Role } from '../constants'

export interface Project {
    branch: string
    comment: string
    id: string
    lockInfo?: {
        lockedAt: string
        lockedBy: string
    }
    modifiedAt: string
    modifiedBy: string
    name: string
    path?: string
    repository: string
    revision: string
    status: ProjectStatus
    tags?: Record<string, string>
    selectedBranches?: string[]
}

export interface ProjectRole {
    id: string
    name: string
    role: Role
}
