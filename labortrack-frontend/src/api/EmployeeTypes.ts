import type { EmployeeStatus } from './DashboardTypes'

export type EmployeeCreationRequest = {
    firstName: string
    lastName: string
    phone: string | null
    hourlyRate: number
    hireDate: string
    profileImageUrl: string | null
    email: string
}

export type EmployeeCreationResponse = {
    employeeId: number
    companyId: number
    userId: number
    email: string
    firstName: string
    lastName: string
    phone: string | null
    hourlyRate: number
    profileImageUrl: string | null
    status: EmployeeStatus
    hireDate: string
    createdAt: string
    temporaryPassword: string
}