import { useState } from 'react'
import { AutoComplete, Button, Space, Tooltip } from 'antd'
import { MoreOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { FsNode } from '../../types/files'
import { ProjectFolderPicker } from './ProjectFolderPicker'

/** The folders of a project, as the paths a new file or folder can be placed under. */
export const projectFolders = (files: FsNode[] | undefined): string[] => (files ?? [])
    .filter(node => node.type === 'folder')
    .map(node => node.path)
    .sort((left, right) => left.localeCompare(right))

interface ProjectFolderInputProps {
    value: string
    onChange: (value: string) => void
    /** Set by {@link FieldRow} so its label points at the input. */
    id?: string
    /** Existing folders of the project, offered while typing and browsable through the picker. */
    folders: string[]
    'data-testid'?: string
}

/**
 * A path inside the project: typed freely, completed from the folders that already exist, or picked from
 * the folder tree next to it.
 *
 * One field carries the whole path — what it shows is exactly where the item lands in the repository.
 */
export const ProjectFolderInput = ({
    value,
    onChange,
    folders,
    id,
    'data-testid': testId,
}: ProjectFolderInputProps) => {
    const { t } = useTranslation('repository')
    const [pickerOpen, setPickerOpen] = useState(false)

    return (
        <>
            <Space.Compact style={{ width: '100%' }}>
                <AutoComplete
                    allowClear
                    data-testid={testId}
                    {...(id ? { id } : {})}
                    onChange={next => onChange(next ?? '')}
                    options={folders.map(folder => ({ value: folder }))}
                    showSearch={{ filterOption: (input, option) => String(option?.value ?? '').toLowerCase().includes(input.toLowerCase()) }}
                    style={{ width: '100%' }}
                    value={value}
                />
                <Tooltip title={t('browser.folder_picker.open')}>
                    <Button
                        data-testid={testId && `${testId}-picker`}
                        icon={<MoreOutlined />}
                        onClick={() => setPickerOpen(true)}
                    />
                </Tooltip>
            </Space.Compact>
            <ProjectFolderPicker
                folders={folders}
                onClose={() => setPickerOpen(false)}
                onSelect={onChange}
                open={pickerOpen}
            />
        </>
    )
}
