import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Logo from './Logo'

describe('Logo', () => {
    it('draws every face and line of the mark in a colour of the theme, none of its own', () => {
        const { container } = render(<Logo />)

        const paths = container.querySelectorAll('path')
        expect(paths).toHaveLength(5)
        paths.forEach(path => {
            expect(path.getAttribute('class')).toBeTruthy()
            expect(path.hasAttribute('fill')).toBe(false)
            expect(path.hasAttribute('stroke')).toBe(false)
        })
    })

    it('takes the size it is given', () => {
        const { container } = render(<Logo height={72} width={72} />)

        expect(container.querySelector('svg')).toHaveAttribute('width', '72')
        expect(container.querySelector('svg')).toHaveAttribute('height', '72')
    })
})
