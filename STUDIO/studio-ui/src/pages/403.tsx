import { Button } from 'antd'
import Logo from '../components/Logo'
import { useStyles } from '../styles/splashCard.styles'

const Forbidden = () => {
    const { styles } = useStyles()

    return (
        <div className={styles.container}>
            <div className={styles.card}>
                <Logo height={72} width={72} />
                <div className={styles.code}>403</div>
                <div className={styles.message}>Access denied.<br />Log out and use other credentials.</div>
                <Button href="logout" size="large" type="primary">Log out</Button>
            </div>
        </div>
    )
}

export default Forbidden
