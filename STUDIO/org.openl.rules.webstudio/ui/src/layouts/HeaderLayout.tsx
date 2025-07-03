import React, { useContext } from 'react'
import { Header } from '../containers/Header'
import { SystemContext } from '../contexts'
import { useScript } from '../hooks'

export const HeaderLayout = () => {
    const { systemSettings } = useContext(SystemContext)
    useScript(systemSettings?.scripts)

    return (
        <Header />
    )
}