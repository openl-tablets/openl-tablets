
interface DataType {
    key: React.Key;
    userName: string;
    firstName: string;
    lastName: string;
    email: string;
    displayName: string;
    groups: string[];
    action: string;
}

const TableUserInfo: DataType[] =
    [
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
        },
        {
            key: "3",
            userName: "u0",
            firstName: "",
            lastName: "",
            email: "u0@example.com",
            displayName: "U0",
            groups: ["Testers"],
            action: "",
        },
        {
            key: "4",
            userName: "u1",
            firstName: "",
            lastName: "",
            email: "u1@example.com",
            displayName: "U1",
            groups: ["Analyst", "Developers"],
            action: "",
        },
        {
            key: "5",
            userName: "u2",
            firstName: "",
            lastName: "",
            email: "u2@example.com",
            displayName: "U2",
            groups: ["Viewers"],
            action: "",
        },
        {
            key: "6",
            userName: "u3",
            firstName: "",
            lastName: "",
            email: "u3@example.com",
            displayName: "U3",
            groups: ["Viewers"],
            action: "",
        },
        {
            key: "7",
            userName: "u4",
            firstName: "",
            lastName: "",
            email: "u4@example.com",
            displayName: "U4",
            groups: ["Deployers"],
            action: "",
        },
        {
            key: "8",
            userName: "user",
            firstName: "",
            lastName: "",
            email: "user@example.com",
            displayName: "User",
            groups: ["Viewers"],
            action: "",
        }

    ];

export default TableUserInfo;
