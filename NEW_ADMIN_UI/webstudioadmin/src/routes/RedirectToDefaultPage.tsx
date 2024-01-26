import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

export const RedirectToDefaultPage = () => {
    const navigate = useNavigate()

    useEffect(() => {
        navigate('/system')
    }, [])

    return null
}