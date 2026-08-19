import { Button, Space } from 'antd'
import { CheckOutlined, CloseOutlined, EditOutlined } from '@ant-design/icons'

interface EditToolbarProps {
    editing: boolean
    saving: boolean
    onEdit: () => void
    onSave: () => void
    onCancel: () => void
    /** Prefix for the button test ids: `${testId}-edit`, `${testId}-save`, `${testId}-cancel`. */
    testId: string
    /** The button captions, so each panel keeps its own translation keys. */
    labels: { edit: string, save: string, cancel: string }
    /** Disables every button — used while an unrelated write (e.g. a migrate) is in flight for the same file. */
    disabled?: boolean | undefined
    /**
     * Disables the Edit button alone — used while the text a draft would be taken from is being replaced.
     * An edit already under way is untouched: it ends by its own Save or Cancel.
     */
    disabledEdit?: boolean | undefined
}

/**
 * The small Edit / Save + Cancel control the editable panels share. One affordance, one look, so the
 * Overview, Publish and other tabs of a project never drift into a zoo of button sizes or orders.
 */
export const EditToolbar = ({ editing, saving, onEdit, onSave, onCancel, testId, labels, disabled = false, disabledEdit = false }: EditToolbarProps) => (
    <Space size={8}>
        {editing
            ? (
                <>
                    <Button data-testid={`${testId}-save`} disabled={disabled} icon={<CheckOutlined />} loading={saving} onClick={onSave} size="small" type="primary">
                        {labels.save}
                    </Button>
                    <Button data-testid={`${testId}-cancel`} disabled={saving || disabled} icon={<CloseOutlined />} onClick={onCancel} size="small">
                        {labels.cancel}
                    </Button>
                </>
            )
            : (
                <Button data-testid={`${testId}-edit`} disabled={disabled || disabledEdit} icon={<EditOutlined />} onClick={onEdit} size="small">
                    {labels.edit}
                </Button>
            )}
    </Space>
)
