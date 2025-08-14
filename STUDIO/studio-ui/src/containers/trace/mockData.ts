import { TraceDetails } from './types'

// Mock details data - keeping this for now since we don't have the details API endpoint
export const mockDetailsData: Record<number, TraceDetails> = {
    1: {
        inputParameters: {
            'Bank Information': {
                bankFullName: 'Commerzbank AG',
                bankID: 'commerz',
                bankRatings: 'Collection of Rating Data',
                country: 'Germany',
                sector: 'Financial Services'
            },
            'Financial Data': {
                totalAssets: 500000000000,
                totalLiabilities: 450000000000,
                netIncome: 2500000000,
                capitalRatio: 0.15
            }
        },
        resultParameters: {
            'Rating Results': {
                overallRating: 'A+',
                riskScore: 85,
                confidenceLevel: 0.92,
                lastUpdated: '2024-01-15'
            },
            'Calculation Details': {
                methodology: 'Basel III Compliant',
                calculationDate: '2024-01-15T10:30:00Z',
                version: '2.1.0'
            }
        },
        spreadsheetSteps: [
            {
                step: 'CheckCurrentFinancialData',
                description: '= SetNonZeroValues(currentFinancialData)',
                value: 'currentFinancialData'
            },
            {
                step: 'CheckPreviousPeriodFinancialData',
                description: '= SetNonZeroValues(previousFinancialData)',
                value: 'previousFinancialData'
            },
            {
                step: 'BalanceQualityIndexCalculation',
                description: '= BalanceQualityIndexCalculation(currentFinancialData)',
                value: 'BalanceQualityIndex'
            },
            {
                step: 'CapitalAdequacyRatio',
                description: '= CalculateCapitalAdequacyRatio(regulatoryCapital, riskWeightedAssets)',
                value: '0.15'
            }
        ]
    },
    2: {
        inputParameters: {
            'Validation Rules': {
                requiredFields: ['bankID', 'totalAssets', 'capitalRatio'],
                dataTypes: {
                    bankID: 'string',
                    totalAssets: 'number',
                    capitalRatio: 'number'
                },
                validationThresholds: {
                    minCapitalRatio: 0.08,
                    maxTotalAssets: 1000000000000
                }
            }
        },
        resultParameters: {
            'Validation Results': {
                isValid: true,
                validationErrors: [],
                validationWarnings: ['Data is more than 30 days old']
            }
        },
        spreadsheetSteps: [
            {
                step: 'DataCompletenessCheck',
                description: '= ValidateRequiredFields(inputData, requiredFields)',
                value: 'true'
            },
            {
                step: 'DataTypeValidation',
                description: '= ValidateDataTypes(inputData, expectedTypes)',
                value: 'true'
            },
            {
                step: 'ThresholdValidation',
                description: '= ValidateThresholds(inputData, thresholds)',
                value: 'true'
            }
        ]
    },
    3: {
        inputParameters: {
            'Risk Parameters': {
                creditRiskWeight: 0.6,
                marketRiskWeight: 0.3,
                operationalRiskWeight: 0.1,
                confidenceLevel: 0.99
            }
        },
        resultParameters: {
            'Risk Assessment': {
                totalRiskScore: 75,
                riskCategory: 'Medium',
                riskBreakdown: {
                    creditRisk: 45,
                    marketRisk: 20,
                    operationalRisk: 10
                }
            }
        },
        spreadsheetSteps: [
            {
                step: 'CreditRiskCalculation',
                description: '= CalculateCreditRisk(creditData, creditRiskWeight)',
                value: '45'
            },
            {
                step: 'MarketRiskAssessment',
                description: '= CalculateMarketRisk(marketData, marketRiskWeight)',
                value: '20'
            },
            {
                step: 'OperationalRiskCalculation',
                description: '= CalculateOperationalRisk(operationalData, operationalRiskWeight)',
                value: '10'
            },
            {
                step: 'TotalRiskAggregation',
                description: '= AggregateRisks(creditRisk, marketRisk, operationalRisk)',
                value: '75'
            }
        ]
    }
}
