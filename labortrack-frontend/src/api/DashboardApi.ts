import { apiRequest } from './apiClient'
import type {
    AdminDashboardResponse,
    AdminEmployeeDetailResponse,
    AdminEmployeeListItemResponse,
    EmployeeDashboardResponse,
    EmployeeStatus,
    WorkSessionSummaryResponse
} from './DashboardTypes'

/**
 * This function fetches the admin dashboard
 * data using the provided authentication token.
 */
export function getAdminDashboard(
    token: string
): Promise<AdminDashboardResponse> {

    return apiRequest<AdminDashboardResponse>(
        '/api/admin/dashboard',
        {
            method: 'GET',
            token,
        }
    )
}

/**
 * This function fetches the list of employees
 * for the admin using the provided authentication
 * token. Also, added EmployeeStatus as filter.
 */
export function getAdminEmployees(
    token: string,
    status?: EmployeeStatus
): Promise<AdminEmployeeListItemResponse[]> {
    const path = status
        ? `/api/admin/employees?status=${status}`
        : '/api/admin/employees'

    return apiRequest<AdminEmployeeListItemResponse[]>(
        path,
        {
            method: 'GET',
            token,
        }
    )
}

/**
 * This function fetches the details of a
 * specific employee using their employeeId.
 */
export function getAdminEmployeeDetail(
    token: string,
    employeeId: number
): Promise<AdminEmployeeDetailResponse> {
    return apiRequest<AdminEmployeeDetailResponse>(
        `/api/admin/employees/${employeeId}`,
        {
            method: 'GET',
            token
        }
    )
}

/**
 * Fetches all currently open work sessions
 * for the admin's employees.
 */
export function getAdminOpenWorkSessions(
    token: string
): Promise<WorkSessionSummaryResponse[]> {
    return apiRequest<WorkSessionSummaryResponse[]>(
        '/api/admin/work-sessions/open',
        {
            method: 'GET',
            token
        }
    )
}

/**
 * This function fetches the dashboard data
 * for the currently authenticated employee.
 */
export function getEmployeeDashboard(
    token: string
): Promise<EmployeeDashboardResponse> {
    return apiRequest<EmployeeDashboardResponse>(
        '/api/employee/me/dashboard',
        {
            method: 'GET',
            token
        }
    )
}