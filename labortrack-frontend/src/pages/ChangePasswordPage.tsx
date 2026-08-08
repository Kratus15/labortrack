import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router'
import { ApiError } from '../api/apiClient'
import { changePassword } from '../auth/AuthApi'
import { useAuth } from '../auth/AuthContext'

/**
 * Allows an authenticated user to replace
 * their temporary/current password with a new one.
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
        <main>
            <h1>Change Password</h1>
            <p>You must change your password before continuing.</p>

            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="currentPassword">Current password</label>
                    <input
                        id="currentPassword"
                        type="password"
                        value={currentPassword}
                        onChange={(event) => setCurrentPassword(event.target.value)}
                        autoComplete="current-password"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="newPassword">New password</label>
                    <input
                        id="newPassword"
                        type="password"
                        value={newPassword}
                        onChange={(event) => setNewPassword(event.target.value)}
                        autoComplete="new-password"
                        minLength={12}
                        maxLength={64}
                        required
                    />
                </div>

                {errorMessage && <p role="alert">{errorMessage}</p>}

                <button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? 'Changing password...' : 'Change password'}
                </button>
            </form>
        </main>
    )
}

export default ChangePasswordPage