import { render, screen } from '@testing-library/react'

// The renderer is a browser component; the screen only has to hand it the specification.
vi.mock('rapidoc', () => ({}))

import { ApiDocs } from './ApiDocs'

describe('ApiDocs', () => {
    it('reads the specification the server generates, from the surface it documents', () => {
        render(<ApiDocs />)

        const docs = screen.getByTestId('api-docs')
        expect(docs.tagName.toLowerCase()).toBe('rapi-doc')
        expect(docs.getAttribute('spec-url')).toBe('/rest/openapi.json')
    })
})
