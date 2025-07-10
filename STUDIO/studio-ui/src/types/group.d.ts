export interface GroupItem {
    admin?: boolean
    description?: string
    id: number
    name: string
    numberOfMembers: number
    oldName?: string
}

export interface Group {
    id: number
    description?: string
    privileges?: string[]
    numberOfMembers: {
        external: number
        internal: number
        total: number
    }
}

export interface GroupList {
    [key: string]: Group
}
