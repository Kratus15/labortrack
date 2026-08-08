/**
 * The frontend type must match the backend
 * response exactly
 */
export type UserRole = 'ADMIN' | 'EMPLOYEE'

export type LoginRequest = {
    email: string
    password: string
}

export type LoginResponse = {
    accessToken: string
    tokenType: string
    expiresInSeconds: number
    userId: number
    companyId: number
    employeeId: number | null
    email: string
    role: UserRole
    mustChangePassword: boolean
}

export type ChangePasswordRequest = {
    currentPassword: string
    newPassword: string
}

export type ChangePasswordResponse = {
    message: string
    mustChangePassword: boolean
}