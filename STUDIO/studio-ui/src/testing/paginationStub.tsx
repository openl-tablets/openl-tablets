/**
 * A mocked antd `Pagination` for page tests: it reads the current page and the page count as `current/pages`.
 */
export const PaginationStub = ({ current, pageSize, total, ...rest }: Record<string, unknown>) => (
    <nav data-testid={rest['data-testid'] as string}>
        {`${current as number}/${Math.ceil((total as number) / (pageSize as number))}`}
    </nav>
)
