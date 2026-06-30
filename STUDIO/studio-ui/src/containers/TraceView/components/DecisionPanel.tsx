import React, { useState } from 'react'
import { Button, Card, Checkbox, Select, Tag, Tooltip } from 'antd'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import type { DecisionView } from 'types/trace'
import { useStyles } from './DecisionPanel.styles'

/** Breakpoint key suffix that suspends when any rule fires; mirrors the backend CurrentLocation.RULE_FIRED_REF. */
const RULE_FIRED_REF = 'rule'

/** A collect-all table can fire hundreds of rules; cap the summary and the per-rule breakdown so the panel stays usable. */
const FIRED_PREVIEW = 12
const RULE_ROWS_CAP = 25

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
    ruleNames?: string[] | null
    frameUri: string
    frameName: string
}

/**
 * Plain-language "why this fired" panel for a decision-table frame. A breakpoint can be set on any
 * firing (the "Break when a rule fires" toggle) or on a specific rule — picked from the dropdown of all
 * rules or via the gutter beside an evaluated rule — all before any rule fires. Once a rule fires the
 * panel shows which rule it was and how each evaluated condition turned out.
 */
const DecisionPanel: React.FC<DecisionPanelProps> = ({ decision, ruleNames, frameUri, frameName }) => {
    const { t } = useTranslation('trace')
    const { styles, cx } = useStyles()
    const breakpoints = useTraceStore(s => s.breakpoints)
    const toggleBreakpoint = useTraceStore(s => s.toggleBreakpoint)
    const [showAllRules, setShowAllRules] = useState(false)

    const bpKey = (ref: string): string => `${frameUri}#${ref}`
    const breakpointKey = bpKey(RULE_FIRED_REF)
    const breakOnFire = breakpoints.includes(breakpointKey)
    const fired = new Set(decision?.firedRules)
    const ruleLabel = (rule: string): string => t('decision.ruleBreakpointLabel', { table: frameName, rule })
    const armedRules = (ruleNames ?? []).filter(rule => breakpoints.includes(bpKey(rule)))

    const firedRules = decision?.firedRules ?? []
    const firedSummary = firedRules.length === 0
        ? t('decision.noneFired')
        : firedRules.length <= FIRED_PREVIEW
            ? t('decision.fired', { rules: firedRules.join(', ') })
            : t('decision.firedCount', { count: firedRules.length, rules: firedRules.slice(0, FIRED_PREVIEW).join(', ') })
    const groups = decision ? groupByRule(decision) : []
    const visibleGroups = showAllRules ? groups : groups.slice(0, RULE_ROWS_CAP)

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
            {ruleNames && ruleNames.length > 0 && (
                <div className={styles.rulePicker}>
                    <span className={styles.rulePickerLabel}>{t('decision.breakOnRule')}</span>
                    <Select
                        className={styles.ruleSelect}
                        data-testid="decision-rule-select"
                        mode="multiple"
                        onDeselect={(rule: string) => toggleBreakpoint(bpKey(rule), ruleLabel(rule))}
                        onSelect={(rule: string) => toggleBreakpoint(bpKey(rule), ruleLabel(rule))}
                        options={ruleNames.map(rule => ({ value: rule, label: rule }))}
                        placeholder={t('decision.breakOnRulePlaceholder')}
                        size="small"
                        value={armedRules}
                    />
                </div>
            )}
            {decision ? (
                <>
                    <div className={styles.summary}>{firedSummary}</div>
                    {visibleGroups.map(({ rule, conditions }) => {
                        const ruleKey = bpKey(rule)
                        const hasRuleBreakpoint = breakpoints.includes(ruleKey)
                        const bpTooltip = hasRuleBreakpoint ? t('debug.removeBreakpoint') : t('debug.addBreakpoint')
                        return (
                            <div key={rule} className={cx(styles.rule, fired.has(rule) && styles.firedRule)}>
                                <Tooltip title={bpTooltip}>
                                    <span
                                        className={cx(styles.gutter, hasRuleBreakpoint && styles.gutterActive)}
                                        data-testid={`decision-rule-bp-${rule}`}
                                        onClick={() => toggleBreakpoint(ruleKey, ruleLabel(rule))}
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
                    {groups.length > RULE_ROWS_CAP && (
                        <Button
                            className={styles.showAll}
                            data-testid="decision-show-all-rules"
                            onClick={() => setShowAllRules(value => !value)}
                            size="small"
                            type="link"
                        >
                            {showAllRules
                                ? t('decision.showFewer')
                                : t('decision.showAllRules', { count: groups.length })}
                        </Button>
                    )}
                </>
            ) : (
                <div className={styles.summary}>{t('decision.notYetFired')}</div>
            )}
        </Card>
    )
}

export default DecisionPanel
