import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { OpenApiGenerationPlan, OpenApiModule } from '../../services/openapi'
import { OpenApiGenerationModal } from './OpenApiGenerationModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

/** A plan whose two modules stand as the test asks for them; both are new unless it says otherwise. */
const planOf = (algorithm: Partial<OpenApiModule> = {}, model: Partial<OpenApiModule> = {}): OpenApiGenerationPlan => ({
    algorithm: { name: 'Algorithms', path: 'rules/Algorithms.xlsx', declared: false, overwrites: false, ...algorithm },
    model: { name: 'Models', path: 'rules/Models.xlsx', declared: false, overwrites: false, ...model },
})

const show = async (plan: OpenApiGenerationPlan, onGenerate = vi.fn()) => {
    render(<OpenApiGenerationModal busy={false} onCancel={vi.fn()} onGenerate={onGenerate} plan={plan} />)
    // The modal renders through a portal, so its body is awaited rather than assumed.
    await screen.findByTestId('openapi-generate-submit')
    return onGenerate
}

const submit = () => screen.getByTestId('openapi-generate-submit')

describe('OpenApiGenerationModal', () => {
    it('opens on the workbooks the plan proposed, and writes them when nothing is changed', async () => {
        const onGenerate = await show(planOf())

        expect(screen.getByTestId('openapi-plan-algorithm-name')).toHaveTextContent('Algorithms')
        expect(screen.getByTestId('openapi-plan-algorithm-path')).toHaveValue('rules/Algorithms.xlsx')

        await userEvent.click(submit())

        expect(onGenerate).toHaveBeenCalledWith({
            algorithmModulePath: 'rules/Algorithms.xlsx',
            modelModulePath: 'rules/Models.xlsx',
        })
    })

    it('lets the reader say where a module the project does not read yet is written', async () => {
        const onGenerate = await show(planOf())

        await userEvent.clear(screen.getByTestId('openapi-plan-model-path'))
        await userEvent.type(screen.getByTestId('openapi-plan-model-path'), 'other/Types.xlsx')
        await userEvent.click(submit())

        expect(onGenerate).toHaveBeenCalledWith({
            algorithmModulePath: 'rules/Algorithms.xlsx',
            modelModulePath: 'other/Types.xlsx',
        })
    })

    it('puts the proposed workbook back when the reset beside it is clicked', async () => {
        await show(planOf())

        await userEvent.clear(screen.getByTestId('openapi-plan-model-path'))
        await userEvent.type(screen.getByTestId('openapi-plan-model-path'), 'elsewhere/Types.xlsx')
        await userEvent.click(screen.getByTestId('openapi-plan-model-path-reset'))

        expect(screen.getByTestId('openapi-plan-model-path')).toHaveValue('rules/Models.xlsx')
    })

    it('states the workbook of a module the project already reads rather than offering it', async () => {
        // The generation writes a declared module where it reads, whatever path is asked for it, so there is
        // nothing for the reader to settle — and they are told what becomes of it instead.
        await show(planOf({ declared: true, overwrites: true }))

        const stated = screen.getByTestId('openapi-plan-algorithm-path')
        expect(stated.tagName).toBe('SPAN')
        expect(stated).toHaveTextContent('rules/Algorithms.xlsx')
        expect(screen.queryByTestId('openapi-plan-algorithm-path-reset')).toBeNull()
        expect(screen.getByText('browser.overview.openapi_plan_replaces')).toBeInTheDocument()
    })

    it('says it overwrites when a module the project reads is among the two', async () => {
        await show(planOf({}, { declared: true, overwrites: true }))

        expect(submit()).toHaveTextContent('browser.overview.openapi_generate_overwrite')
    })

    it('says it generates when neither module is read yet', async () => {
        await show(planOf())

        // Checked exactly: the overwrite label starts with this one, so a substring match would pass either way.
        expect(submit()).toHaveTextContent(/^browser\.overview\.openapi_generate$/)
        expect(screen.getAllByText('browser.overview.openapi_plan_adds')).toHaveLength(2)
    })

    it('says a module declared at a workbook nobody wrote yet is created, not overwritten', async () => {
        // The project settles where it goes, so the path is stated; but no file stands there, so nothing
        // is lost and the reader is not warned about losing it.
        await show(planOf({ declared: true }))

        expect(screen.getByTestId('openapi-plan-algorithm-path').tagName).toBe('SPAN')
        expect(screen.getAllByText('browser.overview.openapi_plan_adds')).toHaveLength(2)
        expect(screen.queryByText('browser.overview.openapi_plan_replaces')).toBeNull()
        expect(submit()).toHaveTextContent(/^browser\.overview\.openapi_generate$/)
    })

    it('refuses a path that names no workbook, as the Editor refused it', async () => {
        await show(planOf())

        await userEvent.clear(screen.getByTestId('openapi-plan-algorithm-path'))
        await userEvent.type(screen.getByTestId('openapi-plan-algorithm-path'), 'rules/Alg.txt')

        expect(screen.getByText('browser.overview.openapi_path_not_excel')).toBeInTheDocument()
        expect(submit()).toBeDisabled()
    })

    it('says why a module declared at a file that is no workbook cannot be generated', async () => {
        // The reader cannot put this right here — the project settles where the module goes — but the
        // server refuses it all the same, so the reason stands beside the path rather than after a click.
        await show(planOf({ path: 'rules/Alg.txt', declared: true, overwrites: true }))

        expect(screen.getByText('browser.overview.openapi_path_not_excel')).toBeInTheDocument()
        expect(submit()).toBeDisabled()
    })

    it('refuses a blank path', async () => {
        await show(planOf())

        await userEvent.clear(screen.getByTestId('openapi-plan-algorithm-path'))

        expect(screen.getByText('browser.overview.openapi_path_required')).toBeInTheDocument()
        expect(submit()).toBeDisabled()
    })

    it('reads a typed path without the spaces around it', async () => {
        const onGenerate = await show(planOf())

        await userEvent.clear(screen.getByTestId('openapi-plan-model-path'))
        await userEvent.type(screen.getByTestId('openapi-plan-model-path'), '  rules/Types.xlsx  ')
        await userEvent.click(submit())

        expect(onGenerate).toHaveBeenCalledWith(
            expect.objectContaining({ modelModulePath: 'rules/Types.xlsx' }))
    })

    it('refuses one workbook for both modules', async () => {
        await show(planOf())

        await userEvent.clear(screen.getByTestId('openapi-plan-model-path'))
        await userEvent.type(screen.getByTestId('openapi-plan-model-path'), 'rules/Algorithms.xlsx')

        expect(screen.getByTestId('openapi-plan-same-path')).toBeInTheDocument()
        expect(submit()).toBeDisabled()
    })
})
