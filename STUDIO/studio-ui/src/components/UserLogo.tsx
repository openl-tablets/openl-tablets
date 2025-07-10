import React, { FC } from 'react'
import { UserOutlined } from '@ant-design/icons'
import './UserLogo.scss'

interface UserLogoProps {
    onClick?: () => void
}

export const UserLogo: FC<UserLogoProps> = ({ onClick }) => {
    return (
        <div className="user-logo">
            <UserOutlined onClick={onClick} />
        </div>
    )
}
