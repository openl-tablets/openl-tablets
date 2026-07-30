import { useEffect, useMemo, useState, type ReactNode, type SyntheticEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Alert, App, Button, Checkbox, Input, Segmented, Select, Tag, Tooltip, Typography, Upload } from 'antd'
import {
    ApartmentOutlined,
    ApiOutlined,
    CodeOutlined,
    DownOutlined,
    ExportOutlined,
    FileTextOutlined,
    FolderOutlined,
    FunctionOutlined,
    ImportOutlined,
    InfoCircleOutlined,
    LockOutlined,
    LoginOutlined,
    LogoutOutlined,
    ProductOutlined,
    QuestionCircleOutlined,
    RightOutlined,
    SettingOutlined,
    SwapOutlined,
    UnlockOutlined,
    UploadOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { Project, ProjectDependency, ProjectModule } from '../../types/projects'
import { getFileContent, rootFileExists, uploadFile, writeRootFile } from '../../services/files'
import {
    EMPTY_RULES_DESCRIPTOR,
    parseRulesDescriptor,
    serializeRulesDescriptor,
    type DeclaredDependency,
    type MethodFilter,
    type ModuleDeclaration,
    type OpenApiMode,
    type RulesDescriptor,
} from '../../services/rulesDescriptor'
import { getProjectIndex } from '../../services/projectIndex'
import { MigrateButton, useDescriptorMigration } from './projectMigration'
import { getProjectFiles } from '../../services/repositories'
import { errorMessage } from '../../utils/errorMessage'
import { EditableList, EditableStringList } from './EditableList'
import { EditToolbar } from './EditToolbar'
import { PropertiesPatternHelpModal } from './PropertiesPatternHelpModal'
import { formatDateTime } from '../../utils/dateFormat'
import { useSharedStyles } from './sharedStyles'
import { StatusPill } from './StatusIndicator'
import { ValueText } from './ValueText'
import { RepoBadge } from './RepoBadge'
import { BranchLabel } from './BranchLabel'
import { BranchSwitcher } from './BranchSwitcher'
import { ManageBranchesModal } from './ManageBranchesModal'
import { GitCommitMessage } from './GitCommitMessage'
import { useProjectTags } from './useProjectTags'
import { shortRevision } from './revisions'

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 24px;
        padding: 16px;

        @media (max-width: 1100px) {
            grid-template-columns: 1fr;
        }
    `,
    left: css`
        display: flex;
        flex-direction: column;
        gap: 16px;
        min-width: 0;
    `,
    right: css`
        display: flex;
        flex-direction: column;
        gap: 20px;
        min-width: 0;
    `,
    lockBanner: css`
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 12px;
        border: 1px solid ${token.colorWarningBorder};
        border-radius: ${token.borderRadius}px;
        background: ${token.colorWarningBg};
        font-size: 14px;

        .anticon-lock {
            color: ${token.colorWarning};
        }
    `,
    lockUnlock: css`
        margin-left: auto;
    `,
    sectionTitle: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin: 0 0 8px;
    `,
    sectionTitleText: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;

        .anticon {
            font-size: 13px;
        }
    `,
    /** A small info mark by the heading, its tooltip anchored to it rather than the whole block. */
    sectionHint: css`
        color: ${token.colorTextTertiary};

        &.anticon {
            font-size: 12px;
        }
    `,
    /** The help mark reads exactly like the info marks, but it is clickable — it opens a dialog. */
    sectionHelp: css`
        color: ${token.colorTextTertiary};
        cursor: pointer;

        &.anticon {
            font-size: 12px;
        }

        &:hover {
            color: ${token.colorText};
        }
    `,
    /** A section hint keeps the line breaks it is written with, so a multi-line note reads as written. */
    hintText: css`
        white-space: pre-line;
    `,
    /** What a section holds is stepped in, so it reads as part of the heading above it. */
    sectionBody: css`
        padding-left: 20px;
    `,
    /** The switcher ends the heading line, after whatever the section itself offers. */
    sectionSwitcher: css`
        display: inline-flex;
        align-items: center;
        flex: none;
        padding: 0;
        border: none;
        background: transparent;
        color: ${token.colorTextTertiary};
        cursor: pointer;

        .anticon {
            font-size: 10px;
        }
    `,
    twoCol: css`
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;

        @media (max-width: 640px) {
            grid-template-columns: 1fr;
        }
    `,
    description: css`
        margin: 0;
        font-size: 14px;
        line-height: 1.6;
        color: ${token.colorTextSecondary};
    `,
    /** The modules stand on their own, so they are spaced instead of framed. */
    moduleRows: css`
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 12px;
    `,
    rowName: css`
        flex: 1;
        min-width: 0;
    `,
    rowLink: css`
        flex: 1;
        min-width: 0;
        color: ${token.colorLink};

        &:hover {
            color: ${token.colorLinkHover};
        }
    `,
    rowMeta: css`
        flex: none;
    `,
    /** A dependency the workspace cannot show is marked, so an empty row does not read as a broken link. */
    missingTag: css`
        flex: none;
        margin: 0;
        color: ${token.colorErrorText};
        background: ${token.colorErrorBg};
        border-color: ${token.colorErrorBorder};
    `,
    /**
     * A module reads in columns — its name, the path beside it, the switcher at the end — so a list of
     * them lines up like a table however long a single name or path is. It carries neither an icon nor a
     * frame: a list of framed rows is mostly lines, and the names are what is read.
     */
    moduleRow: css`
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;
    `,
    moduleName: css`
        flex: 1;
        min-width: 0;
    `,
    /** The path ends the row, and is cut rather than allowed to push the name out of the row. */
    modulePath: css`
        flex: none;
        max-width: 55%;
        color: ${token.colorTextTertiary};
        font-size: 12px;
        text-align: right;
    `,
    /** The switcher opens the row it stands before, as it does in a tree. */
    moduleToggle: css`
        flex: none;
        width: 20px;
        min-width: 20px;
        color: ${token.colorTextTertiary};

        .anticon {
            font-size: 9px;
        }
    `,
    moduleSwitcherSpace: css`
        flex: none;
        width: 20px;
    `,
    /** A matched module keeps the columns of the list and is stepped in, as a child row is. */
    matchedModule: css`
        padding-left: 16px;
    `,
    meta: css`
        margin: 0;
    `,
    metaRow: css`
        padding: 10px 0;
        border-top: 1px solid ${token.colorBorderSecondary};

        &:first-of-type {
            border-top: none;
            padding-top: 0;
        }
    `,
    copyAction: css`
        /* The copy affordance reads like the other row actions, not like a link. */
        .ant-typography-copy {
            color: ${token.colorTextTertiary};

            &:hover {
                color: ${token.colorText};
            }
        }
    `,
    metaLabelRow: css`
        display: flex;
        align-items: center;
        gap: 4px;
        min-height: 22px;
    `,
    metaValue: css`
        margin-top: 4px;
        font-size: 14px;
    `,
    metaSub: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    statusValue: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
    `,
    pattern: css`
        padding: 6px 8px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorFillQuaternary};
        /* The value sits in a <code>, so drop its user-agent monospace and keep the tab's font. */
        font-family: inherit;
        word-break: break-all;
    `,
    /** Includes and excludes read as two columns of text — no box, just the two lists side by side. */
    filterPanel: css`
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;

        @media (max-width: 640px) {
            grid-template-columns: 1fr;
        }
    `,
    filterPanelCompact: css`
        flex: 1;
        min-width: 0;
    `,
    filterColumn: css`
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
    `,
    /** Depends-on and used-by stand as two columns of one Dependencies section. */
    dependencyColumn: css`
        display: flex;
        flex-direction: column;
        gap: 6px;
        min-width: 0;
    `,
    dependencyLabel: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;

        .anticon {
            font-size: 12px;
        }
    `,
    moduleFields: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
    `,
    moduleNameField: css`
        flex: none;
        width: 40%;
    `,
    dependencyFields: css`
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;
    `,
    dependencySelect: css`
        flex: 1;
        min-width: 0;
    `,
    filterEditLabel: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 4px;
        color: ${token.colorTextTertiary};
        font-size: 12px;

        .anticon {
            font-size: 12px;
        }
    `,
    /** The edit controls sit at the top of the tab, not hidden below the content. */
    editBar: css`
        display: flex;
        justify-content: flex-end;
        gap: 8px;
    `,
    /** Values of a labelled column (include/exclude, depends-on, used-by) step in under their label,
     * so the line sits under the label text — past its icon — rather than directly under the icon. */
    filterValues: css`
        margin-left: 18px;
    `,
    /** A plain list read the way exposed methods are: values on their own lines, a line down the left. */
    linedList: css`
        list-style: none;
        margin: 0;
        padding: 0 0 0 12px;
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
        border-left: 2px solid ${token.colorBorderSecondary};
    `,
    linedItem: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        word-break: break-word;
    `,
    moduleFilter: css`
        display: flex;
        align-items: stretch;
    `,
    openapi: css`
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 8px;
    `,
    openapiRow: css`
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
    `,
    openapiValue: css`
        min-width: 0;
        word-break: break-all;
    `,
    /** The file picker fills the row, with the upload beside it. */
    openapiFileRow: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
    `,
    openapiFileSelect: css`
        flex: 1;
        min-width: 0;
    `,
    /** A mode of the toggle with its info mark beside the caption. */
    openapiModeOption: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
    `,
    /** In the editing view the label and its field share one line. */
    openapiEditRow: css`
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;
    `,
    openapiEditLabel: css`
        flex: none;
        width: 140px;
    `,
    openapiEditValue: css`
        flex: 1;
        min-width: 0;
        margin: 0;
    `,
}))

/** A part of the overview, folded away by its own heading when the reader has no use for it. */
const Section = ({ icon, title, action, hint, hintTestId, onHelp, helpLabel, helpTestId, children }: { icon?: ReactNode; title: string; action?: ReactNode; hint?: string | undefined; hintTestId?: string; onHelp?: (() => void) | undefined; helpLabel?: string | undefined; helpTestId?: string; children: ReactNode }) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [open, setOpen] = useState(true)
    // The help mark sits with the heading but opens a dialog rather than folding the section, so its click
    // and keyboard activation are kept from reaching the surrounding toggle.
    const openHelp = (event: SyntheticEvent) => {
        event.stopPropagation()
        onHelp?.()
    }
    return (
        <section>
            <h3 className={cx(shared.microLabel, styles.sectionTitle)}>
                <button
                    aria-expanded={open}
                    className={shared.sectionToggle}
                    onClick={() => setOpen(shown => !shown)}
                    type="button"
                >
                    <span className={styles.sectionTitleText}>
                        {icon}
                        {title}
                        {hint && (
                            <Tooltip title={<span className={styles.hintText}>{hint}</span>}>
                                <InfoCircleOutlined className={styles.sectionHint} data-testid={hintTestId} />
                            </Tooltip>
                        )}
                        {onHelp && (
                            <Tooltip title={helpLabel}>
                                <QuestionCircleOutlined
                                    aria-label={helpLabel}
                                    className={styles.sectionHelp}
                                    data-testid={helpTestId}
                                    onClick={openHelp}
                                    onKeyDown={event => (event.key === 'Enter' || event.key === ' ') && openHelp(event)}
                                    role="button"
                                    tabIndex={0}
                                />
                            </Tooltip>
                        )}
                    </span>
                </button>
                {action}
                <button
                    aria-expanded={open}
                    aria-label={title}
                    className={styles.sectionSwitcher}
                    onClick={() => setOpen(shown => !shown)}
                    type="button"
                >
                    {open ? <DownOutlined /> : <RightOutlined />}
                </button>
            </h3>
            {open && <div className={styles.sectionBody}>{children}</div>}
        </section>
    )
}

/**
 * The two columns a module reads in: its name, and the path it is taken from beside it.
 *
 * Both are cut with an ellipsis instead of widening their column, so a long name or a deep path never
 * pushes the column beside it out of line.
 *
 * A pattern that names no module of its own is read by what it does — it stands for the modules it
 * matched.
 */
const ModuleCells = ({ module, modulesDefault }: { module: ProjectModule, modulesDefault?: boolean | undefined }) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    // A project that declares no modules of its own takes the engine's defaults: those read as the rules
    // and the tests found automatically, not as a "pattern". A pattern the file does declare keeps its own
    // name, or, unnamed, reads by what it matched.
    const autoDiscoveredHeading = module.path?.startsWith('tests/')
        ? t('browser.overview.modules_auto_tests')
        : t('browser.overview.modules_auto')
    const patternHeading = modulesDefault ? autoDiscoveredHeading : t('browser.overview.modules_pattern')
    const name = module.name || (module.modules ? patternHeading : '')
    return (
        <>
            <span className={cx(shared.valueText, shared.ellipsis, styles.moduleName)} title={name}>{name}</span>
            <span className={cx(shared.ellipsis, shared.valueText, styles.modulePath)} title={module.path}>
                {module.path}
            </span>
        </>
    )
}

/**
 * One module of the project, as its {@code rules.xml} declares it.
 *
 * A declaration whose path is a pattern stands for the files it matched: they are folded away under it
 * and opened on demand, so the list stays as long as the file is.
 */
const ModuleRow = ({ module, filter, modulesDefault }: { module: ProjectModule, filter?: MethodFilter | undefined, modulesDefault?: boolean | undefined }) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [open, setOpen] = useState(false)
    const matched = module.modules
    const testId = module.path ?? module.name
    const toggleTitle = t(open ? 'browser.overview.modules_matched_hide' : 'browser.overview.modules_matched_show')
    return (
        <>
            <li className={styles.moduleRow} data-testid={`module-${testId}`}>
                {matched && matched.length > 0 ? (
                    <Tooltip title={toggleTitle}>
                        <Button
                            aria-expanded={open}
                            aria-label={toggleTitle}
                            className={styles.moduleToggle}
                            data-testid={`module-matched-${testId}`}
                            icon={open ? <DownOutlined /> : <RightOutlined />}
                            onClick={() => setOpen(shown => !shown)}
                            size="small"
                            type="text"
                        />
                    </Tooltip>
                ) : (
                    // The place of the switcher is kept, so every row starts where the others do.
                    <span className={styles.moduleSwitcherSpace} data-testid={matched ? `module-unmatched-${testId}` : undefined} />
                )}
                <ModuleCells module={module} modulesDefault={modulesDefault} />
            </li>
            {/* The module's own method filter, declared in rules.xml alongside it. */}
            {filter && (
                <li className={cx(styles.moduleRow, styles.moduleFilter)} data-testid={`module-filter-${testId}`}>
                    <span className={styles.moduleSwitcherSpace} />
                    <FilterPanel compact filter={filter} />
                </li>
            )}
            {open && matched?.map(matchedModule => (
                <li
                    key={matchedModule.path ?? matchedModule.name}
                    className={cx(styles.moduleRow, styles.matchedModule)}
                    data-testid={`module-matched-item-${matchedModule.path ?? matchedModule.name}`}
                >
                    <span className={styles.moduleSwitcherSpace} />
                    <ModuleCells module={matchedModule} />
                </li>
            ))}
        </>
    )
}

/** The editable fields of one declared module: its name and its rules-root path (which may be a pattern). */
const ModuleFields = ({ module, onChange, testId }: {
    module: ModuleDeclaration
    onChange: (module: ModuleDeclaration) => void
    testId: string
}) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    return (
        <div className={styles.moduleFields}>
            <Input
                className={styles.moduleNameField}
                data-testid={testId}
                onChange={event => onChange({ ...module, name: event.target.value })}
                placeholder={t('browser.overview.module_name')}
                size="small"
                value={module.name}
            />
            <Input
                data-testid={`${testId}-path`}
                onChange={event => onChange({ ...module, path: event.target.value })}
                placeholder={t('browser.overview.module_path')}
                size="small"
                value={module.path}
            />
        </div>
    )
}

/**
 * The editable fields of one declared dependency, on one line: the project it points at, chosen from the
 * other projects, and its auto-included flag beside it.
 */
const DependencyFields = ({ dependency, names, onChange, testId }: {
    dependency: DeclaredDependency
    names: string[]
    onChange: (dependency: DeclaredDependency) => void
    testId: string
}) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    // The declared project keeps its place in the list even if the workspace no longer has it.
    const options = [...new Set([...names, dependency.name].filter(Boolean))].map(name => ({ label: name, value: name }))
    return (
        <div className={styles.dependencyFields}>
            <Select
                className={styles.dependencySelect}
                data-testid={testId}
                onChange={value => onChange({ ...dependency, name: value ?? '' })}
                options={options}
                placeholder={t('browser.overview.dependency_name')}
                showSearch={{ optionFilterProp: 'label' }}
                size="small"
                value={dependency.name || undefined}
            />
            <Checkbox
                checked={dependency.autoIncluded}
                data-testid={`${testId}-auto`}
                onChange={event => onChange({ ...dependency, autoIncluded: event.target.checked })}
            >
                {t('browser.overview.dependency_auto_included')}
            </Checkbox>
        </div>
    )
}

/**
 * Applies a change to the OpenAPI settings of a draft. A mode alone keeps the draft alive so the toggle
 * works before a file is picked; the serializer drops a configuration that names no file or module.
 */
const editOpenApi = (
    current: RulesDescriptor,
    editDraft: (change: Partial<RulesDescriptor>) => void,
    change: Partial<NonNullable<RulesDescriptor['openapi']>>
) => {
    const next = { ...current.openapi, ...change }
    const hasValue = [next.path, next.modelModuleName, next.algorithmModuleName].some(value => value?.trim())
        || next.mode !== undefined
    editDraft({ openapi: hasValue ? next : undefined })
}

/** The includes and excludes of a method filter, each an editable list; empties drop the filter. */
const EditableFilter = ({ filter, onChange }: { filter: MethodFilter | undefined, onChange: (filter: MethodFilter | undefined) => void }) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const includes = filter?.includes ?? []
    const excludes = filter?.excludes ?? []
    const update = (next: MethodFilter) =>
        onChange(next.includes.length === 0 && next.excludes.length === 0 ? undefined : next)
    return (
        <div className={styles.filterPanel}>
            <div className={styles.filterColumn}>
                <span className={styles.filterEditLabel}><LoginOutlined /> {t('browser.overview.exposed_includes')}</span>
                <EditableStringList items={includes} onChange={values => update({ includes: values, excludes })} testId="edit-include" />
            </div>
            <div className={styles.filterColumn}>
                <span className={styles.filterEditLabel}><LogoutOutlined /> {t('browser.overview.exposed_excludes')}</span>
                <EditableStringList items={excludes} onChange={values => update({ includes, excludes: values })} testId="edit-exclude" />
            </div>
        </div>
    )
}

/**
 * A method filter as one panel of two columns — the includes beside the excludes — each a list of
 * patterns, rather than a scatter of chips.
 */
const FilterPanel = ({ filter, compact }: { filter: MethodFilter, compact?: boolean }) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const column = (icon: ReactNode, labelKey: string, patterns: string[]) => (
        <div className={styles.filterColumn}>
            <span className={cx(shared.microLabel, styles.dependencyLabel)}>{icon} {t(labelKey)}</span>
            <ul className={cx(styles.linedList, styles.filterValues)}>
                {patterns.map(pattern => <li key={pattern} className={cx(shared.valueText, styles.linedItem)}>{pattern}</li>)}
            </ul>
        </div>
    )
    return (
        <div className={cx(styles.filterPanel, compact && styles.filterPanelCompact)}>
            {filter.includes.length > 0 && column(<LoginOutlined />, 'browser.overview.exposed_includes', filter.includes)}
            {filter.excludes.length > 0 && column(<LogoutOutlined />, 'browser.overview.exposed_excludes', filter.excludes)}
        </div>
    )
}

/** The method-filter each declared module carries, indexed by its rules-root path. */
const moduleFiltersOf = (declarations: ModuleDeclaration[]): Record<string, MethodFilter> => {
    const filters: Record<string, MethodFilter> = {}
    for (const module of declarations) {
        if (module.methodFilter) {
            filters[module.path] = module.methodFilter
        }
    }
    return filters
}

/** What a descriptor section reads and edits: the working copy shown, and how a change lands in the draft. */
interface DescriptorEditor {
    editing: boolean
    shown: RulesDescriptor
    editDraft: (change: Partial<RulesDescriptor>) => void
}

/**
 * The rules.xml of a project as the overview edits it: read on mount and on reload, edited as a working
 * copy, and written back with everything the editor does not manage preserved.
 *
 * Editing is only offered once the file has been read — a failed read must not let a save overwrite a
 * real rules.xml from an empty base, silently dropping everything it declared.
 */
const useRulesDescriptor = (project: Project, reloadToken: number | undefined, onSaved: () => void) => {
    const { t } = useTranslation('repository')
    const { notification } = App.useApp()
    const [rules, setRules] = useState<RulesDescriptor>(EMPTY_RULES_DESCRIPTOR)
    // The raw file is kept so a save rewrites only the managed elements and preserves the rest.
    const [originalXml, setOriginalXml] = useState('')
    const [fileExists, setFileExists] = useState(false)
    const [state, setState] = useState<'loading' | 'ready' | 'error'>('loading')
    const [editing, setEditing] = useState(false)
    const [draft, setDraft] = useState<RulesDescriptor>(EMPTY_RULES_DESCRIPTOR)
    const [saving, setSaving] = useState(false)
    // The projects a dependency can be chosen from — read once when editing starts.
    const [projectNames, setProjectNames] = useState<string[]>([])

    useEffect(() => {
        let cancelled = false
        setEditing(false)
        setState('loading')
        rootFileExists(project.id, 'rules.xml')
            .then(async exists => {
                const xml = exists ? await getFileContent(project.id, 'rules.xml') : ''
                if (!cancelled) {
                    setFileExists(exists)
                    setOriginalXml(xml)
                    setRules(parseRulesDescriptor(xml))
                    setState('ready')
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setRules(EMPTY_RULES_DESCRIPTOR)
                    setState('error')
                }
            })
        return () => { cancelled = true }
    }, [project.id, reloadToken])

    const editDraft = (change: Partial<RulesDescriptor>) => setDraft(previous => ({ ...previous, ...change }))

    const startEditing = () => {
        setDraft(rules)
        setEditing(true)
        // A dependency is picked from the other projects, so their names are loaded for the picker.
        void getProjectIndex()
            .then(index => setProjectNames(index.projects.map(candidate => candidate.name).filter(name => name !== project.name)))
            .catch(() => setProjectNames([]))
    }

    const cancelEditing = () => setEditing(false)

    const saveEditing = async () => {
        setSaving(true)
        try {
            const xml = serializeRulesDescriptor(draft, originalXml)
            await writeRootFile(project.id, 'rules.xml', xml, fileExists ? 'overwrite' : 'create')
            // Adopt the saved text at once, so the read view shows it without waiting for the reload and a
            // second save writes over the file that now exists rather than re-creating it from nothing.
            setRules(draft)
            setOriginalXml(xml)
            setFileExists(true)
            setEditing(false)
            onSaved()
        } catch (e) {
            notification.error({ title: t('browser.overview.save_failed'), description: errorMessage(e) })
        } finally {
            setSaving(false)
        }
    }

    // Each declared module carries its own method filter; index them by path to show on the resolved module.
    const moduleFilters = useMemo(() => moduleFiltersOf(rules.moduleDeclarations), [rules.moduleDeclarations])

    const editor: DescriptorEditor = { editing, shown: editing ? draft : rules, editDraft }
    return { state, editing, saving, fileExists, projectNames, moduleFilters, editor, startEditing, cancelEditing, saveEditing }
}

/**
 * Whether the project can be brought to the current rules.xml conventions, and the action that does it.
 *
 * <p>A project whose workbooks sit in the root has no rules.xml; writing one would switch module discovery
 * to the {@code rules/}/{@code tests/} patterns and lose those workbooks, so a migrate — which moves them
 * under {@code rules/} first — is offered and is required before the descriptor can be edited. A project
 * that already has a rules.xml is offered a migrate only when it would actually be rewritten.
 */
const useProjectMigration = (project: Project, reloadToken: number | undefined, onMigrated: () => void) => {
    const { t } = useTranslation('repository')
    const { modal } = App.useApp()
    const canWrite = project.capabilities?.canWrite ?? false
    const { migration, migrating, run } = useDescriptorMigration(project.id, canWrite, reloadToken, onMigrated)

    // The root workbooks must move under rules/ before a rules.xml exists, so editing is blocked until then.
    const mustMigrateBeforeEditing = migration.rulesXml.movableRootModules.length > 0

    const migrate = () => {
        modal.confirm({
            title: t('browser.overview.migrate'),
            content: mustMigrateBeforeEditing
                ? t('browser.overview.migrate_move_confirm', { count: migration.rulesXml.movableRootModules.length })
                : t('browser.overview.migrate_rewrite_confirm'),
            okText: t('browser.overview.migrate'),
            onOk: () => run('rulesXml', t('browser.overview.migrate_failed')),
        })
    }

    return { canMigrate: migration.rulesXml.migratable, mustMigrateBeforeEditing, migrating, migrate }
}

/** The banner naming who holds the project locked, with the unlock at hand for whoever may use it. */
const LockBanner = ({ project, onUnlock }: { project: Project, onUnlock: () => void }) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    if (!project.lockInfo) {
        return null
    }
    return (
        <div className={styles.lockBanner}>
            <LockOutlined />
            <span>
                {t('browser.locked_by', {
                    by: project.lockInfo.lockedBy,
                    at: formatDateTime(project.lockInfo.lockedAt) ?? project.lockInfo.lockedAt,
                })}
            </span>
            {project.capabilities?.canUnlock && (
                <Button className={styles.lockUnlock} icon={<UnlockOutlined />} onClick={onUnlock} size="small" type="text">
                    {t('browser.overview.unlock')}
                </Button>
            )}
        </div>
    )
}

/** The description of the project, as rules.xml declares it. */
const DescriptionSection = ({ editor }: { editor: DescriptorEditor }) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { editing, shown, editDraft } = editor
    if (!editing && !shown.description) {
        return null
    }
    return (
        <Section icon={<FileTextOutlined />} title={t('browser.overview.description')}>
            {editing
                ? (
                    <Input.TextArea
                        data-testid="edit-description"
                        onChange={event => editDraft({ description: event.target.value })}
                        rows={3}
                        value={shown.description}
                    />
                )
                : <p className={styles.description}>{shown.description}</p>}
        </Section>
    )
}

/**
 * The modules: the declared ones are edited only when rules.xml declares them; when they are auto-discovered
 * (empty rules.xml) they are shown read-only, since editing entries the engine derives makes no sense.
 */
const ModulesSection = ({ editor, modules, modulesDefault, moduleFilters, hasRulesXml }: {
    editor: DescriptorEditor
    modules: ProjectModule[]
    modulesDefault: boolean
    moduleFilters: Record<string, MethodFilter>
    hasRulesXml: boolean
}) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { editing, shown, editDraft } = editor
    // Modules can be edited only when rules.xml declares them. Auto-discovered ones (empty rules.xml) are
    // resolved by the engine, so they stay read-only even while the rest of the descriptor is edited.
    const modulesEditable = hasRulesXml && !modulesDefault
    const asDeclarations = editing && modulesEditable
    if (!editing && modules.length === 0) {
        return null
    }
    return (
        <Section
            hint={!editing && modulesDefault ? t('browser.overview.default_hint') : undefined}
            hintTestId="modules-default"
            icon={<ProductOutlined />}
            title={t('browser.overview.modules', { count: asDeclarations ? shown.moduleDeclarations.length : modules.length })}
        >
            {asDeclarations
                ? (
                    // The declared modules — name and rules-root — are edited; each keeps its own
                    // method filter, and the engine resolves any wildcard after the save.
                    <EditableList
                        items={shown.moduleDeclarations}
                        newItem={() => ({ name: '', path: '' })}
                        onChange={moduleDeclarations => editDraft({ moduleDeclarations })}
                        renderItem={(module, set, id) => <ModuleFields module={module} onChange={set} testId={id} />}
                        testId="edit-module"
                    />
                )
                : (
                    <>
                        {editing && (
                            <Alert
                                data-testid="modules-readonly"
                                style={{ marginBottom: 8 }}
                                title={t('browser.overview.modules_auto_note')}
                                type="info"
                            />
                        )}
                        <ul className={styles.moduleRows}>
                            {modules.map(module => (
                                <ModuleRow
                                    key={module.path ?? module.name}
                                    filter={module.path ? moduleFilters[module.path] : undefined}
                                    module={module}
                                    modulesDefault={modulesDefault}
                                />
                            ))}
                        </ul>
                    </>
                )}
        </Section>
    )
}

/** The version patterns, with the full pattern description a dialog away. */
const VersionPatternsSection = ({ editor, onHelp }: { editor: DescriptorEditor, onHelp: () => void }) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { editing, shown, editDraft } = editor
    if (!editing && shown.versionPatterns.length === 0) {
        return null
    }
    return (
        <Section
            helpLabel={t('browser.overview.pattern_help_open')}
            helpTestId="pattern-help-open"
            icon={<SwapOutlined />}
            onHelp={onHelp}
            title={t('browser.overview.version_patterns')}
        >
            {editing
                ? (
                    <EditableStringList
                        items={shown.versionPatterns}
                        onChange={versionPatterns => editDraft({ versionPatterns })}
                        testId="edit-version-pattern"
                    />
                )
                : (
                    <ul className={styles.linedList}>
                        {shown.versionPatterns.map(pattern => (
                            <li key={pattern} className={cx(shared.valueText, styles.linedItem)}>{pattern}</li>
                        ))}
                    </ul>
                )}
        </Section>
    )
}

/** The properties file name processor class, when the project names one. */
const ProcessorSection = ({ editor }: { editor: DescriptorEditor }) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { editing, shown, editDraft } = editor
    if (!editing && !shown.propertiesFileNameProcessor) {
        return null
    }
    return (
        <Section icon={<FunctionOutlined />} title={t('browser.overview.properties_processor')}>
            {editing
                ? (
                    <Input
                        data-testid="edit-processor"
                        onChange={event => editDraft({ propertiesFileNameProcessor: event.target.value })}
                        value={shown.propertiesFileNameProcessor ?? ''}
                    />
                )
                : <code className={cx(shared.valueText, styles.pattern)}>{shown.propertiesFileNameProcessor}</code>}
        </Section>
    )
}

/** The methods the project exposes when deployed, as its method filter declares them. */
const ExposedMethodsSection = ({ editor }: { editor: DescriptorEditor }) => {
    const { t } = useTranslation('repository')
    const { editing, shown, editDraft } = editor
    if (!editing && !shown.exposedMethods) {
        return null
    }
    return (
        <Section
            hint={t(editing ? 'browser.overview.exposed_hint_edit' : 'browser.overview.exposed_hint')}
            hintTestId="exposed-methods-hint"
            icon={<CodeOutlined />}
            title={t('browser.overview.exposed_methods')}
        >
            {editing
                ? <EditableFilter filter={shown.exposedMethods} onChange={exposedMethods => editDraft({ exposedMethods })} />
                : shown.exposedMethods && <FilterPanel filter={shown.exposedMethods} />}
        </Section>
    )
}

/** One column of related projects: what this one depends on, or what uses it. */
const DependencyList = ({ deps }: { deps: ProjectDependency[] }) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    return (
        <ul className={cx(styles.linedList, styles.filterValues)}>
            {deps.map(dep => (
                <li key={dep.id ?? dep.name} className={styles.linedItem}>
                    {/* rules.xml names a project the workspace does not have: it is shown as declared,
                        with nothing to open. */}
                    {dep.id
                        ? (
                            <Link className={cx(shared.valueText, shared.ellipsis, styles.rowLink)} to={`/projects/${encodeURIComponent(dep.id)}`}>
                                {dep.name}
                            </Link>
                        )
                        : <span className={cx(shared.valueText, shared.ellipsis, styles.rowName)}>{dep.name}</span>}
                    {dep.missing && (
                        <Tag className={styles.missingTag} data-testid={`dependency-missing-${dep.name}`}>
                            {t('browser.overview.dependency_missing')}
                        </Tag>
                    )}
                    {dep.branch && (
                        <BranchLabel
                            className={styles.rowMeta}
                            isDefault={dep.branchDefault}
                            isProtected={dep.branchProtected}
                            name={dep.branch}
                            testId={`dependency-branch-${dep.id}`}
                        />
                    )}
                </li>
            ))}
        </ul>
    )
}

/** The projects this one depends on — the declared list while editing — and the projects using it. */
const DependenciesSection = ({ editor, dependsOn, usedBy, projectNames }: {
    editor: DescriptorEditor
    dependsOn: ProjectDependency[]
    usedBy: ProjectDependency[]
    projectNames: string[]
}) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { editing, shown, editDraft } = editor
    if (!editing && dependsOn.length === 0 && usedBy.length === 0) {
        return null
    }
    return (
        <Section icon={<ApartmentOutlined />} title={t('browser.overview.dependencies')}>
            {editing
                ? (
                    // Editing the declared depends-on; used-by is derived from other projects
                    // and is not part of this file.
                    <EditableList
                        items={shown.dependencies}
                        newItem={() => ({ name: '', autoIncluded: false })}
                        onChange={dependencies => editDraft({ dependencies })}
                        testId="edit-dependency"
                        renderItem={(dependency, set, id) => (
                            <DependencyFields dependency={dependency} names={projectNames} onChange={set} testId={id} />
                        )}
                    />
                )
                : (
                    <div className={styles.twoCol}>
                        {dependsOn.length > 0 && (
                            <div className={styles.dependencyColumn}>
                                <span className={cx(shared.microLabel, styles.dependencyLabel)}>
                                    <ExportOutlined /> {t('browser.overview.depends_on')}
                                </span>
                                <DependencyList deps={dependsOn} />
                            </div>
                        )}
                        {usedBy.length > 0 && (
                            <div className={styles.dependencyColumn}>
                                <span className={cx(shared.microLabel, styles.dependencyLabel)}>
                                    <ImportOutlined /> {t('browser.overview.used_by')}
                                </span>
                                <DependencyList deps={usedBy} />
                            </div>
                        )}
                    </div>
                )}
        </Section>
    )
}

/**
 * The classpath (source path) entries. Declared entries are edited; the engine defaults (an empty
 * classpath) are shown read-only, since editing entries the engine derives makes no sense — a library is
 * added by dropping its file in the standard folder instead.
 */
const SourcesSection = ({ editor, sources, sourcesDefault, hasRulesXml }: {
    editor: DescriptorEditor
    sources: string[]
    sourcesDefault: boolean
    hasRulesXml: boolean
}) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { editing, shown, editDraft } = editor
    // Editable only when rules.xml declares its own classpath. The engine defaults (no rules.xml, or one
    // with an empty classpath) stay read-only even while the rest of the descriptor is edited.
    const sourcesEditable = hasRulesXml && !sourcesDefault
    if (!editing && sources.length === 0) {
        return null
    }
    return (
        <Section
            hint={!editing && sourcesDefault ? t('browser.overview.sources_default_hint') : undefined}
            hintTestId="sources-default"
            icon={<FolderOutlined />}
            title={t('browser.overview.sources')}
        >
            {editing && sourcesEditable
                ? (
                    // Editing the file's own declared classpath entries.
                    <EditableStringList items={shown.sources} onChange={values => editDraft({ sources: values })} testId="edit-source" />
                )
                : (
                    <>
                        {editing && (
                            <Alert
                                data-testid="sources-readonly"
                                style={{ marginBottom: 8 }}
                                title={t('browser.overview.sources_auto_note')}
                                type="info"
                            />
                        )}
                        <ul className={styles.linedList}>
                            {sources.map(source => (
                                <li key={source} className={cx(shared.valueText, styles.linedItem)}>{source}</li>
                            ))}
                        </ul>
                    </>
                )}
        </Section>
    )
}

/** The files of the project an OpenAPI specification can be picked from. */
const isOpenApiFile = (path: string): boolean => /\.(json|yaml|yml)$/i.test(path)

/**
 * The OpenAPI settings, edited in place the way the legacy editor configured them: the specification is
 * picked from the project files or uploaded, and the mode says whether the project is validated against
 * it or its tables are generated from it. The module names only matter for generation, so they only show
 * for it.
 */
const OpenApiSection = ({ editor, projectId }: { editor: DescriptorEditor, projectId: string }) => {
    const { t } = useTranslation('repository')
    const { notification } = App.useApp()
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { editing, shown, editDraft } = editor
    const [files, setFiles] = useState<string[]>([])
    const [uploading, setUploading] = useState(false)

    // The pickable files are read when the editing starts; without them the upload still works.
    useEffect(() => {
        if (!editing) {
            return
        }
        let cancelled = false
        getProjectFiles(projectId)
            .then(nodes => {
                if (!cancelled) {
                    setFiles(nodes.filter(node => node.type === 'file' && isOpenApiFile(node.path)).map(node => node.path))
                }
            })
            .catch(() => setFiles([]))
        return () => { cancelled = true }
    }, [editing, projectId])

    if (!editing && !shown.openapi) {
        return null
    }

    const mode = shown.openapi?.mode ?? 'RECONCILIATION'
    const modeLabel = (labelKey: string, hintKey: string) => (
        <span className={styles.openapiModeOption}>
            {t(labelKey)}
            <Tooltip title={t(hintKey)}>
                <InfoCircleOutlined className={styles.sectionHint} />
            </Tooltip>
        </span>
    )

    const upload = async (file: File) => {
        setUploading(true)
        try {
            await uploadFile(projectId, '', file, file.name)
            editOpenApi(shown, editDraft, { path: file.name })
            setFiles(current => (current.includes(file.name)
                ? current
                : [...current, file.name].sort((left, right) => left.localeCompare(right))))
        } catch (e) {
            notification.error({ title: t('browser.overview.openapi_upload_failed'), description: errorMessage(e) })
        } finally {
            setUploading(false)
        }
    }

    const row = (label: string, value: string) => (
        <div className={styles.openapiRow}>
            <dt className={shared.microLabel}>{label}</dt>
            <dd className={cx(shared.valueText, styles.openapiValue)}>{value}</dd>
        </div>
    )
    const editRow = (label: string, value: ReactNode) => (
        <div className={styles.openapiEditRow}>
            <dt className={cx(shared.microLabel, styles.openapiEditLabel)}>{label}</dt>
            <dd className={styles.openapiEditValue}>{value}</dd>
        </div>
    )
    const moduleRow = (testId: string, label: string, value: string, onChange: (value: string) => void) =>
        editRow(label, <Input data-testid={testId} onChange={event => onChange(event.target.value)} size="small" value={value} />)

    return (
        <Section icon={<ApiOutlined />} title={t('browser.overview.openapi')}>
            {editing
                ? (
                    <dl className={styles.openapi}>
                        {editRow(t('browser.overview.openapi_path'), (
                            <span className={styles.openapiFileRow}>
                                <Select
                                    allowClear
                                    className={styles.openapiFileSelect}
                                    data-testid="edit-openapi-path"
                                    options={files.map(file => ({ label: file, value: file }))}
                                    placeholder={t('browser.overview.openapi_pick')}
                                    showSearch={{ optionFilterProp: 'label' }}
                                    size="small"
                                    value={shown.openapi?.path || undefined}
                                    // Clearing the file removes the whole configuration: without a
                                    // specification the mode and the modules stand for nothing.
                                    onChange={path => (path
                                        ? editOpenApi(shown, editDraft, { path })
                                        : editDraft({ openapi: undefined }))}
                                />
                                <Upload
                                    accept=".json,.yaml,.yml"
                                    beforeUpload={file => { void upload(file); return false }}
                                    showUploadList={false}
                                >
                                    <Tooltip title={t('browser.overview.openapi_upload')}>
                                        <Button
                                            aria-label={t('browser.overview.openapi_upload')}
                                            data-testid="edit-openapi-upload"
                                            icon={<UploadOutlined />}
                                            loading={uploading}
                                            size="small"
                                        />
                                    </Tooltip>
                                </Upload>
                            </span>
                        ))}
                        {editRow(t('browser.overview.openapi_mode'), (
                            <Segmented
                                data-testid="edit-openapi-mode"
                                onChange={value => editOpenApi(shown, editDraft, { mode: value as OpenApiMode })}
                                size="small"
                                value={mode}
                                options={[
                                    { label: modeLabel('browser.overview.openapi_reconciliation', 'browser.overview.openapi_reconciliation_hint'), value: 'RECONCILIATION' },
                                    { label: modeLabel('browser.overview.openapi_generation', 'browser.overview.openapi_generation_hint'), value: 'GENERATION' },
                                ]}
                            />
                        ))}
                        {mode === 'GENERATION' && (
                            <>
                                {moduleRow('edit-openapi-algorithm', t('browser.overview.openapi_algorithm'), shown.openapi?.algorithmModuleName ?? '', algorithmModuleName => editOpenApi(shown, editDraft, { algorithmModuleName }))}
                                {moduleRow('edit-openapi-model', t('browser.overview.openapi_model'), shown.openapi?.modelModuleName ?? '', modelModuleName => editOpenApi(shown, editDraft, { modelModuleName }))}
                            </>
                        )}
                    </dl>
                )
                : shown.openapi && (
                    <dl className={styles.openapi}>
                        {shown.openapi.path && row(t('browser.overview.openapi_path'), shown.openapi.path)}
                        {shown.openapi.mode && row(t('browser.overview.openapi_mode'),
                            t(shown.openapi.mode === 'GENERATION' ? 'browser.overview.openapi_generation' : 'browser.overview.openapi_reconciliation'))}
                        {shown.openapi.algorithmModuleName && row(t('browser.overview.openapi_algorithm'), shown.openapi.algorithmModuleName)}
                        {shown.openapi.modelModuleName && row(t('browser.overview.openapi_model'), shown.openapi.modelModuleName)}
                    </dl>
                )}
        </Section>
    )
}

/** The identity of the project on the right: status, where it lives, its last change and its tags. */
const MetaColumn = ({ project, repoLabel, repoType, supportsBranches, canManageBranches, canEditTags, tagsContent, tagsAction, onChanged, onManageBranches }: {
    project: Project
    repoLabel: string
    repoType?: string | undefined
    supportsBranches: boolean
    canManageBranches: boolean
    canEditTags: boolean
    tagsContent: ReactNode
    tagsAction: ReactNode
    onChanged: () => void
    onManageBranches: () => void
}) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles } = useStyles()
    const tags = Object.entries(project.tags ?? {})
    const date = formatDateTime(project.modifiedAt)

    const metaRow = (label: string, value: ReactNode, action?: ReactNode) => (
        <div className={styles.metaRow}>
            <div className={styles.metaLabelRow}>
                <span className={shared.microLabel}>{label}</span>
                {action}
            </div>
            <div className={styles.metaValue}>{value}</div>
        </div>
    )

    return (
        <div className={styles.right} data-testid="overview-right">
            <dl className={styles.meta}>
                {metaRow(t('browser.overview.status'), (
                    <span className={styles.statusValue}>
                        {/* The project's own screen phrases a status its own way — see STATUS_META. */}
                        <StatusPill status={project.status} />
                    </span>
                ))}
                {metaRow(t('browser.overview.repository'), <RepoBadge name={repoLabel} type={repoType} />)}
                {project.path && metaRow(t('browser.overview.path'), <ValueText>{project.path}</ValueText>)}
                {supportsBranches && project.branch && metaRow(t('browser.overview.branch'), (
                    <BranchSwitcher
                        currentBranch={project.branch}
                        currentBranchDefault={project.branchDefault}
                        currentBranchProtected={project.branchProtected}
                        data-testid="overview-branch"
                        onSwitched={onChanged}
                        projectId={project.id}
                    />
                ), canManageBranches && (
                    <Tooltip title={t('browser.branch.manage')}>
                        <Button
                            aria-label={t('browser.branch.manage')}
                            data-testid="manage-branches"
                            icon={<SettingOutlined />}
                            onClick={onManageBranches}
                            size="small"
                            type="text"
                        />
                    </Tooltip>
                ))}
                {project.revision && metaRow(
                    t('browser.overview.revision'),
                    <ValueText>{shortRevision(project.revision)}</ValueText>,
                    // The shown value is shortened, so copying hands over the whole one.
                    <Typography.Text
                        className={styles.copyAction}
                        copyable={{
                            text: project.revision,
                            tooltips: [t('browser.overview.copy_revision'), t('browser.overview.revision_copied')],
                        }}
                    />
                )}
                {(project.modifiedBy || date) && metaRow(t('browser.overview.last_change'), (
                    <>
                        {project.modifiedBy}
                        {date && <div className={styles.metaSub}>{date}</div>}
                    </>
                ))}
                {project.comment && metaRow(t('browser.overview.comment'), (
                    <GitCommitMessage className={styles.metaSub} message={project.comment} />
                ))}
                {(tags.length > 0 || canEditTags) && metaRow(
                    t('browser.overview.tags'),
                    tagsContent,
                    tagsAction
                )}
            </dl>
        </div>
    )
}

interface OverviewPanelProps {
    project: Project
    repoLabel: string
    repoType?: string | undefined
    supportsBranches?: boolean
    onUnlock: () => void
    /** Called after the project switched to another branch, so the workspace reloads it. */
    onChanged?: () => void
    /** Bumped when the project reloads, so the descriptor text is read from rules.xml again. */
    reloadToken?: number
}

/**
 * The project Overview tab: the lock state, and identity metadata on the right, with
 * descriptive sections on the left. Every section and metadata field is driven by the project model and
 * omitted entirely when it has no value — no placeholders or dashes. Depends-on and used-by entries link
 * to the referenced projects.
 */
export const OverviewPanel = ({
    project,
    repoLabel,
    repoType,
    supportsBranches = true,
    onUnlock,
    onChanged,
    reloadToken,
}: OverviewPanelProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [managingBranches, setManagingBranches] = useState(false)
    const [patternHelpOpen, setPatternHelpOpen] = useState(false)
    const descriptor = useRulesDescriptor(project, reloadToken, () => onChanged?.())
    const migration = useProjectMigration(project, reloadToken, () => onChanged?.())

    const canWrite = project.capabilities?.canWrite ?? false
    // Tags live in a project file, so the right to edit them is the right to write the project.
    const canEditTags = canWrite
    const { action: tagsAction, content: tagsContent } = useProjectTags({
        canEdit: canEditTags,
        onSaved: () => onChanged?.(),
        projectId: project.id,
        tags: project.tags ?? {},
    })
    // Picking the branches the project takes part in changes the project, so it follows write access.
    const canManageBranches = project.capabilities?.canManageBranches ?? false

    return (
        <div className={styles.panel} data-testid="overview-panel">
            <div className={styles.left} data-testid="overview-left">
                {canWrite && descriptor.state === 'ready' && (
                    <div className={styles.editBar}>
                        {!migration.mustMigrateBeforeEditing && (
                            <EditToolbar
                                disabled={migration.migrating}
                                editing={descriptor.editing}
                                labels={{ edit: t('browser.overview.edit'), save: t('browser.overview.save'), cancel: t('browser.overview.cancel') }}
                                onCancel={descriptor.cancelEditing}
                                onEdit={descriptor.startEditing}
                                onSave={() => void descriptor.saveEditing()}
                                saving={descriptor.saving}
                                testId="overview"
                            />
                        )}
                        {migration.canMigrate && !descriptor.editing && (
                            <MigrateButton
                                label={t('browser.overview.migrate')}
                                loading={migration.migrating}
                                onClick={migration.migrate}
                                testId="overview-migrate"
                                tooltip={t(migration.mustMigrateBeforeEditing
                                    ? 'browser.overview.migrate_before_edit'
                                    : 'browser.overview.migrate_rewrite_hint')}
                            />
                        )}
                    </div>
                )}
                {descriptor.state === 'error' && (
                    <Alert
                        showIcon
                        data-testid="overview-descriptor-error"
                        style={{ marginBottom: 12 }}
                        title={t('browser.overview.descriptor_read_failed')}
                        type="warning"
                    />
                )}
                <LockBanner onUnlock={onUnlock} project={project} />
                <DescriptionSection editor={descriptor.editor} />
                <ModulesSection
                    editor={descriptor.editor}
                    hasRulesXml={descriptor.fileExists}
                    moduleFilters={descriptor.moduleFilters}
                    modules={project.descriptor?.modules ?? []}
                    modulesDefault={project.descriptor?.modulesDefault ?? false}
                />
                <VersionPatternsSection editor={descriptor.editor} onHelp={() => setPatternHelpOpen(true)} />
                <ProcessorSection editor={descriptor.editor} />
                <ExposedMethodsSection editor={descriptor.editor} />
                <DependenciesSection
                    dependsOn={project.dependencies ?? []}
                    editor={descriptor.editor}
                    projectNames={descriptor.projectNames}
                    usedBy={project.usedBy ?? []}
                />
                {/* Sources and OpenAPI sit at the very bottom, below what the project is made of. */}
                <SourcesSection
                    editor={descriptor.editor}
                    hasRulesXml={descriptor.fileExists}
                    sources={project.descriptor?.sources ?? []}
                    sourcesDefault={project.descriptor?.sourcesDefault ?? false}
                />
                <OpenApiSection editor={descriptor.editor} projectId={project.id} />
            </div>
            <MetaColumn
                canEditTags={canEditTags}
                canManageBranches={canManageBranches}
                onChanged={() => onChanged?.()}
                onManageBranches={() => setManagingBranches(true)}
                project={project}
                repoLabel={repoLabel}
                repoType={repoType}
                supportsBranches={supportsBranches}
                tagsAction={tagsAction}
                tagsContent={tagsContent}
            />
            {supportsBranches && project.branch && (
                <ManageBranchesModal
                    onClose={() => setManagingBranches(false)}
                    open={managingBranches}
                    projectId={project.id}
                />
            )}
            <PropertiesPatternHelpModal onClose={() => setPatternHelpOpen(false)} open={patternHelpOpen} />
        </div>
    )
}
