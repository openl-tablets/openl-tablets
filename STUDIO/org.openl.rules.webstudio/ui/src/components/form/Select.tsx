import React, { FC, ReactNode, CSSProperties } from 'react'
import { Select as AntdSelect, Form, SelectProps as AntdSelectProps } from 'antd'
import { Rule } from 'antd/es/form'
import type { DefaultOptionType } from 'rc-select/lib/Select'

export interface SelectOption extends DefaultOptionType {}

interface SelectProps extends AntdSelectProps {
    name: string
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
}) => (
    <Form.Item
        label={label}
        name={name}
        style={formItemStyle}
        {...rest}
    >
        <AntdSelect
            defaultActiveFirstOption={defaultActiveFirstOption}
            disabled={disabled}
            filterOption={filterOption}
            mode={mode}
            notFoundContent={notFoundContent}
            onBlur={onBlur}
            onChange={onChange}
            onSearch={onSearch}
            options={options}
            showSearch={showSearch}
            style={style}
            suffixIcon={suffixIcon}
        />
    </Form.Item>
)

export default Select
