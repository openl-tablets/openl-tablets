import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Maven } from './Maven'
import { apiCall } from 'services'
import { notifyLoadFailure } from 'services/apiCall'

vi.mock('services', () => ({ apiCall: vi.fn() }))
vi.mock('services/apiCall', async importOriginal => ({
    ...await importOriginal<typeof import('services/apiCall')>(),
    notifyLoadFailure: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en', resolvedLanguage: 'en' }
    return { useTranslation: () => ({ t, i18n }) }
})

const mockConfirm = vi.fn()
const mockSuccess = vi.fn()
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        ...actual,
        Modal: { ...actual.Modal, confirm: (options: unknown) => mockConfirm(options) },
        notification: { ...actual.notification, success: (options: unknown) => mockSuccess(options) },
    })
})

const SETTINGS_URL = '/admin/settings/maven'

/** Answers the read of the settings with the given ones, and every write with nothing. */
const settingsAre = (settings: object) => {
    vi.mocked(apiCall).mockImplementation(async (_url: string, params?: RequestInit) =>
        params?.method ? undefined : settings)
}

const urlField = () => screen.getByRole('textbox', { name: 'maven:url' })

/** Applies the form and confirms the warning that the application restarts. */
const apply = async () => {
    await userEvent.click(screen.getByRole('button', { name: 'common:btn.apply' }))
    expect(mockConfirm).toHaveBeenCalledWith(expect.objectContaining({
        title: 'maven:confirm_apply',
    }))
    await act(async () => mockConfirm.mock.calls[0]![0].onOk())
}

/** The body of the write the form sent. */
const patched = async () => {
    await waitFor(() => expect(apiCall).toHaveBeenCalledWith(SETTINGS_URL,
        expect.objectContaining({ method: 'PATCH' }), expect.anything()))
    const call = vi.mocked(apiCall).mock.calls.find(([, params]) => params?.method === 'PATCH')!
    return JSON.parse(call[1]!.body as string)
}

describe('Maven', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.spyOn(window, 'location', 'get').mockReturnValue({ ...window.location, reload: vi.fn() })
    })

    it('shows the repository the server answers, but not its password', async () => {
        settingsAre({
            url: 'https://nexus.example.com/repository/public',
            username: 'openl',
            password: { value: '', secret: true },
        })
        render(<Maven />)

        await waitFor(() => expect(urlField()).toHaveValue('https://nexus.example.com/repository/public'))
        expect(screen.getByRole('textbox', { name: 'maven:username' })).toHaveValue('openl')
        expect(screen.getByLabelText('maven:password')).toHaveValue('')
    })

    it('saves the repository as typed and keeps the password the server holds', async () => {
        settingsAre({ url: '', username: '', password: { value: '', secret: true } })
        render(<Maven />)
        await waitFor(() => expect(apiCall).toHaveBeenCalledWith(SETTINGS_URL, undefined, expect.anything()))

        await userEvent.type(urlField(), 'https://repo.maven.apache.org/maven2')
        await apply()

        expect(await patched()).toEqual({ url: 'https://repo.maven.apache.org/maven2', username: '' })
        await waitFor(() => expect(mockSuccess).toHaveBeenCalledWith(expect.objectContaining({
            description: 'maven:saved',
        })))
    })

    it.each(['not a url', 'ftp://repo.example.com/maven2'])('refuses %s, which Maven cannot read', async url => {
        settingsAre({ url: '' })
        render(<Maven />)
        await waitFor(() => expect(apiCall).toHaveBeenCalledTimes(1))

        await userEvent.type(urlField(), url)
        await apply()

        expect(await screen.findByText('maven:url_invalid')).toBeInTheDocument()
        expect(apiCall).toHaveBeenCalledTimes(1)
    })

    it('saves a repository on the file system', async () => {
        settingsAre({ url: '' })
        render(<Maven />)
        await waitFor(() => expect(apiCall).toHaveBeenCalledTimes(1))

        await userEvent.type(urlField(), 'file:///opt/maven-repository')
        await apply()

        expect((await patched()).url).toBe('file:///opt/maven-repository')
    })

    it('reports a configuration it cannot read', async () => {
        const failure = new Error('The server is not available')
        vi.mocked(apiCall).mockRejectedValue(failure)
        render(<Maven />)

        await waitFor(() => expect(notifyLoadFailure).toHaveBeenCalledWith('maven:load_failed', failure))
    })

    it('reports a configuration it cannot save', async () => {
        const failure = new Error('Forbidden')
        vi.mocked(apiCall).mockImplementation(async (_url: string, params?: RequestInit) => {
            if (params?.method) {
                throw failure
            }
            return { url: '' }
        })
        render(<Maven />)
        await waitFor(() => expect(apiCall).toHaveBeenCalledTimes(1))

        await userEvent.type(urlField(), 'https://repo.maven.apache.org/maven2')
        await apply()

        await waitFor(() => expect(notifyLoadFailure).toHaveBeenCalledWith('maven:save_failed', failure))
        expect(mockSuccess).not.toHaveBeenCalled()
    })
})
