import React from 'react'
import { useTranslation } from 'react-i18next'
import { Form, Input, Modal } from 'antd'

interface AddKeyModalProps {
    open: boolean
    /** Whether the messages hold a key already, which a new one may not repeat. */
    exists: (key: string) => Promise<boolean>
    /** The key the dialog offers, as a rule that asks for a missing one names it. */
    initialKey?: string | undefined
    onAdd: (key: string, text: string) => void
    onClose: () => void
}

interface AddKeyForm {
    key: string
    text?: string
}

/** Asks for a new key of the messages and its text in the default language. */
export const AddKeyModal = ({ open, exists, initialKey, onAdd, onClose }: AddKeyModalProps) => {
    const { t } = useTranslation('localization')
    const [form] = Form.useForm<AddKeyForm>()
    return (
        <Modal
            destroyOnHidden
            cancelText={t('common:btn.cancel')}
            okButtonProps={{ 'data-testid': 'localization-add-key-ok' }}
            okText={t('add_key')}
            onOk={() => form.submit()}
            open={open}
            title={t('add_key')}
            onCancel={() => {
                // The values outlive the form, which the dialog takes away: they are cleared for the next key.
                form.resetFields()
                onClose()
            }}
        >
            <Form<AddKeyForm>
                form={form}
                initialValues={{ key: initialKey }}
                layout="vertical"
                onFinish={values => {
                    onAdd(values.key.trim(), values.text ?? '')
                    form.resetFields()
                }}
            >
                <Form.Item
                    label={t('key')}
                    name="key"
                    // The key is looked up once it is written, not at each keystroke.
                    validateTrigger="onBlur"
                    rules={[
                        { required: true, whitespace: true, message: t('key_required') },
                        {
                            validator: async (_, value: string | undefined) => {
                                const key = value?.trim() ?? ''
                                if (key !== '' && await exists(key)) {
                                    throw new Error(t('key_exists'))
                                }
                            },
                        },
                    ]}
                >
                    <Input autoFocus data-testid="localization-add-key-name" placeholder="order.total" />
                </Form.Item>
                <Form.Item label={t('default_text')} name="text">
                    <Input.TextArea autoSize data-testid="localization-add-key-text" />
                </Form.Item>
            </Form>
        </Modal>
    )
}
