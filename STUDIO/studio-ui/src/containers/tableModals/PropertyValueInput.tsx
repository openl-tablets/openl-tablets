import React from 'react'
import { Checkbox, DatePicker, Input, Select } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import customParseFormat from 'dayjs/plugin/customParseFormat'
import type { ProjectProperty, RawTableCellInput } from 'types/tables'

dayjs.extend(customParseFormat)

type PropertyValue = RawTableCellInput['value']
type EditedPropertyValue = string | boolean

interface PropertyValueInputProps {
    'data-testid': string
    definition: ProjectProperty | undefined
    onChange: (value: EditedPropertyValue) => void
    placeholder: string
    value: PropertyValue
}

const ISO_DATE_FORMAT = 'YYYY-MM-DD'
const DATE_PART_TOKENS = {
    day: 'DD',
    month: 'MM',
    year: 'YYYY',
} as const

/** Date picker pattern derived from the user's locale, independent of the value stored in the workbook. */
export const datePickerFormatForLocale = (locale?: string): string =>
    new Intl.DateTimeFormat(locale, { day: '2-digit', month: '2-digit', year: 'numeric' })
        .formatToParts(new Date(2006, 10, 22))
        .map(part => part.type in DATE_PART_TOKENS
            ? DATE_PART_TOKENS[part.type as keyof typeof DATE_PART_TOKENS]
            : `[${part.value}]`)
        .join('')

const parseDate = (value: PropertyValue): Dayjs | null => {
    if (!value) {
        return null
    }
    const parsed = dayjs(String(value), ISO_DATE_FORMAT, true)
    return parsed.isValid() ? parsed : null
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
    value,
    'data-testid': testId,
}) => {
    if (definition?.type === 'date') {
        return (
            <DatePicker
                data-testid={testId}
                format={datePickerFormatForLocale()}
                onChange={date => onChange(date ? date.format(ISO_DATE_FORMAT) : '')}
                placeholder={placeholder}
                style={{ width: '100%' }}
                value={parseDate(value)}
            />
        )
    }
    if (definition?.type === 'boolean') {
        return (
            <Checkbox
                checked={value === true || String(value).toLocaleLowerCase() === 'true'}
                data-testid={testId}
                onChange={event => onChange(event.target.checked)}
            />
        )
    }
    if (definition?.type === 'enum') {
        const options = definition.values.map(option => ({ label: option.value, value: option.code }))
        if (definition.multiple) {
            return (
                <Select
                    allowClear
                    showSearch
                    data-testid={testId}
                    mode="multiple"
                    onChange={selected => onChange(selected.join(', '))}
                    optionFilterProp="label"
                    options={options}
                    placeholder={placeholder}
                    style={{ width: '100%' }}
                    value={enumCodes(value)}
                />
            )
        }
        return (
            <Select
                allowClear
                showSearch
                data-testid={testId}
                onChange={selected => onChange(selected ?? '')}
                optionFilterProp="label"
                options={options}
                placeholder={placeholder}
                style={{ width: '100%' }}
                value={String(value ?? '') || undefined}
            />
        )
    }
    return (
        <Input
            data-testid={testId}
            onChange={event => onChange(event.target.value)}
            placeholder={placeholder}
            value={String(value ?? '')}
        />
    )
}
