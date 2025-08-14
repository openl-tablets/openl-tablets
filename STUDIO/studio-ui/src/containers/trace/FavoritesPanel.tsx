import React from 'react'
import { Button, Typography, Tooltip } from 'antd'
import { StarFilled } from '@ant-design/icons'
import { TraceNodeApi } from './types'

interface FavoritesPanelProps {
    favoriteNodes: TraceNodeApi[]
    selectedKeys: React.Key[]
    expandToNode: (nodeKey: number) => void
    toggleFavorite: (nodeKey: number) => void
}

export const FavoritesPanel: React.FC<FavoritesPanelProps> = ({
    favoriteNodes,
    selectedKeys,
    expandToNode,
    toggleFavorite
}) => (
    <>
        <div style={{ width: 10, cursor: 'ew-resize', background: 'gray', border: '5px solid white' }} />
        <div style={{ minWidth: 200, width: '15%', overflow: 'auto', margin: 5 }}>
            <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8 }}>
                <Typography.Text strong>Favorites ({favoriteNodes.length})</Typography.Text>
            </div>
            <div style={{ paddingTop: 8, height: 'calc(100vh - 120px)', overflow: 'auto' }}>
                {favoriteNodes.length > 0 ? (
                    <div>
                        {favoriteNodes.map(node => (
                            <Tooltip 
                                key={node.key}
                                mouseEnterDelay={0.5}
                                overlayClassName="trace-tooltip"
                                placement="topLeft"
                                title={node.title}
                            >
                                <div 
                                    onClick={() => expandToNode(node.key)}
                                    style={{ 
                                        padding: '8px', 
                                        border: '1px solid #d9d9d9', 
                                        marginBottom: '4px', 
                                        borderRadius: '4px',
                                        cursor: 'pointer',
                                        backgroundColor: selectedKeys.includes(node.key) ? '#e6f7ff' : 'white'
                                    }}
                                >
                                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                        <span style={{ fontSize: '12px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                            {node.title}
                                        </span>
                                        <Button 
                                            icon={<StarFilled style={{ color: '#faad14' }} />} 
                                            size="small" 
                                            type="text"
                                            onClick={(e) => {
                                                e.stopPropagation()
                                                toggleFavorite(node.key)
                                            }}
                                        />
                                    </div>
                                </div>
                            </Tooltip>
                        ))}
                    </div>
                ) : (
                    <Typography.Text type="secondary">No favorites yet. Right-click on tree nodes to add them.</Typography.Text>
                )}
            </div>
        </div>
    </>
)
