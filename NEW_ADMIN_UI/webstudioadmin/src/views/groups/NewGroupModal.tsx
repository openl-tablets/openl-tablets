import React, { useEffect, useState, useMemo } from 'react';
import { Button, Form, Input, Modal, Table } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { stringify } from 'querystring';
import './newGroupModal.css';
import { CheckOutlined } from '@ant-design/icons';

type Group = {
    name: string;
    id: number;
    description: string;
    roles: string[];
    privileges: string[];
};

interface DataType {
    key: React.Key;
    privilege: string;
    [groupName: string]: string | React.Key;
};

interface Privilege {
    [key: string]: string;
};

export const NewGroupModal: React.FC<{ fetchGroups: () => void }> = ({ fetchGroups }) => {

    const apiURL = process.env.REACT_APP_API_URL;

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [privileges, setPrivileges] = useState<CheckboxValueType[]>([]);
    const [selectedColumns, setSelectedColumns] = useState<string[]>([]);
    const [allPrivileges, setAllPrivileges] = useState<Privilege[]>([]);
    const [groupData, setGroupData] = useState<Group[]>([]);
    const [privilegeCheckboxes, setPrivilegeCheckboxes] = useState<{ [privilege: string]: boolean }>({});


    const showModal = () => {
        setIsModalOpen(true);
        setPrivileges([]);
    };

    const hideModal = () => {
        setIsModalOpen(false);
    };

    const fetchGroupData = async () => {
        try {
            const response = await fetch(`${apiURL}/admin/management/groups`, {
                headers: {
                    "Authorization": "Basic YWRtaW46YWRtaW4=",
                }
            });
            if (response.ok) {
                const responseObject = await response.json();
                setGroupData(responseObject);
            } else {
                console.error("Failed to fetch groups:", response.statusText);
            }
        } catch (error) {
            console.error("Error fetching groups:", error);
        }
    };

    useEffect(() => {
        fetchGroupData();
    }, []);

    const fetchPrivileges = async () => {
        try {
            const response = await fetch(`${apiURL}/admin/management/privileges`, {
                headers: {
                    "Authorization": "Basic YWRtaW46YWRtaW4=",
                    "Content-Type": "application/json"
                }
            });
            if (response.ok) {
                const jsonResponse = await response.json();
                setAllPrivileges(jsonResponse);
                console.log("allPrivileges", allPrivileges)
            } else {
                console.error("Failed to fetch privileges:", response.statusText);
            }
        } catch (error) {
            console.error("Error fetching privileges:", error);
        }
    };

    useEffect(() => {
        fetchPrivileges();
    }, []);

    const createGroup = async () => {
        try {
            const requestBody = {
                name,
                description,
                privilege: privileges.map((privilege) => privilege.toString()),
                selectedColumns,
            };

            const encodedBody = stringify(requestBody);
            console.log("1.Request BODY", requestBody);

            await fetch(`${apiURL}/admin/management/groups`, {
                method: "POST",
                headers: {
                    "Authorization": "Basic YWRtaW46YWRtaW4=",
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: encodedBody,

            }).then(applyResult);
            setName("");
            setDescription("");
            setPrivileges([]);
            setSelectedColumns([]);
            console.log("2.ENCODED BODY", encodedBody);

            setIsModalOpen(false);
            console.log("3.Created Group:", requestBody);
            console.log("4.Added privileges:", requestBody.privilege)
            fetchGroups();
        } catch (error) {
            console.error("5.Error creating group:", error);
        }
    };

    const applyResult = (result: any) => {
        hideModal();
    };

    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault();
        createGroup();
    };

    const rowSelection = {
        onChange: (selectedRowKeys: React.Key[], selectedRows: Privilege[], groupKey: string) => {
            const privileges = selectedRows.map((row) => row[Object.keys(row)[1]]) as CheckboxValueType[];
            handleGroupButtonClick(groupKey);
            setPrivileges(privileges);
            console.log("selected privileges: ---> ", privileges);
            markCheckboxes(privileges);
        }
    };

    const handleGroupButtonClick = (groupKey: string) => {
        const selectedPrivileges: string[] = [];

        if (groupData && groupData[groupKey]) {
            const group: Group = groupData[groupKey];
            selectedPrivileges.push(...group.privileges);
        }

        if (selectedPrivileges.length > 0) {
            setPrivileges(selectedPrivileges);
            console.log("selected privileges through roles: ", selectedPrivileges);
        }
        markCheckboxes(selectedPrivileges);
    };

    const markCheckboxes = (selectedPrivileges: string[]) => {
        const updatedCheckboxes = { ...privilegeCheckboxes };
        selectedPrivileges.forEach((privilege) => {
            updatedCheckboxes[privilege] = true;
        });
        setPrivilegeCheckboxes(updatedCheckboxes);
    };


    const dataSource = useMemo(() => {
        if (!allPrivileges || !groupData) {
            return []
        }

        return Object.keys(allPrivileges).map((key: string) => {
            return {
                key,
                title: allPrivileges[key],
                ...groupData
            }
        })

    }, [allPrivileges, groupData]);

    const columns = useMemo(() => {
        if (!allPrivileges || !groupData) {
            return []
        }

        return [
            {
                title: "Privilege",
                dataIndex: "title",
                key: "key",
            },
            ...Object.keys(groupData || {}).map((groupKey) => ({
                title: (
                    <Button type="link" onClick={() => handleGroupButtonClick(groupKey)}>
                        {groupKey}
                    </Button>
                ),
                key: groupKey,
                render: (value: any) => {
                    const privilege = value.key
                    const group = value[groupKey]
                    const hasPrivilege = group.privileges?.includes(privilege)
                    return hasPrivilege ? <CheckOutlined /> : null;
                }
            })),
        ];
    }, [allPrivileges, groupData]);

    return (
        <div >
            <Button onClick={showModal} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                Add new group
            </Button>
            <Modal
                className='new-group-modal'
                title="Create new group"
                open={isModalOpen}
                onCancel={hideModal}
                width={850}
                footer={[
                    <Button key="back" onClick={hideModal}>
                        Cancel
                    </Button>,
                    <Button key="submit" onClick={handleSubmit} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                        Create
                    </Button>]}>
                <div>
                    <Form layout="vertical" style={{ width: 800 }}>
                        <Form.Item><b>Account</b></Form.Item>
                        <Form.Item label="Name">
                            <Input id="name" value={name} onChange={(e) => setName(e.target.value)} />
                        </Form.Item>
                        <Form.Item label="Description">
                            <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} />
                        </Form.Item>
                        <Form.Item><b>Group</b></Form.Item>
                        <Form.Item className="group-create-form_last-form-item">
                            <Form.Item>
                                <Table
                                    rowSelection={rowSelection}
                                    dataSource={dataSource}
                                    columns={columns}
                                    pagination={false}
                                    scroll={{ x: "max-content" }}
                                />
                            </Form.Item>
                        </Form.Item>
                    </Form>
                </div>
            </Modal>
        </div>
    );
};