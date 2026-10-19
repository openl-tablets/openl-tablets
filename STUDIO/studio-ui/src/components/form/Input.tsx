import React, { FC, useEffect, useState } from 'react'
import { Input as AntdInput, Form } from 'antd'
import type { FormItemProps, FormRule } from 'antd'
import { useRules } from './hooks'
import { getFieldValueProps } from './utils'

type InputProps = {
    name: string | (string | number)[]
    label?: string
    disabled?: boolean,
    tooltip?: FormItemProps['tooltip']
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    placeholder?: string
    defaultValue?: string
    hidden?: boolean
    rules?: FormRule[]
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
        <Form.Item
            getValueProps={getFieldValueProps}
            label={label}
            name={name}
            rules={allRules}
            {...(formItemStyle !== undefined && { style: formItemStyle })}
            {...rest}
        >
            <AntdInput
                {...(autoComplete !== undefined && { autoComplete })}
                {...(isDisabled !== undefined && { disabled: isDisabled })}
                {...(placeholder !== undefined && { placeholder })}
                {...(style !== undefined && { style })}
                {...(type !== undefined && { type })}
            />
        </Form.Item>
    )
}

export default Input
