import type { DefaultOptionType, SearchConfig } from 'antd/es/select'
import i18next from '../../i18n'
import { Role } from '../../constants'

export const roleOptions = Object.keys(Role).map(role => ({ label: i18next.t(`role.${role}`, { ns: 'common' }), value: role }))

export const NONE_ROLE_VALUE = '__NONE__'

/** Finds an option whose label holds the typed text, whatever its case. */
const labelIncludes = (input: string, option?: DefaultOptionType) => {
    if (!option || !option.label || !(typeof option.label === 'string')) {
        return false
    }
    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

/** Searches a project by its name. */
export const PROJECT_SEARCH: SearchConfig<DefaultOptionType> = { filterOption: labelIncludes }

/** Searches a repository by its name, listing the ones still free to pick ahead of those already picked. */
export const REPOSITORY_SEARCH: SearchConfig<DefaultOptionType> = {
    filterOption: labelIncludes,
    filterSort: (optionA?: DefaultOptionType, optionB?: DefaultOptionType) => {
        if (!optionA || !optionB || optionA.disabled === optionB.disabled) return 0
        return optionA.disabled ? 1 : -1
    },
}
