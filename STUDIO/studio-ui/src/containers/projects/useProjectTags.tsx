import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AutoComplete, Button, Tooltip } from 'antd'
import { CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { errorMessage } from '../../utils/errorMessage'
import { getTagTypes, type TagType } from '../../services/repositories'
import { deleteFile, rootFileExists, writeRootFile } from '../../services/files'
import { serializeTagsProperties, TAGS_FILE_NAME, type TagEntry } from '../../services/tagsProperties'
import { EditableList } from './EditableList'

const useStyles = createStyles(({ css, token }) => ({
    /** The tags read as a table: the type on the left, what the project is tagged with on the right. */
    table: css`
        display: grid;
        grid-template-columns: minmax(0, auto) minmax(0, 1fr);
        align-items: center;
        column-gap: 12px;
        row-gap: 6px;
    `,
    type: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    value: css`
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    `,
    unset: css`
        color: ${token.colorTextQuaternary};
    `,
    actions: css`
        display: inline-flex;
        align-items: center;
        gap: 2px;
    `,
    /** One editable tag: its key and its value side by side, sharing the row. */
    entry: css`
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
    `,
    entryKey: css`
        flex: 1;
        min-width: 0;
    `,
    entryValue: css`
        flex: 1;
        min-width: 0;
    `,
    error: css`
        margin-top: 8px;
    `,
}))

/** Case-insensitive substring match, the way a value is looked for in a long list. */
const matches = (value: string, input: string): boolean => value.toLowerCase().includes(input.toLowerCase())

/**
 * The keys named by more than one entry. A properties file cannot repeat a key, and tag types match
 * regardless of case, so "Domain" and "domain" collide too.
 */
const duplicateKeys = (entries: TagEntry[]): Set<string> => {
    const counts = new Map<string, number>()
    for (const entry of entries) {
        const key = entry.key.trim().toLowerCase()
        if (key) {
            counts.set(key, (counts.get(key) ?? 0) + 1)
        }
    }
    return new Set([...counts.entries()].filter(([, count]) => count > 1).map(([key]) => key))
}

interface ProjectTagsOptions {
    projectId: string
    tags: Record<string, string>
    /** Whether the user may retag the project — the same right that lets them edit any project file. */
    canEdit: boolean
    /** Called after the tags were saved, so the screen reads the project again. */
    onSaved: () => void
}

/**
 * The tags of a project, and the way to change them: what to put beside the heading of the tags row, and
 * what to put in the row itself.
 *
 * The tags.properties file of the project is the source of truth: every tag it names is shown, and
 * editing writes the file back through the files API, the way rules.xml is edited. Any key and any value
 * can be entered; the tag types an administrator configured are only offered as suggestions.
 */
export const useProjectTags = ({ projectId, tags, canEdit, onSaved }: ProjectTagsOptions) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [editing, setEditing] = useState(false)
    const [entries, setEntries] = useState<TagEntry[]>([])
    const [suggestions, setSuggestions] = useState<TagType[]>([])
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const assigned = Object.entries(tags)
    // A repeated key cannot be written to the properties file, so saving waits until it is resolved.
    const duplicates = editing ? duplicateKeys(entries) : new Set<string>()

    // The configured tag types only feed the suggestions, so they are read when the editing starts and
    // a failed read simply suggests nothing.
    useEffect(() => {
        if (!editing) {
            return
        }
        let cancelled = false
        getTagTypes()
            .then(types => {
                if (!cancelled) {
                    setSuggestions(types)
                }
            })
            .catch(() => setSuggestions([]))
        return () => { cancelled = true }
    }, [editing])

    const startEditing = () => {
        setError(null)
        setEntries(Object.entries(tags).map(([key, value]) => ({ key, value })))
        setEditing(true)
    }

    const save = async () => {
        setSaving(true)
        setError(null)
        try {
            const content = serializeTagsProperties(entries)
            const exists = await rootFileExists(projectId, TAGS_FILE_NAME)
            if (content) {
                await writeRootFile(projectId, TAGS_FILE_NAME, content, exists ? 'overwrite' : 'create')
            } else if (exists) {
                // Every tag was removed: the file says nothing any more, so it goes away entirely.
                await deleteFile(projectId, TAGS_FILE_NAME)
            }
            setEditing(false)
            onSaved()
        } catch (e) {
            setError(errorMessage(e))
        } finally {
            setSaving(false)
        }
    }

    const editButton = canEdit && !editing && (
        <Tooltip title={t('browser.tags.edit')}>
            <Button
                aria-label={t('browser.tags.edit')}
                data-testid="edit-tags"
                icon={<EditOutlined />}
                onClick={startEditing}
                size="small"
                type="text"
            />
        </Tooltip>
    )

    const editActions = editing && (
        <span className={styles.actions}>
            <Tooltip title={t('browser.tags.save')}>
                <Button
                    aria-label={t('browser.tags.save')}
                    data-testid="tags-save"
                    disabled={saving || duplicates.size > 0}
                    icon={<CheckOutlined />}
                    loading={saving}
                    onClick={() => void save()}
                    size="small"
                    type="text"
                />
            </Tooltip>
            <Tooltip title={t('browser.tags.cancel')}>
                <Button
                    aria-label={t('browser.tags.cancel')}
                    data-testid="tags-cancel"
                    disabled={saving}
                    icon={<CloseOutlined />}
                    onClick={() => setEditing(false)}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </span>
    )

    const view = (
        <div className={styles.table} data-testid="project-tags">
            {assigned.map(([type, value]) => (
                <TagRow key={type} type={type} value={value} />
            ))}
            {assigned.length === 0 && <span className={cx(styles.value, styles.unset)}>{t('browser.tags.none')}</span>}
        </div>
    )

    const editor = (
        <div data-testid="project-tags-editor">
            <EditableList
                items={entries}
                newItem={() => ({ key: '', value: '' })}
                onChange={setEntries}
                testId="edit-tag"
                renderItem={(entry, set, id) => (
                    <TagEntryFields
                        duplicate={duplicates.has(entry.key.trim().toLowerCase())}
                        entry={entry}
                        onChange={set}
                        suggestions={suggestions}
                        testId={id}
                    />
                )}
            />
            {duplicates.size > 0 && (
                <Alert
                    showIcon
                    className={styles.error}
                    data-testid="tags-duplicate-keys"
                    title={t('browser.tags.duplicate_keys')}
                    type="warning"
                />
            )}
        </div>
    )

    return {
        action: editActions || editButton,
        content: (
            <>
                {editing ? editor : view}
                {error && <Alert showIcon className={styles.error} data-testid="tags-error" title={error} type="error" />}
            </>
        ),
    }
}

/** One line of the table: the tag type, and what the project carries for it. */
const TagRow = ({ type, value }: { type: string, value: ReactNode }) => {
    const { styles } = useStyles()
    return (
        <>
            <span className={styles.type}>{type}</span>
            <span className={styles.value}>{value}</span>
        </>
    )
}

/**
 * The editable fields of one tag: its key and its value, both free text. The configured tag types are
 * offered for the key, and the values of the matching type for the value — suggestions only, since the
 * file takes whatever the user names.
 */
const TagEntryFields = ({ entry, suggestions, duplicate, onChange, testId }: {
    entry: TagEntry
    suggestions: TagType[]
    /** The key is named by another entry too — the field is marked until one of them changes. */
    duplicate: boolean
    onChange: (entry: TagEntry) => void
    testId: string
}) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const matchingType = suggestions.find(type => type.name.toLowerCase() === entry.key.trim().toLowerCase())
    return (
        <div className={styles.entry}>
            <AutoComplete
                className={styles.entryKey}
                data-testid={testId}
                {...(duplicate ? { status: 'error' as const } : {})}
                onChange={key => onChange({ ...entry, key: (key as string | undefined) ?? '' })}
                options={suggestions.map(type => ({ value: type.name }))}
                placeholder={t('browser.tags.key')}
                showSearch={{ filterOption: (input, option) => matches(String(option?.value), input) }}
                size="small"
                value={entry.key}
            />
            <AutoComplete
                className={styles.entryValue}
                data-testid={`${testId}-value`}
                onChange={value => onChange({ ...entry, value: (value as string | undefined) ?? '' })}
                options={(matchingType?.values ?? []).map(value => ({ value }))}
                placeholder={t('browser.tags.value')}
                showSearch={{ filterOption: (input, option) => matches(String(option?.value), input) }}
                size="small"
                value={entry.value}
            />
        </div>
    )
}
