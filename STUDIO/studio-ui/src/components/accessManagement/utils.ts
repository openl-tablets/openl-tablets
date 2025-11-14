import i18next from '../../i18n'
import { Role } from '../../constants'

export const roleOptions = Object.keys(Role).map(role => ({ label: i18next.t(`role.${role}`, { ns: 'common' }), value: role }))

export const NONE_ROLE_VALUE = '__NONE__'
