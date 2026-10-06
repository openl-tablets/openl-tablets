import { createRef } from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { Form } from 'antd'
import type { FormInstance } from 'antd'
import { describe, expect, it, vi } from 'vitest'
import { RepositoryAWSS3Configuration } from './RepositoryAWSS3Configuration'
import type { AWSS3RepositorySettings } from './index'
import { chooseOption, openOptions } from 'testing/select'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en', resolvedLanguage: 'en' }
    return { useTranslation: () => ({ t, i18n }) }
})

const KMS_ALGORITHMS = ['AWS_KMS', 'AWS_KMS_DSSE']
/** The algorithms that use no KMS key, with the title of their option. */
const NO_KMS_ALGORITHMS: [algorithm: string, title: string][] = [
    ['UNKNOWN_TO_SDK_VERSION', 'repository:none'],
    ['AES256', 'AES256'],
]

const KMS_KEY = 'alias/openl'
const ALGORITHM_LABEL = 'repository:sse_algorithm'
const KMS_KEY_LABEL = 'repository:sse_kms_key_id'

const renderForm = (sseAlgorithm: string, sseKmsKeyId = '') => {
    const form = createRef<FormInstance>()
    const onFinish = vi.fn()
    const settings: AWSS3RepositorySettings = {
        invalidCommentMessage: '',
        bucketName: 'bucket',
        regionName: 'us-east-1',
        listenerTimerPeriod: 10,
        allAllowedRegions: [{ id: 'us-east-1', description: 'US East (N. Virginia)' }],
        allSseAlgorithms: ['AES256', 'AWS_FSX', 'AWS_BACKUP', 'AWS_KMS', 'AWS_KMS_DSSE'],
        sseAlgorithm,
        sseKmsKeyId,
    }
    render(
        <Form ref={form} initialValues={{ settings }} onFinish={onFinish}>
            <RepositoryAWSS3Configuration configuration={{ settings }} />
        </Form>
    )
    return { form, onFinish }
}

/** The settings the form sends, which hold only the fields the form still has. */
const submitted = async ({ form, onFinish }: ReturnType<typeof renderForm>) => {
    await act(async () => form.current!.submit())
    await waitFor(() => expect(onFinish).toHaveBeenCalledTimes(1))
    return onFinish.mock.calls[0]![0].settings
}

const kmsKeyField = () => screen.getByRole('textbox', { name: KMS_KEY_LABEL, hidden: true })

describe('RepositoryAWSS3Configuration, server-side encryption', () => {
    it('offers no encryption and every algorithm the server knows, named as AWS S3 names them', async () => {
        // Nothing is selected, so the title of the selected value does not repeat the title of an option.
        renderForm('')

        openOptions(ALGORITHM_LABEL)

        for (const title of ['repository:none', 'AES256', 'aws:fsx', 'aws:backup', 'aws:kms', 'aws:kms:dsse']) {
            expect(await screen.findByTitle(title)).toBeInTheDocument()
        }
    })

    it.each(KMS_ALGORITHMS)('shows the KMS key of the %s algorithm', algorithm => {
        renderForm(algorithm, KMS_KEY)

        expect(kmsKeyField()).toBeVisible()
        expect(kmsKeyField()).toHaveValue(KMS_KEY)
    })

    it.each(NO_KMS_ALGORITHMS)('hides the KMS key for the %s algorithm', algorithm => {
        renderForm(algorithm)

        expect(kmsKeyField()).not.toBeVisible()
    })

    it('sends the KMS key entered once a KMS algorithm is chosen', async () => {
        const rendered = renderForm('AES256')

        await chooseOption(ALGORITHM_LABEL, 'aws:kms:dsse')
        await waitFor(() => expect(kmsKeyField()).toBeVisible())
        fireEvent.change(kmsKeyField(), { target: { value: KMS_KEY } })

        expect(await submitted(rendered)).toMatchObject({ sseAlgorithm: 'AWS_KMS_DSSE', sseKmsKeyId: KMS_KEY })
    })

    it.each(NO_KMS_ALGORITHMS)(
        'sends the KMS key cleared when the %s algorithm replaces a KMS one',
        async (algorithm, title) => {
            const rendered = renderForm('AWS_KMS', KMS_KEY)

            await chooseOption(ALGORITHM_LABEL, title)

            await waitFor(() => expect(rendered.form.current!.getFieldValue(['settings', 'sseAlgorithm'])).toBe(algorithm))
            expect(await submitted(rendered)).toMatchObject({ sseAlgorithm: algorithm, sseKmsKeyId: '' })
        }
    )

    it('keeps the KMS key when one KMS algorithm replaces another', async () => {
        const rendered = renderForm('AWS_KMS', KMS_KEY)

        await chooseOption(ALGORITHM_LABEL, 'aws:kms:dsse')

        expect(await submitted(rendered)).toMatchObject({ sseAlgorithm: 'AWS_KMS_DSSE', sseKmsKeyId: KMS_KEY })
        expect(kmsKeyField()).toBeVisible()
    })
})
