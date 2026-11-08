import { Flex } from 'antd'
import { warningKeys, type MessageWarning } from './messageWarnings'

/** What is wrong with a text, in a tooltip: a line for each problem. */
export const ProblemList = ({ problems }: { problems: readonly MessageWarning[] }) => {
    // A single warning reads as a sentence, not as a list of one.
    if (problems.length === 1) {
        return <span data-testid="localization-cell-problems">{problems[0]?.summary}</span>
    }
    const keys = warningKeys(problems)
    return (
        <Flex vertical data-testid="localization-cell-problems" gap="small">
            {problems.map((problem, at) => <span key={keys[at]}>{problem.summary}</span>)}
        </Flex>
    )
}
