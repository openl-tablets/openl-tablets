import React, { useState } from 'react';
import { Button, Checkbox, Form, Input, Modal, Row, Table } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import CreateGroupDataSource from './CreateGroupDataSource';
import { AlignType } from 'rc-table/lib/interface'


export const NewGroupModal: React.FC<{ addNewGroup: (newGroup: any) => void }> = ({ addNewGroup }) => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [privileges, setPrivileges] = useState<CheckboxValueType[]>([]);
    const [selectedColumns, setSelectedColumns] = useState<string[]>([]);

    interface DataType {
        key: React.Key;
        privilege: string;
        administrators: string;
        analysts: string;
        deployers: string;
        developers: string;
        testers: string;
        viewers: string;
        [key: string]: string | React.Key;
    };


    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault();
        const newGroup = {
            name,
            description,
            privileges,
            selectedColumns,
        };
        addNewGroup(newGroup);
        setName("");
        setDescription("");
        setPrivileges([]);
        setSelectedColumns([]);
        setIsModalOpen(false);
    };

    const selectPrivileges = (privilege: string) => {
        const selectedRows = CreateGroupDataSource.filter((row) => row[privilege] === "✓");
        const privileges = selectedRows.map((row) => row.key) as CheckboxValueType[];
        setPrivileges(privileges);
        console.log(privileges);
    };

    const rowSelection = {
        onChange: (selectedRowKeys: React.Key[], selectedRows: DataType[]) => {
            const privileges = selectedRows.map((row) => row.key) as CheckboxValueType[];
            setPrivileges(privileges);
            console.log(privileges);

        }
    };

    const columns = [
        {
            title: "Privilege",
            dataIndex: "privilege",
            key: "privilege",
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 95 }} onClick={() => selectPrivileges("administrators")}>
                        Administrators
                    </Button>
                </div>
            ),
            dataIndex: "administrators",
            key: "administrators",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("analysts")}>
                        Analysts
                    </Button>
                </div>
            ),
            dataIndex: "analysts",
            key: "analysts",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("deployers")} >
                        Deployers
                    </Button>
                </div>
            ),
            dataIndex: "deployers",
            key: "deployers",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("developers")}>
                        Developers
                    </Button>
                </div>
            ),
            dataIndex: "developers",
            key: "developers",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("testers")}>
                        Testers
                    </Button>
                </div>
            ),
            dataIndex: "testers",
            key: "testers",
            align: "center" as AlignType,
        },
        {
            title: (
                <div>
                    <Button type="text" style={{ width: 70 }} onClick={() => selectPrivileges("viewers")}>
                        Viewers
                    </Button>
                </div>
            ),
            dataIndex: "viewers",
            key: "viewers",
            align: "center" as AlignType,
        },
    ];

    const showModal = () => {
        setIsModalOpen(true);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
    };

    return (
        <div >
            <Button onClick={showModal} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                Add new group
            </Button>
            <Modal title="Create new group" open={isModalOpen} onCancel={handleCancel} width={850}
                footer={[
                    <Button key="back" onClick={handleCancel}>
                        Cancel
                    </Button>,
                    <Button key="submit" onClick={handleSubmit} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                        Create
                    </Button>]}>
                <div >
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
                                    dataSource={CreateGroupDataSource} columns={columns} />
                            </Form.Item>

                        </Form.Item>
                    </Form>
                </div>
            </Modal>
        </div>
    );
};