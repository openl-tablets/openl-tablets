import { useTranslation } from 'react-i18next'
import { Input } from '../../components'
import { Divider } from 'antd'
import InfoFieldModal from '../../components/modal/InfoFieldModal'

export const DesignRepositoryCommentsConfiguration = () => {
    const { t } = useTranslation()

    return (
        <>
            <Divider titlePlacement="start">{t('repository:comments')}</Divider>
            <Input label={t('repository:user_message_pattern')} name={['settings', 'commentValidationPattern']} tooltip={{ icon: <InfoFieldModal text={<p>{t('repository:user_message_pattern_info')}</p>} /> }} />
            <Input
                label={t('repository:invalid_user_message_hint')}
                name={['settings', 'invalidCommentMessage']}
                rules={[{ required: true, message: t('common:validation.required') }]}
                tooltip={{ icon: <InfoFieldModal text={<p>{t('repository:invalid_user_message_hint_info')}</p>} /> }}
            />
        </>
    )
}
