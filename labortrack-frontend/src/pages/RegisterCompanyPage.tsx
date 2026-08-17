import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { registerCompany } from '../api/CompanyApi'
import { ApiError } from '../api/apiClient'

/**
 * This function displays the company registration
 * page. Allows a user to enter the company name,
 * admin email, and admin password to create a
 * new company and administrator account.
 */
function RegisterCompanyPage() {

    const [companyName, setCompanyName] = useState('')
    const [adminEmail, setAdminEmail] = useState('')
    const [adminPassword, setAdminPassword] = useState('')
    const [errorMessage, setErrorMessage] = useState('')
    const [isSubmitting, setIsSubmitting] = useState(false)
    const navigate = useNavigate()

    // sends the registration request. If successful, redirect to login
    async function handleSubmit(event: FormEvent) {

        event.preventDefault()

        setErrorMessage('')
        setIsSubmitting(true)

        try {
            await registerCompany({
                companyName,
                adminEmail,
                adminPassword,
            })

            /*
            Registration succeeded, send admin to
            log in with special message to confirm
            company register request succeeded.
            */
            navigate('/login', {
                replace: true,
                state: {
                    registrationSuccess:
                        'Company created successfully. Sign in with your administrator account.'
                }
            })

        } catch (error) {

            if (error instanceof ApiError) {
                setErrorMessage(error.message)
            } else {
                setErrorMessage(
                    'Unable to connect to the server.'
                )
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
                    <p>Create your company workspace.</p>
                </div>

                <div className="auth-heading">
                    <h2>Register Company</h2>
                    <p>
                        Create your company and administrator account.
                    </p>
                </div>

                {/* SIGN-UP FORM */}
                <form className="auth-form" onSubmit={handleSubmit}>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="companyName"
                        >
                            Company name
                        </label>

                        <input
                            className="form-input"
                            id="companyName"
                            type="text"
                            value={companyName}
                            onChange={(event) =>
                                setCompanyName(event.target.value)
                            }
                            placeholder="Enter company name"
                            maxLength={150}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="adminEmail"
                        >
                            Admin email
                        </label>

                        <input
                            className="form-input"
                            id="adminEmail"
                            type="email"
                            value={adminEmail}
                            onChange={(event) =>
                                setAdminEmail(event.target.value)
                            }
                            placeholder="admin@company.com"
                            autoComplete="email"
                            maxLength={254}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label
                            className="form-label"
                            htmlFor="adminPassword"
                        >
                            Admin password
                        </label>

                        <input
                            className="form-input"
                            id="adminPassword"
                            type="password"
                            value={adminPassword}
                            onChange={(event) =>
                                setAdminPassword(event.target.value)
                            }
                            placeholder="Create a password"
                            autoComplete="new-password"
                            minLength={8}
                            maxLength={72}
                            required
                        />
                    </div>

                    <p className="password-hint">
                        Password must be between 8 and 72 characters.
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
                            ? 'Creating company...'
                            : 'Create company'}
                    </button>

                </form>

                {/* Redirect to /login endpoint (already have an account) */}
                <div className="auth-footer">
                    <span>Already have an account?</span>

                    <Link to="/login">
                        Sign in
                    </Link>
                </div>

            </div>

        </main>
    )
}

export default RegisterCompanyPage