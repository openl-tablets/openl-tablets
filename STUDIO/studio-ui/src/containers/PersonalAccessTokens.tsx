import React, { useCallback, useContext, useEffect, useRef, useState } from 'react'
import {
    Alert,
    Button,
    DatePicker,
    Drawer,
    Empty,
    Form,
    Input,
    Modal,
    notification,
    Select,
    Space,
    Table,
    type TableColumnsType,
    Tag,
    Tooltip,
    Typography,
} from 'antd'
import { CopyOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import dayjs, { Dayjs } from 'dayjs'
import { apiCall } from '../services'
import { SystemContext } from '../contexts'
import './PersonalAccessTokens.scss'

interface PersonalAccessToken {
    publicId: string
    name: string
    loginName: string
    createdAt: string
    expiresAt: string | null
}

interface CreatedToken extends PersonalAccessToken {
    token: string
}

interface CreateTokenFormValues {
    name: string
    expirationOption: string
    customDate?: Dayjs
}

type ExpirationOption = '7_days' | '30_days' | '60_days' | '90_days' | 'custom' | 'no_expiration'

type DrawerMode = 'create' | 'created'

export const PersonalAccessTokens: React.FC = () => {
    const { t } = useTranslation()
    const { isPersonalAccessTokenEnabled } = useContext(SystemContext)
    const [tokens, setTokens] = useState<PersonalAccessToken[]>([])
    const [loading, setLoading] = useState(true)
    const [drawerOpen, setDrawerOpen] = useState(false)
    const [drawerMode, setDrawerMode] = useState<DrawerMode>('create')
    const [createdToken, setCreatedToken] = useState<CreatedToken | null>(null)
    const [creating, setCreating] = useState(false)
    const [form] = Form.useForm<CreateTokenFormValues>()
    const [expirationOption, setExpirationOption] = useState<ExpirationOption>('7_days')
    const [copyTooltipOpen, setCopyTooltipOpen] = useState(false)
    const copyTooltipTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

    const showError = useCallback((error: unknown) => {
        const errorMessage = error instanceof Error ? error.message : t('common:error')
        notification.error({ message: errorMessage })
    }, [t])

    const fetchTokens = useCallback(async () => {
        setLoading(true)
        try {
            const response = await apiCall('/users/personal-access-tokens', undefined, true)
            if (response) {
                setTokens(response)
            }
        } catch (error) {
            showError(error)
        } finally {
            setLoading(false)
        }
    }, [showError])

    useEffect(() => {
        void fetchTokens()
    }, [fetchTokens])

    useEffect(() => {
        return () => {
            if (copyTooltipTimeoutRef.current) {
                clearTimeout(copyTooltipTimeoutRef.current)
            }
        }
    }, [])

    const calculateExpirationDate = (option: ExpirationOption, customDate?: Dayjs): string | null => {
        const now = dayjs()
        switch (option) {
            case '7_days':
                return now.add(7, 'day').toISOString()
            case '30_days':
                return now.add(30, 'day').toISOString()
            case '60_days':
                return now.add(60, 'day').toISOString()
            case '90_days':
                return now.add(90, 'day').toISOString()
            case 'custom':
                return customDate ? customDate.endOf('day').toISOString() : null
            case 'no_expiration':
                return null
        }
    }

    const openCreateDrawer = () => {
        setDrawerMode('create')
        setCreatedToken(null)
        form.resetFields()
        setExpirationOption('7_days')
        setDrawerOpen(true)
    }

    const closeDrawer = () => {
        setDrawerOpen(false)
        setCreatedToken(null)
        form.resetFields()
        setExpirationOption('7_days')
    }

    const handleCreateToken = async (values: CreateTokenFormValues) => {
        setCreating(true)
        try {
            const expiresAt = calculateExpirationDate(values.expirationOption as ExpirationOption, values.customDate)
            const response = await apiCall('/users/personal-access-tokens', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    name: values.name,
                    expiresAt,
                }),
            }, true)

            if (response) {
                setCreatedToken(response)
                setDrawerMode('created')
                await fetchTokens()
            }
        } catch (error) {
            showError(error)
        } finally {
            setCreating(false)
        }
    }

    const handleDeleteToken = async (publicId: string) => {
        setLoading(true)
        try {
            await apiCall(`/users/personal-access-tokens/${publicId}`, {
                method: 'DELETE',
            }, true)
            await fetchTokens()
        } catch (error) {
            showError(error)
        } finally {
            setLoading(false)
        }
    }

    const confirmDeleteToken = (record: PersonalAccessToken) => {
        Modal.confirm({
            title: t('pat:delete_confirm_title'),
            content: t('pat:delete_confirm_message', { name: record.name }),
            okType: 'danger',
            onOk: () => handleDeleteToken(record.publicId),
        })
    }

    const copyToClipboard = async (text: string) => {
        try {
            await navigator.clipboard.writeText(text)
            if (copyTooltipTimeoutRef.current) {
                clearTimeout(copyTooltipTimeoutRef.current)
            }
            setCopyTooltipOpen(true)
            copyTooltipTimeoutRef.current = setTimeout(() => setCopyTooltipOpen(false), 2000)
        } catch {
            notification.error({ message: t('pat:copy_failed') })
        }
    }

    const isTokenExpired = (expiresAt: string | null): boolean => {
        if (!expiresAt) return false
        return dayjs(expiresAt).isBefore(dayjs())
    }

    const formatDate = (dateString: string | null): string => {
        if (!dateString) return t('pat:never')
        return dayjs(dateString).format('MMM D, YYYY')
    }

    const columns: TableColumnsType<PersonalAccessToken> = [
        {
            title: t('pat:token_name'),
            dataIndex: 'name',
            key: 'name',
            render: (name: string, record) => (
                <Space>
                    <span>{name}</span>
                    {record.expiresAt && isTokenExpired(record.expiresAt) && (
                        <Tag color="error">{t('pat:expired')}</Tag>
                    )}
                </Space>
            ),
        },
        {
            title: t('pat:created_at'),
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (date: string) => formatDate(date),
        },
        {
            title: t('pat:expires_at'),
            dataIndex: 'expiresAt',
            key: 'expiresAt',
            render: (date) => formatDate(date),
        },
        {
            title: t('pat:actions'),
            key: 'actions',
            width: 100,
            render: (_, record) => (
                <Button
                    danger
                    aria-label={t('pat:delete')}
                    icon={<DeleteOutlined />}
                    onClick={() => confirmDeleteToken(record)}
                    type="text"
                />
            ),
        },
    ]

    const expirationOptions = [
        { value: '7_days', label: t('pat:expiration_options.7_days') },
        { value: '30_days', label: t('pat:expiration_options.30_days') },
        { value: '60_days', label: t('pat:expiration_options.60_days') },
        { value: '90_days', label: t('pat:expiration_options.90_days') },
        { value: 'custom', label: t('pat:expiration_options.custom') },
        { value: 'no_expiration', label: t('pat:expiration_options.no_expiration') },
    ]

    const drawerTitle = drawerMode === 'create' ? t('pat:create_token') : t('pat:token_created_title')

    const renderDrawerExtra = () => {
        if (drawerMode === 'create') {
            return (
                <>
                    <Button onClick={closeDrawer} style={{ marginRight: 12 }}>
                        {t('common:btn.cancel')}
                    </Button>
                    <Button loading={creating} onClick={form.submit} type="primary">
                        {t('common:btn.create')}
                    </Button>
                </>
            )
        }
        return (
            <Button onClick={closeDrawer} type="primary">
                {t('common:btn.ok')}
            </Button>
        )
    }

    const renderCreateForm = () => (
        <Form
            form={form}
            initialValues={{ expirationOption: '7_days' }}
            labelCol={{ sm: { span: 6 } }}
            onFinish={handleCreateToken}
            wrapperCol={{ sm: { span: 18 } }}
        >
            <Form.Item
                label={t('pat:token_name')}
                name="name"
                rules={[
                    { required: true, message: t('pat:validation.name_required') },
                    { max: 100, message: t('pat:validation.name_max_length') },
                ]}
            >
                <Input placeholder={t('pat:token_name_placeholder')} />
            </Form.Item>
            <Form.Item
                label={t('pat:expiration')}
                name="expirationOption"
            >
                <Select
                    onChange={(value) => setExpirationOption(value as ExpirationOption)}
                    options={expirationOptions}
                />
            </Form.Item>
            {expirationOption === 'custom' && (
                <Form.Item
                    label={t('pat:expiration_options.custom')}
                    name="customDate"
                    rules={[{ required: true, message: t('common:validation.required') }]}
                >
                    <DatePicker
                        disabledDate={(current) => current && current < dayjs().startOf('day')}
                        placeholder={t('pat:expiration_placeholder')}
                        style={{ width: '100%' }}
                    />
                </Form.Item>
            )}
        </Form>
    )

    const renderCodeBlock = (code: string, onCopy: () => void, showCopyTooltip?: boolean) => (
        <div className="pat-code-block">
            <pre><code>{code}</code></pre>
            <Tooltip open={showCopyTooltip} title={t('pat:token_copied')}>
                <Button
                    className="pat-code-block-copy"
                    icon={<CopyOutlined />}
                    onClick={onCopy}
                    size="small"
                    type="text"
                />
            </Tooltip>
        </div>
    )

    const renderCreatedToken = () => (
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Alert
                showIcon
                message={t('pat:token_created_message')}
                type="warning"
            />
            {renderCodeBlock(
                createdToken?.token ?? '',
                () => { if (createdToken?.token) void copyToClipboard(createdToken.token) },
                copyTooltipOpen
            )}
            <Typography.Text type="secondary">
                {t('pat:usage_hint')}
            </Typography.Text>
            <div className="pat-code-block pat-code-block--no-copy">
                <pre><code>Authorization: Token {createdToken?.token ?? ''}</code></pre>
            </div>
        </Space>
    )

    const hasTokens = tokens.length > 0 || loading

    if (!isPersonalAccessTokenEnabled) {
        return null
    }

    return (
        <>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 0 }}>
                    {t('pat:title')}
                </Typography.Title>
                {hasTokens && (
                    <Button
                        icon={<PlusOutlined />}
                        onClick={openCreateDrawer}
                    >
                        {t('pat:create_token')}
                    </Button>
                )}
            </div>
            <Typography.Paragraph style={{ marginBottom: 16 }} type="secondary">
                {t('pat:description')}
            </Typography.Paragraph>
            {!hasTokens ? (
                <Empty
                    description={
                        <Space direction="vertical" size={0}>
                            <span>{t('pat:no_tokens')}</span>
                            <Typography.Text type="secondary">
                                {t('pat:no_tokens_description')}
                            </Typography.Text>
                        </Space>
                    }
                >
                    <Button
                        icon={<PlusOutlined />}
                        onClick={openCreateDrawer}
                        type="primary"
                    >
                        {t('pat:create_token')}
                    </Button>
                </Empty>
            ) : (
                <Table
                    columns={columns}
                    dataSource={tokens}
                    loading={loading}
                    pagination={false}
                    rowClassName={(record) => isTokenExpired(record.expiresAt) ? 'pat-row-expired' : ''}
                    rowKey="publicId"
                    size="middle"
                />
            )}
            <Drawer
                destroyOnHidden
                extra={renderDrawerExtra()}
                onClose={closeDrawer}
                open={drawerOpen}
                title={drawerTitle}
                width={600}
            >
                {drawerMode === 'create' ? renderCreateForm() : renderCreatedToken()}
            </Drawer>
        </>
    )
}
