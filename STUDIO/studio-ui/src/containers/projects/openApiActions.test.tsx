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
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const notification = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))

vi.mock('antd', async importOriginal => ({
    ...(await importOriginal<typeof import('antd')>()),
    App: { useApp: () => ({ notification }) },
}))

/** The section heading, with the actions it offers for the given settings and the dialog they open. */
const Heading = ({ openapi }: { openapi: DescriptorOpenApi | undefined }) => {
    const openApi = useOpenApiActions('p1', vi.fn())
    return (
        <>
            <OpenApiActions
                onGenerate={() => void openApi.generateTables(openapi ?? {})}
                onWrite={() => void openApi.writeSchema()}
                openapi={openapi}
                running={openApi.running}
            />
            {openApi.generationDialog}
        </>
    )
}

const show = (openapi: DescriptorOpenApi | undefined) => render(<Heading openapi={openapi} />)

const GENERATES: DescriptorOpenApi = { path: 'openapi.json', mode: 'GENERATION' }

describe('OpenApiActions', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(writeOpenApiSchema).mockResolvedValue({ path: 'openapi.json', created: true })
        vi.mocked(getOpenApiGenerationPlan).mockResolvedValue({
            algorithm: { name: 'Algorithms', path: 'rules/Algorithms.xlsx', declared: true, overwrites: true },
            model: { name: 'Models', path: 'rules/Models.xlsx', declared: false, overwrites: false },
        })
        vi.mocked(generateOpenApiTables).mockResolvedValue(undefined)
    })

    it('writes the specification from the rules where the project is held to one', async () => {
        show({ path: 'openapi.json', mode: 'RECONCILIATION' })

        await userEvent.click(screen.getByTestId('openapi-write'))

        await waitFor(() => expect(writeOpenApiSchema).toHaveBeenCalledWith('p1'))
        expect(notification.success).toHaveBeenCalledWith(expect.objectContaining({ description: 'openapi.json' }))
    })

    it('offers to write the specification for a project that declares none', async () => {
        show(undefined)

        // Nothing to generate from yet, so the way in is writing the specification the rules answer to.
        expect(screen.getByTestId('openapi-write')).toBeInTheDocument()
        expect(screen.queryByTestId('openapi-generate')).toBeNull()
    })

    it('generates the tables of a project whose specification leads', async () => {
        show(GENERATES)

        await userEvent.click(screen.getByTestId('openapi-generate'))
        await waitFor(() => expect(getOpenApiGenerationPlan).toHaveBeenCalledWith('p1', undefined, undefined))
        await userEvent.click(screen.getByTestId('openapi-generate-submit'))

        // The modules are named by the plan; the workbooks are what the reader settled in the dialog.
        expect(generateOpenApiTables).toHaveBeenCalledWith('p1', {
            path: 'openapi.json',
            algorithmModuleName: 'Algorithms',
            algorithmModulePath: 'rules/Algorithms.xlsx',
            modelModuleName: 'Models',
            modelModulePath: 'rules/Models.xlsx',
        })
    })

    it('writes the module where the reader said, not where the plan proposed', async () => {
        show(GENERATES)

        await userEvent.click(screen.getByTestId('openapi-generate'))
        await waitFor(() => expect(screen.getByTestId('openapi-plan-model-path')).toBeInTheDocument())
        await userEvent.clear(screen.getByTestId('openapi-plan-model-path'))
        await userEvent.type(screen.getByTestId('openapi-plan-model-path'), 'types/Models.xlsx')
        await userEvent.click(screen.getByTestId('openapi-generate-submit'))

        expect(generateOpenApiTables).toHaveBeenCalledWith('p1',
            expect.objectContaining({ modelModulePath: 'types/Models.xlsx' }))
    })

    it('asks about the modules it would write over before it writes them', async () => {
        show(GENERATES)

        await userEvent.click(screen.getByTestId('openapi-generate'))

        // The reader said nothing yet, so nothing of the project is written.
        await waitFor(() => expect(screen.getByTestId('openapi-generate-submit')).toBeInTheDocument())
        expect(generateOpenApiTables).not.toHaveBeenCalled()
    })

    it('asks the plan about the modules the project names, not about the default pair', async () => {
        show({ ...GENERATES, algorithmModuleName: 'Pricing' })

        await userEvent.click(screen.getByTestId('openapi-generate'))

        await waitFor(() => expect(getOpenApiGenerationPlan).toHaveBeenCalledWith('p1', 'Pricing', undefined))
    })

    it('says what went wrong rather than leaving the reader with nothing', async () => {
        vi.mocked(writeOpenApiSchema).mockRejectedValue(new Error('the project did not compile'))
        show({ path: 'openapi.json', mode: 'RECONCILIATION' })

        await userEvent.click(screen.getByTestId('openapi-write'))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith(
            expect.objectContaining({ description: 'the project did not compile' })))
    })
})
