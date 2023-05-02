import React, { useState } from 'react';
import { Button, Modal } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

export const InvalidUserMessageHintModal:React.FC =() =>{
    const [isModalOpen, setIsModalOpen] = useState(false);

    const showModal = () => {
        setIsModalOpen(true);
    };

    const handleClose = () => {
        setIsModalOpen(false);
    };

    return (
        <>
            <InfoCircleOutlined onClick={showModal} />

            <Modal title="Details" open={isModalOpen} footer={[
                <Button key="back" onClick={handleClose}>
                    OK
                </Button>]} >
                <div>
                    <p>This message is shown to user if user message is not matched to message validation pattern.</p>
                </div>
            </Modal>
        </>
    )
};
