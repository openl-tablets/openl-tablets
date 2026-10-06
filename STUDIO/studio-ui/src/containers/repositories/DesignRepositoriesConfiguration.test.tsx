import { createRef } from 'react'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { App } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { MockedFunction } from 'vitest'
import { DesignRepositoriesConfiguration } from './DesignRepositoriesConfiguration'
import { RepositoryDataType, RepositoryType } from './constants'
import type { FormRefProps } from './index'
import * as services from '../../services'
import { chooseOption, openOptions } from 'testing/select'

vi.mock('../../services', () => ({ apiCall: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en', resolvedLanguage: 'en' }
    return { useTranslation: () => ({ t, i18n }) }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const REGION = { id: 'us-east-1', description: 'US East (N. Virginia)' }

const S3_SETTINGS = {
    bucketName: '',
    regionName: '',
    listenerTimerPeriod: 10,
    allAllowedRegions: [REGION],
    allSseAlgorithms: ['AES256'],
}

const template = (type: string) => ({
    id: 'new-id',
    name: 'New repository',
    type,
    settings: type === RepositoryType.AWS_S3 ? S3_SETTINGS : { uri: 'git://new', listenerTimerPeriod: 10 },
})

const existingGit = {
    id: 'existing-id',
    name: 'Existing Git',
    type: RepositoryType.GIT,
    settings: { uri: 'git://existing', listenerTimerPeriod: 10 },
}

const existingS3 = {
    id: 'existing-s3-id',
    name: 'Existing S3',
    type: RepositoryType.AWS_S3,
    settings: { ...S3_SETTINGS, bucketName: 'bucket', regionName: REGION.id },
}

const templateRequests = () => mockApiCall.mock.calls
    .filter(([url]) => url.endsWith('/template'))
    .map(([, params]) => JSON.parse(params!.body as string).type)

const nextFrames = (count: number) => new Promise<void>(resolve => {
    const step = (left: number) => left === 0 ? resolve() : requestAnimationFrame(() => step(left - 1))
    step(count)
})

const renderAndAddRepository = async (repositoryDataType: RepositoryDataType, existing: { id: string, name: string }[]) => {
    mockApiCall.mockImplementation(async (url: string, params?: RequestInit) => {
        if (url.endsWith('/template')) {
            return template(JSON.parse(params!.body as string).type)
        }
        return params?.method === 'PATCH' ? undefined : existing
    })
    const ref = createRef<FormRefProps>()
    render(
        <MemoryRouter>
            <App>
                <DesignRepositoriesConfiguration ref={ref} repositoryDataType={repositoryDataType} />
            </App>
        </MemoryRouter>
    )
    if (existing.length) {
        await screen.findByDisplayValue(existing[0]!.name)
        await act(() => nextFrames(3))
    } else {
        await screen.findByText('repository:no_repositories_available')
    }
    await act(() => ref.current!.addRepository())
    await screen.findByDisplayValue('New repository')
    return ref
}

describe('DesignRepositoriesConfiguration, adding a repository', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it.each([RepositoryDataType.DESIGN, RepositoryDataType.DEPLOYMENT])(
        'offers the AWS S3 regions when the first %s repository is switched to AWS S3',
        async repositoryDataType => {
            await renderAndAddRepository(repositoryDataType, [])

            await chooseOption('repository:type', 'AWS S3')

            await waitFor(() => expect(templateRequests()).toEqual([RepositoryType.GIT, RepositoryType.AWS_S3]))
            openOptions('repository:region_name')
            expect(await screen.findByTitle(REGION.description)).toBeInTheDocument()
        }
    )

    it('applies the first repository under its own id with the chosen region', async () => {
        const reload = vi.fn()
        vi.stubGlobal('location', { ...window.location, reload })
        await renderAndAddRepository(RepositoryDataType.DEPLOYMENT, [])
        await chooseOption('repository:type', 'AWS S3')
        await chooseOption('repository:region_name', REGION.description)
        fireEvent.change(screen.getByRole('textbox', { name: 'repository:bucket_name' }), { target: { value: 'bucket' } })

        await userEvent.click(screen.getByRole('button', { name: 'repository:buttons.apply_changes' }))
        const confirmButtons = within(await screen.findByRole('dialog')).getAllByRole('button')
        await userEvent.click(confirmButtons[confirmButtons.length - 1]!)

        await waitFor(() => expect(reload).toHaveBeenCalledTimes(1))
        const [, params] = mockApiCall.mock.calls.find(([, call]) => call?.method === 'PATCH')!
        expect(JSON.parse(params!.body as string)).toMatchObject({
            id: 'new-id',
            type: RepositoryType.AWS_S3,
            settings: { bucketName: 'bucket', regionName: REGION.id },
        })
    })

    it('keeps the new repository in the form when its type is switched to AWS S3 and back to Git', async () => {
        const ref = await renderAndAddRepository(RepositoryDataType.DESIGN, [existingGit])

        await chooseOption('repository:type', 'AWS S3')
        await screen.findByRole('combobox', { name: 'repository:region_name' })
        await chooseOption('repository:type', 'Git')

        await waitFor(() => expect(ref.current!.getForm().getFieldValue('type')).toBe(RepositoryType.GIT))
        expect(ref.current!.getForm().getFieldValue('id')).toBe('new-id')
        expect(ref.current!.getForm().getFieldValue('name')).toBe('New repository')
    })

    it('asks the AWS S3 template for a new repository while an AWS S3 repository is selected', async () => {
        const ref = await renderAndAddRepository(RepositoryDataType.DEPLOYMENT, [existingS3])

        await chooseOption('repository:type', 'AWS S3')

        await waitFor(() => expect(templateRequests()).toEqual([RepositoryType.GIT, RepositoryType.AWS_S3]))
        expect(ref.current!.getForm().getFieldValue('id')).toBe('new-id')
        expect(ref.current!.getForm().getFieldValue('name')).toBe('New repository')
    })
})
