import { FC, useMemo } from 'react'
import { Form } from 'antd'
import { Input, InputNumber, Select } from '../../components'
import { useTranslation } from 'react-i18next'
import { AWS_KMS_SSE_ALGORITHMS, AWS_SSE_ALGORITHM } from './constants'
import { AWSS3RepositorySettings } from './index'

interface RepositoryAWSS3ConfigurationProps {
    configuration: {
        settings: AWSS3RepositorySettings
    }
}

export const RepositoryAWSS3Configuration: FC<RepositoryAWSS3ConfigurationProps> = ({ configuration }) => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const sseAlgorithm = Form.useWatch(['settings', 'sseAlgorithm'], form)
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

    // A KMS key belongs to the algorithms that use KMS, so it goes away with them. The field stays in the form while
    // it is hidden: a form submits only the fields it holds, and the server keeps the stored value of a missing one.
    const clearKmsKeyUnlessUsed = (algorithm: string) => {
        if (!AWS_KMS_SSE_ALGORITHMS.includes(algorithm)) {
            form.setFieldValue(['settings', 'sseKmsKeyId'], '')
        }
    }

    return (
        <>
            <Input label={t('repository:service_endpoint')} name={['settings', 'serviceEndpoint']} />
            <Input label={t('repository:bucket_name')} name={['settings', 'bucketName']} rules={[{ required: true, message: t('common:validation.required') }]} />
            <Select label={t('repository:region_name')} name={['settings', 'regionName']} options={regionOptions} rules={[{ required: true, message: t('common:validation.required') }]} />
            <Input label={t('repository:access_key')} name={['settings', 'accessKey']} />
            <Input label={t('repository:secret_key')} name={['settings', 'secretKey']} />
            <InputNumber label={t('repository:listener_timer_period_sec')} name={['settings', 'listenerTimerPeriod']} />
            <Select label={t('repository:sse_algorithm')} name={['settings', 'sseAlgorithm']} onChange={clearKmsKeyUnlessUsed} options={sseAlgorithmOptions} />
            <Input hidden={!AWS_KMS_SSE_ALGORITHMS.includes(sseAlgorithm)} label={t('repository:sse_kms_key_id')} name={['settings', 'sseKmsKeyId']} />
        </>
    )
}
