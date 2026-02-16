import React, { FC, ReactNode, useEffect, useMemo, useState } from 'react'
import { InputNumber as AntdInputNumber, Form, TooltipProps } from 'antd'
import { useRules } from './hooks'
import { getFieldValueProps } from './utils'
import { RuleObject } from 'rc-field-form/lib/interface'

type InputNumberProps = {
    name: string | (string | number)[]
    label?: string
    tooltip?: ReactNode | TooltipProps & { icon: any }
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    placeholder?: string
    defaultValue?: string
    rules?: RuleObject[]
    required?: boolean
};

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
            label={label}
            name={name}
            rules={allRules}
            style={formItemStyle}
            getValueProps={getFieldValueProps}
            {...rest}
        >
            <AntdInputNumber disabled={isDisabled} placeholder={placeholder} style={inputStyle} />
        </Form.Item>
    )
}

export default InputNumber
