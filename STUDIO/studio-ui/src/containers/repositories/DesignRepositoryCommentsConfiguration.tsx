import { useTranslation } from 'react-i18next'
import { Input } from '../../components'
import { Divider } from 'antd'
import { InvalidUserMessageHintModal, UserMessagePatternModal } from './InfoFieldModals'

export const DesignRepositoryCommentsConfiguration = () => {
    const { t } = useTranslation()

    return (
        <>
            <Divider titlePlacement="start">{t('repository:comments')}</Divider>
            <Input label={t('repository:user_message_pattern')} name={['settings', 'commentValidationPattern']} tooltip={{ icon: UserMessagePatternModal }} />
            <Input
                label={t('repository:invalid_user_message_hint')}
                name={['settings', 'invalidCommentMessage']}
                rules={[{ required: true, message: t('common:validation.required') }]}
                tooltip={{ icon: InvalidUserMessageHintModal }}
            />
        </>
    )
}
