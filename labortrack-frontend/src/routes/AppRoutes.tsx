import { Navigate, Route, Routes } from 'react-router'
import LoginPage from '../pages/LoginPage'
import AdminDashboardPage from '../pages/AdminDashboardPage'
import EmployeeDashboardPage from '../pages/EmployeeDashboardPage'
import ProtectedRoute from './ProtectedRoute'
import ChangePasswordPage from '../pages/ChangePasswordPage'
import AppLayout from '../layouts/AppLayout'
import AdminEmployeesPage from '../pages/AdminEmployeesPage'
import AdminEmployeeDetailPage from '../pages/AdminEmployeeDetailPage'
import RegisterCompanyPage from '../pages/RegisterCompanyPage'
import AdminEmployeeCreatePage from '../pages/AdminEmployeeCreatePage'
import AdminOpenWorkSessionsPage from '../pages/AdminOpenWorkSessionsPage'

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

            {/* PUBLIC */}
            {/*
            When the URL is /login display the LoginPage component
            */}
            <Route path="/login" element={<LoginPage />}/>

            {/* Register a new company*/}
            <Route
                path="/register"
                element={<RegisterCompanyPage />}
            />


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
                <Route element={<AppLayout />}>
                        <Route
                                path="/admin/dashboard"
                                element={<AdminDashboardPage />}
                        />
                        <Route
                            path="/admin/employees"
                            element={<AdminEmployeesPage />}
                        />
                        <Route
                            path="/admin/employees/new"
                            element={<AdminEmployeeCreatePage />}
                        />
                        <Route
                            path="/admin/employees/:employeeId"
                            element={<AdminEmployeeDetailPage />}
                        />
                        <Route
                            path="/admin/work-sessions/open"
                            element={<AdminOpenWorkSessionsPage />}
                        />
                </Route>
            </Route>

            {/*
            ONLY allowed users with EMPLOYEE role to access this endpoint
            */}
            <Route element={<ProtectedRoute allowedRole="EMPLOYEE" />}>
                <Route element={<AppLayout />}>
                    <Route
                        path="/employee/dashboard"
                        element={<EmployeeDashboardPage />}
                    />
                </Route>
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