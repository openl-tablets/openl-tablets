import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { App, Button, Spin } from 'antd'
import Logo from '../components/Logo'
import { apiCall } from '../services'
import { useUserStore } from 'store'
import { useTranslation } from 'react-i18next'
import { useStyles } from '../styles/splashCard.styles'

export const EmailVerification = () => {
    const { notification } = App.useApp()
    const location = useLocation()
    const navigate = useNavigate()
    const [loading, setLoading] = useState(false)
    const [status, setStatus] = useState<'success' | 'error' | null>(null)
    const [resendLoading, setResendLoading] = useState(false)
    const { userProfile } = useUserStore()
    const [cooldown, setCooldown] = useState(0)
    const [redirectCountdown, setRedirectCountdown] = useState(10)
    const { t } = useTranslation()
    const { cx, styles } = useStyles()

    // navigate and reload method
    const navigateAndReload = (path: string) => {
        navigate(path)
        window.location.reload()
    }
    // Handle verification on mount
    useEffect(() => {
        const queryParams = new URLSearchParams(location.search)
        const token = queryParams.get('token')
        if (!token) {
            navigateAndReload('/')
            return
        }
        setLoading(true)
        apiCall(`/mail/verify/${token}`, undefined, true)
            .then(() => setStatus('success'))
            .catch(() => setStatus('error'))
            .finally(() => setLoading(false))
    }, [location.search, navigate])

    // Redirect after success with timer
    useEffect(() => {
        if (status === 'success') {
            setRedirectCountdown(10)
            const timer = setInterval(() => {
                setRedirectCountdown((prev) => {
                    if (prev <= 1) {
                        navigateAndReload('/')
                        return 0
                    }
                    return prev - 1
                })
            }, 1000)
            return () => clearInterval(timer)
        }
        return () => {}
    }, [status])

    // Cooldown timer effect
    useEffect(() => {
        let timer: NodeJS.Timeout | undefined
        if (cooldown > 0) {
            timer = setInterval(() => {
                setCooldown((prev) => (prev > 0 ? prev - 1 : 0))
            }, 1000)
        }
        return () => {
            if (timer) clearInterval(timer)
        }
    }, [cooldown])

    const handleResend = async () => {
        if (!userProfile?.username) {
            notification.error({ title: t('users:cannot_determine_current_user') })
            return
        }
        setResendLoading(true)
        try {
            await apiCall(`/mail/send/${userProfile.username}`, { method: 'POST' }, true)
            notification.success({ title: t('users:verification_email_sent') })
            setCooldown(60)
        } catch (_) {
            notification.error({ title: t('users:failed_to_send_verification_email') })
        } finally {
            setResendLoading(false)
        }
    }

    if (loading) {
        return (
            <div className={styles.container}>
                <Spin size="large" />
            </div>
        )
    }

    if (status === 'success') {
        return (
            <div className={styles.container}>
                <div className={styles.card}>
                    <Logo height={72} width={72} />
                    <div className={styles.title}>{t('users:email_verified_title')}</div>
                    <div className={styles.subTitle}>
                        {t('users:email_verified_message')}<br /><br />
                        <span className={styles.note}>
                            {t('users:email_verified_redirect', { seconds: redirectCountdown })}
                        </span>
                    </div>
                    <Button block onClick={() => navigateAndReload('/')} size="large" type="primary">
                        {t('users:go_to_main_page')}
                    </Button>
                </div>
            </div>
        )
    }
    if (status === 'error') {
        return (
            <div className={styles.container}>
                <div className={styles.card}>
                    <Logo height={72} width={72} />
                    <div className={cx(styles.title, styles.titleError)}>{t('users:verification_failed_title')}</div>
                    <div className={styles.subTitle}>
                        {t('users:verification_failed_message_1')}<br /><br />
                        {t('users:verification_failed_message_2')}
                    </div>
                    <div className={styles.note} style={{ textAlign: 'center', width: '100%' }}>
                        {t('users:verification_failed_resend_prompt')}
                        <Button
                            block
                            disabled={!userProfile?.username || cooldown > 0}
                            loading={resendLoading}
                            onClick={handleResend}
                            style={{ marginTop: 12 }}
                            type="primary"
                        >
                            {t('users:send_verification_email')}
                        </Button>
                        {cooldown > 0 && (
                            <div style={{ marginTop: 8 }}>
                                {t('users:resend_verification_email_timer', { seconds: cooldown })}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        )
    }
    return null
}
