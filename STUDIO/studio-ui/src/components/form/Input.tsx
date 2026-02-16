import React, { FC, ReactNode, useEffect, useState } from 'react'
import { Input as AntdInput, Form, TooltipProps } from 'antd'
import { useRules } from './hooks'
import { RuleObject } from 'rc-field-form/lib/interface'

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
    rules?: RuleObject[]
    required?: boolean
    type?: string
    autoComplete?: string
}

const Input: FC<InputProps> = ({
    name,
    label,
    disabled,
    style,
    formItemStyle,
    placeholder,
    required,
    rules = [],
    type,
    autoComplete,
    ...rest
}) => {
    const form = Form.useFormInstance()
    const value = Form.useWatch(name, form)
    const [isDisabled, setIsDisabled] = useState(disabled)
    const { allRules } = useRules({ required, rules })

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
    }, [value])

    return (
        <Form.Item label={label} name={name} rules={allRules} style={formItemStyle} {...rest}>
            <AntdInput autoComplete={autoComplete} disabled={isDisabled} placeholder={placeholder} style={style} type={type} />
        </Form.Item>
    )
}

export default Input
