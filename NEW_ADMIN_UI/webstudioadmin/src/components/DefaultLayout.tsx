import React from 'react';
import { AdminMenu } from './AdminMenu';

const DefaultLayout = ({ children }: { children: React.ReactNode }) => {
    return (
        <div>
            <div style={{ display: 'flex', flexDirection: 'row', justifyContent: 'center' }}>
                <AdminMenu />
                {children}
            </div>
        </div>
    );
};
export default DefaultLayout;