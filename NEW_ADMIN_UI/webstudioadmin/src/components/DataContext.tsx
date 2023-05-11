import React, { createContext, useContext, useState, ReactNode } from 'react';

const users = [
    {
        key: "1",
        userName: "a1",
        firstName: "",
        lastName: "",
        email: "a1@example.com",
        displayName: "A1",
        groups: ["Administrators"],
        action: "",
    },
    {
        key: "2",
        userName: "admin",
        firstName: "",
        lastName: "",
        email: "admin@example.com",
        displayName: "Admin",
        groups: ["Administrators"],
        action: "",
    },];

interface User {
    key: string;
    userName: string;
    firstName: string;
    lastName: string;
    email: string;
    displayName: string;
    groups: string[];
    action: string;
}

interface DataContextProps {
    users: User[];
    addUser: (user: User) => void;
}

export const DataContext = createContext<DataContextProps>({
    users: [],
    addUser: () => { },
});

const DataProvider = ({ children }: { children: ReactNode }) => {
    const [users, setUsers] = useState<User[]>([]);

    const addUser = (user: User) => {
        setUsers([...users, user]);
    };

    return (
        <DataContext.Provider value={{ users, addUser }}>
            {children}
        </DataContext.Provider>
    );
};

export default DataProvider;
