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
    defaultValue?: string
};

const Select: FC<SelectProps> = ({
    name,
    label,
    options,
    disabled = false,
    style,
    formItemStyle,
}) => (
    <Field name={name} >
        {({ input, meta }) => (
            <Form.Item
                help={meta.error && meta.touched && meta.error}
                label={label}
                style={formItemStyle}
                validateStatus={meta.error && meta.touched && 'error'}
            >
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
