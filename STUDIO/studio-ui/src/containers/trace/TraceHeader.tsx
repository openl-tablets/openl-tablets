import React from 'react'
import { Button, Checkbox, Typography, Badge } from 'antd'
import { StarOutlined } from '@ant-design/icons'

interface TraceHeaderProps {
    showDetailed: boolean
    setShowDetailed: (value: boolean) => void
    showFavorites: boolean
    setShowFavorites: (value: boolean) => void
    favoritesCount: number
    clearFavorites: () => void
}

export const TraceHeader: React.FC<TraceHeaderProps> = ({
    showDetailed,
    setShowDetailed,
    showFavorites,
    setShowFavorites,
    favoritesCount,
    clearFavorites
}) => (
    <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
        <Typography.Text strong>Trace Tree</Typography.Text>
        <Checkbox 
            checked={showDetailed} 
            onChange={(e) => setShowDetailed(e.target.checked)}
            style={{ marginLeft: 'auto' }}
        >
            Detailed
        </Checkbox>
        <Typography.Text>Hide ms</Typography.Text>
        <Button 
            icon={<StarOutlined />} 
            onClick={() => setShowFavorites(!showFavorites)}
            size="small"
        >
            <Badge count={favoritesCount} size="small">
                Favorites
            </Badge>
        </Button>
        <Button onClick={clearFavorites} size="small">Clear</Button>
    </div>
)
