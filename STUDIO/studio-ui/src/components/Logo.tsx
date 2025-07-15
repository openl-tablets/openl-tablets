import React from 'react'

interface LogoProps {
    width?: number | string
    height?: number | string
}

const Logo: React.FC<LogoProps> = ({ width = 24, height = 24 }) => (
    <svg fill="none" height={height} viewBox="-1066 -1200 2132 2400" width={width} xmlns="http://www.w3.org/2000/svg">
        <path d="M0 1000 866 500 866-500 0-1000-866-500-866 500Z" stroke="#062261" strokeLinejoin="round" strokeWidth="400" />
        <path d="M0 1000 l866-500 0-500-866 500-866-500 0 500z" fill="#1763C6" />
        <path d="M0 500 l866-500 0-500-866 500-866-500 0 500z" fill="#0F4093" />
        <path d="M0-1000-866-500 0 0 866-500Z" fill="#062462" />
        <path d="M0 500 433 250 433-250 0-500-433-250-433 250Z M0 1000 866 500 866-500 0-1000-866-500-866 500Z M0 1000 L0 0-866-500M866-500 0 0" stroke="white" strokeLinejoin="round" strokeWidth="75" />
    </svg>
)

export default Logo 