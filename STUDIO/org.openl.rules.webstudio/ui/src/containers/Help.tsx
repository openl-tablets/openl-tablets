import React, { useContext } from 'react'
import { Col, Divider, List, Row, Typography } from 'antd'
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
        <Row justify="center">
            <Col style={{ marginTop: 16 }} span={6}>
                <Divider orientation="left">{t('common:help.openl_tablets_documentation')}</Divider>
                <List
                    dataSource={documentationItems}
                    renderItem={(item) => (
                        <List.Item>
                            <Link rel="noopener noreferrer" target="_blank" to={`${baseLink}${item}.pdf`}>{item}</Link>
                        </List.Item>
                    )}
                />
                <Divider orientation="left">{t('common:help.openl_tablets_on_the_internet')}</Divider>
                <List>
                    <List.Item>
                        <Link rel="noopener noreferrer" target="_blank" to={openlUrl}>Official Website</Link>
                    </List.Item>
                    <List.Item>
                        <Link rel="noopener noreferrer" target="_blank" to={`${openlUrl}/news`}>OpenL Tablets News</Link>
                    </List.Item>
                </List>
            </Col>
            <Col span={1} />
            <Col span={6} style={{ marginTop: 16 }}>
                <Divider orientation="left">{t('common:help.additional_information')}</Divider>
                <List>
                    <List.Item>
                        <Link rel="noopener noreferrer" target="_blank" to="/web/config/application.properties">application.properties example</Link>
                    </List.Item>
                    <List.Item>
                        <Link rel="noopener noreferrer" target="_blank" to="/rest/api-docs">Internal REST API Documentation</Link>
                    </List.Item>
                </List>
                <Divider orientation="left">{t('common:help.openl_tablets_issues')}</Divider>
                <List>
                    <List.Item>
                        <Link rel="noopener noreferrer" target="_blank" to="https://github.com/openl-tablets/openl-tablets/discussions">Ask a Question</Link>
                    </List.Item>
                    <List.Item>
                        <Link rel="noopener noreferrer" target="_blank" to="https://github.com/openl-tablets/openl-tablets/issues/">Report a Problem</Link>
                    </List.Item>
                    <List.Item>
                        {openlInfo && (
                            <Typography.Text type="secondary">
                                {t('common:user_menu.version', { version: openlInfo['openl.version'] })}
                            </Typography.Text>
                        )}
                    </List.Item>
                </List>
            </Col>
        </Row>
    )
}