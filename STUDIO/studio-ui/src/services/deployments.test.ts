import apiCall from './apiCall'
import { getProductionRepositories, getDeployments, getProjectDeployments, getDeployment, hasDeploymentRepositories } from './deployments'

vi.mock('./apiCall', () => ({
    default: vi.fn(),
    asArray: <T, >(value: unknown): T[] => (Array.isArray(value) ? value as T[] : []),
}))

describe('deployments service', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('loads production repositories', async () => {
        const repos = [{ id: 'prod', name: 'Production' }]
        vi.mocked(apiCall).mockResolvedValue(repos)

        await expect(getProductionRepositories()).resolves.toBe(repos)
        expect(apiCall).toHaveBeenCalledWith('/production-repos', undefined, { throwError: true })
    })

    it('loads deployments for a repository', async () => {
        const deployments = [{ id: 'd1', name: 'Service' }]
        vi.mocked(apiCall).mockResolvedValue(deployments)

        await expect(getDeployments('prod repo')).resolves.toBe(deployments)
        expect(apiCall).toHaveBeenCalledWith('/deployments?repository=prod%20repo', undefined, { throwError: true })
    })

    it('loads deployments filtered by deployed project', async () => {
        const deployments = [{ id: 'd1', name: 'Service', repository: 'prod', items: []}]
        vi.mocked(apiCall).mockResolvedValue(deployments)

        await expect(getProjectDeployments('prod repo', 'Alpha Project')).resolves.toBe(deployments)

        expect(apiCall).toHaveBeenCalledWith(
            '/deployments?repository=prod%20repo&project=Alpha%20Project',
            undefined,
            { throwError: true }
        )
    })

    it('loads a single deployment with its items', async () => {
        const deployment = { id: 'd1', name: 'Service', repository: 'prod', items: []}
        vi.mocked(apiCall).mockResolvedValue(deployment)

        await expect(getDeployment('d 1')).resolves.toBe(deployment)
        expect(apiCall).toHaveBeenCalledWith('/deployments/d%201', undefined, { throwError: true })
    })

    it('answers a failed access check with true but probes again next time', async () => {
        // A transient error must not hide the tab, yet the guess is not remembered for the session.
        vi.mocked(apiCall).mockRejectedValueOnce(new Error('blip'))
        await expect(hasDeploymentRepositories()).resolves.toBe(true)

        vi.mocked(apiCall).mockResolvedValueOnce([])
        await expect(hasDeploymentRepositories()).resolves.toBe(false)
        expect(apiCall).toHaveBeenCalledTimes(2)
    })
})
