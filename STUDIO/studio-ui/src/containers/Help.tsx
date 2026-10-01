import React, { useContext } from 'react'
import { Card, Col, Row, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { SystemContext } from '../contexts'
import { Link } from 'react-router-dom'
import { CompressOutlined, FileTextOutlined, GlobalOutlined, QuestionCircleOutlined } from '@ant-design/icons'

// Each guide is a folder of `Docs/user-guides`, which OpenL Studio ships and shows at `/docs/<path>/`.
const documentationItems = [
    { path: 'getting-started', title: 'Getting Started' },
    { path: 'installation-guide', title: 'Installation Guide' },
    { path: 'reference-guide', title: 'Reference Guide' },
    { path: 'openl-studio', title: 'OpenL Studio User Guide' },
    { path: 'rule-services', title: 'Rule Services Usage and Customization Guide' }
]

export const Help: React.FC = () => {
    const { t } = useTranslation()
    const { openlInfo } = useContext(SystemContext)

    const openlUrl = openlInfo?.['openl.site'] || 'https://openl-tablets.org'

    return (
        <>
            <Row justify="center" style={{ padding: '24px 0 0' }}>
                <Col>
                    <Typography.Title level={2}>{t('common:help.openl_tablets_help_title')}</Typography.Title>
                </Col>
            </Row>
            <Row justify="center">
                <Col>
                    <Typography.Title level={4} type="secondary">{t('common:help.openl_tablets_help_description')}</Typography.Title>
                </Col>
            </Row>
            <Row gutter={[24, 24]} style={{ padding: '24px 50px' }}>
                <Col md={8} sm={12} xs={24}>
                    <Card
                        variant="outlined"
                        title={(
                            <>
                                <FileTextOutlined style={{ marginRight: 10 }} />
                                {t('common:help.documentation')}
                            </>
                        )}
                    >
                        {documentationItems.map((item) => (
                            <React.Fragment key={item.path}>
                                <Link to={`/docs/${item.path}/`}>{item.title}</Link>
                                <br />
                            </React.Fragment>
                        ))}
                    </Card>
                </Col>
                <Col md={8} sm={12} xs={24}>
                    <Card
                        variant="outlined"
                        title={(
                            <>
                                <CompressOutlined style={{ marginRight: 10 }} />
                                {t('common:help.additional_information')}
                            </>
                        )}
                    >
                        <Link rel="noopener noreferrer" target="_blank" to="/application.properties">application.properties example</Link>
                        <br />
                        <Link rel="noopener noreferrer" target="_blank" to="/api-docs">Internal REST API Documentation</Link>
                    </Card>
                </Col>
                <Col md={8} sm={12} xs={24}>
                    <Card
                        variant="outlined"
                        title={(
                            <>
                                <QuestionCircleOutlined style={{ marginRight: 10 }} />
                                {t('common:help.support')}
                            </>
                        )}
                    >
                        <Link rel="noopener noreferrer" target="_blank" to="https://github.com/openl-tablets/openl-tablets/discussions">Ask a Question</Link>
                        <br />
                        <Link rel="noopener noreferrer" target="_blank" to="https://github.com/openl-tablets/openl-tablets/issues/">Report a Problem</Link>
                    </Card>
                </Col>
                <Col md={8} sm={12} xs={24}>
                    <Card
                        variant="outlined"
                        title={(
                            <>
                                <GlobalOutlined style={{ marginRight: 10 }} />
                                {t('common:help.on_the_internet')}
                            </>
                        )}
                    >
                        <Link rel="noopener noreferrer" target="_blank" to={openlUrl}>Official Website</Link>
                        <br />
                        <Link rel="noopener noreferrer" target="_blank" to={`${openlUrl}/news`}>OpenL Tablets News</Link>
                    </Card>
                </Col>
            </Row>
        </>
    )
}
