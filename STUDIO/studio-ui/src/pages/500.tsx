import { Button } from 'antd'
import Logo from '../components/Logo'
import { useStyles } from '../styles/splashCard.styles'

const ServerError = () => {
    const { styles } = useStyles()

    return (
        <div className={styles.container}>
            <div className={styles.card}>
                <Logo height={72} width={72} />
                <div className={styles.code}>500</div>
                <div className={styles.message}>Internal server error.<br />Try again later.</div>
                <Button href="" size="large" type="primary">Home</Button>
            </div>
        </div>
    )
}

export default ServerError
