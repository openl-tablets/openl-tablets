import { FC, Fragment, useContext, useEffect, useState } from 'react'
import { Modal, Spin, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { SystemContext } from '../../contexts'
import {
    expressionParts,
    fetchLicenses,
    type License,
    type LicenseSide,
    openText,
    publicLicense,
} from '../../services/licenses'
import { formatDate } from '../../utils/dateFormat'
import { errorHandler } from '../../utils/errorHandling'
import { useStyles } from './AboutModal.styles'

/** Where the license OpenL Studio is distributed under is published. */
const LGPL_URL = 'https://www.gnu.org/licenses/lgpl-3.0.html'

/** The libraries are listed in one region that scrolls, so the dialog fits the window with both sides expanded. */
const DIALOG_BODY = { body: { maxHeight: '70vh', overflow: 'auto' } }

/** Wide enough for the longest license of a library, with its NOTICE, to stay on one line beside the library. */
const DIALOG_WIDTH = 880

/** The libraries of each side, `null` where their list could not be read. */
type Libraries = Record<LicenseSide, License[] | null>

type Styles = ReturnType<typeof useStyles>['styles']

const readLibraries = (side: LicenseSide): Promise<License[] | null> =>
    fetchLicenses(side).catch((error: unknown) => {
        errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
        return null
    })

interface LibraryLicenseProps {
    library: License
    /** What the license of a library that names none is called. */
    unnamed: string
    styles: Styles
}

/**
 * The license of a library.
 *
 * The text the library ships opens in a new window, as plain text. A library shipping none links each standard license
 * it names to its public text, or, naming no standard license, to the address its POM gives.
 */
const LibraryLicense: FC<LibraryLicenseProps> = ({ library: { identifier, text, url }, unnamed, styles }) => {
    const parts = expressionParts(identifier ?? '')
    if (text) {
        return (
            <button className={styles.license} onClick={() => openText(text)} type="button">
                {/* Each license stays whole on a line, as a link does: the line breaks at the spaces between. */}
                {identifier
                    ? parts.map((part, index) => part.trim() ? <span key={`${index}:${part}`}>{part}</span> : part)
                    : unnamed}
            </button>
        )
    }
    if (!parts.some(publicLicense)) {
        return url ? <a href={url} rel="noopener noreferrer" target="_blank">{identifier ?? unnamed}</a> : identifier
    }
    return parts.map((part, index) => {
        const address = publicLicense(part)
        return (
            <Fragment key={`${index}:${part}`}>
                {address ? <a href={address} rel="noopener noreferrer" target="_blank">{part}</a> : part}
            </Fragment>
        )
    })
}

interface SideProps {
    title: string
    libraries: License[] | null
    styles: Styles
}

/** The libraries of one side, drawn only while the reader keeps the side open. */
const Side: FC<SideProps> = ({ title, libraries, styles }) => {
    const { t } = useTranslation()
    const [open, setOpen] = useState(false)
    const unnamed = t('common:about.license')

    return (
        <details className={styles.side} onToggle={event => setOpen(event.currentTarget.open)}>
            <summary>{title}</summary>
            {open && (libraries ? (
                <ul className={styles.list}>
                    {libraries.map(library => (
                        <li key={`${library.name}@${library.version}`}>
                            <span>{library.name} {library.version}</span>
                            <span className={styles.licenses}>
                                <LibraryLicense library={library} styles={styles} unnamed={unnamed} />
                            </span>
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
        <Modal
            destroyOnHidden
            footer={null}
            onCancel={onClose}
            open={open}
            styles={DIALOG_BODY}
            title={t('common:about.title')}
            width={DIALOG_WIDTH}
        >
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
