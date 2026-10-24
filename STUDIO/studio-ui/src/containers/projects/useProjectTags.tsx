import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Tooltip, Typography } from 'antd'
import { CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { errorMessage } from '../../utils/errorMessage'
import { getTagTypes, type TagType } from '../../services/repositories'
import { deleteFile, rootFileExists, writeRootFile } from '../../services/files'
import { serializeTagsProperties, TAGS_FILE_NAME, type TagEntry } from '../../services/tagsProperties'
import { SuggestInput } from '../../components/SuggestInput'
import { EditableList } from './EditableList'

const useStyles = createStyles(({ css, token }) => ({
    /** The tags read as a table: the type on the left, what the project is tagged with on the right. */
    table: css`
        display: grid;
        grid-template-columns: minmax(0, auto) minmax(0, 1fr);
        /* A warning below a value adds a line, so the type keeps to the first one. */
        align-items: baseline;
        column-gap: 12px;
        row-gap: 6px;
    `,
    type: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    /** A tag type no administrator configured: the project carries it, the database does not know it. */
    unconfigured: css`
        color: ${token.colorWarningText};
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
    /** A tag and, below it, what is wrong with it, if anything. */
    flagged: css`
        display: flex;
        flex-direction: column;
        min-width: 0;
    `,
    warning: css`
        font-size: 12px;
    `,
    error: css`
        margin-top: 8px;
    `,
}))

/** What a tag type name is matched by: tag types match regardless of case and surrounding spaces. */
const nameKey = (name: string) => name.trim().toLowerCase()

const sameName = (one: string, other: string) => nameKey(one) === nameKey(other)

/** The styles of the tags section, read once by the section and handed down to each tag it draws. */
type TagStyles = ReturnType<typeof useStyles>

/**
 * The keys named by more than one entry. A properties file cannot repeat a key, and tag types match
 * regardless of case, so "Domain" and "domain" collide too.
 */
const duplicateKeys = (entries: TagEntry[]): Set<string> => {
    const counts = new Map<string, number>()
    for (const entry of entries) {
        const key = nameKey(entry.key)
        if (key) {
            counts.set(key, (counts.get(key) ?? 0) + 1)
        }
    }
    return new Set([...counts.entries()].filter(([, count]) => count > 1).map(([key]) => key))
}

/** The configured tag type a key names, matched regardless of case the way the server resolves it. */
const typeOf = (types: TagType[], key: string): TagType | undefined => types.find(type => sameName(type.name, key))

/**
 * Whether the tag type does not take the value: the type is not extensible and none of its values matches,
 * regardless of case. A value of an extensible type is always allowed — it simply is a new one.
 */
const isNotAllowed = (type: TagType | undefined, value: string): boolean => !!type && !type.extensible
    && value.trim() !== '' && !(type.values ?? []).some(allowed => sameName(allowed, value))

/**
 * The tags in the order an administrator arranged the tag types in. The tags of types that are not configured
 * follow, in the order the file names them.
 */
const inTypeOrder = (types: TagType[], entries: TagEntry[]): TagEntry[] => {
    const ranks = new Map(types.map((type, index) => [nameKey(type.name), index]))
    const rank = (entry: TagEntry) => ranks.get(nameKey(entry.key)) ?? types.length
    return entries.toSorted((one, other) => rank(one) - rank(other))
}

/** Whether a key names a tag type that is not configured. Nothing is, while the configured types are unknown. */
const isUnconfigured = (types: TagType[] | null, key: string): boolean =>
    types !== null && key.trim() !== '' && !typeOf(types, key)

/** The tag types the tags name that are not configured, each once, in the order the tags name them. */
const unconfiguredTypes = (types: TagType[] | null, entries: TagEntry[]): string[] => {
    const keys = entries.map(entry => entry.key.trim()).filter(key => isUnconfigured(types, key))
    return keys.filter((key, index) => keys.findIndex(other => sameName(other, key)) === index)
}

/** The mandatory tag types the tags leave without a value, whether the key is absent or its value is blank. */
const missingMandatory = (types: TagType[], entries: TagEntry[]): string[] => types
    .filter(type => !type.nullable && !entries.some(entry => sameName(entry.key, type.name) && entry.value.trim()))
    .map(type => type.name)

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
 * The tags.properties file of the project is the source of truth: every tag it names is shown, in the order
 * an administrator arranged the tag types in, and editing writes the file back through the files API, the way
 * rules.xml is edited. Any key and any value can be entered; the tag types an administrator configured are
 * offered as suggestions.
 *
 * The tags are checked against the configured tag types, both while viewed and while edited: a tag type
 * that is not configured, a mandatory type left without a value, and a value a non-extensible type does
 * not take are warned about. A warning never blocks saving, since a project may carry tags the
 * configuration no longer allows.
 */
export const useProjectTags = ({ projectId, tags, canEdit, onSaved }: ProjectTagsOptions) => {
    const { t } = useTranslation('repository')
    // Read once for every tag the section draws, and handed down to them.
    const ui = useStyles()
    const { styles, cx } = ui
    const [editing, setEditing] = useState(false)
    const [entries, setEntries] = useState<TagEntry[]>([])
    // Null until the configured tag types are read, and when they cannot be: nothing is checked then.
    const [tagTypes, setTagTypes] = useState<TagType[] | null>(null)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const types = tagTypes ?? []
    const assigned = inTypeOrder(types, Object.entries(tags).map(([key, value]) => ({ key, value })))
    // A repeated key cannot be written to the properties file, so saving waits until it is resolved.
    const duplicates = editing ? duplicateKeys(entries) : new Set<string>()
    // The warnings follow what is on the screen: the edited tags while editing, so a fix clears them at once.
    const shown = editing ? entries : assigned
    const missing = missingMandatory(types, shown)
    const unconfigured = unconfiguredTypes(tagTypes, shown)

    // The configured tag types feed the checks and the suggestions. They are read again whenever the editing
    // starts or ends, so a change an administrator made meanwhile shows. A failed read keeps what was read
    // before, or checks and suggests nothing when nothing was, and the tags are still shown and edited.
    useEffect(() => {
        let cancelled = false
        getTagTypes()
            .then(read => !cancelled && setTagTypes(read))
            .catch(() => undefined)
        return () => { cancelled = true }
    }, [editing])

    const startEditing = () => {
        setError(null)
        setEntries(assigned)
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
            {assigned.map(({ key, value }) => (
                <TagRow
                    key={key}
                    notAllowed={isNotAllowed(typeOf(types, key), value)}
                    type={key}
                    ui={ui}
                    unconfigured={isUnconfigured(tagTypes, key)}
                    value={value}
                />
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
                        duplicate={duplicates.has(nameKey(entry.key))}
                        entry={entry}
                        onChange={set}
                        suggestions={types}
                        testId={id}
                        ui={ui}
                        unconfigured={isUnconfigured(tagTypes, entry.key)}
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

    const mandatory = missing.length > 0 && (
        <Alert
            showIcon
            className={styles.error}
            data-testid="tags-mandatory"
            title={missing.map(type => <div key={type}>{t('browser.tags.mandatory', { type })}</div>)}
            type="warning"
        />
    )

    const unknown = unconfigured.length > 0 && (
        <Alert
            showIcon
            className={styles.error}
            data-testid="tags-unconfigured"
            title={t('browser.tags.unconfigured', { types: unconfigured.join(', ') })}
            type="warning"
        />
    )

    return {
        action: editActions || editButton,
        content: (
            <>
                {editing ? editor : view}
                {unknown}
                {mandatory}
                {error && <Alert showIcon className={styles.error} data-testid="tags-error" title={error} type="error" />}
            </>
        ),
    }
}

/**
 * One line of the table: the tag type, and what the project carries for it. A type that is not configured
 * is marked, and a value the type does not take is followed by the warning about it.
 */
const TagRow = ({ ui, type, value, notAllowed, unconfigured }: {
    ui: TagStyles
    type: string
    value: string
    notAllowed: boolean
    unconfigured: boolean
}) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = ui
    return (
        <>
            {unconfigured ? (
                <Tooltip title={t('browser.tags.unconfigured_type')}>
                    <span className={cx(styles.type, styles.unconfigured)} data-testid={`tag-unconfigured-${type}`}>
                        {type}
                    </span>
                </Tooltip>
            ) : <span className={styles.type}>{type}</span>}
            <span className={styles.flagged}>
                <span className={styles.value}>{value}</span>
                {notAllowed && <NotAllowedWarning className={styles.warning} type={type} value={value} />}
            </span>
        </>
    )
}

/** Says that the tag type does not take the value. */
const NotAllowedWarning = ({ className, type, value }: { className: string, type: string, value: string }) => {
    const { t } = useTranslation('repository')
    return (
        <Typography.Text className={className} data-testid={`tag-not-allowed-${type}`} type="warning">
            {t('browser.tags.not_allowed', { type, value })}
        </Typography.Text>
    )
}

/** How the key field is marked. A repeated key blocks the save, so it outweighs an unconfigured one. */
const keyStatusOf = (duplicate: boolean, unconfigured: boolean) => {
    if (duplicate) {
        return 'error' as const
    }
    return unconfigured ? 'warning' as const : undefined
}

/**
 * The editable fields of one tag: its key and its value, both free text. The configured tag types are
 * offered for the key, and the values of the matching type for the value — suggestions only, since the
 * file takes whatever the user names. A value the matching type does not take is marked and warned about.
 */
const TagEntryFields = ({ ui, entry, suggestions, duplicate, unconfigured, onChange, testId }: {
    ui: TagStyles
    entry: TagEntry
    suggestions: TagType[]
    /** The key is named by another entry too — the field is marked until one of them changes. */
    duplicate: boolean
    /** The key names a tag type that is not configured — the field is marked as a warning. */
    unconfigured: boolean
    onChange: (entry: TagEntry) => void
    testId: string
}) => {
    const { t } = useTranslation('repository')
    const { styles } = ui
    const matchingType = typeOf(suggestions, entry.key)
    const notAllowed = isNotAllowed(matchingType, entry.value)
    const keyStatus = keyStatusOf(duplicate, unconfigured)
    // The warning goes below the fields in the same wrapper either way, so the fields keep their focus.
    return (
        <div className={styles.flagged}>
            <div className={styles.entry}>
                <SuggestInput
                    className={styles.entryKey}
                    data-testid={testId}
                    {...(keyStatus ? { status: keyStatus } : {})}
                    onChange={key => onChange({ ...entry, key })}
                    options={suggestions.map(type => ({ label: type.name, value: type.name }))}
                    placeholder={t('browser.tags.key')}
                    size="small"
                    value={entry.key}
                />
                <SuggestInput
                    className={styles.entryValue}
                    data-testid={`${testId}-value`}
                    {...(notAllowed ? { status: 'warning' as const } : {})}
                    onChange={value => onChange({ ...entry, value })}
                    options={(matchingType?.values ?? []).map(value => ({ label: value, value }))}
                    placeholder={t('browser.tags.value')}
                    size="small"
                    value={entry.value}
                />
            </div>
            {notAllowed && (
                <NotAllowedWarning className={styles.warning} type={entry.key.trim()} value={entry.value.trim()} />
            )}
        </div>
    )
}
