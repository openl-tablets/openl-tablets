import React, { useState } from 'react';
import { Button, Modal } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

export const RestoreFromOldVersionModal: React.FC = () => {
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
                </Button>]}>

                <ul>
                This specified value is used as default message when a project is restored from old version.

                    <li>&#123;revision&#125; is replaced by old revision number.</li>
                    <li>&#123;author&#125; is replaced by the author of old project version.</li>
                    <li>&#123;datetime&#125; is replaced by the date of old project version.</li>
                </ul>


            </Modal>
        </>
    );
};