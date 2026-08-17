import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import PageContainer from '../components/PageContainer'
import LoadingState from '../components/LoadingState'
import ErrorMessage from '../components/ErrorMessage'
import { useAuth } from '../auth/AuthContext'
import { getAdminEmployeeDetail } from '../api/DashboardApi'
import type { AdminEmployeeDetailResponse } from '../api/DashboardTypes'
import EmptyState from '../components/EmptyState'
import SimpleTable from '../components/SimpleTable'
import { formatMinutes } from '../utils/timeUtils'
import UserAvatar from '../components/UserAvatar'

/**
 * This function displays the details of one employee.
 * Gets the employee ID from the URL and uses it
 * to load the employee information from the
 * backend.
 */
function AdminEmployeeDetailPage() {

    // get authenticated current session
    const { session } = useAuth()

    // get employeeId param from URL
    const { employeeId } = useParams()

    // stores the employee information returned by backend
    const [employee, setEmployee] =
        useState<AdminEmployeeDetailResponse | null>(null)

    // tracks whether the employee's data is currently loading
    const [isLoading, setIsLoading] = useState(true)

    // stores any given error message if something goes wrong
    const [error, setError] = useState<string | null>(null)

    // runs when the page run/or something changes
    useEffect(() => {

        // do not continue if there is no authenticated session
        if (!session || !employeeId) {
            return
        }

        // parsed the string employeeId to number
        const parsedEmployeeId = Number(employeeId)

        // make sure that number is valid
        if (Number.isNaN(parsedEmployeeId)) {
            setError('Invalid employee ID.')
            setIsLoading(false)
            return
        }

        // load the employee
        async function loadEmployee() {
            try {

                // shows the loading state while the request is loading
                setIsLoading(true)
                // clear any previous error message
                setError(null)

                // make the requests to the backend using employeeId & token
                const data = await getAdminEmployeeDetail(
                    session!.accessToken,
                    parsedEmployeeId
                )

                // save the returned data
                setEmployee(data)

            } catch (error) {
                // if the API call fails save an error message
                setError(
                    error instanceof Error
                        ? error.message
                        : 'Unable to load employee.'
                )
            } finally {
                setIsLoading(false)
            }
        }

        // load the employee (call the function)
        void loadEmployee()

        // run the effect again if session or employeeId changes
    }, [session, employeeId])

    // while waiting for the request show the loading component
    if (isLoading) {
        return <LoadingState message="Loading employee..." />
    }

    // if something went wrong show the error message
    if (error) {
        return <ErrorMessage message={error} />
    }

    // safety check in case loading is finish and no emp data is returned
    if (!employee) {
        return <ErrorMessage message="Employee data is unavailable." />
    }

    // if everything loaded successfully
    // display the employee's information
    return (
        <PageContainer>

            {/* EMPLOYEE PROFILE IMAGE AND NAME */}
            <div className="employee-heading">

                <UserAvatar
                    imageUrl={employee.profileImageUrl}
                    name={`${employee.firstName} ${employee.lastName}`}
                />

                <h1>
                    {employee.firstName} {employee.lastName}
                </h1>

            </div>

            {/* EMPLOYEE DETAIL CARD */}
            <div className="employee-detail-card">

                <p>
                    <strong>Email:</strong>{' '}
                    {employee.email}
                </p>

                <p>
                    <strong>Phone:</strong>{' '}
                    {employee.phone || 'Not provided'}
                </p>

                <p>
                    <strong>Hourly Rate:</strong>{' '}
                    ${employee.hourlyRate.toFixed(2)}
                </p>

                <p>
                    <strong>Hire Date:</strong>{' '}
                    {employee.hireDate}
                </p>

                <p>
                    <strong>Status:</strong>{' '}
                    <span
                        className={`status-badge ${
                            employee.status === 'ACTIVE'
                                ? 'status-active'
                                : employee.status === 'INACTIVE'
                                    ? 'status-inactive'
                                    : 'status-terminated'
                        }`}
                    >
                    {employee.status}
                </span>
                </p>

                <p>
                    <strong>Clock Status:</strong>{' '}

                    <span
                        className={
                            employee.currentlyClockedIn
                                ? 'status-badge status-open'
                                : 'status-badge status-closed'
                        }
                    >
                    {employee.currentlyClockedIn
                        ? 'Clocked In'
                        : 'Clocked Out'}
                </span>
                </p>

            </div>

            {/* EMPLOYEE DETAILS CURRENT SESSION CARD */}
            <section className="current-session-card">
                <h2>Current Session</h2>

                {employee.currentOpenSession ? (
                    <>
                        <p>
                            <strong>Clocked In:</strong>{' '}
                            {new Date(
                                employee.currentOpenSession.clockInTime
                            ).toLocaleString()}
                        </p>

                        <p>
                            <strong>Status:</strong>{' '}
                            <span className="status-badge status-open">
                    {employee.currentOpenSession.status}
                </span>
                        </p>
                    </>
                ) : (
                    <EmptyState message="No active work session." />
                )}
            </section>

            {/* EMPLOYEE DETAILS RECENT WORK SESSION TABLE CARD */}
            <section>
                <h2>Recent Work Sessions</h2>

                {employee.recentWorkSessions.length === 0 ? (
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
                        {employee.recentWorkSessions.map((workSession) => (
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

                                <td>
                                    {workSession.status}
                                </td>

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

export default AdminEmployeeDetailPage