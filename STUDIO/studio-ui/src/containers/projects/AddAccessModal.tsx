import { useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, AutoComplete, Modal, Select, Spin } from 'antd'
import { TeamOutlined, UserAddOutlined, UserOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useSharedStyles } from './sharedStyles'
import { FieldRow } from '../../components/FieldRow'
import { Role } from '../../constants'
import { searchProjectAclSubjects, setProjectAcl } from '../../services/acl'
import { SystemContext } from '../../contexts'

const SUBJECT_SEARCH_DELAY_MS = 300
const MIN_SUBJECT_SEARCH_LENGTH = 2
const SUBJECT_PAGE_SIZE = 10

const useStyles = createStyles(({ css, token }) => ({
    kinds: css`
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
    `,
    kind: css`
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        height: 36px;
        color: ${token.colorTextTertiary};
    `,
    kindActive: css`
        color: ${token.colorText};
    `,
}))

interface AddAccessModalProps {
    open: boolean
    projectId: string
    projectName: string
    onClose: () => void
    onGranted: () => void
}

/**
 * Grant a role to a user or group on a project. The subject-type toggle only tunes the label and
 * placeholder — the backend resolves the subject and enforces the change.
 */
export const AddAccessModal = ({ open, projectId, projectName, onClose, onGranted }: AddAccessModalProps) => {
    const { t } = useTranslation('repository')
    const { isGroupsManagementEnabled } = useContext(SystemContext)
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [kind, setKind] = useState<'user' | 'group'>('user')
    const [sid, setSid] = useState('')
    const [role, setRole] = useState<Role>(Role.VIEWER)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [suggestions, setSuggestions] = useState<string[]>([])
    const [searching, setSearching] = useState(false)
    const suggestionGeneration = useRef(0)

    const resetSubjectSearch = useCallback(() => {
        setSid('')
        setSuggestions([])
        setSearching(false)
        suggestionGeneration.current += 1
    }, [])

    useEffect(() => {
        if (open) {
            setKind('user')
            setRole(Role.VIEWER)
            setError(null)
            resetSubjectSearch()
        }
    }, [open, resetSubjectSearch])

    useEffect(() => {
        if (!isGroupsManagementEnabled && kind === 'group') {
            setKind('user')
            resetSubjectSearch()
        }
    }, [isGroupsManagementEnabled, kind, resetSubjectSearch])

    useEffect(() => {
        if (!open) {
            return
        }
        const search = sid.trim()
        const generation = suggestionGeneration.current + 1
        suggestionGeneration.current = generation
        if (search.length < MIN_SUBJECT_SEARCH_LENGTH) {
            setSuggestions([])
            setSearching(false)
            return
        }
        const timeoutId = window.setTimeout(() => {
            setSearching(true)
            searchProjectAclSubjects(projectId, kind === 'user', search, SUBJECT_PAGE_SIZE)
                .then(result => {
                    if (suggestionGeneration.current === generation) {
                        setSuggestions(result)
                    }
                })
                .catch(() => {
                    if (suggestionGeneration.current === generation) {
                        setSuggestions([])
                    }
                })
                .finally(() => {
                    if (suggestionGeneration.current === generation) {
                        setSearching(false)
                    }
                })
        }, SUBJECT_SEARCH_DELAY_MS)
        return () => window.clearTimeout(timeoutId)
    }, [kind, open, projectId, sid])

    const subjectOptions = useMemo(
        () => suggestions.map(value => ({ value, label: value })),
        [suggestions]
    )

    const grant = async () => {
        const trimmed = sid.trim()
        if (submitting || !trimmed) {
            return
        }
        setSubmitting(true)
        setError(null)
        try {
            await setProjectAcl(projectId, trimmed, role, kind === 'user')
            onGranted()
            onClose()
        } catch (e) {
            setError(errorMessage(e))
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <Modal
            destroyOnHidden
            confirmLoading={submitting}
            okButtonProps={{ 'data-testid': 'add-access-submit', disabled: !sid.trim(), icon: <UserAddOutlined /> }}
            okText={t('browser.access.grant')}
            onCancel={onClose}
            onOk={grant}
            open={open}
            title={<><UserAddOutlined /> {t('browser.access.dialog_title')}</>}
        >
            <p style={{ marginTop: 0, color: 'inherit' }}>{t('browser.access.dialog_desc', { name: projectName })}</p>
            <FieldRow alignTop label={t('browser.access.subject_type')} labelWidth={110}>
                <div className={styles.kinds} style={{ gridTemplateColumns: isGroupsManagementEnabled ? '1fr 1fr' : '1fr' }}>
                    <button className={cx(shared.selectableCard, styles.kind, kind === 'user' && cx(shared.selectedCard, styles.kindActive))} onClick={() => setKind('user')} type="button">
                        <UserOutlined /> {t('browser.access.type_user')}
                    </button>
                    {isGroupsManagementEnabled && (
                        <button className={cx(shared.selectableCard, styles.kind, kind === 'group' && cx(shared.selectedCard, styles.kindActive))} onClick={() => setKind('group')} type="button">
                            <TeamOutlined /> {t('browser.access.type_group')}
                        </button>
                    )}
                </div>
            </FieldRow>
            <FieldRow
                alignTop
                required
                label={kind === 'user' ? t('browser.access.subject_user_label') : t('browser.access.subject_group_label')}
                labelWidth={110}
            >
                <AutoComplete
                    autoFocus
                    data-testid="add-access-sid"
                    filterOption={false}
                    notFoundContent={searching ? <Spin size="small" /> : null}
                    onChange={value => setSid(value)}
                    options={subjectOptions}
                    placeholder={kind === 'user' ? t('browser.access.subject_user_ph') : t('browser.access.subject_group_ph')}
                    style={{ width: '100%' }}
                    value={sid}
                    onInputKeyDown={event => {
                        if (event.key === 'Enter') {
                            void grant()
                        }
                    }}
                />
            </FieldRow>
            <FieldRow label={t('browser.access.role')} labelWidth={110}>
                <Select
                    data-testid="add-access-role"
                    onChange={setRole}
                    options={Object.values(Role).map(value => ({ value, label: t(`browser.access.role_${value}`) }))}
                    style={{ width: '100%' }}
                    value={role}
                />
            </FieldRow>
            {error && <Alert showIcon data-testid="add-access-error" style={{ marginTop: 12 }} title={error} type="error" />}
        </Modal>
    )
}
