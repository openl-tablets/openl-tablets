import React, { FC } from 'react'
import { Checkbox as AntdCheckbox, Form } from 'antd'

type InputProps = {
    name: string
    checked?: boolean
    label?: string
    disabled?: boolean,
    valuePropName?: string
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
};

const Checkbox: FC<InputProps> = ({
    name,
    label,
    disabled,
    style,
    valuePropName = 'checked',
    formItemStyle,
}) => (
    <Form.Item label={label} name={name} style={formItemStyle} valuePropName={valuePropName}>
        <AntdCheckbox disabled={disabled} style={style} />
    </Form.Item>
)

export default Checkbox
