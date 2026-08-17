import { Outlet } from 'react-router'
import { Header } from '../components/Header'
import { Sidebar } from '../components/Sidebar'
import { useAuth } from '../auth/AuthContext'

/**
 * Main Application layout. Displays the
 * Header and Sidebar using the current
 * user's information and role, and renders
 * the active page inside the main content
 * area using React Router's Outlet.
 */
function AppLayout() {

    // get the logged-in session and logout function
    const { session, signOut } = useAuth()

    if (!session) {
        return null
    }

    return (
        <div className="app-layout">
            <Header
                email={session.email}
                role={session.role}
                onLogout={signOut}
            />

            <div className="app-body">

                <Sidebar role={session.role} />

                {/*
                This is the only part that will change when the
                user navigates to another route. Header and Sidebar
                stays the same always except when the role changes.
                */}
                <main className="app-main">
                    <Outlet />
                </main>
            </div>
        </div>
    )
}

export default AppLayout
