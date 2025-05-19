import React, { FC, ReactNode } from 'react'
import { Input as AntdInput, Form, TooltipProps } from 'antd'
import { Rule } from 'antd/es/form'

type InputProps = {
    name: string | (string | number)[]
    label?: string
    disabled?: boolean,
    tooltip?: ReactNode | TooltipProps & { icon: any }
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    placeholder?: string
    defaultValue?: string
    hidden?: boolean
    rules?: Rule[]
};

const Input: FC<InputProps> = ({
    name,
    label,
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
