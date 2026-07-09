import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, AutoComplete, Button, Flex, Modal, Select } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { getTagTypes, updateProjectTags, type TagType } from '../../services/repositories'
import { useCommitInfoGuard } from '../../hooks'

interface TagRow {
    id: number
    name: string
    value: string
}

interface TagsModalProps {
    open: boolean
    projectId: string | null
    initialTags: Record<string, string>
    onClose: () => void
    onSaved: () => void
}

const toRows = (tags: Record<string, string>): TagRow[] =>
    Object.entries(tags).map(([name, value], index) => ({ id: index, name, value }))

const containsInput = (input: string, option?: { value: string }): boolean =>
    (option?.value ?? '').toLowerCase().includes(input.toLowerCase())

/**
 * Assign tags to a project. Tag names and their values are offered from the configured tag-type
 * catalog: a fixed-value type restricts its values to a dropdown, an extensible one also allows custom
 * values, and names outside the catalog stay free-form. The backend replaces the whole set on save;
 * rows with a blank name are dropped. On success the caller refreshes the project.
 */
export const TagsModal = ({ open, projectId, initialTags, onClose, onSaved }: TagsModalProps) => {
    const { t } = useTranslation('repository')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const [rows, setRows] = useState<TagRow[]>([])
    const [nextId, setNextId] = useState(0)
    const [tagTypes, setTagTypes] = useState<TagType[]>([])
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (open) {
            const initial = toRows(initialTags)
            setRows(initial)
            setNextId(initial.length)
            setError(null)
            getTagTypes().then(setTagTypes).catch(() => setTagTypes([]))
        }
    }, [open, initialTags])

    const setRow = (id: number, patch: Partial<TagRow>) =>
        setRows(prev => prev.map(row => (row.id === id ? { ...row, ...patch } : row)))

    const addRow = () => {
        setRows(prev => [...prev, { id: nextId, name: '', value: '' }])
        setNextId(id => id + 1)
    }

    const removeRow = (id: number) => setRows(prev => prev.filter(row => row.id !== id))

    const save = async () => {
        if (!projectId) {
            return
        }
        const tags: Record<string, string> = Object.fromEntries(
            rows.map(row => [row.name.trim(), row.value.trim()]).filter(([name]) => name)
        )
        await runWithCommitInfo(async () => {
            setSaving(true)
            setError(null)
            try {
                await updateProjectTags(projectId, tags)
                onSaved()
            } catch (e) {
                setError(errorMessage(e))
            } finally {
                setSaving(false)
            }
        })
    }

    return (
        <>
            <Modal
                destroyOnHidden
                onCancel={onClose}
                open={open}
                title={t('browser.tags.title')}
                footer={[
                    <Button key="cancel" data-testid="tags-cancel" disabled={saving} onClick={onClose}>
                        {t('browser.tags.cancel')}
                    </Button>,
                    <Button key="save" data-testid="tags-save" loading={saving} onClick={save} type="primary">
                        {t('browser.tags.save')}
                    </Button>,
                ]}
            >
                <Flex vertical gap={8}>
                    {rows.map(row => {
                        const type = tagTypes.find(tagType => tagType.name === row.name)
                        return (
                            <Flex key={row.id} gap={8}>
                                <AutoComplete
                                    data-testid={`tag-name-${row.id}`}
                                    filterOption={containsInput}
                                    onChange={value => setRow(row.id, { name: value })}
                                    options={tagTypes.map(tagType => ({ value: tagType.name }))}
                                    placeholder={t('browser.tags.name')}
                                    style={{ flex: 1 }}
                                    value={row.name}
                                />
                                {type && !type.extensible ? (
                                    <Select
                                        showSearch
                                        allowClear={type.nullable}
                                        data-testid={`tag-value-${row.id}`}
                                        onChange={value => setRow(row.id, { value: value ?? '' })}
                                        options={type.values.map(value => ({ label: value, value }))}
                                        placeholder={t('browser.tags.value')}
                                        style={{ flex: 1 }}
                                        value={row.value || undefined}
                                    />
                                ) : (
                                    <AutoComplete
                                        data-testid={`tag-value-${row.id}`}
                                        filterOption={containsInput}
                                        onChange={value => setRow(row.id, { value })}
                                        options={(type?.values ?? []).map(value => ({ value }))}
                                        placeholder={t('browser.tags.value')}
                                        style={{ flex: 1 }}
                                        value={row.value}
                                    />
                                )}
                                <Button
                                    aria-label={t('browser.tags.remove')}
                                    data-testid={`tag-remove-${row.id}`}
                                    icon={<DeleteOutlined />}
                                    onClick={() => removeRow(row.id)}
                                />
                            </Flex>
                        )
                    })}
                    <Button block data-testid="tag-add" icon={<PlusOutlined />} onClick={addRow} type="dashed">
                        {t('browser.tags.add')}
                    </Button>
                    {error && <Alert showIcon data-testid="tags-error" title={error} type="error" />}
                </Flex>
            </Modal>
            {commitInfoModal}
        </>
    )
}
