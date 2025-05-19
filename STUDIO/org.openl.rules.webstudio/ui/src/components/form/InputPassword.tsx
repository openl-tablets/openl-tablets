import React, { FC, ReactNode, useEffect } from 'react'
import { Input as AntdInput, Form, TooltipProps } from 'antd'
import { Rule } from 'antd/es/form'

type InputProps = {
    name: string | (string | number)[]
    label?: string
    tooltip?: ReactNode | TooltipProps & { icon: any }
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    placeholder?: string
    defaultValue?: string
    rules?: Rule[]
};

const InputPassword: FC<InputProps> = ({
    name,
    label,
    disabled,
    style,
    formItemStyle,
    placeholder,
    ...rest
}) => {
    const form = Form.useFormInstance()
    const value = Form.useWatch(name, form)

    useEffect(() => {
        if (typeof value === 'object') {
            form.setFieldValue(name, '')
        }
    }, [value])

    return (
        <Form.Item label={label} name={name} style={formItemStyle} {...rest}>
            <AntdInput.Password disabled={disabled} placeholder={placeholder} style={style}/>
        </Form.Item>
    )
}

export default InputPassword
