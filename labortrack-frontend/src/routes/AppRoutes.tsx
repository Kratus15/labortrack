import { Navigate, Route, Routes } from 'react-router'
import LoginPage from '../pages/LoginPage'
import AdminDashboardPage from '../pages/AdminDashboardPage'
import EmployeeDashboardPage from '../pages/EmployeeDashboardPage'
import ProtectedRoute from './ProtectedRoute'
import ChangePasswordPage from '../pages/ChangePasswordPage'

/**
 * This function defines the frontend URLs and
 * the page displayed for each URL. Also protected
 * the displayed dashboard based on authentication
 * and user role.
 */
function AppRoutes() {
    return (
        // Routes the container that holds all individual Route components
        <Routes>

            {/*
            When the URL is /login display the LoginPage component
            */}
            <Route path="/login" element={<LoginPage />}/>

            {/*
            When the user wants to change password. This is enforced when the user
            still have the temporary password. Otherwise, changing password is
            optional.
            */}
            <Route
                path="/change-password"
                element={<ChangePasswordPage />}
            />

            {/*
            ONLY allowed users with ADMIN role to access this endpoint
            */}
            <Route element={<ProtectedRoute allowedRole="ADMIN" />}>
            <Route
                path="/admin/dashboard"
                element={<AdminDashboardPage />}
            />
            </Route>

            {/*
            ONLY allowed users with EMPLOYEE role to access this endpoint
            */}
            <Route element={<ProtectedRoute allowedRole="EMPLOYEE" />}>
            <Route
                path="/employee/dashboard"
                element={<EmployeeDashboardPage />}
            />
            </Route>

            {/*
            When the user visits the root URL "/" redirect them to "/login"
            */}
            <Route path="/" element={<Navigate to="/login" replace />} />

            {/*
            When the user type any unrecognized/unknown URL redirect to /login
            */}
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    )
}

export default AppRoutes