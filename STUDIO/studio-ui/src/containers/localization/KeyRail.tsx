import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Checkbox, Input, Modal, Select, Skeleton, Tooltip, Tree } from 'antd'
import type { TreeDataNode } from 'antd'
import { SlidersOutlined, TranslationOutlined } from '@ant-design/icons'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { useSharedStyles } from '../projects/sharedStyles'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import type { MessageFilter } from './messageFilter'
import type { KeyProblems, ProblemIndex } from './messageProblems'
import { useElementSize } from './useElementSize'

/** The height of one row of the tree, which the virtual list counts in. */
const ROW_HEIGHT = 24

/** The width the rail was last dragged to, kept apart from the rail of the modules. */
const WIDTH_STORAGE_KEY = 'openl.localization.rail.width'
const WIDTH = { min: 180, max: 640, fallback: 280 }

/** The key of the root of the tree, which no message key can be: it holds a control character. */
const ROOT = '\u0000all'

interface KeyRailProps {
    /** The keys of the messages, or null while they are read. */
    keys: string[] | null
    /** The key on screen, or null for all of them. */
    selectedKey: string | null
    onSelectKey: (key: string | null) => void
    /** The problems of the keys, marked on each key. */
    problems: ProblemIndex
    /** What the keys are narrowed down to; the keys handed in are the ones it leaves. */
    filter: MessageFilter
    onFilter: (filter: MessageFilter) => void
    /** The languages a filter can ask for the keys not translated into, the default one left out. */
    languages: Array<{ value: string, label: string }>
    styles: LocalizationStyles
}

/** The classes the counts are drawn in, read once rather than for every row. */
interface Marks {
    error: string
    warning: string
}

const NONE: KeyProblems = { count: 0, error: false }

/** A row of the list: the key, and how many problems it has in the colour of the worst of them. */
const titleOf = (key: string, problems: KeyProblems, marks: Marks) => (
    <>
        {key}
        {problems.count > 0 && (
            <span className={problems.error ? marks.error : marks.warning} data-testid="localization-key-problems">
                {problems.count}
            </span>
        )}
    </>
)

/**
 * The left rail of the messages: every key in the order it is written, under the root that stands for all of them.
 *
 * <p>The root opens the table of every key in every language, a key opens that key alone. The list draws the rows
 * in sight and no more, so a bundle of thousands of keys scrolls as fast as one of ten; the key on screen is brought
 * into sight.
 *
 * <p>The search finds a key by a piece of its name or of any of its texts. The filter behind the sliders leaves
 * only the keys with problems, or only those a language has no translation for; the filter dialog works on a copy
 * of the choice, applied or thrown away when it closes.
 */
export const KeyRail = ({
    keys,
    selectedKey,
    onSelectKey,
    problems,
    filter,
    onFilter,
    languages,
    styles,
}: KeyRailProps) => {
    const { t } = useTranslation('localization')
    const { styles: shared } = useSharedStyles()
    const { size: width, startResize } = useDragSize(WIDTH_STORAGE_KEY, 'right', WIDTH)
    const { ref: bodyRef, size: body } = useElementSize<HTMLDivElement>()
    const treeRef = useRef<React.ComponentRef<typeof Tree>>(null)
    const [expanded, setExpanded] = useState<React.Key[]>([ROOT])
    const [filterOpen, setFilterOpen] = useState(false)
    const [draft, setDraft] = useState(filter)

    // Built once per list: the classes are named by their strings, which stay while the theme does.
    const marks = useMemo(
        () => ({ error: styles.errorCount, warning: styles.warningCount }),
        [styles.errorCount, styles.warningCount]
    )
    const treeData = useMemo<TreeDataNode[]>(() => [{
        key: ROOT,
        title: t('all_messages'),
        icon: <TranslationOutlined />,
        children: (keys ?? []).map(key => ({
            key,
            title: titleOf(key, problems.byKey.get(key) ?? NONE, marks),
            isLeaf: true,
        })),
    }], [keys, problems, marks, t])

    useEffect(() => {
        if (selectedKey !== null && keys !== null) {
            setExpanded(current => (current.includes(ROOT) ? current : [...current, ROOT]))
            treeRef.current?.scrollTo({ key: selectedKey, align: 'auto' })
        }
    }, [keys, selectedKey])

    return (
        <aside className={`${shared.rail} ${styles.rail}`} data-testid="localization-rail" style={{ width }}>
            <ResizeHandle edge="right" onPointerDown={startResize} testId="localization-rail-resizer" />
            <div className={styles.railTop}>
                <Input
                    allowClear
                    data-testid="localization-search"
                    onChange={event => onFilter({ ...filter, search: event.target.value })}
                    placeholder={t('search_placeholder')}
                    size="small"
                    value={filter.search}
                    suffix={(
                        <Tooltip title={t('filter')}>
                            <Badge dot={filter.onlyProblems || filter.notTranslatedInto !== null}>
                                <Button
                                    aria-label={t('filter')}
                                    data-testid="localization-filter"
                                    icon={<SlidersOutlined />}
                                    size="small"
                                    type="text"
                                    onClick={() => {
                                        setDraft(filter)
                                        setFilterOpen(true)
                                    }}
                                />
                            </Badge>
                        </Tooltip>
                    )}
                />
            </div>
            <div ref={bodyRef} className={styles.railBody}>
                {keys === null
                    ? <Skeleton active paragraph={{ rows: 8 }} title={false} />
                    : (
                        <Tree
                            ref={treeRef}
                            blockNode
                            showIcon
                            className={shared.railTree}
                            data-testid="localization-keys"
                            expandedKeys={expanded}
                            height={body.height}
                            itemHeight={ROW_HEIGHT}
                            motion={false}
                            onExpand={setExpanded}
                            onSelect={(_, info) => onSelectKey(info.node.key === ROOT ? null : String(info.node.key))}
                            selectedKeys={[selectedKey ?? ROOT]}
                            treeData={treeData}
                        />
                    )}
            </div>
            <Modal
                cancelText={t('common:btn.cancel')}
                okButtonProps={{ 'data-testid': 'localization-filter-apply' }}
                okText={t('common:btn.apply')}
                onCancel={() => setFilterOpen(false)}
                open={filterOpen}
                title={t('filter')}
                width={420}
                onOk={() => {
                    setFilterOpen(false)
                    onFilter({ ...draft, search: filter.search })
                }}
            >
                <Checkbox
                    checked={draft.onlyProblems}
                    className={styles.filterRow}
                    data-testid="localization-only-problems"
                    onChange={event => setDraft(current => ({ ...current, onlyProblems: event.target.checked }))}
                >
                    {t('only_problems')}
                </Checkbox>
                <Select
                    allowClear
                    className={styles.filterRow}
                    data-testid="localization-not-translated"
                    onChange={(tag?: string) => setDraft(current => ({ ...current, notTranslatedInto: tag ?? null }))}
                    options={languages}
                    placeholder={t('not_translated_into')}
                    value={draft.notTranslatedInto ?? undefined}
                />
            </Modal>
        </aside>
    )
}
