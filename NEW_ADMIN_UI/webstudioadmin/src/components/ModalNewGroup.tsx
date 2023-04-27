import React, { useState } from 'react';
import { Button, } from 'antd';

export const ModalNewGroup: React.FC = () => {
    const [isModalOpen, setIsModalOpen] = useState(false);

    const showModal = () => {
        setIsModalOpen(true);
    };

    const handleCreate = () => {
        setIsModalOpen(false);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
    };

    return (
        <div>
            <Button onClick={showModal}>
                Add new group
            </Button>
        </div>
    )
};
