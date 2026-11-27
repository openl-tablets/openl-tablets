import { useMemo, useState, type ReactNode } from 'react'
import { Dropdown } from 'antd'
import { DownOutlined, LoadingOutlined } from '@ant-design/icons'
import { SearchInput } from './SearchInput'
import { ListLoading } from './ListLoading'
import { useStyles } from './CrumbSwitcher.styles'

/** One thing the reader can switch to. */
interface CrumbSwitcherItem {
    key: string
    label: ReactNode
    /** The text the search reads, which is not always what the item shows. */
    search: string
    /** Set on an item of another kind than the ones before it, which a line sets apart from them. */
    divided?: boolean | undefined
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
    /**
     * The keys of the items the reader chose most recently, the most recent first. Given them, the list leads with
     * those items in that order, a line under them, and goes on with the others by name; given none, the items keep
     * the order they come in.
     */
    recent?: readonly string[] | undefined
    testId: string
}

const byName = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' })

/** The items the recent ones first, in the order they were chosen, then the others by name, a line between. */
const recentFirst = (items: CrumbSwitcherItem[], recent: readonly string[]): CrumbSwitcherItem[] => {
    const known = new Map(items.map(item => [item.key, item]))
    const first = recent.flatMap(key => known.get(key) ?? [])
    const chosen = new Set(recent)
    const others = items
        .filter(item => !chosen.has(item.key))
        .sort((one, other) => byName.compare(one.search, other.search))
    return [...first, ...others.map((item, at) => (at === 0 && first.length > 0 ? { ...item, divided: true } : item))]
}

/**
 * What the breadcrumb offers instead of what it names: the branches of a project, the projects of the
 * workspace, the modules of a project.
 *
 * <p>It reads as plain text with a caret rather than as a form input, so it stays unobtrusive wherever it is
 * put. What there is to switch to is read when the list first opens — a workspace of any size takes a moment
 * to list it, and the list says it is being read rather than showing nothing to choose from — and the search
 * narrows what was read without going back to the server.
 *
 * <p>A switcher handed the recent choices leads with them, the most recent first, and lists the others by name
 * after a line: the projects and the branches are switched that way.
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
    recent,
    testId,
}: CrumbSwitcherProps) => {
    const { styles, cx } = useStyles()
    const [query, setQuery] = useState('')

    const needle = query.trim().toLowerCase()
    // Worked out again only for other items, another text or other recent choices: a switcher once opened keeps its
    // list while the screen around it is drawn again.
    const shown = useMemo(() => {
        const found = needle ? (items ?? []).filter(item => item.search.toLowerCase().includes(needle)) : items ?? []
        return recent === undefined ? found : recentFirst(found, recent)
    }, [items, needle, recent])

    const listContent = (menu: ReactNode) => {
        if (items === null && loading) {
            return <ListLoading testId={`${testId}-list-loading`} />
        }
        return shown.length === 0 ? <div className={styles.empty}>{emptyText}</div> : menu
    }

    const renderPopup = (menu: ReactNode) => (
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
    )

    const list = (
        <Dropdown
            popupRender={renderPopup}
            trigger={['click']}
            menu={{
                items: shown.flatMap(item => [
                    ...(item.divided ? [{ type: 'divider' as const, key: `${item.key}-divider` }] : []),
                    { key: item.key, label: item.label },
                ]),
                selectedKeys: [selectedKey],
                onClick: ({ key }) => onSelect(key),
            }}
            onOpenChange={open => {
                setQuery('')
                if (open) {
                    onOpen?.()
                }
            }}
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
