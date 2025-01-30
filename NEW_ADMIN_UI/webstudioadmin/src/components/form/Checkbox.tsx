import React, { FC } from 'react'
import { Field } from 'react-final-form'
import { Checkbox as AntdCheckbox, Form } from 'antd'

type InputProps = {
    name: string
    checked?: boolean
    label?: string
    disabled?: boolean,
    style?: React.CSSProperties
    formItemStyle?: React.CSSProperties,
};

const Checkbox: FC<InputProps> = ({
    name,
    label,
    disabled,
    style,
    formItemStyle,
}) => (
    <Field name={name} type="checkbox">
        {({ input }) => (
            <Form.Item label={label} style={formItemStyle}>
                <AntdCheckbox {...input} disabled={disabled} style={style} />
            </Form.Item>
        )}
    </Field>
)

export default Checkbox
