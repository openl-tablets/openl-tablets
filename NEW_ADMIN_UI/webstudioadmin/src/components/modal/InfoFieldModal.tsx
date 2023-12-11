import React, { useState } from 'react'
import { Button, Modal } from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'

interface InfoFieldModalProps {
  text: React.ReactNode;
}

export const InfoFieldModal: React.FC<InfoFieldModalProps> = ({ text }) => {
    const [ isModalOpen, setIsModalOpen ] = useState(false)

    const showModal = () => {
        setIsModalOpen(true)
    }

    const handleClose = () => {
        setIsModalOpen(false)
    }

    return (
        <>
            <InfoCircleOutlined onClick={showModal} />
            <Modal
                footer={[ <Button onClick={handleClose}>OK</Button> ]}
                open={isModalOpen}
                title="Details"
            >
                <div>
                    <p>
                        {text}
                    </p>
                </div>
            </Modal>
        </>
    )
}

export default InfoFieldModal
