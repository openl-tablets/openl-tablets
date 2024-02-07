import React, { useMemo } from 'react'
import { useParams } from 'react-router-dom'

const iFrameStyle: React.CSSProperties = {
    border: 'none',
    width: '100%',
    height: '100%',
    minHeight: '100vh',
}

export const IFrame = () => {
    const { ...params  } = useParams()

    const url = useMemo(() => {
        return 'http://localhost:8080/webstudio/faces/' + params['*']
    }, [ params ])

    return (
        <div>
            <iframe src={url} style={iFrameStyle} />
        </div>
    )
}