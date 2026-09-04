import { useCallback, useEffect, useState, type Key } from 'react'
import { Empty, Modal, Spin, Tree, type TreeDataNode } from 'antd'
import { FolderOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useStyles } from './folderPicker.styles'
import { useSharedStyles } from './sharedStyles'

interface FolderPickerDialogProps {
    open: boolean
    /** The folders to browse; null while they are still loading. */
    treeData: TreeDataNode[] | null
    /** Loads the children of a node, for trees that fill in level by level. */
    loadData?: (node: TreeDataNode) => Promise<void>
    /** Called with the path of the picked folder. */
    onSelect: (path: string) => void
    onClose: () => void
    'data-testid'?: string
}

/**
 * The dialog behind every folder picker: the folder tree styled like the Files tab, with a loading and an
 * empty state. Where the folders come from — a repository read level by level or a project already loaded —
 * is the caller's business.
 */
export const FolderPickerDialog = ({
    open,
    treeData,
    loadData,
    onSelect,
    onClose,
    'data-testid': testId = 'folder-tree',
}: FolderPickerDialogProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [selected, setSelected] = useState<string | null>(null)

    useEffect(() => {
        if (open) {
            setSelected(null)
        }
    }, [open])

    const confirm = () => {
        if (selected !== null) {
            // Paths are relative to the repository or the project, so drop any leading slash.
            onSelect(selected.replace(/^\/+/, ''))
        }
        onClose()
    }

    return (
        <Modal
            destroyOnHidden
            okButtonProps={{ disabled: selected === null }}
            okText={t('browser.folder_picker.select')}
            onCancel={onClose}
            onOk={confirm}
            open={open}
            title={t('browser.folder_picker.open')}
        >
            {/* The dialog hugs the tree: only a very deep one starts scrolling. */}
            <div className={styles.tree} data-testid={testId} style={{ maxHeight: 360, overflow: 'auto' }}>
                {treeData === null && <div style={{ padding: 24, textAlign: 'center' }}><Spin /></div>}
                {treeData !== null && treeData.length === 0 && (
                    <Empty description={t('browser.folder_picker.empty')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                )}
                {treeData !== null && treeData.length > 0 && (
                    <Tree
                        blockNode
                        showIcon
                        onSelect={keys => setSelected(keys.length ? String(keys[0]) : null)}
                        selectedKeys={selected === null ? [] : [selected]}
                        treeData={treeData}
                        {...(loadData ? { loadData } : {})}
                    />
                )}
            </div>
        </Modal>
    )
}

/**
 * A folder row of the picker tree, drawn the way the Files tab draws its folders.
 *
 * The builder keeps its identity between renders, so callers can safely depend on it from an effect.
 */
export const useFolderNode = () => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    return useCallback((key: Key, name: string): TreeDataNode => ({
        key,
        icon: <FolderOutlined className={styles.folderIcon} />,
        title: <span className={styles.node}><span className={cx(shared.ellipsis, styles.name)}>{name}</span></span>,
    }), [cx, shared, styles])
}
