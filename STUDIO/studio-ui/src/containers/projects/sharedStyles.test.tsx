import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { useSharedStyles } from './sharedStyles'
import { renderInTheme, tokenFor } from '../../testing/theme'

const Frame = () => {
    const { styles, cx } = useSharedStyles()
    return (
        <div className={styles.page} data-testid="page">
            <div className={styles.rail} data-testid="rail" />
            <table className={cx(styles.listTable, styles.flushTable)} data-testid="list" />
        </div>
    )
}

describe('useSharedStyles', () => {
    it.each([false, true])('lays a list page on the surface of a container, beside a rail of the layout (dark: %s)', isDarkMode => {
        renderInTheme(<Frame />, { mode: isDarkMode ? 'dark' : 'light' })
        const token = tokenFor('standard', isDarkMode)

        expect(screen.getByTestId('page')).toHaveStyle({ backgroundColor: token.colorBgContainer })
        expect(screen.getByTestId('rail')).toHaveStyle({ backgroundColor: token.colorBgLayout })
    })

    it('lays the list of a list page from edge to edge of it, not as a card inset in it', () => {
        renderInTheme(<Frame />)

        expect(screen.getByTestId('list')).toHaveStyle({ marginLeft: '0px', marginTop: '0px', width: '100%', borderRadius: '0px' })
        expect(getComputedStyle(screen.getByTestId('list')).borderLeftStyle).not.toBe('solid')
    })
})
