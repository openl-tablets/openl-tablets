import React from 'react'
import { useTranslation } from 'react-i18next'
import { Input, InputNumber } from '../../components'

export const RepositoryAzureBlobConfiguration = () => {
    const { t } = useTranslation()

    return (
        <>
            <Input label={t('repository:url')} name={['settings', 'uri']} rules={[{ required: true, message: t('common:validation.required') }]} />
            <InputNumber label={t('repository:listener_timer_period_sec')} name={['settings', 'listenerTimerPeriod']} />
        </>
    )
}
