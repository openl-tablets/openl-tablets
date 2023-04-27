import React, { useState } from 'react';
import { Button, Modal } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

export const DefaultBranchModal:React.FC =() =>{
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
                    <p>This is a pattern for new branches in Git repository.</p>
                    <ul>

                        <li>&#123;project - name&#125; is replaced by project name.</li>
                        <li>&#123;username&#125; is replaced by username.</li>
                        <li>&#123;current - date&#125; is replaced by current date.</li>
                    </ul>

                    <p>NOTE: Must not contain the following characters:&#92; &#58; &#42; &#63; &#34; &#39; &#60; &#62; &#124; &#123; &#125; &#126; &#94; </p>

                </div>
            </Modal>
        </>
    )
};
