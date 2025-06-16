import React, { useContext } from 'react'
import { Card, Col, Divider, List, Row, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { SystemContext } from '../contexts'
import { Link } from 'react-router-dom'

const documentationItems = [
    'Installation Guide',
    'Reference Guide',
    'Developer Guide',
    'WebStudio User Guide',
    'Rule Services Usage and Customization Guide'
]

export const Help: React.FC = () => {
    const { t } = useTranslation()
    const { openlInfo } = useContext(SystemContext)

    const openlVersion = openlInfo['openl.version'] || 'unknown'
    const openlUrl = openlInfo['openl.site'] || 'https://openl-tablets.org'

    const baseLink = `${openlUrl}/files/openl-tablets/${openlVersion}/OpenL Tablets - `

    return (
        <Row gutter={[24, 24]} style={{ padding: '24px 50px' }}>
            <Col xs={24} sm={12} md={8}>
                <Card title={t('common:help.openl_tablets_documentation')} bordered>
                    <List
                        dataSource={documentationItems}
                        renderItem={(item) => (
                            <>
                                <Link rel="noopener noreferrer" target="_blank" to={`${baseLink}${item}.pdf`}>{item}</Link>
                                <br />
                            </>
                        )}
                    />
                </Card>
            </Col>
            <Col xs={24} sm={12} md={8}>
                <Card title={t('common:help.additional_information')} bordered>
                    <Link rel="noopener noreferrer" target="_blank" to="/web/config/application.properties">application.properties example</Link>
                    <br />
                    <Link rel="noopener noreferrer" target="_blank" to="/rest/api-docs">Internal REST API Documentation</Link>
                </Card>
                <Divider />
                <Card title={t('common:help.openl_tablets_issues')} bordered>
                    <Link rel="noopener noreferrer" target="_blank" to="https://github.com/openl-tablets/openl-tablets/discussions">Ask a Question</Link>
                    <br />
                    <Link rel="noopener noreferrer" target="_blank" to="https://github.com/openl-tablets/openl-tablets/issues/">Report a Problem</Link>
                    <br />
                    <br />
                    {openlInfo && (
                        <Row justify="end">
                            <Typography.Text type="secondary" style={{ fontSize: '0.8em' }}>
                                {t('common:user_menu.version', { version: openlInfo['openl.version'] })}
                            </Typography.Text>
                        </Row>
                    )}

                </Card>
            </Col>
            <Col xs={24} sm={12} md={8}>
                <Card title={t('common:help.openl_tablets_on_the_internet')} bordered>
                    <Link rel="noopener noreferrer" target="_blank" to={openlUrl}>Official Website</Link>
                    <br />
                    <Link rel="noopener noreferrer" target="_blank" to={`${openlUrl}/news`}>OpenL Tablets News</Link>
                </Card>
            </Col>
        </Row>
    )
}