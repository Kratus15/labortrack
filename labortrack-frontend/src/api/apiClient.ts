import { clearAuthSession } from '../auth/AuthStorage'
/**
 * This file is the doorway between React and our Spring
 * boots project backend. Instead of constantly repeating
 * fetch, headers, JWT handling, etc. and more boilerplate,
 * we simply build this common logic.
 */

// backend URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

if (!API_BASE_URL) {
    throw new Error('VITE_API_BASE_URL is not configured')
}

export class ApiError extends Error {
    status: number
    body: unknown

    constructor(status: number, message: string, body: unknown) {
        super(message)
        this.name = 'ApiError'
        this.status = status
        this.body = body
    }
}

type ApiRequestOptions = RequestInit & {
    token?: string
}

export async function apiRequest<T>(
    path: string,
    options: ApiRequestOptions = {},
): Promise<T> {
    const { token, headers, ...requestOptions } = options

    const requestHeaders = new Headers(headers)

    if (requestOptions.body && !requestHeaders.has('Content-Type')) {
        // sends JSON
        requestHeaders.set('Content-Type', 'application/json')
    }

    if (token) {
        // sends JWT
        requestHeaders.set('Authorization', `Bearer ${token}`)
    }

    // sends HTTP requests
    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...requestOptions,
        headers: requestHeaders,
    })

    // reads the backend response
    const responseText = await response.text()
    let responseBody: unknown = null

    if (responseText) {
        try {
            // convert response into JSON for JavaScript
            responseBody = JSON.parse(responseText)
        } catch {
            responseBody = responseText
        }
    }

    /*
    if an authenticated request receives 401, the JWT
    is no longer valid. Clear the session and send the
    user back to log in (401 + JWT = token expired/invalid).
    (401 + no JWT = login failed)
    */
    if (response.status === 401 && token) {
        clearAuthSession()
        window.location.replace('/login')
    }

    if (!response.ok) {
        // handle backend errors
        const message =
            typeof responseBody === 'object' &&
            responseBody !== null &&
            'message' in responseBody
                ? String(responseBody.message)
                : response.status === 403
                    ? 'You do not have permission to perform this action.'
                    : `Request failed with status ${response.status}`


        throw new ApiError(response.status, message, responseBody)
    }

    return responseBody as T
}