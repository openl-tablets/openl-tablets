import type { CSSProperties } from 'react'
import { useMemo, useState } from 'react'
import { Select } from 'antd'
import { BranchesOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { BranchListLoading } from './BranchListLoading'
import { BranchMarks } from './BranchMarks'

const useStyles = createStyles(({ css }) => ({
    /** The branch reads in the select's own text; only the marks are borrowed. */
    option: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
    `,
}))

/** The marks a branch carries, told from its name. */
export interface BranchMarksInfo {
    isDefault?: boolean | undefined
    isProtected?: boolean | undefined
}

interface BranchSelectProps {
    value?: string | undefined
    onChange?: ((value: string) => void) | undefined
    /** The branches to offer, in the order they should appear. */
    branchNames: string[]
    /** The marks each branch carries; branches read without marks when omitted. */
    marksOf?: ((name: string) => BranchMarksInfo) | undefined
    /** Let a branch name that is not offered be entered and kept — the create and copy forms. */
    allowNew?: boolean | undefined
    /** The branches are still being read, so the field says so instead of reading as a repository without any. */
    loading?: boolean | undefined
    placeholder?: string | undefined
    disabled?: boolean | undefined
    style?: CSSProperties | undefined
    'data-testid'?: string | undefined
    'aria-label'?: string | undefined
}

/**
 * Picks a branch, the same way everywhere it is picked — the Sync updates dialog, the create and the copy
 * forms. The chosen branch reads with its Default badge and protected shield and a branch icon, the list
 * offers every branch with the same marks, and the list narrows as the user types.
 *
 * With {@link BranchSelectProps.allowNew} the forms may also name a branch that does not exist yet: the
 * typed name is offered as a choice and kept when the field loses focus, so a new branch can be created.
 *
 * A heavy repository takes seconds to list its branches. While that read runs the field spins in place of
 * the branch icon, and the open list spins in the branches' own place as long as it has none to show —
 * rather than the settled empty list that would claim there is none to pick. The forms preselect the
 * configured branch, so the list often offers that one alone while the rest are still on their way; the
 * spinning field is what says so there.
 */
export const BranchSelect = ({
    value,
    onChange,
    branchNames,
    marksOf,
    allowNew,
    loading,
    style,
    placeholder,
    disabled,
    ...rest
}: BranchSelectProps) => {
    const { styles } = useStyles()
    const [search, setSearch] = useState('')

    // The offered branches, plus — while a new name may be entered — the typed name and the current one, so
    // both stay selectable and the chosen branch always has a label to show.
    const optionNames = useMemo(() => {
        const names = new Set(branchNames)
        if (allowNew) {
            const typed = search.trim()
            if (typed) {
                names.add(typed)
            }
            if (value) {
                names.add(value)
            }
        }
        return [...names]
    }, [branchNames, allowNew, search, value])

    const options = optionNames.map(name => ({
        value: name,
        label: (
            <span className={styles.option}>
                {name}
                <BranchMarks {...marksOf?.(name)} testId={`branch-option-${name}`} />
            </span>
        ),
    }))

    // While a new branch may be named, the typed text is the value as it is typed — the way a plain text
    // field behaves — including an empty value when the field is cleared. AntD fires this only for user
    // typing, never for a blur or an option-select (both reset the search box on their own), so committing
    // here can never wipe a branch that was picked from the list.
    const onSearch = (typed: string) => {
        setSearch(typed)
        onChange?.(typed)
    }

    return (
        <Select
            {...rest}
            loading={!!loading}
            notFoundContent={loading ? <BranchListLoading /> : undefined}
            options={options}
            style={{ width: '100%', ...style }}
            suffixIcon={loading ? undefined : <BranchesOutlined />}
            value={value || undefined}
            onChange={next => {
                setSearch('')
                onChange?.(next ?? '')
            }}
            showSearch={{
                filterOption: (input, option) =>
                    String(option?.value ?? '').toLowerCase().includes(input.toLowerCase()),
                ...(allowNew ? { onSearch } : {}),
            }}
            {...(placeholder !== undefined && { placeholder })}
            {...(disabled !== undefined && { disabled })}
        />
    )
}
