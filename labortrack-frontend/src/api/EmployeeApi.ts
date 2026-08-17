import { apiRequest } from './apiClient'

import type {
    EmployeeCreationRequest,
    EmployeeCreationResponse
} from './EmployeeTypes'

/**
 * This function sends the requests to create
 * a new employee for a given company by sending
 * the employee information to the backend. Requires
 * the companyId and authenticated access token.
 */
export function createEmployee(
    token: string,
    companyId: number,
    request: EmployeeCreationRequest
): Promise<EmployeeCreationResponse> {

    return apiRequest<EmployeeCreationResponse>(
        `/api/companies/${companyId}/employees`,
        {
            method: 'POST',
            token,
            body: JSON.stringify(request),
        }
    )
}