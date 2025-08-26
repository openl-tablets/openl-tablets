import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { RuleObject } from 'rc-field-form/lib/interface'

export const useRules = ({ required, rules = []}: { required?: boolean, rules: RuleObject[] }) => {
    const { t } = useTranslation()
    const [additionalRules, setAdditionalRules] = useState<RuleObject[]>([])

    useEffect(() => {
        if (required) {
            setAdditionalRules(prev => {
                const updatedRules = prev.filter(rule => !rule.required)
                return [...updatedRules, {
                    required: true,
                    message: t('common:validation.required')
                }]
            })
        } else {
            setAdditionalRules(prev => prev.filter(rule => !rule.required))
        }
    }, [required])

    const allRules = useMemo(() => {
        return rules.concat(additionalRules)
    }, [rules, additionalRules])

    return { allRules }
}

export const usePasswordRules = ({ required, isSecret, rules = []}: { required?: boolean, isSecret: boolean, rules: RuleObject[] }) => {
    const { t } = useTranslation()
    const [additionalRules, setAdditionalRules] = useState<RuleObject[]>([])

    useEffect(() => {
        if (required && !isSecret) {
            setAdditionalRules(prev => {
                const updatedRules = prev.filter(rule => !rule.required)
                return [...updatedRules, {
                    required: true,
                    message: t('common:validation.required')
                }]
            })
        } else {
            setAdditionalRules(prev => prev.filter(rule => !rule.required))
        }
    }, [required, isSecret])

    const allRules = useMemo(() => {
        return rules.concat(additionalRules)
    }, [rules, additionalRules])

    return { allRules }
}