import React from 'react'
import { useTranslation } from 'react-i18next'
import { Checkbox, Flex, Input, theme } from 'antd'
import type { FilterDropdownProps, FilterValue } from 'antd/es/table/interface'
import type { MessageFilter } from './messageFilter'

/** The value of the filter of the column of the keys that leaves only the keys with warnings: no search holds it. */
const ONLY_PROBLEMS = '\u0000problems'

/** The value the column of the keys is filtered by, as the table holds it: the search, and the keys with warnings. */
export const keyFilterOf = ({ search, onlyProblems }: MessageFilter): string[] | null => {
    const value = [...(search === '' ? [] : [search]), ...(onlyProblems ? [ONLY_PROBLEMS] : [])]
    return value.length === 0 ? null : value
}

/** The search and the keys with warnings, as the filter of the column of the keys holds them. */
export const fromKeyFilter = (value: FilterValue | null | undefined): Pick<MessageFilter, 'search' | 'onlyProblems'> => {
    const values = (value ?? []).map(String)
    return {
        search: values.find(item => item !== ONLY_PROBLEMS) ?? '',
        onlyProblems: values.includes(ONLY_PROBLEMS),
    }
}

/**
 * The filter of the column of the keys: a search of a piece of a key or of a text in any language, applied by Enter,
 * and the keys with warnings alone, applied at once.
 */
export const KeyColumnFilter = ({ selectedKeys, setSelectedKeys, confirm }: FilterDropdownProps) => {
    const { t } = useTranslation('localization')
    const { token } = theme.useToken()
    const { search, onlyProblems } = fromKeyFilter(selectedKeys)
    const select = (next: Pick<MessageFilter, 'search' | 'onlyProblems'>) =>
        setSelectedKeys(keyFilterOf({ ...next, notTranslatedInto: [], order: null }) ?? [])
    return (
        <Flex vertical gap="small" style={{ padding: token.paddingXS }}>
            <Input.Search
                allowClear
                autoFocus
                data-testid="localization-key-search"
                onChange={event => select({ search: event.target.value, onlyProblems })}
                placeholder={t('search_placeholder')}
                value={search}
                onSearch={text => {
                    select({ search: text.trim(), onlyProblems })
                    confirm()
                }}
            />
            <Checkbox
                checked={onlyProblems}
                data-testid="localization-only-problems"
                onChange={event => {
                    select({ search, onlyProblems: event.target.checked })
                    confirm({ closeDropdown: false })
                }}
            >
                {t('only_problems')}
            </Checkbox>
        </Flex>
    )
}
