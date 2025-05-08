import React, { FC } from 'react'
import { Checkbox as AntdCheckbox, Form } from 'antd'
import { CheckboxChangeEvent } from 'antd/es/checkbox'

type InputProps = {
    name: string
    checked?: boolean
    label?: string
    disabled?: boolean,
    valuePropName?: string
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
    onChange?: (e: CheckboxChangeEvent) => void
};

const Checkbox: FC<InputProps> = ({
    name,
    label,
    disabled,
    style,
    valuePropName = 'checked',
    formItemStyle,
    ...rest
}) => (
    <Form.Item label={label} name={name} style={formItemStyle} valuePropName={valuePropName}>
        <AntdCheckbox disabled={disabled} style={style} {...rest} />
    </Form.Item>
)

export default Checkbox
