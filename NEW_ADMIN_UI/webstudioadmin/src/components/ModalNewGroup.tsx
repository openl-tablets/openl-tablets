import React, { useState } from 'react';
import { Button, } from 'antd';

export function ModalNewGroup() {
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

    return(
        <div>
            <Button onClick={showModal}>
                Add new group
            </Button>
        </div>
    )}