import { FC, useContext, useEffect, useState } from 'react'
import { Modal, Spin, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { SystemContext } from '../../contexts'
import { canOpenLicense, fetchLicenses, type License, type LicenseSide, openLicense } from '../../services/licenses'
import { formatDate } from '../../utils/dateFormat'
import { errorHandler } from '../../utils/errorHandling'
import { useStyles } from './AboutModal.styles'

/** Where the license OpenL Studio is distributed under is published. */
const LGPL_URL = 'https://www.gnu.org/licenses/lgpl-3.0.html'

/** The libraries of each side, `null` where their list could not be read. */
type Libraries = Record<LicenseSide, License[] | null>

const readLibraries = (side: LicenseSide): Promise<License[] | null> =>
    fetchLicenses(side).catch((error: unknown) => {
        errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
        return null
    })

interface SideProps {
    title: string
    libraries: License[] | null
    styles: ReturnType<typeof useStyles>['styles']
}

/** The libraries of one side, drawn only while the reader keeps the side open. */
const Side: FC<SideProps> = ({ title, libraries, styles }) => {
    const { t } = useTranslation()
    const [open, setOpen] = useState(false)

    return (
        <details className={styles.side} onToggle={event => setOpen(event.currentTarget.open)}>
            <summary>{title}</summary>
            {open && (libraries ? (
                <ul className={styles.list}>
                    {libraries.map(library => (
                        <li key={`${library.name}@${library.version}`}>
                            <span>{library.name} {library.version}</span>
                            {canOpenLicense(library) ? (
                                <button className={styles.license} onClick={() => openLicense(library)} type="button">
                                    {library.identifier ?? t('common:about.license')}
                                </button>
                            ) : library.identifier}
                        </li>
                    ))}
                </ul>
            ) : <Typography.Text type="secondary">{t('common:about.unavailable')}</Typography.Text>)}
        </details>
    )
}

interface AboutModalProps {
    open: boolean
    onClose: () => void
}

/**
 * The version, the build date and the license of OpenL Studio, and the third-party libraries it ships, each with its
 * license.
 *
 * The lists are read once, when the dialog is shown for the first time. A list the server cannot answer with is
 * reported as not available, and the other list is still shown.
 */
export const AboutModal: FC<AboutModalProps> = ({ open, onClose }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const { appVersion, openlInfo } = useContext(SystemContext)
    const [libraries, setLibraries] = useState<Libraries>()

    useEffect(() => {
        void Promise.all([readLibraries('frontend'), readLibraries('backend')])
            .then(([frontend, backend]) => setLibraries({ frontend, backend }))
    }, [])

    return (
        <Modal destroyOnHidden footer={null} onCancel={onClose} open={open} title={t('common:about.title')}>
            <dl className={styles.facts}>
                <dt>{t('common:about.version')}</dt>
                <dd>{appVersion}</dd>
                <dt>{t('common:about.build_date')}</dt>
                <dd>{openlInfo && formatDate(openlInfo['openl.build.date'])}</dd>
                <dt>{t('common:about.license')}</dt>
                <dd>
                    <Typography.Link href={LGPL_URL} rel="noopener noreferrer" target="_blank">
                        {t('common:about.lgpl')}
                    </Typography.Link>
                </dd>
            </dl>
            <Typography.Paragraph>{t('common:about.libraries')}</Typography.Paragraph>
            {libraries ? (
                <>
                    <Side
                        libraries={libraries.frontend}
                        styles={styles}
                        title={t('common:about.frontend', { count: libraries.frontend?.length ?? 0 })}
                    />
                    <Side
                        libraries={libraries.backend}
                        styles={styles}
                        title={t('common:about.backend', { count: libraries.backend?.length ?? 0 })}
                    />
                </>
            ) : <Spin />}
        </Modal>
    )
}
