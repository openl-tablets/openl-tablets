import React from 'react'
import { useTranslation } from 'react-i18next'
import { Input, InputNumber } from '../../components'

export const RepositoryAzureBlobConfiguration = () => {
    const { t } = useTranslation()

    return (
        <>
            <Input name={['settings', 'uri']} label={t('repository:url')} rules={[{ required: true, message: t('common:validation.required') }]} />
            <InputNumber name={['settings', 'listenerTimerPeriod']} label={t('repository:listener_timer_period_sec')} />
        </>
    )
}