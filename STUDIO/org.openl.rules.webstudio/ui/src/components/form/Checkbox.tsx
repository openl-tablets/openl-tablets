import React, { FC, ReactNode } from 'react'
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
}) => (
    <Form.Item label={label} name={name} style={formItemStyle} tooltip={tooltip} valuePropName={valuePropName}>
        <AntdCheckbox disabled={disabled} style={style} {...rest} />
    </Form.Item>
)

export default Checkbox
