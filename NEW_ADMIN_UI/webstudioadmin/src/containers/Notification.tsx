import React, { useState } from 'react'
import { Input, Button, Typography, Row } from 'antd'
import { useTranslation } from 'react-i18next'

const { TextArea } = Input

export const Notification: React.FC = () => {
    const { t } = useTranslation()
    const [ text, setText ] = useState('')

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setText(e.target.value)
    }

    const handleClear = () => {
        setText('')
    }

    // TODO: Add form
    return (
        <>
            <Typography.Title level={4}>
                { t('notification:notify_all_active_users') }
            </Typography.Title>
            <Typography>
                { t('notification:message') }
            </Typography>
            <TextArea onChange={handleChange} value={text} />
            <Row align="bottom" justify="end">
                <Button onClick={handleClear} style={{ marginTop: 20, marginRight: 20 }}>
                    { t('notification:clear') }
                </Button>
                <Button type="primary">
                    { t('notification:notify') }
                </Button>
            </Row>
        </>
    )
}
