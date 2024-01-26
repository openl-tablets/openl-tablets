import React, { useEffect, useMemo } from 'react'
import { Layout, Row, Col, Typography } from 'antd'
import { apiCall } from '../services'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import './Footer.scss'

const { Footer: AntFooter } = Layout

const footerStyle: React.CSSProperties = {
    backgroundColor: '#fff',
}

export const Footer = () => {
    const { t } = useTranslation()
    const [ info, setInfo ] = React.useState({} as any)

    const loadInfo = async () => {
        const response = await apiCall('/public/info/openl.json')
        setInfo(response)
    }

    useEffect(() => {
        loadInfo()
    }, [])

    const year = useMemo(() => {
        const y = info && info['openl.build.date']
        return y ? y.split('-')[0] : new Date().getFullYear()
    }, [ info ])

    const site = useMemo(() => {
        return (info && info['openl.site']) || '/'
    }, [ info ])

    const version = useMemo(() => {
        return (info && info['openl.version']) || ''
    }, [ info ])

    return (
        <AntFooter style={footerStyle}>
            <Row justify="space-between">
                <Col />
                <Col>
                    <Typography.Text style={{ fontSize: '12px' }} type="secondary">
                        &copy;
                        {' '}
                        { year }
                        {' '}
                    </Typography.Text>
                    <Link style={{ fontSize: '12px' }} to={site}>{t('common:openl_tables')}</Link>
                    <Typography.Text style={{ margin: '0 10px', fontSize: '12px' }} type="secondary">|</Typography.Text>
                    <Link style={{ fontSize: '12px' }} to="https://github.com/openl-tablets/openl-tablets/issues/">{t('common:report_a_problem')}</Link>
                </Col>
                <Col>
                    <Typography.Text style={{ fontSize: '12px' }} type="secondary">
                        {version}
                    </Typography.Text>
                </Col>
            </Row>
        </AntFooter>
    )
}