import { useTranslation } from 'react-i18next'
import { Checkbox, Input } from '../../components'
import { Form, Divider } from 'antd'
import { MAX_COMMENT_LENGTH } from '../../utils/repositoryConfig'
import {
    InvalidUserMessageHintModal,
    MessageCopyProjectModal,
    MessageCreateProjectModal,
    MessageRestoreFromOldVersionModal,
    MessageSaveProjectModal,
    UserMessagePatternModal,
} from './InfoFieldModals'

export const DesignRepositoryCommentsConfiguration = () => {
    const { t } = useTranslation()
    // A template becomes a comment, so it cannot be longer than one.
    const templateRules = [{
        max: MAX_COMMENT_LENGTH,
        message: t('repository:user_message_template_too_long', { max: MAX_COMMENT_LENGTH }),
    }]
    const form = Form.useFormInstance()
    const isCustomCommentsEnabled = Form.useWatch(['settings', 'useCustomComments'], form)

    return (
        <>
            <Divider titlePlacement="start">{t('repository:comments')}</Divider>
            <Checkbox label={t('repository:customize_comments')} name={['settings', 'useCustomComments']} />
            {isCustomCommentsEnabled && (
                <>
                    <Input label={t('repository:user_message_pattern')} name={['settings', 'commentValidationPattern']} tooltip={{ icon: UserMessagePatternModal }} />
                    <Input
                        label={t('repository:invalid_user_message_hint')}
                        name={['settings', 'invalidCommentMessage']}
                        rules={[{ required: true, message: t('common:validation.required') }]}
                        tooltip={{ icon: InvalidUserMessageHintModal }}
                    />
                    <Divider titlePlacement="start">{t('repository:user_message_templates')}</Divider>
                    <Input label={t('repository:save_project')} name={['settings', 'defaultCommentSave']} rules={templateRules} tooltip={{ icon: MessageSaveProjectModal }} />
                    <Input label={t('repository:create_project')} name={['settings', 'defaultCommentCreate']} rules={templateRules} tooltip={{ icon: MessageCreateProjectModal }} />
                    <Input label={t('repository:copy_project')} name={['settings', 'defaultCommentCopiedFrom']} rules={templateRules} tooltip={{ icon: MessageCopyProjectModal }} />
                    <Input label={t('repository:restore_from_old_version')} name={['settings', 'defaultCommentRestoredFrom']} rules={templateRules} tooltip={{ icon: MessageRestoreFromOldVersionModal }} />
                </>
            )}
        </>
    )
}
