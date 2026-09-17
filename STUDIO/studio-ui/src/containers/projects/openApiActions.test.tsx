import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { generateOpenApiTables, getOpenApiGenerationPlan, writeOpenApiSchema } from '../../services/openapi'
import type { DescriptorOpenApi } from '../../services/rulesDescriptor'
import { OpenApiActions, useOpenApiActions } from './openApiActions'

vi.mock('../../services/openapi', () => ({
    writeOpenApiSchema: vi.fn(),
    getOpenApiGenerationPlan: vi.fn(),
    generateOpenApiTables: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string, values?: Record<string, unknown>) =>
        values?.['path'] === undefined ? key : `${key}:${values['path'] as string}`
    return { useTranslation: () => ({ t }) }
})

const notification = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))
// The confirmation is answered as soon as it is asked, so the test is about what is asked and what follows.
const confirm = vi.hoisted(() => vi.fn())

vi.mock('antd', async importOriginal => ({
    ...(await importOriginal<typeof import('antd')>()),
    App: { useApp: () => ({ modal: { confirm }, notification }) },
}))

/** The section heading, with the actions it offers for the given settings. */
const Heading = ({ openapi }: { openapi: DescriptorOpenApi | undefined }) => {
    const openApi = useOpenApiActions('p1', vi.fn())
    return (
        <OpenApiActions
            onGenerate={() => void openApi.generateTables(openapi ?? {})}
            onWrite={() => void openApi.writeSchema()}
            openapi={openapi}
            running={openApi.running}
        />
    )
}

describe('OpenApiActions', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        confirm.mockImplementation(({ onOk }: { onOk: () => void }) => onOk())
        vi.mocked(writeOpenApiSchema).mockResolvedValue({ path: 'openapi.json', created: true })
        vi.mocked(getOpenApiGenerationPlan).mockResolvedValue({
            algorithm: { name: 'Algorithms', path: 'rules/Algorithms.xlsx', declared: true },
            model: { name: 'Models', path: 'rules/Models.xlsx', declared: false },
        })
        vi.mocked(generateOpenApiTables).mockResolvedValue(undefined)
    })

    it('writes the specification from the rules where the project is held to one', async () => {
        render(<Heading openapi={{ path: 'openapi.json', mode: 'RECONCILIATION' }} />)

        await userEvent.click(screen.getByTestId('openapi-write'))

        await waitFor(() => expect(writeOpenApiSchema).toHaveBeenCalledWith('p1'))
        expect(notification.success).toHaveBeenCalledWith(expect.objectContaining({ description: 'openapi.json' }))
    })

    it('offers to write the specification for a project that declares none', async () => {
        render(<Heading openapi={undefined} />)

        // Nothing to generate from yet, so the way in is writing the specification the rules answer to.
        expect(screen.getByTestId('openapi-write')).toBeInTheDocument()
        expect(screen.queryByTestId('openapi-generate')).toBeNull()
    })

    it('generates the tables of a project whose specification leads', async () => {
        render(<Heading openapi={{ path: 'openapi.json', mode: 'GENERATION' }} />)

        await userEvent.click(screen.getByTestId('openapi-generate'))

        // What it would write is asked for first, and what the reader agreed to is what is written.
        await waitFor(() => expect(getOpenApiGenerationPlan).toHaveBeenCalledWith('p1', undefined, undefined))
        expect(generateOpenApiTables).toHaveBeenCalledWith('p1', {
            path: 'openapi.json',
            algorithmModuleName: 'Algorithms',
            algorithmModulePath: 'rules/Algorithms.xlsx',
            modelModuleName: 'Models',
            modelModulePath: 'rules/Models.xlsx',
        })
    })

    it('asks about the modules it would write over before it writes them', async () => {
        confirm.mockImplementation(() => undefined)
        render(<Heading openapi={{ path: 'openapi.json', mode: 'GENERATION' }} />)

        await userEvent.click(screen.getByTestId('openapi-generate'))
        await waitFor(() => expect(confirm).toHaveBeenCalled())

        // The reader said nothing yet, so nothing of the project is written.
        expect(generateOpenApiTables).not.toHaveBeenCalled()
    })

    it('asks the plan about the modules the project names, not about the default pair', async () => {
        render(<Heading openapi={{ path: 'openapi.json', mode: 'GENERATION', algorithmModuleName: 'Pricing' }} />)

        await userEvent.click(screen.getByTestId('openapi-generate'))

        await waitFor(() => expect(getOpenApiGenerationPlan).toHaveBeenCalledWith('p1', 'Pricing', undefined))
    })

    it('says what went wrong rather than leaving the reader with nothing', async () => {
        vi.mocked(writeOpenApiSchema).mockRejectedValue(new Error('the project did not compile'))
        render(<Heading openapi={{ path: 'openapi.json', mode: 'RECONCILIATION' }} />)

        await userEvent.click(screen.getByTestId('openapi-write'))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith(
            expect.objectContaining({ description: 'the project did not compile' })))
    })
})
