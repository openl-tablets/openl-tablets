import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { DependencyList, LibraryList } from './DependencyList'
import type { ProjectDependency } from '../../types/projects'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en', resolvedLanguage: 'en' }
    return { useTranslation: () => ({ t, i18n }) }
})

const renderList = (deps: ProjectDependency[]) => render(
    <MemoryRouter><DependencyList deps={deps} /></MemoryRouter>
)

describe('DependencyList', () => {
    it('links a project of the workspace and marks one it lacks', () => {
        renderList([
            { name: 'Common', id: 'ABC', branch: 'master' },
            { name: 'Ghost', missing: true },
        ])

        expect(screen.getByRole('link', { name: 'Common' })).toHaveAttribute('href', '/projects/ABC')
        expect(screen.getByTestId('dependency-missing-Ghost')).toBeInTheDocument()
    })

    it('marks a project compiled from its Maven archive and links the archive', () => {
        renderList([{ name: 'Domain', maven: true, mavenArtifact: 'com.example:domain:1.0.0' }])

        expect(screen.getByTestId('dependency-maven-Domain')).toHaveTextContent('browser.overview.dependency_maven')
        expect(screen.getByTestId('dependency-maven-link-Domain'))
            .toHaveAttribute('href', '/maven/com.example/domain/1.0.0')
        expect(screen.queryByTestId('dependency-missing-Domain')).not.toBeInTheDocument()
    })

    it('links no archive whose coordinates name no project', () => {
        renderList([{ name: 'Odd', maven: true, mavenArtifact: 'com.example:odd:jar:1.0.0' }])

        expect(screen.getByTestId('dependency-maven-Odd')).toBeInTheDocument()
        expect(screen.queryByTestId('dependency-maven-link-Odd')).not.toBeInTheDocument()
    })

    it('shows the coordinates of an open project that is compiled instead of its archive', () => {
        renderList([{ name: 'Domain', id: 'D1', mavenArtifact: 'com.example:domain:1.0.0' }])

        expect(screen.getByTestId('dependency-maven-declared-Domain')).toHaveTextContent('com.example:domain:1.0.0')
        expect(screen.queryByTestId('dependency-maven-Domain')).not.toBeInTheDocument()
    })
})

describe('DependencyList conflicts', () => {
    it('marks a project another version of which is asked for and left out', async () => {
        renderList([{
            name: 'Domain',
            maven: true,
            mavenArtifact: 'com.example:domain:1.0.0',
            conflicts: [{ requestedBy: 'CustomDomain', mavenArtifact: 'com.example:domain:2.0.0' }],
        }])

        const tag = screen.getByTestId('dependency-conflict-Domain')
        expect(tag).toHaveTextContent('browser.overview.dependency_conflict')
        await userEvent.hover(tag)
        expect(await screen.findByText('browser.overview.dependency_conflict_item')).toBeInTheDocument()
    })

    it('marks no conflict where there is none', () => {
        renderList([{ name: 'Domain', maven: true, mavenArtifact: 'com.example:domain:1.0.0' }])

        expect(screen.queryByTestId('dependency-conflict-Domain')).not.toBeInTheDocument()
    })
})

describe('LibraryList', () => {
    it('lists the libraries by their coordinates', () => {
        render(<LibraryList libraries={['org.apache.commons:commons-text:jar:1.15.0', 'com.example:lib:jar:2.0']} />)

        expect(screen.getByTestId('library-org.apache.commons:commons-text:jar:1.15.0')).toBeInTheDocument()
        expect(screen.getByTestId('library-com.example:lib:jar:2.0')).toHaveTextContent('com.example:lib:jar:2.0')
    })
})
