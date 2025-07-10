import React, { FC, ReactNode, CSSProperties, useEffect, useState } from 'react'
import { Select as AntdSelect, Form, SelectProps as AntdSelectProps } from 'antd'
import type { DefaultOptionType } from 'rc-select/lib/Select'
import { useRules } from './hooks'
import { RuleObject } from 'rc-field-form/lib/interface'

// @ts-ignore
export interface SelectOption extends DefaultOptionType {
    value: string | number | boolean | null;
}

// @ts-ignore
interface SelectProps extends AntdSelectProps {
    name: string | string[]
    label?: string
    options: SelectOption[]
    disabled?: boolean
    style?: CSSProperties
    formItemStyle?: CSSProperties
    defaultValue?: string
    rules?: RuleObject[]
    mode?: 'multiple' | 'tags'
    showSearch?: boolean
    defaultActiveFirstOption?: boolean
    suffixIcon?: ReactNode
    required?: boolean
}

const Select: FC<SelectProps> = ({
    name,
    label,
    options,
    disabled = false,
    style,
    formItemStyle,
    mode,
    onChange,
    onSearch,
    onBlur,
    showSearch = false,
    notFoundContent,
    defaultActiveFirstOption = true,
    suffixIcon,
    filterOption = true,
    open,
    tokenSeparators,
    required,
    rules = [],
    ...rest
}) => {
    const form = Form.useFormInstance()
    const value = Form.useWatch(name, form)
    const [isDisabled, setIsDisabled] = useState(disabled)
    const [predefinedOptions, setPredefinedOptions] = useState<SelectOption[] | null>(null)
    const { allRules } = useRules({ required, rules })

    useEffect(() => {
        if (disabled !== undefined) {
            setIsDisabled(disabled)
        }
    }, [disabled])

    useEffect(() => {
        if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
            if (value.readOnly) {
                setIsDisabled(true)
            }
            form.setFieldValue(name, value.value)
        }
    }, [value])

    useEffect(() => {
        if (Array.isArray(value) && !open && mode === 'tags') {
            setPredefinedOptions(value.map(option => ({
                value: option,
                label: option,
            })))
        }
    }, [value, open, mode])

    return (
        <Form.Item
            label={label}
            name={name}
            rules={allRules}
            style={formItemStyle}
            {...rest}
        >
            <AntdSelect
                defaultActiveFirstOption={defaultActiveFirstOption}
                disabled={isDisabled}
                filterOption={filterOption}
                mode={mode}
                notFoundContent={notFoundContent}
                onBlur={onBlur}
                onChange={onChange}
                onSearch={onSearch}
                open={open}
                // @ts-ignore
                options={predefinedOptions || options}
                showSearch={showSearch}
                style={style}
                suffixIcon={suffixIcon}
                tokenSeparators={tokenSeparators}
            />
        </Form.Item>
    )
}

export default Select
