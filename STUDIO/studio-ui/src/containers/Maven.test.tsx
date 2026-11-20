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
    const t = (key: string, options?: { size?: string }) => options?.size ? `${key} ${options.size}` : key
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
const CACHE_URL = '/admin/settings/maven/cache'
const MEGABYTE = 1024 * 1024

/**
 * Answers the reads of the settings and of the size of the cache, and every write with nothing, or with the failure
 * a test gives for writes.
 */
const serverAnswers = (settings: object, cacheSizes: number[] = [0], writeFailure?: Error) => {
    const sizes = [...cacheSizes]
    vi.mocked(apiCall).mockImplementation(async (url: string, params?: RequestInit) => {
        if (params?.method) {
            if (writeFailure) {
                throw writeFailure
            }
            return undefined
        }
        return url === CACHE_URL ? { size: sizes.length > 1 ? sizes.shift() : sizes[0] } : settings
    })
}

const urlField = () => screen.getByRole('textbox', { name: 'maven:url' })
const writes = (method: string) => vi.mocked(apiCall).mock.calls.filter(([, params]) => params?.method === method)

/** Waits until the screen has read the settings and the size of the cache. */
const loaded = async () => {
    await waitFor(() => expect(apiCall).toHaveBeenCalledWith(SETTINGS_URL, undefined, expect.anything()))
    await screen.findByText(/maven:cache_size /)
}

/** Applies the form and confirms the warning that the application restarts. */
const apply = async () => {
    await userEvent.click(screen.getByRole('button', { name: 'common:btn.apply' }))
    expect(mockConfirm).toHaveBeenCalledWith(expect.objectContaining({ title: 'maven:confirm_apply' }))
    await act(async () => mockConfirm.mock.lastCall![0].onOk())
}

/** The body of the write the form sent. */
const patched = async () => {
    await waitFor(() => expect(writes('PATCH')).toHaveLength(1))
    return JSON.parse(writes('PATCH')[0]![1]!.body as string)
}

describe('Maven', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.spyOn(window, 'location', 'get').mockReturnValue({ ...window.location, reload: vi.fn() })
    })

    it('shows the repository the server answers, but not its password', async () => {
        serverAnswers({
            url: 'https://nexus.example.com/repository/public',
            username: 'openl',
            password: { value: '', secret: true },
            cleanupPeriod: 30,
            cleanupThreshold: 1024,
        })
        render(<Maven />)

        // The number fields take their values a moment after the text fields
        await waitFor(() => {
            expect(urlField()).toHaveValue('https://nexus.example.com/repository/public')
            expect(screen.getByRole('spinbutton', { name: /^maven:cleanup_period/ })).toHaveValue('30')
            expect(screen.getByRole('spinbutton', { name: /^maven:cleanup_threshold/ })).toHaveValue('1024')
        })
        expect(screen.getByRole('textbox', { name: 'maven:username' })).toHaveValue('openl')
        expect(screen.getByLabelText('maven:password')).toHaveValue('')
    })

    it('saves the repository as typed and keeps the password the server holds', async () => {
        serverAnswers({ url: '', username: '', password: { value: '', secret: true } })
        render(<Maven />)
        await loaded()

        await userEvent.type(urlField(), 'https://repo.maven.apache.org/maven2')
        await apply()

        expect(await patched()).toEqual({ url: 'https://repo.maven.apache.org/maven2', username: '' })
        await waitFor(() => expect(mockSuccess).toHaveBeenCalledWith(expect.objectContaining({
            description: 'maven:saved',
        })))
    })

    it('saves the cleanup period and threshold', async () => {
        serverAnswers({ url: '', cleanupPeriod: 30, cleanupThreshold: 1024 })
        render(<Maven />)
        await loaded()

        const period = screen.getByRole('spinbutton', { name: /^maven:cleanup_period/ })
        await userEvent.clear(period)
        await userEvent.type(period, '7')
        const threshold = screen.getByRole('spinbutton', { name: /^maven:cleanup_threshold/ })
        await userEvent.clear(threshold)
        await userEvent.type(threshold, '0')
        await apply()

        expect(await patched()).toEqual(expect.objectContaining({ cleanupPeriod: 7, cleanupThreshold: 0 }))
    })

    it('refuses a negative cleanup period', async () => {
        serverAnswers({ url: '', cleanupPeriod: 30 })
        render(<Maven />)
        await loaded()

        const period = screen.getByRole('spinbutton', { name: /^maven:cleanup_period/ })
        await userEvent.clear(period)
        await userEvent.type(period, '-1')
        await apply()

        expect(await screen.findByText('maven:not_negative')).toBeInTheDocument()
        expect(writes('PATCH')).toHaveLength(0)
    })

    it.each(['not a url', 'ftp://repo.example.com/maven2'])('refuses %s, which Maven cannot read', async url => {
        serverAnswers({ url: '' })
        render(<Maven />)
        await loaded()

        await userEvent.type(urlField(), url)
        await apply()

        expect(await screen.findByText('maven:url_invalid')).toBeInTheDocument()
        expect(writes('PATCH')).toHaveLength(0)
    })

    it('saves a repository on the file system', async () => {
        serverAnswers({ url: '' })
        render(<Maven />)
        await loaded()

        await userEvent.type(urlField(), 'file:///opt/maven-repository')
        await apply()

        expect((await patched()).url).toBe('file:///opt/maven-repository')
    })

    it('shows how much the downloaded dependencies take', async () => {
        serverAnswers({ url: '' }, [125.6 * MEGABYTE])
        render(<Maven />)

        expect(await screen.findByTestId('maven-cache-size')).toHaveTextContent('maven:cache_size 125.6 MB')
    })

    it('deletes the downloaded dependencies once confirmed, and shows what stays', async () => {
        serverAnswers({ url: '' }, [125.6 * MEGABYTE, 0])
        render(<Maven />)
        await loaded()

        await userEvent.click(screen.getByRole('button', { name: 'maven:delete_cache' }))
        expect(mockConfirm).toHaveBeenCalledWith(expect.objectContaining({ title: 'maven:confirm_delete_cache' }))
        expect(writes('DELETE')).toHaveLength(0)
        await act(async () => mockConfirm.mock.lastCall![0].onOk())

        expect(writes('DELETE')).toEqual([[CACHE_URL, { method: 'DELETE' }, expect.anything()]])
        expect(await screen.findByTestId('maven-cache-size')).toHaveTextContent('maven:cache_size 0.0 MB')
        expect(mockSuccess).toHaveBeenCalledWith(expect.objectContaining({ description: 'maven:cache_deleted' }))
    })

    it('reports downloaded dependencies it cannot delete', async () => {
        const failure = new Error('Forbidden')
        serverAnswers({ url: '' }, [MEGABYTE], failure)
        render(<Maven />)
        await loaded()

        await userEvent.click(screen.getByRole('button', { name: 'maven:delete_cache' }))
        await act(async () => mockConfirm.mock.lastCall![0].onOk())

        expect(notifyLoadFailure).toHaveBeenCalledWith('maven:cache_delete_failed', failure)
        expect(mockSuccess).not.toHaveBeenCalled()
    })

    it('reports a configuration and a size it cannot read', async () => {
        const failure = new Error('The server is not available')
        vi.mocked(apiCall).mockRejectedValue(failure)
        render(<Maven />)

        await waitFor(() => expect(notifyLoadFailure).toHaveBeenCalledWith('maven:load_failed', failure))
        expect(notifyLoadFailure).toHaveBeenCalledWith('maven:cache_load_failed', failure)
        expect(screen.getByTestId('maven-cache-size')).toHaveTextContent('maven:cache_size_unknown')
    })

    it('reports a configuration it cannot save', async () => {
        const failure = new Error('Forbidden')
        serverAnswers({ url: '' }, [0], failure)
        render(<Maven />)
        await loaded()

        await userEvent.type(urlField(), 'https://repo.maven.apache.org/maven2')
        await apply()

        await waitFor(() => expect(notifyLoadFailure).toHaveBeenCalledWith('maven:save_failed', failure))
        expect(mockSuccess).not.toHaveBeenCalled()
    })
})
