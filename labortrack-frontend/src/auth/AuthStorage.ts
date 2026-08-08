import type {LoginResponse } from './AuthTypes.ts'

const AUTH_SESSION_KEY = 'labortrack_auth_session'

/**
 * Stores, retrieve and delete the authenticated user's
 * login response for the current browser tab
 */

export function saveAuthSession(session: LoginResponse): void {
    sessionStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session))
}

export function getAuthSession(): LoginResponse | null {
    const storedSession = sessionStorage.getItem(AUTH_SESSION_KEY)

    if (!storedSession) {
        return null
    }

    try {
        return JSON.parse(storedSession) as LoginResponse
    }
    catch {
        return null
    }
}

export function clearAuthSession(): void {
    sessionStorage.removeItem(AUTH_SESSION_KEY)
}