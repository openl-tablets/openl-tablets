import React, { FC, ReactNode, useEffect, useMemo, useRef } from 'react'
import { InputNumber as AntdInputNumber, Form, TooltipProps } from 'antd'
import { Rule } from 'antd/es/form'

type InputNumberProps = {
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

const InputNumber: FC<InputNumberProps> = ({
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
    const isDisabled = useRef(disabled)

    useEffect(() => {
        if (value !== null && typeof value === 'object') {
            if (value.readOnly) {
                isDisabled.current = true
            }
            form.setFieldValue(name, value.value)
        }
    }, [])

    const inputStyle = useMemo(() => ({
        width: '100%',
        ...style,
    }), [style])

    return (
        <Form.Item label={label} name={name} style={formItemStyle} {...rest}>
            <AntdInputNumber disabled={isDisabled.current} placeholder={placeholder} style={inputStyle} />
        </Form.Item>
    )
}

export default InputNumber
