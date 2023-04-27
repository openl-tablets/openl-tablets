import React, { useState } from 'react';
import { Button, Modal } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

export const RemoteRepoModal:React.FC = () => {
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

            <Modal title="Details" open={isModalOpen}  footer={[
          <Button key="back" onClick={handleClose}>
            OK
          </Button>]}>
                <ul><li>If checked, use remote Git repository. WebStudio will pull and push changes to it.</li>
                    <li>If unchecked, repository is stored in local file system only.</li></ul>
            </Modal>
        </>
    );
};

