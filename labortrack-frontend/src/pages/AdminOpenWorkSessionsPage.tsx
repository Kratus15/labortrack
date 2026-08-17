import { useEffect, useState } from 'react'

import PageContainer from '../components/PageContainer'
import LoadingState from '../components/LoadingState'
import ErrorMessage from '../components/ErrorMessage'
import EmptyState from '../components/EmptyState'
import SimpleTable from '../components/SimpleTable'
import UserAvatar from '../components/UserAvatar'

import { useAuth } from '../auth/AuthContext'
import { getAdminOpenWorkSessions } from '../api/DashboardApi'

import type {
    WorkSessionSummaryResponse
} from '../api/DashboardTypes'

/**
 * This function displays the admin open work
 * sessions page. Shows employees who are
 * currently clocked in and have an active work
 * session.
 */
function AdminOpenWorkSessionsPage() {
    const { session } = useAuth()

    const [sessions, setSessions] =
        useState<WorkSessionSummaryResponse[]>([])

    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {

        if (!session) {
            return
        }

        async function loadOpenSessions() {

            try {
                const response =
                    await getAdminOpenWorkSessions(
                        session!.accessToken
                    )

                setSessions(response)

            } catch {
                setError(
                    'Unable to load open work sessions.'
                )
            } finally {
                setIsLoading(false)
            }
        }

        loadOpenSessions()

    }, [session])

    if (isLoading) {
        return <LoadingState />
    }

    if (error) {
        return <ErrorMessage message={error} />
    }

    return (
        <PageContainer>

            <h1>Currently Clocked In</h1>

            <p>
                Employees who currently have an open work session.
            </p>

            {/* If no employees are currently clock in. Displays
            this EmptyState card with this message. */}
            {sessions.length === 0 ? (

                <EmptyState
                    message="No employees are currently clocked in."
                />

            ) : (

                <SimpleTable
                    headers={[
                        'Employee',
                        'Clock In',
                        'Status'
                    ]}
                >
                    {/* If any employee currently clock-in displays the
                    employee's details below. */}
                    {sessions.map((workSession) => (

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
                                <span className="status-badge status-open">
                                    {workSession.status}
                                </span>
                            </td>

                        </tr>

                    ))}
                </SimpleTable>

            )}

        </PageContainer>
    )
}

export default AdminOpenWorkSessionsPage