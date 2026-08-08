import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import type { UserRole } from '../auth/AuthTypes'

/**
 * This component protects routes that should only
 * be accessible to logged-in users with a specific
 * role. It checks: 1- is the user logged in?,
 * 2- does the user have required role? if both
 * fails user gets redirected. If both pass, the
 * protected page is displayed.
 */
type ProtectedRouteProps = {
    // the role that is allowed to access this route
    allowedRole: UserRole
}

/**
 * This function creates the React component
 * ProtectedRoute.
 */
function ProtectedRoute({ allowedRole }: ProtectedRouteProps) {

    // calls our authentication context and gets the current session
    const { session } = useAuth()

    // checks if there is a logged-in user session. If not redirect to /login
    if (!session) {
        return <Navigate to="/login" replace />
    }

    // if the user still have temp password. Must change password before accessing any other endpoint
    if (session.mustChangePassword) {
        return <Navigate to="/change-password" replace />
    }

    // checks whether the logged-in user's role matches. The role required by this route
    if (session.role !== allowedRole) {

        // select the correct dashboard for the logged-in user using his role
        // if ADMIN admin-dashboard, if EMPLOYEE employee-dashboard
        const correctDashboard =
            session.role === 'ADMIN'
        ? '/admin/dashboard'
                : '/employee/dashboard'

        return <Navigate to={correctDashboard} replace />
    }

    return <Outlet />
}

export default ProtectedRoute