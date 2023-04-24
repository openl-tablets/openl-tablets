import React, { useState } from 'react';
import { Button, Cascader, Checkbox, Col, Form, Input, Modal, Row } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import Icon, { InfoCircleOutlined } from '@ant-design/icons';

export function InvalidBranchModal() {
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
                    <p>An error message that will be shown to the user when trying to create a new branch with a name that does not match the additional regular expression.
</p>
                </div>
            </Modal>
        </>
    )
}