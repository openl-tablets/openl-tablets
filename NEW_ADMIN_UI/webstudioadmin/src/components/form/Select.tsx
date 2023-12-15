import React, { FC } from 'react'
import { Field } from 'react-final-form'
import { Select as AntdSelect, Form } from 'antd'

type SelectProps = {
    name: string
    label?: string
    options: {
        value: string | number
        label: string
    }[]
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties
};

const Select: FC<SelectProps> = ({
    name,
    label,
    options,
    disabled = false,
    style,
    formItemStyle,
}) => (
    <Field name={name}>
        {({ input }) => (
            <Form.Item label={label} style={formItemStyle}>
                <AntdSelect
                    disabled={disabled}
                    options={options}
                    style={style}
                    {...input}
                />
            </Form.Item>
        )}
    </Field>
)

export default Select
