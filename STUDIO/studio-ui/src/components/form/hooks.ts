import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { FormRule } from 'antd'

const isRequired = (rule: FormRule) => typeof rule !== 'function' && rule.required

export const useRules = ({ required, rules = []}: { required?: boolean | undefined, rules: FormRule[] }) => {
    const { t } = useTranslation()
    const [additionalRules, setAdditionalRules] = useState<FormRule[]>([])

    useEffect(() => {
        if (required) {
            setAdditionalRules(prev => {
                const updatedRules = prev.filter(rule => !isRequired(rule))
                return [...updatedRules, {
                    required: true,
                    message: t('common:validation.required')
                }]
            })
        } else {
            setAdditionalRules(prev => prev.filter(rule => !isRequired(rule)))
        }
    }, [required])

    const allRules = useMemo(() => {
        return rules.concat(additionalRules)
    }, [rules, additionalRules])

    return { allRules }
}

export const usePasswordRules = ({ required, isSecret, rules = []}: { required?: boolean | undefined, isSecret: boolean, rules: FormRule[] }) => {
    const { t } = useTranslation()
    const [additionalRules, setAdditionalRules] = useState<FormRule[]>([])

    useEffect(() => {
        if (required && !isSecret) {
            setAdditionalRules(prev => {
                const updatedRules = prev.filter(rule => !isRequired(rule))
                return [...updatedRules, {
                    required: true,
                    message: t('common:validation.required')
                }]
            })
        } else {
            setAdditionalRules(prev => prev.filter(rule => !isRequired(rule)))
        }
    }, [required, isSecret])

    const allRules = useMemo(() => {
        return rules.concat(additionalRules)
    }, [rules, additionalRules])

    return { allRules }
}
