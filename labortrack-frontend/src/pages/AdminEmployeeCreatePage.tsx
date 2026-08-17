import { Link } from 'react-router'
import {
    useState,
    type FormEvent
} from 'react'

import PageContainer from '../components/PageContainer'
import { useAuth } from '../auth/AuthContext'
import { createEmployee } from '../api/EmployeeApi'
import { ApiError } from '../api/apiClient'
import type {
    EmployeeCreationResponse
} from '../api/EmployeeTypes'

/**
 * This function displays the admin create
 * employee page. Provides the page where
 * an admin can create a new employee
 * account for their company.
 */
function AdminEmployeeCreatePage() {
    // fields for the form
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [email, setEmail] = useState('')
    const [phone, setPhone] = useState('')
    const [hourlyRate, setHourlyRate] = useState('')
    const [hireDate, setHireDate] = useState('')
    const [profileImageUrl, setProfileImageUrl] = useState('')

    // gets the authenticated admin session
    const { session } = useAuth()

    // stores API errors from employee creation
    const [errorMessage, setErrorMessage] = useState('')

    // tracks whether employee creation is running
    const [isSubmitting, setIsSubmitting] = useState(false)

    // stores the newly created employee response.
    // important because it contains the temporary password.
    const [createdEmployee, setCreatedEmployee] =
        useState<EmployeeCreationResponse | null>(null)

    // handles the employee creation request
    async function handleSubmit(event: FormEvent) {

        event.preventDefault()

        if (!session) {
            return
        }

        setErrorMessage('')
        setIsSubmitting(true)

        try {

            const response = await createEmployee(
                session.accessToken,
                session.companyId,
                {
                    firstName,
                    lastName,
                    email,

                    // optional values become null when empty
                    phone: phone.trim() || null,

                    hourlyRate: Number(hourlyRate),

                    hireDate,

                    profileImageUrl:
                        profileImageUrl.trim() || null,
                }
            )

            // keep the response so the temporary password
            // can be shown to the admin
            setCreatedEmployee(response)

        } catch (error) {

            if (error instanceof ApiError) {
                setErrorMessage(error.message)
            } else {
                setErrorMessage(
                    'Unable to create employee.'
                )
            }

        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <PageContainer>

            <h1>Create Employee</h1>

            <p>
                Create a new employee account for your company.
            </p>

            {/* Display a message to the admin showing the credentials of the newly created employee.
    This disables the form and keeps the success card visible, preventing the admin
    from accidentally creating the employee twice. */}
            {createdEmployee && (
                <div className="employee-created-card">

                    <h2>Employee created successfully</h2>

                    <p>
                        <strong>Employee:</strong>{' '}
                        {createdEmployee.firstName} {createdEmployee.lastName}
                    </p>

                    <p>
                        <strong>Email:</strong>{' '}
                        {createdEmployee.email}
                    </p>

                    <p>
                        <strong>Temporary Password:</strong>
                    </p>

                    <div className="temporary-password">
                        {createdEmployee.temporaryPassword}
                    </div>

                    <p className="password-warning">
                        Save this password now and provide it securely to the employee.
                        The employee will be required to change it after login.
                    </p>

                    <Link
                        className="primary-link-button"
                        to="/admin/employees"
                    >
                        Back to Employees
                    </Link>

                </div>
            )}

            {/* If the employee is not created, them allow to submit a form request to
             create one.*/}
            {!createdEmployee && (
                <div className="employee-form-card">

                {/* CREATE EMPLOYEE FORM */}
                <form className="employee-form" onSubmit={handleSubmit}>

                    <div className="form-row">

                        <div className="form-group">
                            <label
                                className="form-label"
                                htmlFor="firstName"
                            >
                                First name
                            </label>

                            <input
                                className="form-input"
                                id="firstName"
                                type="text"
                                value={firstName}
                                onChange={(event) =>
                                    setFirstName(event.target.value)
                                }
                                maxLength={100}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label
                                className="form-label"
                                htmlFor="lastName"
                            >
                                Last name
                            </label>

                            <input
                                className="form-input"
                                id="lastName"
                                type="text"
                                value={lastName}
                                onChange={(event) =>
                                    setLastName(event.target.value)
                                }
                                maxLength={100}
                                required
                            />
                        </div>

                    </div>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="email"
                        >
                            Email
                        </label>

                        <input
                            className="form-input"
                            id="email"
                            type="email"
                            value={email}
                            onChange={(event) =>
                                setEmail(event.target.value)
                            }
                            maxLength={254}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="phone"
                        >
                            Phone
                        </label>

                        <input
                            className="form-input"
                            id="phone"
                            type="tel"
                            value={phone}
                            onChange={(event) =>
                                setPhone(event.target.value)
                            }
                            maxLength={30}
                        />
                    </div>

                    <div className="form-row">

                        <div className="form-group">
                            <label
                                className="form-label"
                                htmlFor="hourlyRate"
                            >
                                Hourly rate
                            </label>

                            <input
                                className="form-input"
                                id="hourlyRate"
                                type="number"
                                min="0"
                                step="0.01"
                                value={hourlyRate}
                                onChange={(event) =>
                                    setHourlyRate(event.target.value)
                                }
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label
                                className="form-label"
                                htmlFor="hireDate"
                            >
                                Hire date
                            </label>

                            <input
                                className="form-input"
                                id="hireDate"
                                type="date"
                                value={hireDate}
                                onChange={(event) =>
                                    setHireDate(event.target.value)
                                }
                                required
                            />
                        </div>

                    </div>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="profileImageUrl"
                        >
                            Profile image URL
                        </label>

                        <input
                            className="form-input"
                            id="profileImageUrl"
                            type="url"
                            value={profileImageUrl}
                            onChange={(event) =>
                                setProfileImageUrl(event.target.value)
                            }
                            maxLength={500}
                            placeholder="Optional"
                        />
                    </div>

                    {/* POPS OUT SPECIAL MESSAGE */}
                    {errorMessage && (
                        <p
                            className="auth-error"
                            role="alert"
                        >
                            {errorMessage}
                        </p>
                    )}

                    {/* CREATE EMPLOYEE BUTTON */}
                    <button
                        className="auth-submit-button"
                        type="submit"
                        disabled={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Creating employee...'
                            : 'Create Employee'}
                    </button>

                </form>

            </div>
            )}

        </PageContainer>
    )
}

export default AdminEmployeeCreatePage