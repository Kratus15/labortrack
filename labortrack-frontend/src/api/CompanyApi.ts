import { apiRequest } from './apiClient'
import type {
    CompanyRegistrationRequest,
    CompanyRegistrationResponse,
} from './CompanyTypes'

/**
 * This function registers a new company
 * by sending the company registration
 * information to the backend. Returns the
 * created company and admin account information.
 */
export function registerCompany(
    request: CompanyRegistrationRequest
): Promise<CompanyRegistrationResponse> {
    return apiRequest<CompanyRegistrationResponse>(
        '/api/companies/register',
        {
            method: 'POST',
            body: JSON.stringify(request),
        }
    )
}