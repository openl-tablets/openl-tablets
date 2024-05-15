import React, { FC } from 'react'
import { loadRemote } from '@module-federation/runtime'
import { LoadingOutlined } from '@ant-design/icons'
import { Col, Row, Typography } from 'antd'
import { useTranslation } from 'react-i18next'

interface SystemProps {
    request: string
}

export const PluginLoader: FC<SystemProps> = ({ request }) => {
    const { t } = useTranslation()

    if (!request) {
        return <Typography.Text>{t('common:plugin:no_plugin_specified')}</Typography.Text>
    }

    // @ts-ignore
    const Component = React.lazy(() => loadRemote(request))

    const Loading = () => (
        <Row>
            <Col span={24} style={{ textAlign: 'center', marginTop: 30 }}>
                <Typography.Text style={{ marginRight: 15 }}>{t('common:plugin:loading_plugin')}</Typography.Text>
                <LoadingOutlined />
            </Col>
        </Row>
    )

    return (
        <React.Suspense fallback={<Loading />} >
            <Component />
        </React.Suspense>
    )
}