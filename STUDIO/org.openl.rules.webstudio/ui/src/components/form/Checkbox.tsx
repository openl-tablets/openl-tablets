import React, { FC, ReactNode, useEffect, useRef, useState } from 'react'
import { Checkbox as AntdCheckbox, Form, TooltipProps } from 'antd'
import { CheckboxChangeEvent } from 'antd/es/checkbox'

type InputProps = {
    name: string | string[]
    checked?: boolean
    label?: string
    tooltip?: ReactNode | TooltipProps & { icon: any }
    disabled?: boolean,
    valuePropName?: string
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    onChange?: (e: CheckboxChangeEvent) => void
};

const Checkbox: FC<InputProps> = ({
    name,
    label,
    tooltip,
    disabled,
    style,
    valuePropName = 'checked',
    formItemStyle,
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
            }
            form.setFieldValue(name, value.value)
        }
    }, [])

    return (
        <Form.Item label={label} name={name} style={formItemStyle} tooltip={tooltip} valuePropName={valuePropName}>
            <AntdCheckbox disabled={isDisabled} style={style} {...rest} />
        </Form.Item>
    )
}

export default Checkbox
