import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { ApiError } from '../api/apiClient'
import { changePassword } from '../auth/AuthApi'
import { useAuth } from '../auth/AuthContext'

/**
 * This function displays the change-password page form.
 * Allows an authenticated user to replace their
 * temporary/current password with a new one. This will
 * be enforced if it is an employee-user account, and it is
 * first login.
 */
function ChangePasswordPage() {
    // get the current user's session. Get signIn to update user's session state.
    const { session, signIn } = useAuth()

    // helps send the user to another route/page
    const navigate = useNavigate()

    // stores what the user types into the fileds
    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')

    // stores an error message that can be displayed to the user
    const [errorMessage, setErrorMessage] = useState('')

    // tracks whether the form is being submitted. Helps prevent the user from multiple clicking.
    const [isSubmitting, setIsSubmitting] = useState(false)

    // user must already be authenticated to access this page
    if (!session) {
        return <Navigate to="/login" replace />
    }

    const currentSession = session

    // This function runs when the user submits the password-change form
    async function handleSubmit(event: FormEvent<HTMLFormElement>) {

        // prevent reloading while submitting
        event.preventDefault()

        // remove any prev error message
        setErrorMessage('')

        // mark the form as submitting and disable the button
        setIsSubmitting(true)

        try {
            // send the current password and new password change form to the backend
            // and JWT as well
            const response = await changePassword(
                {
                    currentPassword,
                    newPassword,
                },
                currentSession.accessToken,
            )

            // Password change successfully. Update the stored authentication session.
            signIn({
                ...currentSession,
                mustChangePassword: response.mustChangePassword,
            })

            // send the user to dashboard. Use their role to determine which path
            const dashboardPath =
                currentSession.role === 'ADMIN'
                    ? '/admin/dashboard'
                    : '/employee/dashboard'

            navigate(dashboardPath, { replace: true })
        } catch (error) {
            if (error instanceof ApiError) {
                setErrorMessage(error.message)
            } else {
                setErrorMessage('Unable to connect to the server.')
            }
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main className="auth-page">

            <div className="auth-card">

                <div className="auth-brand">
                    <h1>LaborTrack</h1>
                    <p>Secure your account.</p>
                </div>

                <div className="auth-heading">
                    <h2>Change Password</h2>

                    <p>
                        {currentSession.mustChangePassword
                            ? 'You must change your temporary password before continuing.'
                            : 'Choose a new password for your LaborTrack account.'}
                    </p>
                </div>

                <form
                    className="auth-form"
                    onSubmit={handleSubmit}
                >

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="currentPassword"
                        >
                            Current password
                        </label>

                        <input
                            className="form-input"
                            id="currentPassword"
                            type="password"
                            value={currentPassword}
                            onChange={(event) =>
                                setCurrentPassword(event.target.value)
                            }
                            autoComplete="current-password"
                            placeholder="Enter your current password"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="newPassword"
                        >
                            New password
                        </label>

                        <input
                            className="form-input"
                            id="newPassword"
                            type="password"
                            value={newPassword}
                            onChange={(event) =>
                                setNewPassword(event.target.value)
                            }
                            autoComplete="new-password"
                            placeholder="Enter your new password"
                            minLength={12}
                            maxLength={64}
                            required
                        />
                    </div>

                    <p className="password-hint">
                        Password must be between 12 and 64 characters.
                    </p>

                    {errorMessage && (
                        <p
                            className="auth-error"
                            role="alert"
                        >
                            {errorMessage}
                        </p>
                    )}

                    <button
                        className="auth-submit-button"
                        type="submit"
                        disabled={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Changing password...'
                            : 'Change password'}
                    </button>

                </form>

            </div>

        </main>
    )
}

export default ChangePasswordPage