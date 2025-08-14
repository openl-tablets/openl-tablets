import React from 'react'
import { Button, Divider, Spin, Table, Typography, Switch, Collapse } from 'antd'
import { TraceDetails } from './types'

interface DetailsPanelProps {
    selectedKey?: React.Key
    detailsLoading: boolean
    selectedNodeDetails: TraceDetails | null
    detailsView: 'json' | 'tree'
    setDetailsView: (view: 'json' | 'tree') => void
}

export const DetailsPanel: React.FC<DetailsPanelProps> = ({
    selectedKey,
    detailsLoading,
    selectedNodeDetails,
    detailsView,
    setDetailsView
}) => {
    const spreadsheetStepsColumns = [
        {
            title: 'Step',
            dataIndex: 'step',
            key: 'step',
            width: '30%',
        },
        {
            title: 'Description',
            dataIndex: 'description',
            key: 'description',
            width: '40%',
        },
        {
            title: 'Value',
            dataIndex: 'value',
            key: 'value',
            width: '30%',
        },
    ]

    const renderTreeView = (data: any) => {
        const renderNode = (obj: any, path: string = '') => {
            return Object.entries(obj).map(([key, value]) => {
                const currentPath = path ? `${path}.${key}` : key
                if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
                    return (
                        <Collapse key={currentPath} size="small" style={{ marginBottom: 8 }}>
                            <Collapse.Panel key={currentPath} header={key}>
                                {renderNode(value, currentPath)}
                            </Collapse.Panel>
                        </Collapse>
                    )
                } else {
                    return (
                        <div key={currentPath} style={{ marginBottom: 4 }}>
                            <Typography.Text strong>{key}:</Typography.Text>
                            <Typography.Text style={{ marginLeft: 8 }}>
                                {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                            </Typography.Text>
                        </div>
                    )
                }
            })
        }
        return renderNode(data)
    }

    return (
        <div style={{ minWidth: 300, flex: 1, overflow: 'auto', margin: 5 }}>
            <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8 }}>
                <Typography.Text strong>Details Panel</Typography.Text>
                <div style={{ float: 'right', display: 'flex', gap: 8 }}>
                    <Switch 
                        checked={detailsView === 'json'} 
                        checkedChildren="Raw JSON"
                        onChange={(checked) => setDetailsView(checked ? 'json' : 'tree')}
                        unCheckedChildren="Tree View"
                    />
                </div>
            </div>
            <Divider />
            <div style={{ height: 'calc(100vh - 120px)', overflow: 'auto' }}>
                {selectedKey === undefined ? (
                    <Typography.Text>Select a trace element on the left to see its details.</Typography.Text>
                ) : detailsLoading ? (
                    <Spin />
                ) : selectedNodeDetails ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                        {/* Input and Result Parameters */}
                        <div style={{ display: 'flex', gap: 16 }}>
                            <div style={{ flex: 1 }}>
                                <Typography.Text strong>Input parameters:</Typography.Text>
                                <div style={{ marginTop: 8 }}>
                                    {detailsView === 'json' ? (
                                        <pre style={{ fontSize: 12, backgroundColor: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                                            {JSON.stringify(selectedNodeDetails.inputParameters, null, 2)}
                                        </pre>
                                    ) : (
                                        renderTreeView(selectedNodeDetails.inputParameters)
                                    )}
                                </div>
                            </div>
                            <div style={{ flex: 1 }}>
                                <Typography.Text strong>Result parameters:</Typography.Text>
                                <div style={{ marginTop: 8 }}>
                                    {detailsView === 'json' ? (
                                        <pre style={{ fontSize: 12, backgroundColor: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                                            {JSON.stringify(selectedNodeDetails.resultParameters, null, 2)}
                                        </pre>
                                    ) : (
                                        renderTreeView(selectedNodeDetails.resultParameters)
                                    )}
                                </div>
                            </div>
                        </div>
                        {/* Spreadsheet Steps */}
                        <div>
                            <div style={{ borderBottom: '1px solid #ddd', paddingBottom: 8, marginBottom: 8 }}>
                                <Typography.Text strong>Spreadsheet Steps</Typography.Text>
                                <div style={{ float: 'right', display: 'flex', gap: 8 }}>
                                    <Button size="small">Open in Editor</Button>
                                    <Button size="small">Visible Columns</Button>
                                </div>
                            </div>
                            {selectedNodeDetails.spreadsheetSteps && selectedNodeDetails.spreadsheetSteps.length > 0 ? (
                                <Table 
                                    columns={spreadsheetStepsColumns} 
                                    dataSource={selectedNodeDetails.spreadsheetSteps}
                                    pagination={false}
                                    rowKey="step"
                                    scroll={{ y: 200 }}
                                    size="small"
                                />
                            ) : (
                                <Typography.Text type="secondary">No spreadsheet steps available</Typography.Text>
                            )}
                        </div>
                    </div>
                ) : null}
            </div>
        </div>
    )
}
