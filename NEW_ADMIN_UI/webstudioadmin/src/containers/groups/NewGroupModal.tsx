import React, { useEffect, useState } from 'react'
import { Button, Checkbox, Form, Input, Modal, Row } from 'antd'
import { stringify } from 'querystring'
import { apiCall } from '../../services'
import { useTranslation } from 'react-i18next'
import { Role } from '../../constants'

export const NewGroupModal: React.FC = ({}) => {
    const { t } = useTranslation()
    const [isModalOpen, setIsModalOpen] = useState(false)
    const [name, setName] = useState('')
    const [description, setDescription] = useState('')
    const [admin, setAdmin] = useState(false)

    const showModal = () => {
        setIsModalOpen(true)
    }

    const hideModal = () => {
        setIsModalOpen(false)
    }

    const createGroup = async () => {
        try {
            const requestBody = {
                name,
                description,
            }

            const encodedBody = stringify(requestBody)

            await apiCall('/admin/management/groups', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: encodedBody,

            }).then(applyResult)
            setName('')
            setDescription('')
            setIsModalOpen(false)
        } catch (error) {
            console.error('Error creating group:', error)
        }
    }

    const applyResult = (result: any) => {
        hideModal()
    }

    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault()
        createGroup()
    }

    const formItemLayout = {
        labelCol: {
            sm: { span: 6 },
        },
    };

    return (
        <div>
            <Row justify="end">
                <Button onClick={showModal} style={{ marginTop: 20 }} type="primary">
                    {t('groups:invite_group')}
                </Button>
            </Row>
            <Modal
                className="new-group-modal"
                onCancel={hideModal}
                open={isModalOpen}
                title={t('groups:invite_group')}
                width={850}
                footer={[
                    <Button key="back" onClick={hideModal}>
                        {t('groups:cancel')}
                    </Button>,
                    <Button key="submit" onClick={handleSubmit} style={{ marginTop: 15 }} type="primary">
                        {t('groups:invite')}
                    </Button>]}
            >
                <div>
                    <Form style={{ width: 800 }} {...formItemLayout}>
                        <Form.Item label="Name">
                            <Input id="name" onChange={(e) => setName(e.target.value)} value={name} />
                        </Form.Item>
                        <Form.Item label="Description">
                            <Input id="description" onChange={(e) => setDescription(e.target.value)} value={description} />
                        </Form.Item>
                        <Form.Item label={'Admin'}>
                            <Checkbox name={'admin'} onChange={(e) => setAdmin(e.target.checked)} checked={admin}/>
                        </Form.Item>
                    </Form>
                </div>
            </Modal>
        </div>
    )
}
