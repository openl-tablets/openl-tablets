export interface GroupItem {
    admin?: boolean | undefined
    description?: string | undefined
    id: number
    name: string
    numberOfMembers: number
    oldName?: string | undefined
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
