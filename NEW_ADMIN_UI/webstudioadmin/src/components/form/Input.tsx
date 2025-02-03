import React, { FC } from 'react'
import { Input as AntdInput, Form } from 'antd'
import { Rule } from 'antd/es/form'

type InputProps = {
    name: string | (string | number)[]
    label?: string
    type?: string
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    placeholder?: string
    defaultValue?: string
    rules?: Rule[]
};

const Input: FC<InputProps> = ({
    name,
    label,
    type = 'text',
    disabled,
    style,
    formItemStyle,
    placeholder,
    ...rest
}) => (
    <Form.Item label={label} name={name} style={formItemStyle} {...rest}>
        <AntdInput disabled={disabled} placeholder={placeholder} style={style} />
    </Form.Item>
)

export default Input
