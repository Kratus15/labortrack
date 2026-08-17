import { Link } from 'react-router'
import { useEffect, useState } from 'react'
import PageContainer from '../components/PageContainer'
import LoadingState from '../components/LoadingState'
import ErrorMessage from '../components/ErrorMessage'
import UserAvatar from '../components/UserAvatar'
import { useAuth } from '../auth/AuthContext'
import { getAdminDashboard } from '../api/DashboardApi'
import EmptyState  from '../components/EmptyState'
import SimpleTable from '../components/SimpleTable'
import StatCard from '../components/StatCard'
import { formatMinutes } from '../utils/timeUtils'
import type { AdminDashboardResponse } from '../api/DashboardTypes'

/**
 * This function Displays the admin
 * dashboard ad loads dashboard
 * information from the API.
 */
function AdminDashboardPage() {

    // gets the current authenticated session
    const { session } = useAuth()

    // stores the dashboard data returned by the API.
    const [dashboard, setDashboard] =
        useState<AdminDashboardResponse | null>(null)

    // tracks whether the dashboard data is currently loading
    const [isLoading, setIsLoading] = useState(true)

    // stores error message if API call fails
    const [error, setError] = useState<string | null>(null)

    // run when the component loads or when the session changes
    useEffect(() => {

        // stops if there isn't authenticated session
        if (!session) {
            return
        }

        // loads the dashboard
        async function loadDashboard() {
            try {
                // marks page as loading
                setIsLoading(true)
                // clear any previous error
                setError(null)

                // requests the admin dashboard data using the access token
                const data = await getAdminDashboard(
                    session!.accessToken
                )

                // saved returned dashboard data
                setDashboard(data)

            } catch (error) {
                // saves error message if the request fails
                setError(
                    error instanceof Error
                        ? error.message
                        : 'Unable to load dashboard.'
                )
            } finally {
                // stops the loading state whether the request succeeds or fails
                setIsLoading(false)
            }
        }

        // calls the above function
        void loadDashboard()

        // run the effect again if the session changes
    }, [session])

    // show the loading component while dashboard data is being fetched
    if (isLoading) {
        return <LoadingState message="Loading admin dashboard..." />
    }

    // shows the error message if dashboard request fails
    if (error) {
        return <ErrorMessage message={error} />
    }

    // returns this message if there is nothing on dashboard
    if (!dashboard) {
        return <EmptyState message="Dashboard data is unavailable." />
    }

    // displays the dashboard after the data has successfully loaded
    return (
        <PageContainer>
            <h1>Admin Dashboard</h1>

            <div className="stats-grid">

                <Link
                    to="/admin/employees"
                    className="dashboard-card-link"
                >
                    <StatCard
                        title="Total Employees"
                        value={dashboard.totalEmployees}
                    />
                </Link>

                <Link
                    to="/admin/employees?status=ACTIVE"
                    className="dashboard-card-link"
                >
                    <StatCard
                        title="Active Employees"
                        value={dashboard.activeEmployees}
                    />
                </Link>

                <Link
                    to="/admin/employees?status=INACTIVE"
                    className="dashboard-card-link"
                >
                    <StatCard
                        title="Inactive Employees"
                        value={dashboard.inactiveEmployees}
                    />
                </Link>

                <Link
                    to="/admin/work-sessions/open"
                    className="dashboard-card-link"
                >
                    <StatCard
                        title="Currently Clocked In"
                        value={dashboard.currentlyClockedInEmployees}
                    />
                </Link>

                <StatCard
                    title="Today Worked"
                    value={formatMinutes(dashboard.todayWorkedMinutes)}
                />

            </div>

            {/* Recent work-session table*/}
            <section>
                <h2>Recent Work Sessions</h2>

                {dashboard.recentWorkSessions.length === 0 ? (
                    <EmptyState message="No recent work sessions found." />
                ) : (
                    <SimpleTable
                        headers={[
                            'Employee',
                            'Clock In',
                            'Clock Out',
                            'Status',
                            'Worked Time'
                        ]}
                    >
                        {dashboard.recentWorkSessions.map((workSession) => (
                            <tr key={workSession.workSessionId}>

                                {/* User Avatar */}
                                <td>
                                    <div className="employee-name-cell">

                                        <UserAvatar
                                            imageUrl={null}
                                            name={workSession.employeeName}
                                        />

                                        <span>
                                            {workSession.employeeName}
                                        </span>

                                    </div>
                                </td>

                                <td>
                                    {new Date(
                                        workSession.clockInTime
                                    ).toLocaleString()}
                                </td>

                                <td>
                                    {workSession.clockOutTime
                                        ? new Date(
                                            workSession.clockOutTime
                                        ).toLocaleString()
                                        : '—'}
                                </td>

                                <td>{workSession.status}</td>

                                <td>
                                    {workSession.workedMinutes !== null
                                        ? formatMinutes(workSession.workedMinutes)
                                        : 'In progress'}
                                </td>
                            </tr>
                        ))}
                    </SimpleTable>
                )}
            </section>
        </PageContainer>
    )
}

export default AdminDashboardPage;