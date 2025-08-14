import React from 'react'
import { StarOutlined, StarFilled } from '@ant-design/icons'

interface ContextMenuProps {
    visible: boolean
    x: number
    y: number
    isFavorite: boolean
    onToggleFavorite: () => void
    onSelectNode: () => void
}

export const ContextMenu: React.FC<ContextMenuProps> = ({
    visible,
    x,
    y,
    isFavorite,
    onToggleFavorite,
    onSelectNode
}) => {
    if (!visible) return null

    const menuItems = [
        {
            key: 'favorite',
            icon: isFavorite ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />,
            label: isFavorite ? 'Remove from Favorites' : 'Add to Favorites',
            onClick: onToggleFavorite
        },
        {
            key: 'select',
            label: 'Select Node',
            onClick: onSelectNode
        }
    ]

    return (
        <div
            data-context-menu="true"
            style={{
                position: 'fixed',
                top: y,
                left: x,
                zIndex: 1000,
                backgroundColor: 'white',
                border: '1px solid #d9d9d9',
                borderRadius: '6px',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
                padding: '4px 0',
                minWidth: '150px'
            }}
        >
            {menuItems.map(item => (
                <div
                    key={item.key}
                    data-context-menu="true"
                    onClick={(e) => {
                        e.stopPropagation()
                        item.onClick()
                    }}
                    onMouseEnter={(e) => {
                        e.currentTarget.style.backgroundColor = '#f5f5f5'
                    }}
                    onMouseLeave={(e) => {
                        e.currentTarget.style.backgroundColor = 'transparent'
                    }}
                    style={{
                        padding: '8px 16px',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                        fontSize: '14px',
                        userSelect: 'none'
                    }}
                >
                    {item.icon}
                    {item.label}
                </div>
            ))}
        </div>
    )
}
