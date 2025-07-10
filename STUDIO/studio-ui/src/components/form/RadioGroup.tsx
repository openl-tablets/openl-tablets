import React, { ReactNode, useEffect, useState } from 'react'
import { Form, Radio as AntdRadio, TooltipProps } from 'antd'
import { useRules } from './hooks'
import { RuleObject } from 'rc-field-form/lib/interface'

interface RadioGroupProps {
    label?: string
    name: string
    disabled?: boolean
    style?: React.CSSProperties;
    formItemStyle?: React.CSSProperties
    placeholder?: string
    options: { label: string; value: string }[]
    required?: boolean
    rules?: RuleObject[]
    tooltip?: ReactNode | TooltipProps & { icon: any }
}

const RadioGroup: React.FC<RadioGroupProps> = ({
    label,
    name,
    disabled,
    style,
    formItemStyle,
    placeholder,
    options,
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
        <Form.Item label={label} name={name} rules={allRules} style={formItemStyle} {...rest}>
            <AntdRadio.Group disabled={isDisabled} options={options} style={style} />
        </Form.Item>
    )
}

export default RadioGroup
