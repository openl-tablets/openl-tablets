export enum Role {
    VIEWER = 'VIEWER',
    CONTRIBUTOR = 'CONTRIBUTOR',
    MANAGER = 'MANAGER'
}

export const RoleWeight = {
    [Role.VIEWER]: 0,
    [Role.CONTRIBUTOR]: 1,
    [Role.MANAGER]: 2
}
