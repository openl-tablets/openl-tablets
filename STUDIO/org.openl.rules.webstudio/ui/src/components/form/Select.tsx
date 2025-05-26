import React, { FC, ReactNode, CSSProperties, useRef, useEffect, useState } from 'react'
import { Select as AntdSelect, Form, SelectProps as AntdSelectProps } from 'antd'
import { Rule } from 'antd/es/form'
import type { DefaultOptionType } from 'rc-select/lib/Select'

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
    rules?: Rule[]
    mode?: 'multiple' | 'tags'
    showSearch?: boolean
    defaultActiveFirstOption?: boolean
    suffixIcon?: ReactNode
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
    ...rest
}) => {
    const form = Form.useFormInstance()
    const value = Form.useWatch(name, form)
    const [isDisabled, setIsDisabled] = useState(disabled)

    useEffect(() => {
        if (disabled !== undefined) {
            setIsDisabled(disabled)
        }
    }, [disabled])

    useEffect(() => {
        if (value !== null && typeof value === 'object') {
            if (value.readOnly) {
                setIsDisabled(true)
            }
            form.setFieldValue(name, value.value)
        }
    }, [])

    return (
        <Form.Item
            label={label}
            name={name}
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
                // @ts-ignore
                options={options}
                showSearch={showSearch}
                style={style}
                suffixIcon={suffixIcon}
            />
        </Form.Item>
    )
}

export default Select
