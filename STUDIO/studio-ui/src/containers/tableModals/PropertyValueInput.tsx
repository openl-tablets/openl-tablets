import React from 'react'
import { Checkbox, DatePicker, Input, Select } from 'antd'
import type { ProjectProperty, RawTableCellInput } from 'types/tables'
import { datePickerFormatForLocale, ISO_DATE_FORMAT, parseDateValue } from './dateValue'
import { useSharedStyles } from './sharedStyles'

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
}

const enumCodes = (value: PropertyValue): string[] =>
    String(value ?? '')
        .split(',')
        .map(code => code.trim())
        .filter(Boolean)

/** Initial value for a newly selected property. A boolean is complete when its checkbox is clear. */
export const initialPropertyValue = (definition?: ProjectProperty): EditedPropertyValue =>
    definition?.type === 'boolean' ? false : ''

/** An editor matching the value type declared by the project metadata endpoint. */
export const PropertyValueInput: React.FC<PropertyValueInputProps> = ({
    definition,
    onChange,
    placeholder,
    status = '',
    value,
    'aria-label': ariaLabel = placeholder,
    'data-testid': testId,
}) => {
    const { styles } = useSharedStyles()
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
                    showSearch
                    aria-label={ariaLabel}
                    data-testid={testId}
                    mode="multiple"
                    onChange={selected => onChange(selected.join(', '))}
                    optionFilterProp="label"
                    options={options}
                    placeholder={placeholder}
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
                optionFilterProp="label"
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
