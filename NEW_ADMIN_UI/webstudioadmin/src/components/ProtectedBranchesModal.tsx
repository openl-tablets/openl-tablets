import React, { useState } from 'react';
import { Button, Modal } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

export const ProtectedBranchesModal:React.FC = () => {
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
                <p>
                    <ol>
                        The list of protected branches must be comma separated.
                        <li>? - maches any single characters</li>
                        <li>* - matches simple branch names like master. If a branch name has a path separator, it will be skipped</li>
                        <li>** - matches all branches</li>
                        <li>*.* - matches simple branches containing a dot</li>
                        <li>*.&#123;10, 11&#125;- matches branch ending with .10 or .11</li>
                    </ol>
                    Example: release-*
                </p>
            </Modal>
        </>
    );
};