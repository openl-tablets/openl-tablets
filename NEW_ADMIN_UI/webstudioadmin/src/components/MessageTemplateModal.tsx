import React, { useState } from 'react';
import { Button, Modal } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

export const MessageTemplateModal:React.FC =() =>{
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
                    <p>Comment message template for Git commits.</p>
                    <ul>

                        <li>&#123;user-message&#125;  is replaced by user defined commit message. This part of commit message is mandatory.</li>
                        <li>&#123;commit-type&#125; is a system property for commits to recognize commit type from a message. This part of commit message is mandatory.</li>
                    </ul>

                    <p>NOTE: Keep default value for non Git repositories.</p>

                </div>
            </Modal>
        </>
    )
};
