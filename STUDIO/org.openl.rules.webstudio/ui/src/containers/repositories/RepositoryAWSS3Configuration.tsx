import React, { FC, useMemo } from 'react'
import { Input, InputNumber, Select } from '../../components'
import { useTranslation } from 'react-i18next'
import { AWS_SSE_ALGORITHM } from './constants'
import { AWSS3RepositorySettings } from './index'

interface RepositoryAWSS3ConfigurationProps {
    configuration: {
        settings: AWSS3RepositorySettings
    }
}

export const RepositoryAWSS3Configuration: FC<RepositoryAWSS3ConfigurationProps> = ({ configuration }) => {
    const { t } = useTranslation()
    const { allAllowedRegions, allSseAlgorithms } = configuration?.settings || {}

    const regionOptions = allAllowedRegions?.map(region => ({
        label: region.description,
        value: region.id,
    })) || []

    const sseAlgorithmOptions = useMemo(() => {
        const options = allSseAlgorithms?.map((algorithm: string) => {
            if (AWS_SSE_ALGORITHM[algorithm]) {
                return {
                    label: AWS_SSE_ALGORITHM[algorithm],
                    value: algorithm,
                }
            }
            return {
                label : algorithm,
                value : algorithm,
            }
        }) || []

        return [
            { label: t('repository:none'), value: 'UNKNOWN_TO_SDK_VERSION' },
            ...options,
        ]
    }, [allSseAlgorithms, t])
    
    return (
        <>
            <Input label={t('repository:service_endpoint')} name={['settings', 'serviceEndpoint']} />
            <Input label={t('repository:bucket_name')} name={['settings', 'bucketName']} rules={[{ required: true, message: t('common:validation.required') }]} />
            <Select label={t('repository:region_name')} name={['settings', 'regionName']} options={regionOptions} rules={[{ required: true, message: t('common:validation.required') }]} />
            <Input label={t('repository:access_key')} name={['settings', 'accessKey']} />
            <Input label={t('repository:secret_key')} name={['settings', 'secretKey']} />
            <InputNumber label={t('repository:listener_timer_period_sec')} name={['settings', 'listenerTimerPeriod']} />
            <Select label={t('repository:sse_algorithm')} name={['settings', 'sseAlgorithm']} options={sseAlgorithmOptions} />
        </>
    )
}
