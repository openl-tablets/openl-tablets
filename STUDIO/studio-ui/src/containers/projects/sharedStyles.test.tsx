import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { useSharedStyles } from './sharedStyles'
import { renderInTheme, tokenFor } from '../../testing/theme'

const Frame = () => {
    const { styles } = useSharedStyles()
    return (
        <div className={styles.page} data-testid="page">
            <div className={styles.rail} data-testid="rail" />
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
})
