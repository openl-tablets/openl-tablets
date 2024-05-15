import React, { useEffect, useState } from 'react'
import { Input, Button, Typography, Row, InputRef } from 'antd'
import { useTranslation } from 'react-i18next'
import { setNotification } from './notificationSlice'
import { RootState, useAppDispatch, useAppSelector } from 'store'

const { TextArea } = Input

export const Notification: React.FC = () => {
    const inputRef = React.useRef<InputRef>(null)
    const { t } = useTranslation()
    const dispatch = useAppDispatch()
    const [text, setText] = useState('')
    const [focused, setFocused] = React.useState(false)

    const onFocus = () => setFocused(true)
    const onBlur = () => setFocused(false)

    const notificationMessage = useAppSelector((state: RootState) => state.notification.notification)

    useEffect(() => {
        if (!focused && inputRef.current) {
            setText(notificationMessage || '')
        }
    }, [notificationMessage])

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

    return (
        <>
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                { t('notification:notify_all_active_users') }
            </Typography.Title>
            <Typography>
                { t('notification:message') }
            </Typography>
            <TextArea ref={inputRef} onBlur={onBlur} onChange={handleChange} onFocus={onFocus} value={text} />
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
