import { Button } from 'antd'
import Logo from '../components/Logo'
import { useStyles } from '../styles/splashCard.styles'

const NotFound = () => {
    const { styles } = useStyles()

    return (
        <div className={styles.container}>
            <div className={styles.card}>
                <Logo height={72} width={72} />
                <div className={styles.code}>404</div>
                <div className={styles.message}>Page not found.<br />Go to the home page.</div>
                <Button href="" size="large" type="primary">Home</Button>
            </div>
        </div>
    )
}

export default NotFound
