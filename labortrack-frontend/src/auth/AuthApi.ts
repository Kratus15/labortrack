import { apiRequest } from '../api/apiClient'
import type { LoginRequest,
    LoginResponse,
    ChangePasswordRequest,
    ChangePasswordResponse}
    from './AuthTypes.ts'

/**
 * This function sends login credentials to the backend
 * and returns the authenticated user's login data.
 */
export function login(
    credentials: LoginRequest,
): Promise<LoginResponse> {
    return apiRequest<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials),
    })
}

/**
 * This function send change-password request
 * to the backend. Sends the authenticated
 * user's current and new password to complete
 * a successful password change. If validation
 * gets passed.
 */
export function changePassword(
    request: ChangePasswordRequest,
    token: string,
): Promise<ChangePasswordResponse> {
    return apiRequest<ChangePasswordResponse>(
        '/api/auth/change-password',
        {
            method: 'POST',
            token,
            body: JSON.stringify(request),
        },
    )
}
