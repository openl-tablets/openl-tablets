import { Suspense } from 'react'
import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ModuleWorkspaceScreen, preloadProjectScreens, preloadProjectScreensWhenIdle, ProjectWorkspaceScreen } from './projectScreens'

vi.mock('../containers/ProjectWorkspace', () => ({ ProjectWorkspace: () => <div data-testid="project-page" /> }))
vi.mock('../containers/ModuleWorkspace', () => ({ ModuleWorkspace: () => <div data-testid="module-editor" /> }))

describe('the screens of a project', () => {
    it('draws a screen whose code was loaded ahead in its first frame, with no placeholder between', async () => {
        await act(async () => {
            preloadProjectScreens()
        })

        // Rendered with no Suspense boundary at all: a screen that suspended would have nowhere to fall back to.
        render(<><ProjectWorkspaceScreen /><ModuleWorkspaceScreen /></>)

        expect(screen.getByTestId('project-page')).toBeInTheDocument()
        expect(screen.getByTestId('module-editor')).toBeInTheDocument()
    })
})

describe('a screen whose code is not loaded yet', () => {
    it('shows the placeholder of its route until its code arrives', async () => {
        vi.resetModules()
        const { ModuleWorkspaceScreen: Fresh } = await import('./projectScreens')

        render(<Suspense fallback={<div data-testid="placeholder" />}><Fresh /></Suspense>)

        expect(screen.getByTestId('placeholder')).toBeInTheDocument()
        expect(await screen.findByTestId('module-editor')).toBeInTheDocument()
    })
})

describe('loading the screens ahead', () => {
    afterEach(() => {
        vi.unstubAllGlobals()
        vi.useRealTimers()
    })

    it('waits for the browser to be idle, and lets the one who asked cancel it', () => {
        const ask = vi.fn(() => 7)
        const cancel = vi.fn()
        vi.stubGlobal('requestIdleCallback', ask)
        vi.stubGlobal('cancelIdleCallback', cancel)

        const stop = preloadProjectScreensWhenIdle()
        stop()

        expect(ask).toHaveBeenCalledWith(preloadProjectScreens)
        expect(cancel).toHaveBeenCalledWith(7)
    })

    it('waits a moment in a browser that tells no idle time', () => {
        vi.stubGlobal('requestIdleCallback', undefined)
        vi.useFakeTimers()
        const started = vi.spyOn(window, 'setTimeout')

        const stop = preloadProjectScreensWhenIdle()

        expect(started).toHaveBeenCalledWith(preloadProjectScreens, 1000)
        stop()
        expect(vi.getTimerCount()).toBe(0)
    })
})

describe('loading the screens ahead when their code cannot be had', () => {
    afterEach(() => {
        vi.doUnmock('../containers/ProjectWorkspace')
        vi.resetModules()
    })

    it('leaves the failure to the first use of the screen, and raises nothing meanwhile', async () => {
        vi.resetModules()
        vi.doMock('../containers/ProjectWorkspace', () => {
            throw new Error('The code of the page is gone')
        })
        const { preloadProjectScreens: preload } = await import('./projectScreens')
        const unhandled = vi.fn()
        window.addEventListener('unhandledrejection', unhandled)
        try {
            preload()
            await new Promise(resolve => setTimeout(resolve, 10))

            expect(unhandled).not.toHaveBeenCalled()
        } finally {
            window.removeEventListener('unhandledrejection', unhandled)
        }
    })
})
