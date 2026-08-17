import { apiRequest } from './apiClient'
import type {
    ClockInResponse,
    ClockOutResponse
} from './DashboardTypes'

/**
 * This function clocks in an employee
 * by sending a POST request to the employee
 * clock-in endpoint on backend. Requires the
 * employeeId and authenticated access token.
 */
export function clockIn(
    token: string,
    employeeId: number,
): Promise<ClockInResponse> {
    return apiRequest<ClockInResponse>(
        `/api/employees/${employeeId}/clock-in`,
        {
            method: 'POST',
            token
        }
    )
}

/**
 * This function clocks out an employee by
 * sending a POST request to the employee
 * clock-out endpoint on backend. Requires
 * the employeeId and authenticated access
 * token.
 */
export function clockOut(
    token: string,
    employeeId: number
): Promise<ClockOutResponse> {
    return apiRequest<ClockOutResponse>(
        `/api/employees/${employeeId}/clock-out`,
        {
            method: 'POST',
            token
        }
    )
}