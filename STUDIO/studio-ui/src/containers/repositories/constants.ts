export const RepositoryType = {
    JDBC: 'repo-jdbc',
    JNDI: 'repo-jndi',
    AWS_S3: 'repo-aws-s3',
    AZURE_BLOB: 'repo-azure-blob',
    GIT: 'repo-git',
    FILE: 'repo-file'
}

export const AWS_SSE_ALGORITHM = {
    AES256: 'AES256',
    AWS_KMS: 'aws:kms',
    AWS_KMS_DSSE: 'aws:kms:dsse',
    UNKNOWN_TO_SDK_VERSION: 'None'
} as Record<string, string>

export enum RepositoryDataType {
    DESIGN = 'design',
    DEPLOYMENT = 'production',
}

const repositoryTypeOptions = [
    {
        value: RepositoryType.JDBC,
        label: 'Database JDBC',
    },
    {
        value: RepositoryType.JNDI,
        label: 'Database JNDI',
    },
    {
        value: RepositoryType.AWS_S3,
        label: 'AWS S3',
    },
    {
        value: RepositoryType.AZURE_BLOB,
        label: 'Azure Blob Storage',
    },
    {
        value: RepositoryType.GIT,
        label: 'Git',
    },
    {
        value: RepositoryType.FILE,
        label: 'Local',
    },
]

export const designRepositoryTypeOptions = repositoryTypeOptions.filter(
    (option) => option.value !== RepositoryType.FILE
)
