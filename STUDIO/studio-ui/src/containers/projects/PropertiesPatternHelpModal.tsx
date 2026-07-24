import { useTranslation } from 'react-i18next'
import { Modal } from 'antd'
import { createStyles } from 'antd-style'

/** The property names a module-level pattern may carry, as the engine reads them. */
const PROPERTY_NAMES = [
    'effectiveDate', 'expirationDate', 'startRequestDate', 'endRequestDate', 'lob', 'usregion',
    'country', 'currency', 'lang', 'state', 'region', 'origin', 'caProvinces', 'caRegions',
]

const DATE_FORMAT_URL = 'https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html'

/** Several patterns on their own lines, shown verbatim as the legacy help did. */
const SEVERAL_PATTERNS_EXAMPLE = [
    '%state%-%startRequestDate:yyyyMMdd%',
    '%state%-%startRequestDate:yyyyMMdd%-%effectiveDate:yyyyMMdd%',
    'Common-%startRequestDate,effectiveDate:yyyyMMdd%',
].join('\n')

const useStyles = createStyles(({ css, token }) => ({
    body: css`
        font-size: 13px;
        line-height: 1.6;

        p {
            margin: 0 0 12px;
        }

        ul {
            margin: 0 0 12px;
            padding-left: 20px;
        }

        li {
            margin-bottom: 8px;
        }

        code {
            font-family: ${token.fontFamilyCode};
            font-size: 12px;
        }

        pre {
            margin: 6px 0 0;
            padding: 8px 10px;
            border-radius: ${token.borderRadiusSM}px;
            background: ${token.colorFillQuaternary};
            font-family: ${token.fontFamilyCode};
            font-size: 12px;
            white-space: pre-wrap;
        }
    `,
}))

interface PropertiesPatternHelpModalProps {
    open: boolean
    onClose: () => void
}

/**
 * The full explanation of a module's file name pattern — how module-level properties are read from a file
 * name — carried over from the legacy screen. It is long, so it opens in its own dialog rather than a hint.
 */
export const PropertiesPatternHelpModal = ({ open, onClose }: PropertiesPatternHelpModalProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    return (
        <Modal
            footer={null}
            onCancel={onClose}
            open={open}
            title={t('browser.overview.pattern_help_title')}
            width={640}
        >
            <div className={styles.body} data-testid="pattern-help-body">
                <p>{t('browser.overview.pattern_help_intro')}</p>
                <ul>
                    <li>{t('browser.overview.pattern_help_text_symbols')}</li>
                    <li>
                        {t('browser.overview.pattern_help_property_names')}{' '}
                        {PROPERTY_NAMES.map((name, index) => (
                            <span key={name}>
                                <code>{name}</code>{index < PROPERTY_NAMES.length - 1 ? ', ' : ';'}
                            </span>
                        ))}
                    </li>
                    <li>
                        {t('browser.overview.pattern_help_date_format')}{' '}
                        <a href={DATE_FORMAT_URL} rel="noreferrer" target="_blank">
                            {t('browser.overview.pattern_help_date_format_link')}
                        </a>
                        <pre>%effectiveDate:yyyyMMdd%</pre>
                    </li>
                    <li>
                        {t('browser.overview.pattern_help_same_type')}
                        <pre>%effectiveDate,startRequestDate:yyyyMMdd%</pre>
                    </li>
                    <li>
                        {t('browser.overview.pattern_help_several')}
                        <pre>{SEVERAL_PATTERNS_EXAMPLE}</pre>
                    </li>
                </ul>
                <p>
                    {t('browser.overview.pattern_help_example', { pattern: 'AUTO-%state%-%effectiveDate:MMddyyyy%' })}
                </p>
                <p>
                    {t('browser.overview.pattern_help_inherited', { file: 'AUTO-FL-01022014.xlsx' })}
                </p>
                <p>
                    {t('browser.overview.pattern_help_comma', { file: 'AUTO-FL,NY-01022014.xlsx' })}
                </p>
                <p>
                    {t('browser.overview.pattern_help_any', { file: 'AUTO-Any-01022020.xlsx' })}
                </p>
                <p>{t('browser.overview.pattern_help_no_match')}</p>
            </div>
        </Modal>
    )
}
