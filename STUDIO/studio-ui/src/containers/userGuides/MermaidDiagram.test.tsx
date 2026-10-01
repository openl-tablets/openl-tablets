import React from 'react'
import { render, screen } from '@testing-library/react'
import { ThemeProvider } from 'antd-style'
import { MermaidDiagram } from './MermaidDiagram'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const mermaid = vi.hoisted(() => ({ initialize: vi.fn(), render: vi.fn() }))
vi.mock('mermaid', () => ({ default: mermaid }))

describe('MermaidDiagram', () => {
    it('draws the diagram in the appearance of the application', async () => {
        mermaid.render.mockResolvedValue({ svg: '<svg><text>dark diagram</text></svg>' })

        render(
            <ThemeProvider appearance="dark">
                <MermaidDiagram source="flowchart LR" />
            </ThemeProvider>
        )

        expect(await screen.findByTestId('guide-diagram')).toHaveTextContent('dark diagram')
        expect(mermaid.initialize).toHaveBeenCalledWith({ startOnLoad: false, securityLevel: 'strict', theme: 'dark' })
    })

    it('shows the text of a diagram that cannot be drawn', async () => {
        mermaid.render.mockRejectedValue(new Error('Parse error'))

        render(<MermaidDiagram source="not a diagram" />)

        expect(await screen.findByText('guides:diagram_failed')).toBeInTheDocument()
        expect(screen.getByText('not a diagram')).toBeInTheDocument()
        expect(mermaid.initialize).toHaveBeenCalledWith(expect.objectContaining({ theme: 'default' }))
    })
})
