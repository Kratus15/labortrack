import { useState } from 'react'

type HeaderProps = {
    email: string
    role: string
    onLogout: () => void
}

/**
 * Header component. Displays the app's name,
 * the currently logged-in user's email and
 * role, and provides a Logout button that
 * calls the provided onLogout function.
 */
export function Header({ email, role, onLogout }: HeaderProps) {

    const [isAccountMenuOpen, setIsAccountMenuOpen] =
        useState(false)

    return (
        <header className="app-header">

            <div className="app-brand">
                <strong>LaborTrack</strong>
            </div>

            <div className="account-menu">

                <button
                    type="button"
                    className="account-button"
                    aria-label="Open account menu"
                    aria-expanded={isAccountMenuOpen}
                    onClick={() =>
                        setIsAccountMenuOpen(
                            !isAccountMenuOpen
                        )
                    }
                >
                    <svg
                        width="22"
                        height="22"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        aria-hidden="true"
                    >
                        <circle cx="12" cy="8" r="4" />
                        <path d="M4 21a8 8 0 0 1 16 0" />
                    </svg>
                </button>

                {isAccountMenuOpen && (
                    <div className="account-dropdown">

                        <div className="account-info">
                            <strong>{email}</strong>
                            <span>{role}</span>
                        </div>

                        <button
                            type="button"
                            className="account-logout-button"
                            onClick={onLogout}
                        >
                            Logout
                        </button>

                    </div>
                )}

            </div>
        </header>
    );
}
