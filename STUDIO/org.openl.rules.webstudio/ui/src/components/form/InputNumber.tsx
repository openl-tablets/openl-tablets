import React, { FC, useMemo } from 'react'
import { InputNumber as AntdInputNumber, Form } from 'antd'
import { Rule } from 'antd/es/form'

type InputNumberProps = {
    name: string | (string | number)[]
    label?: string
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
    const inputStyle = useMemo(() => ({
        width: '100%',
        ...style,
    }), [style])

    return (
        <Form.Item label={label} name={name} style={formItemStyle} {...rest}>
            <AntdInputNumber disabled={disabled} placeholder={placeholder} style={inputStyle} />
        </Form.Item>
    )
}

export default InputNumber
