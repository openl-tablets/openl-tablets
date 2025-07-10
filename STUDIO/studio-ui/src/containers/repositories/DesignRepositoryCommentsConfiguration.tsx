import React from 'react'
import { useTranslation } from 'react-i18next'
import { Checkbox, Input } from '../../components'
import { Form, Divider } from 'antd'
import {
    InvalidUserMessageHintModal,
    MessageArchiveProjectModal, MessageCopyProjectModal,
    MessageCreateProjectModal,
    MessageEraseProjectModal, MessageRestoreFromOldVersionModal,
    MessageRestoreProjectModal,
    MessageSaveProjectModal,
    MessageTemplateModal,
    UserMessagePatternModal,
} from './InfoFieldModals'

export const DesignRepositoryCommentsConfiguration = () => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const isCustomCommentsEnabled = Form.useWatch(['settings', 'useCustomComments'], form)

    return (
        <>
            <Divider orientation="left">{t('repository:comments')}</Divider>
            <Checkbox label={t('repository:customize_comments')} name={['settings', 'useCustomComments']} />
            {isCustomCommentsEnabled && (
                <>
                    <Input
                        label={t('repository:message_template')}
                        name={['settings', 'commentTemplate']}
                        rules={[{ required: true, message: t('common:validation.required') }]}
                        tooltip={{ icon: MessageTemplateModal }}
                    />
                    <Input label={t('repository:user_message_pattern')} name={['settings', 'commentValidationPattern']} tooltip={{ icon: UserMessagePatternModal }} />
                    <Input
                        label={t('repository:invalid_user_message_hint')}
                        name={['settings', 'invalidCommentMessage']}
                        rules={[{ required: true, message: t('common:validation.required') }]}
                        tooltip={{ icon: InvalidUserMessageHintModal }}
                    />
                    <Divider orientation="left">{t('repository:user_message_templates')}</Divider>
                    <Input label={t('repository:save_project')} name={['settings', 'defaultCommentSave']} tooltip={{ icon: MessageSaveProjectModal }} />
                    <Input label={t('repository:create_project')} name={['settings', 'defaultCommentCreate']} tooltip={{ icon: MessageCreateProjectModal }} />
                    <Input label={t('repository:archive_project')} name={['settings', 'defaultCommentArchive']} tooltip={{ icon: MessageArchiveProjectModal }} />
                    <Input label={t('repository:restore_project')} name={['settings', 'defaultCommentRestore']} tooltip={{ icon: MessageRestoreProjectModal }} />
                    <Input label={t('repository:erase_project')} name={['settings', 'defaultCommentErase']} tooltip={{ icon: MessageEraseProjectModal }} />
                    <Input label={t('repository:copy_project')} name={['settings', 'defaultCommentCopiedFrom']} tooltip={{ icon: MessageCopyProjectModal }} />
                    <Input label={t('repository:restore_from_old_version')} name={['settings', 'defaultCommentRestoredFrom']} tooltip={{ icon: MessageRestoreFromOldVersionModal }} />
                </>
            )}
        </>
    )
}
