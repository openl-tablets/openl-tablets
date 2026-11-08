import { Flex, Typography } from 'antd'
import type { TextSpot } from '../../services/projectStatus'
import { MessageCode, useCompileMessageStyles } from '../../components/CompileMessages'
import { warningKeys, type MessageWarning } from './messageWarnings'

/** How much of the text is shown on each side of the piece a warning is about. */
const AROUND = 12

/** The piece a warning is about, with a little of the text on each side, so it is found in a long text. */
const excerptOf = (text: string, { start, end }: TextSpot): { text: string, start: number, end: number } => {
    const from = Math.max(0, start - AROUND)
    const to = Math.min(text.length, end + AROUND)
    const before = from > 0 ? '…' : ''
    const after = to < text.length ? '…' : ''
    return {
        text: `${before}${text.slice(from, to)}${after}`,
        start: before.length + start - from,
        end: before.length + end - from,
    }
}

/**
 * The warnings about a text, listed under it the way the problems of a table are: each marked by the stripe of a
 * warning, with the piece of the text it is about.
 *
 * <p>A warning shows its first line until it is opened, so a long one does not push the text away. A warning about
 * the text as a whole has no piece to show.
 *
 * <p>Nothing in the list takes the focus: a text being written over the list stays open while its warnings are read.
 */
export const ProblemLines = ({ problems, text }: {
    problems: readonly MessageWarning[]
    /** The saved text the warnings are about. */
    text: string
}) => {
    const { styles: message, cx } = useCompileMessageStyles()
    const keys = warningKeys(problems)
    return (
        // A press on the list leaves the focus where it is: the text being written stays open.
        <Flex vertical onMouseDown={event => event.preventDefault()}>
            <ul className={message.list} data-testid="localization-cell-problems">
                {problems.map((problem, at) => (
                    <li key={keys[at]} className={cx(message.message, message.warning)}>
                        <Typography.Paragraph
                            ellipsis={{ rows: 1, expandable: 'collapsible' }}
                            style={{ marginBottom: 0 }}
                            type="secondary"
                        >
                            {problem.summary}
                        </Typography.Paragraph>
                        {problem.spots
                            .filter(spot => spot.end <= text.length)
                            .map(spot => (
                                <MessageCode
                                    key={spot.start}
                                    testId="localization-cell-problem-code"
                                    {...excerptOf(text, spot)}
                                />
                            ))}
                    </li>
                ))}
            </ul>
        </Flex>
    )
}
