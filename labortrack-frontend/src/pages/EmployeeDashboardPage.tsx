import { useEffect, useState } from 'react'
import PageContainer from '../components/PageContainer'
import LoadingState from '../components/LoadingState'
import ErrorMessage from '../components/ErrorMessage'
import { useAuth } from '../auth/AuthContext'
import { getEmployeeDashboard } from '../api/DashboardApi'
import type { EmployeeDashboardResponse } from '../api/DashboardTypes'
import { formatMinutes } from '../utils/timeUtils'
import EmptyState from '../components/EmptyState'
import SimpleTable from '../components/SimpleTable'
import { clockIn, clockOut } from '../api/WorkSessionApi'
import UserAvatar from '../components/UserAvatar'

/**
 * This function displays the employee dashboard
 * and loads the employee's information from the
 * API.
 */
function EmployeeDashboardPage() {

    // get current authenticated session
    const { session } = useAuth()

    // stores the dashboard data returned by backend
    const [dashboard, setDashboard] =
        useState<EmployeeDashboardResponse | null>(null)

    // tracks whether the dashboard data is currently loading
    const [isLoading, setIsLoading] = useState(true)

    // stores an error message if something goes wrong
    const [error, setError] = useState<string | null>(null)

    // tracks whether a clock-in/out action is running
    const [isClockActionLoading, setIsClockActionLoading] =
        useState(false)

    // stores errors specifically from clock actions
    const [clockActionError, setClockActionError] =
        useState<string | null>(null)

    // runs when the page loads/or something changes
    useEffect(() => {

        // don't continue if not authenticated session
        if (!session) {
            return
        }

        // load employee dashboard
        async function loadDashboard() {
            try {
                // shows the loading state while the request is loading
                setIsLoading(true)
                // clear any previous error message
                setError(null)

                // stores the response from the call
                const data = await getEmployeeDashboard(
                    session!.accessToken
                )

                setDashboard(data)

            } catch (error) {
                // if the request fails save the error message
                setError(
                    error instanceof Error
                        ? error.message
                        : 'Unable to load employee dashboard.'
                )
            } finally {
                setIsLoading(false)
            }
        }

        void loadDashboard()

        // run the effect when the page loads/or something changes
    }, [session])

    // while waiting for the response show the loading component
    if (isLoading) {
        return <LoadingState message="Loading dashboard..." />
    }

    // if the request fail show the error message
    if (error) {
        return <ErrorMessage message={error} />
    }

    // if the response is empty display message
    if (!dashboard) {
        return <EmptyState message="Dashboard data is unavailable." />
    }

    // handles employee clock-in request
    async function handleClockIn() {

        // stop if there is no authenticated session
        if (!session || !dashboard) {
            return
        }

        try {
            // disable clock actions while the request is running
            setIsClockActionLoading(true)
            // clear any previous clock action error
            setClockActionError(null)

            // perform the clock-in request
            await clockIn(
                session.accessToken,
                dashboard.employeeId
            )

            // reload dashboard so the UI reflects
            // the newly created open work session
            const updatedDashboard =
                await getEmployeeDashboard(session.accessToken)

            // save the updated dashboard data into state
            setDashboard(updatedDashboard)

        } catch (error) {
            // if the clock-in request fails, stores an error message
            setClockActionError(
                error instanceof Error
                    ? error.message
                    : 'Unable to clock in.'
            )
        } finally {
            // re-enable the clock action when finish
            setIsClockActionLoading(false)
        }
    }

    // handles employee clock-out request
    async function handleClockOut() {

        // stop if there is no authenticated session
        if (!session || !dashboard) {
            return
        }

        try {
            // disable clock actions while request is running
            setIsClockActionLoading(true)

            // clear previous clock action error
            setClockActionError(null)

            // perform clock-out request
            await clockOut(
                session.accessToken,
                dashboard.employeeId
            )

            // reload dashboard with updated work-session data
            const updatedDashboard =
                await getEmployeeDashboard(session.accessToken)

            setDashboard(updatedDashboard)

        } catch (error) {
            setClockActionError(
                error instanceof Error
                    ? error.message
                    : 'Unable to clock out.'
            )
        } finally {
            // re-enable the clock action when finish
            setIsClockActionLoading(false)
        }
    }

    // display the dashboard after the data loads successfully
    return (
        <PageContainer>

            {/* EMPLOYEE PROFILE IMAGE AND NAME */}
            <div className="employee-heading">

                <UserAvatar
                    imageUrl={dashboard.profileImageUrl}
                    name={`${dashboard.firstName} ${dashboard.lastName}`}
                />

                <h1>
                    Welcome, {dashboard.firstName} {dashboard.lastName}
                </h1>

            </div>

            <p>Email: {dashboard.email}</p>
            <p>
                Status:{' '}
                <span className="status-badge status-active">
                    {dashboard.status}
                </span>
            </p>

            <p>
                Clock Status:{' '}
                <span
                    className={
                        dashboard.currentlyClockedIn
                            ? 'status-badge status-open'
                            : 'status-badge status-closed'
                    }
                >
                    {dashboard.currentlyClockedIn
                        ? 'Clocked In'
                        : 'Clocked Out'}
                </span>
            </p>

            {/* ONLY ONE ACTION WILL BE AVAILABLE AT THE TIME */}
            {/* CLOCK-IN ACTION BUTTON */}
            {!dashboard.currentlyClockedIn && (
                <button
                    type="button"
                    className="clock-button clock-in-button"
                    onClick={handleClockIn}
                    disabled={
                        isClockActionLoading ||
                        dashboard.status !== 'ACTIVE'
                    }
                >
                    {isClockActionLoading
                        ? 'Clocking In...'
                        : 'Clock In'}
                </button>
            )}
            {clockActionError && (
                <ErrorMessage message={clockActionError} />
            )}

            {/* CLOCK-OUT ACTION BUTTON */}
            {dashboard.currentlyClockedIn && (
                <button
                    type="button"
                    className="clock-button clock-out-button"
                    onClick={handleClockOut}
                    disabled={isClockActionLoading}
                >
                    {isClockActionLoading
                        ? 'Clocking Out...'
                        : 'Clock Out'}
                </button>
            )}

            {/* CURRENT SESSION SECTION */}
            <section className="current-session-card">
                <h2>Current Session</h2>

                {dashboard.currentOpenSession ? (
                    <>
                        <p>
                            Clocked In:{' '}
                            {new Date(
                                dashboard.currentOpenSession.clockInTime
                            ).toLocaleString()}
                        </p>

                        <p>
                            Status:{' '}
                            <span className="status-badge status-open">
                                {dashboard.currentOpenSession.status}
                            </span>
                        </p>
                    </>
                ) : (
                    <EmptyState message="No active work session." />
                )}
            </section>

            <p>
                Today Worked:{' '}
                {formatMinutes(dashboard.todayWorkedMinutes)}
            </p>

            {/* RECENT WORK SESSIONS SECTION */}
            <section>
                <h2>Recent Work Sessions</h2>

                {dashboard.recentWorkSessions.length === 0 ? (
                    <EmptyState message="No recent work sessions found." />
                ) : (
                    <SimpleTable
                        headers={[
                            'Clock In',
                            'Clock Out',
                            'Status',
                            'Worked Time'
                        ]}
                    >
                        {dashboard.recentWorkSessions.map((workSession) => (
                            <tr key={workSession.workSessionId}>
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

export default EmployeeDashboardPage