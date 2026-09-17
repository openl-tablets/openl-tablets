/** A JSON answer as the endpoint writes it, for a test of a service that reads the body itself. */
export const jsonResponse = (body: unknown): Response =>
    new Response(JSON.stringify(body), { headers: { 'Content-Type': 'application/json' } })
