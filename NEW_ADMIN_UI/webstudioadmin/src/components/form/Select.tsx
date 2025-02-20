import React, { FC } from 'react'
import { Select as AntdSelect, Form } from 'antd'
import { Rule } from 'antd/es/form'
import type { DefaultOptionType } from 'rc-select/lib/Select'

export interface SelectOption extends DefaultOptionType {
    // label: string
}

type SelectProps = {
    name: string
    label?: string
    options: SelectOption[]
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties
    defaultValue?: string
    rules?: Rule[]
};

const Select: FC<SelectProps> = ({
    name,
    label,
    options,
    disabled = false,
    style,
    formItemStyle,
    ...rest
}) => (
    <Form.Item
        label={label}
        name={name}
        style={formItemStyle}
        {...rest}
    >
        <AntdSelect
            disabled={disabled}
            options={options}
            style={style}
        />
    </Form.Item>
)

export default Select
