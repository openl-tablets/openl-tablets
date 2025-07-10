import React, { FC, ReactNode, useEffect, useState } from 'react'
import { Form, Input, TooltipProps } from 'antd'
import { useRules } from './hooks'
import { RuleObject } from 'rc-field-form/lib/interface'

const { TextArea: AntdTextArea } = Input

type TextAreaProps = {
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
    rows?: number
}

const TextArea: FC<TextAreaProps> = ({
    name,
    label,
    disabled,
    style,
    formItemStyle,
    placeholder,
    required,
    rules = [],
    rows = 4,
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
            <AntdTextArea disabled={isDisabled} placeholder={placeholder} rows={rows} style={style} />
        </Form.Item>
    )
}

export default TextArea
