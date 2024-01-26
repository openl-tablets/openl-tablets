import React, { useEffect, useState } from 'react'
import { Input, Button, Typography, Row } from 'antd'
import { useTranslation } from 'react-i18next'
import { setNotification } from './notificationSlice'
import { RootState, useAppDispatch, useAppSelector } from 'store'

const { TextArea } = Input

export const Notification: React.FC = () => {
    const { t } = useTranslation()
    const dispatch = useAppDispatch()
    const [ text, setText ] = useState('')

    const notificationMessage = useAppSelector((state: RootState) => state.notification.notification)

    useEffect(() => {
        setText(notificationMessage || '')
    }, [ notificationMessage ])

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        setText(e.target.value)
    }

    const handleClear = () => {
        setText('')
        postNotification('')
    }

    const handleNotify = () => {
        postNotification(text)
    }

    const postNotification = (notification: string) => {
        dispatch(setNotification(notification))
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
                <Button onClick={handleNotify} type="primary">
                    { t('notification:notify') }
                </Button>
            </Row>
        </>
    )
}
