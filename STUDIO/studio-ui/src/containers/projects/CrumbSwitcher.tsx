import { useState, type ReactNode } from 'react'
import { Dropdown } from 'antd'
import { DownOutlined, LoadingOutlined } from '@ant-design/icons'
import { SearchInput } from '../../components/SearchInput'
import { ListLoading } from './ListLoading'
import { useStyles } from './CrumbSwitcher.styles'

/** One thing the reader can switch to. */
export interface CrumbSwitcherItem {
    key: string
    label: ReactNode
    /** The text the search reads, which is not always what the item shows. */
    search: string
}

interface CrumbSwitcherProps {
    /** What the trigger shows: the thing that is open. */
    current: ReactNode
    /** What there is to switch to, or null while it is still being read. */
    items: CrumbSwitcherItem[] | null
    /** The item the trigger stands on, marked in the list. */
    selectedKey: string
    loading: boolean
    /** Asked for when the list first opens, so nothing is read until the reader looks. */
    onOpen?: (() => void) | undefined
    onSelect: (key: string) => void
    /**
     * Where the name itself leads, for a breadcrumb that names somewhere the reader can go. Given one, the
     * name is followed and the list is opened from the caret beside it; given none, the whole trigger opens
     * the list.
     */
    onFollow?: (() => void) | undefined
    /** Whether the switch this trigger started is still running. */
    busy?: boolean | undefined
    disabled?: boolean | undefined
    searchPlaceholder: string
    /** What stands in the list's place when the search matches nothing. */
    emptyText: string
    testId: string
}

/**
 * What the breadcrumb offers instead of what it names: the branches of a project, the projects of the
 * workspace, the modules of a project.
 *
 * <p>It reads as plain text with a caret rather than as a form input, so it stays unobtrusive wherever it is
 * put. What there is to switch to is read when the list first opens — a workspace of any size takes a moment
 * to list it, and the list says it is being read rather than showing nothing to choose from — and the search
 * narrows what was read without going back to the server.
 */
export const CrumbSwitcher = ({
    current,
    items,
    selectedKey,
    loading,
    onOpen,
    onSelect,
    onFollow,
    busy = false,
    disabled = false,
    searchPlaceholder,
    emptyText,
    testId,
}: CrumbSwitcherProps) => {
    const { styles, cx } = useStyles()
    const [query, setQuery] = useState('')

    const needle = query.trim().toLowerCase()
    const shown = needle
        ? (items ?? []).filter(item => item.search.toLowerCase().includes(needle))
        : items ?? []

    const listContent = (menu: ReactNode) => {
        if (items === null && loading) {
            return <ListLoading testId={`${testId}-list-loading`} />
        }
        return shown.length === 0 ? <div className={styles.empty}>{emptyText}</div> : menu
    }

    const list = (
        <Dropdown
            trigger={['click']}
            menu={{
                items: shown.map(item => ({ key: item.key, label: item.label })),
                selectedKeys: [selectedKey],
                onClick: ({ key }) => onSelect(key),
            }}
            onOpenChange={open => {
                setQuery('')
                if (open) {
                    onOpen?.()
                }
            }}
            popupRender={menu => (
                <div className={styles.popup}>
                    {/* The search sits at the top of the list: a workspace carries more names than a reader
                        wants to walk through, so the list narrows as they type. */}
                    <div className={styles.search}>
                        <SearchInput
                            autoFocus
                            data-testid={`${testId}-search`}
                            onChange={event => setQuery(event.target.value)}
                            placeholder={searchPlaceholder}
                            value={query}
                        />
                    </div>
                    {listContent(menu)}
                </div>
            )}
        >
            <button
                aria-busy={busy}
                className={styles.trigger}
                data-testid={`${testId}-trigger`}
                disabled={busy || disabled}
                type="button"
            >
                {onFollow === undefined && current}
                {busy
                    ? <LoadingOutlined spin className={cx(styles.caret, styles.busy)} data-testid={`${testId}-switching`} />
                    : <DownOutlined className={styles.caret} />}
            </button>
        </Dropdown>
    )

    if (onFollow === undefined) {
        return list
    }
    return (
        <span className={styles.split}>
            <button
                className={styles.trigger}
                data-testid={`${testId}-follow`}
                disabled={busy || disabled}
                onClick={onFollow}
                type="button"
            >
                {current}
            </button>
            {list}
        </span>
    )
}

export default CrumbSwitcher
