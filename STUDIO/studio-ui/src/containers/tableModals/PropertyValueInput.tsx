import React from 'react'
import { Checkbox, DatePicker, Input, Select } from 'antd'
import type { ProjectProperty, RawTableCellInput, TableVersions } from 'types/tables'
import { datePickerFormatForLocale, ISO_DATE_FORMAT, parseDateValue } from './dateValue'
import { VERSION_PROPERTY } from './shared'
import { useSharedStyles } from './sharedStyles'
import { VersionInput } from './VersionInput'

type PropertyValue = RawTableCellInput['value']
type EditedPropertyValue = string | boolean

interface PropertyValueInputProps {
    'aria-label'?: string
    'data-testid': string
    definition: ProjectProperty | undefined
    onChange: (value: EditedPropertyValue) => void
    placeholder: string
    /** Marks the editor as holding a value the property does not accept. */
    status?: '' | 'error'
    value: PropertyValue
    /** Versions of the table a copy is made from, shown beside the version editor. */
    versions?: TableVersions | undefined
}

const enumCodes = (value: PropertyValue): string[] =>
    String(value ?? '')
        .split(',')
        .map(code => code.trim())
        .filter(Boolean)

/**
 * Value a newly selected property opens on: the one it stands for while a table declares none.
 *
 * <p>The version opens on the one offered to the copy — the first the table's versions leave free — so an author
 * never has to work out which numbers are taken. A boolean is complete when its checkbox is clear.
 */
export const initialPropertyValue = (
    definition?: ProjectProperty,
    versions?: TableVersions | undefined
): EditedPropertyValue => {
    if (definition?.name === VERSION_PROPERTY) {
        return versions?.next ?? '0.0.1'
    }
    if (definition?.type === 'boolean') {
        return definition.defaultValue === 'true'
    }
    return definition?.defaultValue ?? ''
}

/** An editor matching the value type declared by the project metadata endpoint. */
export const PropertyValueInput: React.FC<PropertyValueInputProps> = ({
    definition,
    onChange,
    placeholder,
    status = '',
    value,
    versions,
    'aria-label': ariaLabel = placeholder,
    'data-testid': testId,
}) => {
    const { styles } = useSharedStyles()
    if (definition?.name === VERSION_PROPERTY) {
        return (
            <VersionInput
                aria-label={ariaLabel}
                current={versions?.current}
                data-testid={testId}
                onChange={onChange}
                status={status}
                value={String(value ?? '')}
            />
        )
    }
    if (definition?.type === 'date') {
        return (
            <DatePicker
                aria-label={ariaLabel}
                data-testid={testId}
                format={datePickerFormatForLocale()}
                onChange={date => onChange(date ? date.format(ISO_DATE_FORMAT) : '')}
                placeholder={placeholder}
                status={status}
                style={{ width: '100%' }}
                value={parseDateValue(value)}
            />
        )
    }
    if (definition?.type === 'boolean') {
        return (
            <div className={styles.checkboxEditor} data-testid={`${testId}-wrapper`}>
                <Checkbox
                    aria-label={ariaLabel}
                    checked={value === true || String(value).toLocaleLowerCase() === 'true'}
                    data-testid={testId}
                    onChange={event => onChange(event.target.checked)}
                />
            </div>
        )
    }
    if (definition?.type === 'enum') {
        const options = definition.values.map(option => ({ label: option.value, value: option.code }))
        if (definition.multiple) {
            return (
                <Select
                    allowClear
                    aria-label={ariaLabel}
                    data-testid={testId}
                    mode="multiple"
                    onChange={selected => onChange(selected.join(', '))}
                    options={options}
                    placeholder={placeholder}
                    showSearch={{ optionFilterProp: 'label' }}
                    status={status}
                    style={{ width: '100%' }}
                    value={enumCodes(value)}
                />
            )
        }
        return (
            <Select
                allowClear
                aria-label={ariaLabel}
                data-testid={testId}
                onChange={selected => onChange(selected ?? '')}
                options={options}
                placeholder={placeholder}
                showSearch={false}
                status={status}
                style={{ width: '100%' }}
                value={String(value ?? '') || undefined}
            />
        )
    }
    return (
        <Input
            aria-label={ariaLabel}
            data-testid={testId}
            onChange={event => onChange(event.target.value)}
            placeholder={placeholder}
            status={status}
            value={String(value ?? '')}
        />
    )
}
