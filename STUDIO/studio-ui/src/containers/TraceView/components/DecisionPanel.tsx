import React from 'react'
import { Card, Checkbox, Tag, Tooltip } from 'antd'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { DecisionView } from 'types/trace'
import { useStyles } from './DecisionPanel.styles'

/** Breakpoint key suffix that suspends when any rule fires; mirrors the backend CurrentLocation.RULE_FIRED_REF. */
const RULE_FIRED_REF = 'rule'

interface RuleGroup {
    rule: string
    conditions: { condition: string; matched: boolean }[]
}

/** Group the flat condition list by rule, preserving first-seen order. */
const groupByRule = (decision: DecisionView): RuleGroup[] => {
    const byRule = new Map<string, RuleGroup>()
    const order: RuleGroup[] = []
    decision.conditions.forEach(({ condition, rule, matched }) => {
        let group = byRule.get(rule)
        if (!group) {
            group = { rule, conditions: []}
            byRule.set(rule, group)
            order.push(group)
        }
        group.conditions.push({ condition, matched })
    })
    return order
}

interface DecisionPanelProps {
    decision: DecisionView | null
    frameUri: string
    frameName: string
}

/**
 * Plain-language "why this fired" panel for a decision-table frame. The "Break when a rule fires"
 * toggle suspends on any firing, and each rule has a gutter to break on that specific rule; both can be
 * set before any rule fires. Once a rule fires the panel shows which rule it was and how each evaluated
 * condition turned out, mirroring the green/red table highlight as a scannable list.
 */
const DecisionPanel: React.FC<DecisionPanelProps> = ({ decision, frameUri, frameName }) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const breakpoints = useTraceStore(s => s.breakpoints)
    const toggleBreakpoint = useTraceStore(s => s.toggleBreakpoint)

    const breakpointKey = `${frameUri}#${RULE_FIRED_REF}`
    const breakOnFire = breakpoints.includes(breakpointKey)
    const fired = new Set(decision?.firedRules)

    const ruleFireToggle = (
        <Tooltip title={t('decision.breakOnFireHint')}>
            <Checkbox
                checked={breakOnFire}
                data-testid="decision-break-on-fire"
                onChange={() => toggleBreakpoint(breakpointKey, t('decision.breakpointLabel', { table: frameName }))}
            >
                {t('decision.breakOnFire')}
            </Checkbox>
        </Tooltip>
    )

    return (
        <Card
            className={styles.card}
            data-testid="decision-panel"
            extra={ruleFireToggle}
            size="small"
            title={t('details.decision')}
        >
            {decision ? (
                <>
                    <div className={styles.summary}>
                        {decision.firedRules.length > 0
                            ? t('decision.fired', { rules: decision.firedRules.join(', ') })
                            : t('decision.noneFired')}
                    </div>
                    {groupByRule(decision).map(({ rule, conditions }) => {
                        const ruleKey = `${frameUri}#${rule}`
                        const hasRuleBreakpoint = breakpoints.includes(ruleKey)
                        const bpTooltip = hasRuleBreakpoint ? t('debug.removeBreakpoint') : t('debug.addBreakpoint')
                        const ruleLabel = t('decision.ruleBreakpointLabel', { table: frameName, rule })
                        return (
                            <div key={rule} className={cx(styles.rule, fired.has(rule) && styles.firedRule)}>
                                <Tooltip title={bpTooltip}>
                                    <span
                                        className={cx(styles.gutter, hasRuleBreakpoint && styles.gutterActive)}
                                        data-testid={`decision-rule-bp-${rule}`}
                                        onClick={() => toggleBreakpoint(ruleKey, ruleLabel)}
                                    />
                                </Tooltip>
                                <span className={styles.ruleName}>{rule}</span>
                                <span className={styles.conditions}>
                                    {conditions.map((c, i) => (
                                        <Tag
                                            key={i}
                                            color={c.matched ? 'success' : 'error'}
                                            icon={c.matched ? <CheckOutlined /> : <CloseOutlined />}
                                        >
                                            {c.condition}
                                        </Tag>
                                    ))}
                                </span>
                            </div>
                        )
                    })}
                </>
            ) : (
                <div className={styles.summary}>{t('decision.notYetFired')}</div>
            )}
        </Card>
    )
}

export default DecisionPanel
