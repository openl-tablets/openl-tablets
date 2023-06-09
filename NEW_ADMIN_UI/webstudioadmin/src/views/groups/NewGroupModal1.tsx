import React, { useEffect, useState } from 'react';
import { Button, Checkbox, Form, Input, Modal, Table, TableColumnProps } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { stringify } from 'querystring';
import './newGroupModal.css';
import { ColumnProps } from 'antd/es/table';
import { TableRowSelection, RowSelectMethod } from 'antd/es/table/interface';
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
    // {
    //     value: string;
    //     reactKey: React.Key;
    // };
};

export const NewGroupModal1: React.FC<{ fetchGroups: () => void }> = ({ fetchGroups }) => {

    const apiURL = process.env.REACT_APP_API_URL;

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [privileges, setPrivileges] = useState<CheckboxValueType[]>([]);
    const [selectedColumns, setSelectedColumns] = useState<string[]>([]);
    const [allPrivileges, setAllPrivileges] = useState<Privilege[]>([]);
    const [columns, setColumns] = useState<TableColumnProps<Group>[]>([]);
    const [rows, setRows] = useState([]);
    const [groupData, setGroupData] = useState<Group[]>([]);

    const showModal = () => {
        setIsModalOpen(true);
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
                const groups = Object.entries(responseObject).map(([groupName, group]: [string, unknown]) => ({
                    groupName,
                    ...(group as Group),
                    privileges: (group as Group).privileges || [],
                    roles: (group as Group).roles || [],
                }));
                setGroupData(groups);
                console.log('8.', responseObject);

                const groupColumns: ColumnProps<Group>[] = [
                    {
                        title: "Privilege",
                        dataIndex: "Privileges",
                        key: "Privilege",
                        render: (value: any, record: Group, index: number) => (
                            <div>
                                {record.privilege.map((privilege: string) => (
                                    <div key={privilege}>{privilege}</div>
                                ))}
                            </div>
                        ),
                    },
                    ...groups.map((group) => ({
                        title: group.groupName,
                        // dataIndex: group.groupName,
                        key: `column_${group.groupName}`,
                        render: (value: any, record: Group, index: number) => {
                            
                            const privilegeValues = allPrivileges.map(privilege => privilege.value);
                            const hasPrivilege = privilegeValues.includes(value);
                            console.log("555", privilegeValues, hasPrivilege )
                            console.log("666", allPrivileges, value);
                            return hasPrivilege ? <CheckOutlined /> : null;
                        }
                    })),
                ];

                setColumns(groupColumns);

                // console.log("groups are: ", groups);
                // console.log("groupColumns are: ", groupColumns);
                // console.log("columns are: ", columns);

            } else {
                console.error("9. Failed to fetch groups:", response.statusText);
            }
        } catch (error) {
            console.error("10. Error fetching groups:", error);
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
            console.log("55. RESPONSE OK?? : ", response.ok);
            if (response.ok) {
                const jsonResponse = await response.json();

                const privileges: Privilege[] = Object.entries(jsonResponse).map(([key, value], index) => ({
                    privilege: Array.isArray(value) ? value : [value],
                    description: value as string,
                    key: index.toString(),
                }));

                setAllPrivileges(privileges);

                console.log("666.Privileges are: ", privileges);

            } else {
                console.error("15. Failed to fetch privileges:", response.statusText);
            }
        } catch (error) {
            console.error("16. Error fetching privileges:", error);
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
                privileges: privileges.map((privilege) => privilege.toString()),
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
            console.log("4.Added privileges:", requestBody.privileges)
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

    // const selectPrivileges = (privilege: string) => {
    //     const selectedRows = rows?.filter((row) => row[privilege] === '✓');
    //     const privileges = selectedRows.map((row) => row.key) as CheckboxValueType[];
    //     setPrivileges(privileges);
    //     console.log('7.', privileges);
    // };

    const rowSelection = {
        onChange: (selectedRowKeys: React.Key[], selectedRows: DataType[]) => {
            const privileges = selectedRows.map((row) => row.privilege) as CheckboxValueType[];
            setPrivileges(privileges);
            console.log('7.', privileges);
            // console.log('7.1', privilegesArray);
        }
    };

    const onChange = (selectedRowKeys: React.Key[], selectedRows: DataType[]) => {
        const privileges = selectedRows.map((row) => row.privilege) as CheckboxValueType[];
        setPrivileges(privileges);
    };

    return (
        <div >
            <Button onClick={showModal} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                ADD NEW GROUP
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
                                    dataSource={allPrivileges}
                                    columns={columns}
                                    pagination={false} />
                            </Form.Item>
                        </Form.Item>
                    </Form>
                </div>
            </Modal>
        </div>
    );
};