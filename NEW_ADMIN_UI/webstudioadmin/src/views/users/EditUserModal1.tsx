import React, { useState, useEffect, useRef, useMemo } from 'react'
import { Button, Row, Select } from 'antd';
import type { CheckboxValueType } from 'antd/es/checkbox/Group';
import { Form as FinalForm, Field, Form, FormSpy } from 'react-final-form'
import arrayMutators from 'final-form-arrays'
import { Input, CheckboxGroup } from '../../components/form'
import './userModal.css';
import { FormApi } from 'final-form'

const DISPLAY_NAME_FIRST_LAST = "firstLast";
const DISPLAY_NAME_LAST_FIRST = "lastFirst";
const DISPLAY_NAME_OTHER = "other";

const displayNameOptions = [
    {
        value: DISPLAY_NAME_FIRST_LAST,
        label: "First last",
    },
    {
        value: DISPLAY_NAME_LAST_FIRST,
        label: "Last first",
    },
    {
        value: DISPLAY_NAME_OTHER,
        label: "Other",
    },
];

interface EditUserProps {
    user: {
        email: string;
        displayName: string;
        firstName: string;
        lastName: string;
        password: string;
        groups: string[];
        username: string;
        internalPassword: {
            password: string;
        };
        currentUser: boolean;
        superUser: boolean;
        unsafePassword: boolean;
        externalFlags: {
            firstNameExternal: boolean;
            displayNameExternal: boolean;
            emailExternal: boolean;
            lastNameExternal: boolean;
            emailVerified: boolean;
        };
        notMatchedExternalGroupsCount: number;
        online: boolean;
        userGroups: {
            name: string;
            type: "ADMIN" | "DEFAULT" | "EXTERNAL";
        }[];
    };
    updateUser: (updatedUser: any) => void;
    onSave: () => void;
}
interface Group {
    name: string;
    id: number;
    description: string;
    roles: string[];
    privileges: string[];
}

export const EditUserModal1: React.FC<EditUserProps> = ({ user, updateUser, onSave }) => {
    const apiURL = "http://localhost:8080/webstudio/rest";

    const [allGroups, setAllGroups] = useState<Group[]>([]);
    const [groupNames, setGroupNames] = useState<string[]>([]);
    const [isModalOpen, setIsModalOpen] = useState(true);
    const [selectedGroupValues, setSelectedGroupValues] = useState<string[]>(user.groups);
    const [isDisplayNameFieldDisabled, setIsDisplayNameFieldDisabled] = useState<boolean>(true);
    const formRef = useRef<FormApi>();

    const displayNameSelectInitialValue = useMemo(() => {
        if (user.displayName === user.firstName + " " + user.lastName) {
            return DISPLAY_NAME_FIRST_LAST;
        } else if (user.displayName === user.lastName + " " + user.firstName) {
            return DISPLAY_NAME_LAST_FIRST;
        } else {
            return DISPLAY_NAME_OTHER;
        }
    }, [user])

    const initialValues = useMemo(() => ({
        username: user.username,
        email: user.email,
        password: user.password,
        firstName: user.firstName,
        lastName: user.lastName,
        displayName: user.displayName,
        displayNameSelect: displayNameSelectInitialValue,
        groups: user.userGroups.map((group) => group.name),
    }), [user]);

    const fetchGroupData = async () => {
        try {
            const response = await fetch(`${apiURL}/admin/management/groups`, {
                headers: {
                    "Authorization": "Basic YWRtaW46YWRtaW4=",
                }
            });
            if (response.ok) {
                const responseObject = await response.json();
                setAllGroups(responseObject);
                const names = Object.keys(responseObject);
                setGroupNames(names);
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

    useEffect(() => {
        if (groupNames && user.groups) {
            setSelectedGroupValues(user.groups.filter((group) => groupNames.includes(group)));
        }
    }, [groupNames, user.groups]);


    const handleSubmit = async (values: any) => {
        const { email, displayName, firstName, lastName, password, groups, username } = values;

        const updatedUser = {
            email,
            displayName,
            firstName,
            lastName,
            password,
            groups,
        };

        console.log("updated user: -> ", updatedUser);
        try {
            const response = await fetch(`${apiURL}/users/${username}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Basic YWRtaW46YWRtaW4='
                },
                body: JSON.stringify(updatedUser)
            });
            console.log('12. Response:', response);
            if (response.status === 204) {
                console.log("response body: ", response.body);
                console.log('User updated successfully!');
                updateUser(updatedUser);
                setIsModalOpen(false);
                onSave();
                return;
            }
            else {
                console.log('Response status:', response.status);
                console.error('Error updating user:', response.statusText);
                const responseText = await response.text();
                console.log('Response Text:', responseText);
            }
        } catch (error) {
            console.error('Error updating user:', error);
        }
    };

    const handleUserGroupChange = (selectedGroupValues: CheckboxValueType[]) => {
        const groupValues = selectedGroupValues.map((value) => value.toString());
        setSelectedGroupValues(groupValues);
        console.log("selected group values: ", selectedGroupValues);
    };

    // TODO: values is object with all form values. create a type for it
    const displayNameSetter = (values: any, form: FormApi) => {
        if (values.displayNameSelect === DISPLAY_NAME_OTHER) {
            setIsDisplayNameFieldDisabled(false);
        } else {
            if (values.displayNameSelect === DISPLAY_NAME_FIRST_LAST) {
                form.change('displayName', `${values.firstName} ${values.lastName}`);
            } else  {
                form.change('displayName', `${values.lastName} ${values.firstName}`);
            }
            setIsDisplayNameFieldDisabled(true);
        }


        return null;
    }

    return (
        <div>
            <FinalForm
                onSubmit={handleSubmit}
                mutators={{...arrayMutators}}
                initialValues={initialValues} >
                {({ handleSubmit, form }) => {
                    formRef.current = form;
                    return (
                        <form onSubmit={handleSubmit}>
                            <br></br>
                            <label><b>Account</b></label>
                            <div className="user-label">
                                <label>Username</label>
                                <Input name="username" disabled />
                            </div>
                            <div className="user-label">
                                <label>Email</label>
                                <Input name="email" />
                            </div>
                            <div className="user-label">
                                <label>Password</label>
                                <Input name="password" type="password"/>
                            </div>
                            <br></br>
                            <label><b>Name</b></label>
                            <div className="user-label">
                                <label >First name (given name)</label>
                                <Input name="firstName" />
                            </div>
                            <div className="user-label">
                                <label >Last name (family name)</label>
                                <Input name="lastName" />
                            </div>
                            <div className="user-label">
                                <label>Display name:</label>
                                <div>
                                    <Field name="displayNameSelect">
                                        {({ input }) => (
                                            <Select
                                                style={{ width: 100 }}
                                                options={displayNameOptions}
                                                {...input}
                                            />
                                        )}
                                    </Field>
                                </div>
                            </div>
                            <div className="user-label">
                                <Input name="displayName" disabled={isDisplayNameFieldDisabled} />
                            </div>
                            <br></br>
                            <label><b>Groups</b></label>
                            <CheckboxGroup name="groups" options={groupNames} />
                            <div style={{ display: "grid", justifyContent: "end" }}>
                                <Row>
                                    <Button key="submit" htmlType="submit" style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                                        Update
                                    </Button>
                                </Row>
                            </div>
                            <FormSpy subscription={{ values: true }}>
                                {({ values }) => displayNameSetter(values, form)}
                            </FormSpy>
                        </form>
                    )}
                }
            </FinalForm>

        </div>
    );
};