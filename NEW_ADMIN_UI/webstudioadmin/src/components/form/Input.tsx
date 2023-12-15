import React, { FC } from 'react'
import { Field } from 'react-final-form'
import { Input as AntdInput, Form } from 'antd'

type InputProps = {
    name: string
    label?: string
    type?: string
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    placeholder?: string
};

const Input: FC<InputProps> = ({
    name,
    label,
    type = 'text',
    disabled,
    style,
    formItemStyle,
    placeholder,
}) => (
    <Field name={name} type={type}>
        {({ input }) => (
            <Form.Item label={label} style={formItemStyle}>
                <AntdInput {...input} disabled={disabled} placeholder={placeholder} style={style} />
            </Form.Item>
        )}
    </Field>
)

export default Input
