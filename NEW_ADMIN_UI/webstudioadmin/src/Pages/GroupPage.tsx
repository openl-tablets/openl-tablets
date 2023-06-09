import React, { useEffect, useState } from 'react';
import { Button, Card, Table, Tag, Form, Input, Divider, Row, Col, Modal } from 'antd';
import { CloseCircleOutlined } from '@ant-design/icons';
import DefaultLayout from '../components/DefaultLayout';
import { NewGroupModal } from 'views/groups/NewGroupModal';
import { EditGroupModal } from 'views/groups/EditGroupModal';
import './groupPage.css';
import { NewGroupModal1 } from 'views/groups/NewGroupModal1';


type Group = {
    name: string;
    id: number;
    description: string;
    roles: string[];
    privileges: string[];
};

export const GroupPage: React.FC = () => {

    const apiURL = process.env.REACT_APP_API_URL;
    const authorization = process.env.REACT_APP_AUTHORIZATION;

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [groupData, setGroupData] = useState<Group[]>([]);
    const [selectedGroup, setSelectedGroup] = useState<any>({});
    const [privileges, setPrivileges] = useState([]);
    const [selecetedGroupPrivileges, setSelectedGroupPrivileges] = useState([]);
    const [allPrivileges, setAllPrivileges] = useState([]);

    const showEditGroupModal = () => {
        setIsModalOpen(true);
    };

    const hideEditGroupModal = () => {
        setIsModalOpen(false);
    };

    // const headers = new Headers();
    // headers.append('Authorization', authorization || '');

    const fetchGroups = async () => {
        try {
            const headers = new Headers();
            headers.append('Authorization', authorization || '');

            const response = await fetch(`${apiURL}/admin/management/groups`, {
                headers
            });
            if (response.ok) {
                const responseObject = await response.json();
                const groups = Object.entries(responseObject).map(([groupName, group]: [string, unknown]) => ({
                    groupName,
                    ...(group as Group),
                    privileges: (group as Group).privileges || [],
                }));
                setGroupData(groups);
                console.log('8.', responseObject)
            } else {
                console.error("9. Failed to fetch groups:", response.statusText);
            }
        } catch (error) {
            console.error("10. Error fetching groups:", error);
        }
    };

    useEffect(() => {
        fetchGroups();
    }, []);

    const removeGroup = (id: number) => {
        Modal.confirm({
            className: "confirm-group-modal",
            title: "Confirm Deletion",
            content: "Are you sure you want to delete this group?",

            onOk: () => {
                const headers = new Headers();
                headers.append('Authorization', authorization || '');

                fetch(`${apiURL}/admin/management/groups/${id}`, {
                    method: "DELETE",
                    headers
                })
                    .then(fetchGroups);
            },
            onCancel: () => {
            }
        });
    };

    const updateGroup = (updatedGroup: any) => {
        setGroupData((groupData) =>
            groupData.map((group: any) => (group.key === updatedGroup.key ? updatedGroup : group))
        );
    };

    // const fetchPrivileges = async () => {
    //     try {
    //         const headers = new Headers();
    //         headers.append('Authorization', authorization || '');

    //         const response = await fetch(`${apiURL}/admin/management/privileges`, {
    //             headers
    //         });

    //         if (response.ok) {
    //             const jsonResponse = await response.json();
    //             setAllPrivileges(jsonResponse);
    //         } else {
    //             console.error("15. Failed to fetch privileges:", response.statusText);
    //         }
    //     } catch (error) {
    //         console.error("16. Error fetching privileges:", error);
    //     }
    // };

    const handleDoubleRowClick = (record: any) => {
        setSelectedGroup({ ...record });
        setSelectedGroupPrivileges(record.privileges);
        showEditGroupModal();
        console.log('11. Clicked row:', record);
        console.log('12. Privileges:', record.privileges);
    };

    const columns = [
        {
            title: "Name",
            dataIndex: "groupName",
            key: "name",
            render: (text: string) => <span>{text}</span>,
        },
        {
            title: "Description",
            dataIndex: "description",
            key: "description",
        },
        {
            title: "Roles",
            key: "roles",
            render: (data: { roles: string[], privileges: string[] }) => (
                <div>
                    {data.roles &&
                        data.roles.length > 0 &&
                        data.roles.map((role: string) => {
                            let color = "default";
                            if (["Administrators"].includes(role)) {
                                color = "red";
                            } else if (["Developers", "Testers", "Viewers", "Deployers", "Analysts"].includes(role)) {
                                color = "blue";
                            }
                            return (
                                <Tag color={color} key={role} style={{ margin: 2 }}>
                                    {role}
                                </Tag>
                            );
                        })}

                    {data.privileges &&
                        data.privileges.length > 0 &&
                        data.privileges.map((privilege: string) => {
                            let color = "";
                            if (["ADMIN", "Administrate"].includes(privilege)) {
                                color = "red";
                            } else {
                                color = "default"
                            }
                            return (
                                <Tag color={color} key={privilege} style={{ margin: 2 }}>
                                    {privilege}
                                </Tag>
                            );
                        })}
                </div>
            ),
        },
        {
            title: "Action",
            dataIndex: "Action",
            key: "Action",
            render: (text: string, record: any) => (
                <Button
                    type="text"
                    icon={<CloseCircleOutlined />}
                    onClick={() => removeGroup(record.id)}
                />
            ),
        },
    ]

    return (
        <DefaultLayout>
            <Card style={{ margin: 20, width: 900 }}>
                <Form>
                    <Form.Item label={
                        <span>
                            Default group for all users * &nbsp;
                        </span>
                    }
                    >
                        <Row>
                            <Col flex="auto"><Input /></Col>
                            <Col offset={1}><Button style={{ color: "green", borderColor: "green" }}>Apply and Restart</Button></Col>
                        </Row>
                    </Form.Item>
                </Form>
                <Divider />
                <Table
                    columns={columns}
                    dataSource={groupData}
                    pagination={{ hideOnSinglePage: true }}
                    onRow={(record) => ({
                        onDoubleClick: () => handleDoubleRowClick(record),
                    })}
                />
                <NewGroupModal fetchGroups={fetchGroups} />
                <NewGroupModal1 fetchGroups={fetchGroups} />
                <Modal
                    className='edit-group-modal'
                    open={isModalOpen}
                    onCancel={hideEditGroupModal}
                    footer={null}
                >
                    {isModalOpen && (
                        <EditGroupModal
                            group={selectedGroup}
                            updateGroup={updateGroup}
                            onSave={hideEditGroupModal}
                        />
                    )}
                </Modal>
            </Card>
        </DefaultLayout>
    )
};

