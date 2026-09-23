import { useState } from 'react'
import { Input } from 'antd'
import { MoreOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { CompactField } from './CompactField'
import { RepoFolderPicker } from './RepoFolderPicker'

interface RepoFolderInputProps {
    value: string
    onChange: (value: string) => void
    /** Set by {@link FieldRow} so its label points at the input. */
    id?: string
    /** Set by {@link FieldRow} from the field label. */
    name?: string
    /** Design repository whose folders the picker browses. */
    repositoryId: string
    placeholder?: string
    'data-testid'?: string
}

/**
 * A path inside a design repository: typed freely, or picked from the repository's folder tree next to it.
 *
 * The project-side twin is {@link ProjectFolderInput}; both put the whole path in one field, so what the
 * field shows is exactly where the project lands.
 */
export const RepoFolderInput = ({
    value,
    onChange,
    repositoryId,
    placeholder,
    id,
    name,
    'data-testid': testId,
}: RepoFolderInputProps) => {
    const { t } = useTranslation('repository')
    const [pickerOpen, setPickerOpen] = useState(false)

    return (
        <>
            <CompactField
                action={{
                    icon: <MoreOutlined />,
                    title: t('browser.folder_picker.open'),
                    onClick: () => setPickerOpen(true),
                    'data-testid': testId && `${testId}-picker`,
                }}
            >
                <Input
                    data-testid={testId}
                    {...(id ? { id } : {})}
                    {...(name ? { name } : {})}
                    onChange={event => onChange(event.target.value)}
                    placeholder={placeholder}
                    value={value}
                />
            </CompactField>
            <RepoFolderPicker
                onClose={() => setPickerOpen(false)}
                onSelect={onChange}
                open={pickerOpen}
                repositoryId={repositoryId}
            />
        </>
    )
}
