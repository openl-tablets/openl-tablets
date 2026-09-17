import { describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import { App as AntApp, Modal, notification as staticNotification } from 'antd'
import { modal, notification, PopupsBridge } from './popups'

describe('popups', () => {
    it('falls back to the static notification until the application mounts', () => {
        const success = vi.spyOn(staticNotification, 'success').mockImplementation(() => {})
        try {
            notification.success({ title: 'Saved' })
            expect(success).toHaveBeenCalledWith({ title: 'Saved' })
        } finally {
            success.mockRestore()
        }
    })

    it('falls back to the static dialogs until the application mounts', () => {
        const confirm = vi.spyOn(Modal, 'confirm').mockImplementation(() => ({ destroy: vi.fn(), update: vi.fn() }))
        try {
            modal.confirm({ title: 'Sure?' })
            expect(confirm).toHaveBeenCalledWith({ title: 'Sure?' })
        } finally {
            confirm.mockRestore()
        }
    })

    it('shows the notice through the mounted application instead of the static holder', async () => {
        const success = vi.spyOn(staticNotification, 'success').mockImplementation(() => {})
        try {
            const { unmount } = render(
                <AntApp>
                    <PopupsBridge />
                </AntApp>
            )
            act(() => notification.success({ title: 'Branch created' }))
            expect(await screen.findByText('Branch created')).toBeInTheDocument()
            expect(success).not.toHaveBeenCalled()

            unmount()
            notification.success({ title: 'After unmount' })
            expect(success).toHaveBeenCalledWith({ title: 'After unmount' })
        } finally {
            success.mockRestore()
        }
    })

    it('opens the dialog through the mounted application', async () => {
        const confirm = vi.spyOn(Modal, 'confirm').mockImplementation(() => ({ destroy: vi.fn(), update: vi.fn() }))
        try {
            render(
                <AntApp>
                    <PopupsBridge />
                </AntApp>
            )
            act(() => {
                modal.confirm({ title: 'Delete the project?' })
            })
            // the dialog names its title twice: once for the eye, once for assistive technology
            expect(await screen.findAllByText('Delete the project?')).not.toHaveLength(0)
            expect(confirm).not.toHaveBeenCalled()
        } finally {
            confirm.mockRestore()
        }
    })
})
