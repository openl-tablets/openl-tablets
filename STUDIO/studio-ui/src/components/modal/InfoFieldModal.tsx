import React, { useState } from 'react'
import { Button, Modal, theme } from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'

interface InfoFieldModalProps {
  text: React.ReactNode;
}

export const InfoFieldModal: React.FC<InfoFieldModalProps> = ({ text }) => {
    const { t } = useTranslation()
    const { token } = theme.useToken()
    const [isModalOpen, setIsModalOpen] = useState(false)

    const showModal = () => {
        setIsModalOpen(true)
    }

    const handleClose = () => {
        setIsModalOpen(false)
    }

    return (
        <>
            <InfoCircleOutlined onClick={showModal} style={{ color: token.colorTextTertiary, marginLeft: 5 }} />
            <Modal
                footer={[<Button onClick={handleClose}>OK</Button>]}
                onCancel={handleClose}
                open={isModalOpen}
                title={t('common:details')}
            >
                <p>
                    {text}
                </p>
            </Modal>
        </>
    )
}

export default InfoFieldModal
