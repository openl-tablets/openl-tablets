import React, { FC, useEffect, useState } from 'react'
import { Checkbox as AntdCheckbox, Form } from 'antd'
import type { FormItemProps, FormRule } from 'antd'
import { CheckboxChangeEvent } from 'antd/es/checkbox'
import { useRules } from './hooks'

type InputProps = {
    name: string | string[]
    checked?: boolean
    label?: string
    tooltip?: FormItemProps['tooltip']
    disabled?: boolean,
    valuePropName?: string
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    onChange?: (e: CheckboxChangeEvent) => void
    required?: boolean
    rules?: FormRule[]
}

const Checkbox: FC<InputProps> = ({
    name,
    label,
    tooltip,
    disabled,
    style,
    valuePropName = 'checked',
    formItemStyle,
    required,
    rules = [],
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
            label={label}
            name={name}
            rules={allRules}
            tooltip={tooltip}
            valuePropName={valuePropName}
            {...(formItemStyle !== undefined && { style: formItemStyle })}
        >
            <AntdCheckbox
                {...(isDisabled !== undefined && { disabled: isDisabled })}
                {...(style !== undefined && { style })}
                {...rest}
            />
        </Form.Item>
    )
}

export default Checkbox
