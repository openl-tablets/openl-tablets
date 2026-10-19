import React, { FC, useEffect, useMemo, useState } from 'react'
import { InputNumber as AntdInputNumber, Form } from 'antd'
import type { FormItemProps, FormRule } from 'antd'
import { useRules } from './hooks'
import { getFieldValueProps } from './utils'

type InputNumberProps = {
    name: string | (string | number)[]
    label?: string
    tooltip?: FormItemProps['tooltip']
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    placeholder?: string
    defaultValue?: string
    rules?: FormRule[]
    required?: boolean
}

const InputNumber: FC<InputNumberProps> = ({
    name,
    label,
    disabled,
    style,
    formItemStyle,
    placeholder,
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

    const inputStyle = useMemo(() => ({
        width: '100%',
        ...style,
    }), [style])

    return (
        <Form.Item
            getValueProps={getFieldValueProps}
            label={label}
            name={name}
            rules={allRules}
            {...(formItemStyle !== undefined && { style: formItemStyle })}
            {...rest}
        >
            <AntdInputNumber
                {...(isDisabled !== undefined && { disabled: isDisabled })}
                {...(placeholder !== undefined && { placeholder })}
                style={inputStyle}
            />
        </Form.Item>
    )
}

export default InputNumber
