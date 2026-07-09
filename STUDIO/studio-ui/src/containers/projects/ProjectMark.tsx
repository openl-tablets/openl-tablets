import { useMemo } from 'react'
import { createStyles } from 'antd-style'

/**
 * Muted two-tone palettes derived from the Ant Design preset hues. Each entry pairs a quiet
 * background with a saturated cell ink, so a mark stays legible at 20px without shouting.
 */
const PALETTES: ReadonlyArray<{ bg: string, fg: string }> = [
    { bg: '#e6f4ff', fg: '#1677ff' }, // blue
    { bg: '#e6fffb', fg: '#13c2c2' }, // cyan
    { bg: '#f6ffed', fg: '#52c41a' }, // green
    { bg: '#fff7e6', fg: '#fa8c16' }, // orange
    { bg: '#f9f0ff', fg: '#722ed1' }, // purple
    { bg: '#fff0f6', fg: '#eb2f96' }, // magenta
]

/** Stable 32-bit FNV-1a hash of the project name; the same name always yields the same mark. */
const hashName = (name: string): number => {
    let hash = 0x811c9dc5
    for (let i = 0; i < name.length; i += 1) {
        hash ^= name.charCodeAt(i)
        hash = Math.imul(hash, 0x01000193)
    }
    return hash >>> 0
}

const useStyles = createStyles(({ css }) => ({
    mark: css`
        display: grid;
        flex: none;
        grid-template: 1fr 1fr / 1fr 1fr;
        gap: 1.5px;
        padding: 2.5px;
        border-radius: 22%;
        box-sizing: border-box;
    `,
    cell: css`
        border-radius: 18%;
    `,
}))

interface ProjectMarkProps {
    name: string
    /** Rendered width/height in pixels. */
    size?: number
}

/**
 * The project's visual identity: a 2x2 grid of cells — the spreadsheet motif OpenL rules live in —
 * whose hue and fill pattern derive deterministically from the project name. In a flat list it gives
 * each project a recognizable mark, the way avatars do on code-hosting sites.
 */
export const ProjectMark = ({ name, size = 20 }: ProjectMarkProps) => {
    const { styles } = useStyles()
    const { palette, cells } = useMemo(() => {
        const hash = hashName(name)
        // Low bits pick the hue, next bits pick which of the 4 cells are inked (never none).
        const filled = ((hash >>> 3) & 0b1111) || 0b1001
        return {
            palette: PALETTES[hash % PALETTES.length] ?? { bg: '#e6f4ff', fg: '#1677ff' },
            cells: [0, 1, 2, 3].map(index => (filled & (1 << index)) !== 0),
        }
    }, [name])

    return (
        <span
            aria-hidden
            className={styles.mark}
            data-testid="project-mark"
            style={{ width: size, height: size, background: palette.bg }}
        >
            {cells.map((inked, index) => (
                <span
                    // The grid is positional and static; the index is the identity of a cell.
                    key={index}
                    className={styles.cell}
                    data-inked={inked || undefined}
                    style={{ background: inked ? palette.fg : 'transparent' }}
                />
            ))}
        </span>
    )
}
