import React, { useCallback, useMemo, useState } from 'react'
import { CopyOutlined, DeleteRowOutlined } from '@ant-design/icons'
import { Input, Modal, notification, Select, Space, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { FieldRow } from 'components/FieldRow'
import { IconAction } from 'components/IconAction'
import { SuggestInput } from 'components/SuggestInput'
import { useGlobalEvents } from 'hooks'
import { getModuleSheets, getProjectModules, getProjectProperties } from 'services/projects'
import { copyTable, getTableCopyInfo } from 'services/tables'
import type { ProjectProperty, SummaryTable, TableCopyInfo } from 'types/tables'
import { errorMessage } from 'utils/errorMessage'
import {
    asOptions,
    defaultModulePath,
    deleteAt,
    IDENTIFIER,
    isValidPropertyValue,
    isValidSheetName,
    type ModuleOption,
    sheetNameFrom,
    toModuleOptions,
    toPropertyGroups,
    toSortedOptions,
    VERSION_PROPERTY,
    withTrailingBlank,
} from '../tableModals/shared'
import { useSharedStyles } from '../tableModals/sharedStyles'
import { useSheetLoader } from '../tableModals/useSheetLoader'
import { initialPropertyValue, PropertyValueInput } from '../tableModals/PropertyValueInput'
import { useStyles } from './CopyTableModal.styles'

/** A property row being edited: its value is whatever {@link PropertyValueInput} produces for the property's type. */
interface TablePropertyInput {
    name: string
    value: string | number | boolean | null
}

const blankProperty = (): TablePropertyInput => ({ name: '', value: '' })

export interface CopyTableModalDetail {
    projectId: string
    currentModuleName?: string
    sourceTableId: string
    onSuccess?: (table: SummaryTable, moduleName: string) => void
}

const propertyHasValue = (property: TablePropertyInput): boolean =>
    property.value !== null && String(property.value).trim().length > 0

const isEmptyProperty = (property: TablePropertyInput): boolean =>
    !property.name.trim() && !propertyHasValue(property)

const isCompleteProperty = (property: TablePropertyInput): boolean =>
    Boolean(property.name.trim()) && propertyHasValue(property)

const normalizeProperties = (properties: TablePropertyInput[]): TablePropertyInput[] =>
    withTrailingBlank(properties, isCompleteProperty, blankProperty)

const CopyTableForm: React.FC<{ detail: CopyTableModalDetail }> = ({ detail }) => {
    const { t } = useTranslation()
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [sourceInfo, setSourceInfo] = useState<TableCopyInfo | null>(null)
    const [modules, setModules] = useState<ModuleOption[]>([])
    const [projectProperties, setProjectProperties] = useState<ProjectProperty[]>([])
    const [tableName, setTableName] = useState('')
    const [selectedModule, setSelectedModule] = useState('')
    const [properties, setProperties] = useState<TablePropertyInput[]>([blankProperty()])
    const [loading, setLoading] = useState(true)
    const [copying, setCopying] = useState(false)
    const sheetLoader = useSheetLoader(t('project:copy_table_modal.options_load_failed'))
    const { sheets, sheetName, setSheetName } = sheetLoader

    const close = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openCopyTableModal', { detail: null }))
    }, [])

    // The form is one session of one global event, and the dialog is unmounted when it closes, so everything it
    // holds is discarded with it. Depending on translated callbacks would restart that session after any render in
    // test adapters and would also discard input when translations are reloaded.
    React.useEffect(() => {
        let active = true
        const infoRequest = getTableCopyInfo(detail.projectId, detail.sourceTableId)
        Promise.all([
            infoRequest,
            getProjectModules(detail.projectId),
            infoRequest.then(info => getProjectProperties(detail.projectId, info.kind)),
            detail.currentModuleName
                ? getModuleSheets(detail.projectId, detail.currentModuleName).catch(() => [])
                : Promise.resolve<string[]>([]),
        ])
            .then(([info, loadedModules, loadedProperties, currentSheets]) => {
                if (!active) {
                    return
                }
                const available = toModuleOptions(loadedModules)
                const current = available.find(module => module.name === detail.currentModuleName)
                const destination = current?.name ?? available[0]?.name ?? ''
                setSourceInfo(info)
                setModules(available)
                setProjectProperties(loadedProperties)
                setTableName(info.name)
                setSheetName(sheetNameFrom(info.name))
                const applicableNames = new Set(loadedProperties.map(property => property.name))
                setProperties(normalizeProperties(
                    (info.properties ?? [])
                        .filter(property => applicableNames.has(property.name))
                        // The copy is offered the first version the table's versions leave free: the one the source
                        // stands for is by definition taken, and a copy under that name could not be written with it.
                        .map(property => property.name === VERSION_PROPERTY && info.versions
                            ? { ...property, value: info.versions.next }
                            : property)
                ))
                setSelectedModule(destination)
                if (currentSheets.length && destination === detail.currentModuleName) {
                    sheetLoader.prime(destination, currentSheets)
                } else {
                    void sheetLoader.load(detail.projectId, destination, available)
                }
            })
            .catch(error => {
                if (active) {
                    notification.error({
                        title: t('project:copy_table_modal.options_load_failed'),
                        description: errorMessage(error),
                    })
                    close()
                }
            })
            .finally(() => {
                if (active) {
                    setLoading(false)
                }
            })
        return () => {
            active = false
        }
    }, [detail])

    const moduleName = selectedModule.trim()
    const isNewModule = Boolean(moduleName) && !modules.some(module => module.name === moduleName)
    const moduleOptions = useMemo(() => toSortedOptions(modules), [modules])
    const sheetOptions = useMemo(() => asOptions(sheets), [sheets])
    const propertyOptions = useMemo(() => toPropertyGroups(projectProperties), [projectProperties])
    /** What the project says about a property, by the name a row holds. */
    const definitionOf = (name: string) => projectProperties.find(definition => definition.name === name.trim())
    // The value the source already carries is let through as it stands: it was written when a shorter version was
    // documented as valid, and refusing it would leave such a table impossible to copy at all.
    const carriedOver = (property: TablePropertyInput) =>
        (sourceInfo?.properties ?? []).some(source =>
            source.name === property.name.trim() && source.value === String(property.value ?? ''))
    /** Whether a dimension property of the copy differs from the source's, which makes it another table. */
    const dimensionsChanged = () => {
        const isDimensional = (name: string) => Boolean(definitionOf(name)?.dimensional)
        const source = new Map((sourceInfo?.properties ?? [])
            .filter(property => isDimensional(property.name))
            .map(property => [property.name, String(property.value ?? '')]))
        const declared = new Map(properties
            .filter(property => isCompleteProperty(property) && isDimensional(property.name.trim()))
            .map(property => [property.name.trim(), String(property.value ?? '')]))
        return source.size !== declared.size
            || [...source].some(([name, value]) => declared.get(name) !== value)
    }

    /** Whether the copy would be a new version of the source: same name, same requests answered. */
    const versionsTheSource = tableName.trim() === sourceInfo?.name && !dimensionsChanged()
    /**
     * A row the copy cannot be written with: the property is named, but the value is not one it accepts.
     *
     * <p>A version another version of the table already carries is refused — the two would be tables the engine
     * cannot order — unless the copy answers other requests, where it is a table of its own. The version the source
     * itself stands for is one of those, so a value carried over from the source is no excuse here.
     */
    const rejectedProperty = (property: TablePropertyInput) => {
        if (!isCompleteProperty(property)) {
            return false
        }
        if (property.name.trim() === VERSION_PROPERTY && versionsTheSource) {
            return (sourceInfo?.versions?.taken ?? []).includes(String(property.value ?? '').trim())
        }
        return !carriedOver(property) && !isValidPropertyValue(definitionOf(property.name), property.value)
    }
    const partialProperty = properties.some(property =>
        !isEmptyProperty(property) && !isCompleteProperty(property))
    const submittedProperties = properties.filter(isCompleteProperty)
    const propertyNames = submittedProperties.map(property => property.name.trim().toLocaleLowerCase())
    const propertyNamesUnique = new Set(propertyNames).size === propertyNames.length
    const valid = Boolean(
        sourceInfo
        && IDENTIFIER.test(tableName.trim())
        && moduleName
        && isValidSheetName(sheetName)
        && !partialProperty
        && !properties.some(rejectedProperty)
        && propertyNamesUnique
        && !loading
        && !sheetLoader.loading
    )

    const handleTableNameChange = (value: string) => {
        setTableName(value)
        // The sheet mirrors the name until the author points it elsewhere, following the name clipped to Excel's
        // limit: a longer name would otherwise leave a sheet Excel rejects and disable Copy.
        setSheetName(current => current === sheetNameFrom(tableName) ? sheetNameFrom(value) : current)
    }

    const handleModuleChange = (value: string) => {
        setSelectedModule(value)
        void sheetLoader.load(detail.projectId, value, modules)
    }

    const changeProperties = (transform: (current: TablePropertyInput[]) => TablePropertyInput[]) =>
        setProperties(current => normalizeProperties(transform(current)))

    const updateProperty = (
        index: number,
        field: keyof TablePropertyInput,
        value: TablePropertyInput[typeof field]
    ) =>
        changeProperties(current => current.map((property, propertyIndex) =>
            propertyIndex === index ? { ...property, [field]: value } : property))

    const updatePropertyName = (index: number, name: string) => {
        const definition = definitionOf(name)
        changeProperties(current => current.map((property, propertyIndex) =>
            propertyIndex === index
                ? { name, value: initialPropertyValue(definition, sourceInfo?.versions) }
                : property))
    }

    const removeProperty = (index: number) =>
        changeProperties(current => deleteAt(current, index))

    const handleCopy = async () => {
        if (!sourceInfo || !valid || copying) {
            return
        }
        setCopying(true)
        try {
            const copyName = tableName.trim()
            const table = await copyTable(detail.projectId, detail.sourceTableId, {
                moduleName,
                sheetName: sheetName.trim(),
                ...(isNewModule ? { modulePath: defaultModulePath(moduleName, sourceInfo.kind) } : {}),
                name: copyName,
                properties: submittedProperties.map(property => ({
                    name: property.name.trim(),
                    value: property.value == null ? null : String(property.value),
                })),
            })
            if (table) {
                close()
                detail.onSuccess?.(table, moduleName)
            }
        } catch (error) {
            notification.error({
                title: t('project:copy_table_modal.copy_failed'),
                description: errorMessage(error),
            })
        } finally {
            setCopying(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            open
            cancelButtonProps={{ disabled: copying }}
            cancelText={t('common:btn.cancel')}
            closable={!copying}
            keyboard={!copying}
            mask={{ closable: false }}
            okButtonProps={{ disabled: !valid, loading: copying }}
            okText={t('project:copy_table_modal.copy')}
            onCancel={close}
            onOk={handleCopy}
            width={760}
            title={(
                <Space>
                    <CopyOutlined />
                    {t('project:copy_table_modal.title', { table: sourceInfo?.name ?? '' })}
                </Space>
            )}
        >
            <Spin spinning={loading || sheetLoader.loading}>
                <div className={shared.form}>
                    <div className={shared.settings}>
                        <div className={shared.fields}>
                            <div className={styles.tableName}>
                                <FieldRow
                                    required
                                    htmlFor="copy-table-name"
                                    label={t('project:copy_table_modal.table_name')}
                                >
                                    <Input
                                        data-testid="copy-table-name"
                                        id="copy-table-name"
                                        onChange={event => handleTableNameChange(event.target.value)}
                                        value={tableName}
                                    />
                                </FieldRow>
                            </div>
                        </div>
                        <div className={shared.fields}>
                            <FieldRow
                                required
                                htmlFor="copy-table-module"
                                label={t('project:copy_table_modal.module')}
                            >
                                <SuggestInput
                                    className={shared.fullWidth}
                                    data-testid="copy-table-module"
                                    id="copy-table-module"
                                    onChange={handleModuleChange}
                                    options={moduleOptions}
                                    value={selectedModule}
                                />
                            </FieldRow>
                            <FieldRow
                                required
                                htmlFor="copy-table-sheet"
                                label={t('project:copy_table_modal.sheet')}
                            >
                                <SuggestInput
                                    className={shared.fullWidth}
                                    data-testid="copy-table-sheet"
                                    id="copy-table-sheet"
                                    onChange={setSheetName}
                                    options={sheetOptions}
                                    value={sheetName}
                                />
                            </FieldRow>
                        </div>
                        <div className={shared.section}>
                            <FieldRow alignTop label={t('project:copy_table_modal.properties')}>
                                <div className={shared.rowList}>
                                    {properties.map((property, index) => (
                                        <div
                                            key={`property-${index}`}
                                            className={cx(shared.rowColumns, shared.editableRow)}
                                            data-testid={`copy-table-property-row-${index}`}
                                        >
                                            <Select
                                                allowClear
                                                data-testid={`copy-table-property-name-${index}`}
                                                onChange={value => updatePropertyName(index, value ?? '')}
                                                options={propertyOptions}
                                                placeholder={t('project:copy_table_modal.property_name')}
                                                showSearch={{ optionFilterProp: 'label' }}
                                                value={property.name || undefined}
                                            />
                                            <PropertyValueInput
                                                data-testid={`copy-table-property-value-${index}`}
                                                definition={definitionOf(property.name)}
                                                onChange={value => updateProperty(index, 'value', value)}
                                                placeholder={t('project:copy_table_modal.property_value')}
                                                status={rejectedProperty(property) ? 'error' : ''}
                                                value={property.value}
                                                versions={sourceInfo?.versions}
                                            />
                                            <Space.Compact>
                                                <IconAction
                                                    icon={<DeleteRowOutlined />}
                                                    onClick={() => removeProperty(index)}
                                                    title={t('project:copy_table_modal.delete_property')}
                                                />
                                            </Space.Compact>
                                        </div>
                                    ))}
                                </div>
                            </FieldRow>
                        </div>
                    </div>
                </div>
            </Spin>
        </Modal>
    )
}

/**
 * Copies a table into a module of the same project, with the name and properties the author chooses.
 *
 * <p>The form is mounted only while the dialog is open, so closing it discards every field, every cached module and
 * the copied table's cells rather than holding them until the page is reloaded.
 */
export const CopyTableModal: React.FC = () => {
    const { detail } = useGlobalEvents<CopyTableModalDetail>('openCopyTableModal')
    return detail
        ? <CopyTableForm key={`${detail.projectId} ${detail.sourceTableId}`} detail={detail} />
        : null
}
