import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ProjectsThemeProvider } from './ProjectsThemeProvider'

describe('ProjectsThemeProvider', () => {
    it('renders children inside the scoped projects theme', () => {
        render(
            <ProjectsThemeProvider>
                <span data-testid="projects-child">Projects</span>
            </ProjectsThemeProvider>
        )

        expect(screen.getByTestId('projects-child').textContent).toBe('Projects')
    })
})
