import React, { FC, ReactNode, useEffect, useMemo, useState } from 'react'
import { Input as AntdInput, Form, TooltipProps } from 'antd'
import { usePasswordRules } from './hooks'
import { RuleObject } from 'rc-field-form/lib/interface'
import { useTranslation } from 'react-i18next'

type InputProps = {
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
    autoComplete?: string
};

const InputPassword: FC<InputProps> = ({
    name,
    label,
    disabled,
    style,
    formItemStyle,
    placeholder,
    required,
    rules = [],
    autoComplete,
    ...rest
}) => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const value = Form.useWatch(name, form)
    const [isDisabled, setIsDisabled] = useState(disabled)
    const [isSecret, setSecret] = useState(false)
    const { allRules } = usePasswordRules({ required, isSecret, rules })

    useEffect(() => {
        if (disabled !== undefined) {
            setIsDisabled(disabled)
        }
    }, [disabled])

    useEffect(() => {
        if (value !== null && typeof value === 'object') {
            if (value.readOnly) {
                setIsDisabled(true)
                form.setFieldValue(name, value.value)
            }
            if (value.secret) {
                setSecret(true)
                form.setFieldValue(name, undefined)
            }
        }
    }, [value])

    const passwordPlaceholder = useMemo(() => {
        if (isSecret && !isDisabled) {
            return t('common:validation.leave_blank')
        }
        return placeholder
    }, [placeholder, isSecret, isDisabled])

    return (
        <Form.Item label={label} name={name} rules={allRules} style={formItemStyle} {...rest}>
            <AntdInput.Password autoComplete={autoComplete} disabled={isDisabled} placeholder={passwordPlaceholder} style={style} />
        </Form.Item>
    )
}

export default InputPassword
