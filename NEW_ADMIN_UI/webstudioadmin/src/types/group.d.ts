import { Role } from '../constants'

export interface Group {
    oldName?: string;
    name: string;
    id: number;
    description?: string;
    designRole?: Role
    prodRole?: Role
    admin?: boolean;
}

export interface GroupList {
    [key: string]: {
        id: number;
        description: string;
        privileges: string[];
    }
}