import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/apiClient'
import { login } from '../auth/AuthApi.ts'
import {
    Link,
    Navigate,
    useLocation,
    useNavigate
} from 'react-router'

/**
 * This function displays the login form on the
 * frontend and sends the user's email and password
 * to the backend.
 */
function LoginPage() {
    // save what the user types from the email field form
    const [email, setEmail] = useState('')
    // save what the user types from the password field form
    const [password, setPassword] = useState('')

    // save error message so it can be shown in the page
    const [errorMessage, setErrorMessage] = useState('')

    // detects weather the login request is being sent.
    const [isSubmitting, setIsSubmitting] = useState(false)

    const { session, signIn } = useAuth()

    const navigate = useNavigate()

    const location = useLocation()

    const registrationSuccess =
        location.state?.registrationSuccess as string | undefined

    // already authenticated users should not see the login page
    if (session) {
        if (session.mustChangePassword) {
            return <Navigate to="/change-password" replace />
        }

        const dashboardPath =
            session.role === 'ADMIN'
                ? '/admin/dashboard'
                : '/employee/dashboard'

        return <Navigate to={dashboardPath} replace />
    }

    // run this function when the user submits the login form. (submit button or ENTER are triggers)
    async function handleSubmit(event: FormEvent<HTMLFormElement>) {

        // stop browser from refreshing the page
        event.preventDefault()

        // remove any old error message
        setErrorMessage('')

        // disable the button submit while request is running
        setIsSubmitting(true)

        try {
            // send the user's email and password to the backend and get the response back
            const response = await login({
                email,
                password,
            })

            // signs the user in
            signIn(response)

            // check if user still have temp password. If yes, must change password before anything
            if (response.mustChangePassword) {
                navigate('/change-password', {replace: true})
                return
            }
            // use the authenticated user's role to determine next path
            const dashboardPath =
                response.role === 'ADMIN'
            ? '/admin/dashboard'
                    : '/employee/dashboard'

            // navigate to the correct dashboard path
            navigate(dashboardPath, {replace: true})
        } catch (error) {
            // check whether the backend returned a known API error
            if (error instanceof ApiError) {
                // show the error message returned by the backend
                setErrorMessage(error.message)
            } else {
                // show general message for network or unexpected errors
                setErrorMessage('Unable to connect to the server.')
            }
        } finally {
            // enabled the button back again
            setIsSubmitting(false)
        }
    }

    // Display the login page
    return (
        <main className="auth-page">

            <div className="auth-card">

                <div className="auth-brand">
                    <h1>LaborTrack</h1>
                    <p>Workforce time tracking made simple.</p>
                </div>

                <div className="auth-heading">
                    <h2>Sign in</h2>
                    <p>Enter your account information to continue.</p>
                </div>

                {/* SPECIAL MESSAGE WHEN NEW COMPANY REGISTERS */}
                {registrationSuccess && (
                    <p className="auth-success">
                        {registrationSuccess}
                    </p>
                )}

                {/* LOGIN FORM*/}
                <form
                    className="auth-form"
                    onSubmit={handleSubmit}
                >

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
                            autoComplete="email"
                            placeholder="you@example.com"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="password"
                        >
                            Password
                        </label>

                        <input
                            className="form-input"
                            id="password"
                            type="password"
                            value={password}
                            onChange={(event) =>
                                setPassword(event.target.value)
                            }
                            autoComplete="current-password"
                            placeholder="Enter your password"
                            required
                        />
                    </div>

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
                            ? 'Signing in...'
                            : 'Sign in'}
                    </button>

                </form>

                {/* Redirect to /register endpoint (new users) */}
                <div className="auth-footer">
                    <span>New to LaborTrack?</span>

                    <Link to="/register">
                        Create your company
                    </Link>
                </div>

            </div>

        </main>
    )
}

export default LoginPage