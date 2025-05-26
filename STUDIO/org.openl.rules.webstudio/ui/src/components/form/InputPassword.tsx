import React, { FC, ReactNode, useEffect, useRef, useState } from 'react'
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
                form.setFieldValue(name, value.value)
            }
            if (value.secret) {
                form.setFieldValue(name, '')
            }
        }
    }, [value])

    return (
        <Form.Item label={label} name={name} style={formItemStyle} {...rest}>
            <AntdInput.Password disabled={isDisabled} placeholder={placeholder} style={style} />
        </Form.Item>
    )
}

export default InputPassword
