import React from 'react'
import { Link } from 'react-router-dom'

const noMatchStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center'
}

export const NoMatch = () => {
    return (
        <div style={noMatchStyle}>
            <h2>Nothing to see here!</h2>
            <p>
                <Link to="/">Go to the home page</Link>
            </p>
        </div>
    )
}