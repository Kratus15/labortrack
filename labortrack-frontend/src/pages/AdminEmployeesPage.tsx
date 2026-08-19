import { useEffect, useState } from 'react'
import PageContainer from '../components/PageContainer'
import LoadingState from '../components/LoadingState'
import ErrorMessage from '../components/ErrorMessage'
import EmptyState from '../components/EmptyState'
import SimpleTable from '../components/SimpleTable'
import { useAuth } from '../auth/AuthContext'
import { getAdminEmployees } from '../api/DashboardApi'
import type { AdminEmployeeListItemResponse, EmployeeStatus } from '../api/DashboardTypes'
import { Link, useSearchParams } from 'react-router'
import UserAvatar from '../components/UserAvatar'

/**
 * This function Renders the admin employees
 * page, displaying and managing employee
 * information.
 */
function AdminEmployeesPage() {

    // get the authenticated session
    const { session } = useAuth()

    // store the list of employees returned by the backend
    const [employees, setEmployees] =
        useState<AdminEmployeeListItemResponse[]>([])

    // tracks whether the employee data is currently loading
    const [isLoading, setIsLoading] = useState(true)

    // stores error message if API call fails
    const [error, setError] = useState<string | null>(null)

    const [searchParams, setSearchParams] = useSearchParams()

    /*
    Read the employee status filter from URL param.
    using the URL keeps the selected filter after page is refreshed.
    only accept valid employee status filter from param.
    if the url has no status use '' - all.
     */
    const statusParam = searchParams.get('status')
    const statusFilter: EmployeeStatus | '' =
        statusParam === 'ACTIVE' ||
        statusParam === 'INACTIVE' ||
        statusParam === 'TERMINATED'
            ? statusParam
            : ''

    // pagination vars
    const [currentPage, setCurrentPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [isFirstPage, setIsFirstPage] = useState(true)
    const [isLastPage, setIsLastPage] = useState(true)

    // DEFAULT VALUE
    const pageSize = 20

    // runs whenever the session changes
    useEffect(() => {

        // if there is no authenticated session, don't make the call
        if (!session) {
            return
        }

        let cancelled = false

        // load employees
        async function loadEmployees() {
            try {
                // show the running state while the request is loading
                setIsLoading(true)
                // clean any previous error before trying again
                setError(null)

                // make the request
                const data = await getAdminEmployees(
                    session!.accessToken,
                    currentPage,
                    pageSize,
                    statusFilter || undefined
                )

                // ignore response if this effect is already outdated
                if (cancelled) {
                    return
                }

                // set the pagination vars
                setEmployees(data.content)
                setCurrentPage(data.page)
                setTotalPages(data.totalPages)
                setIsFirstPage(data.first)
                setIsLastPage(data.last)

            } catch (error) {

                if (cancelled) {
                    return
                }

                // if something goes wrong save an error message
                setError(
                    error instanceof Error
                        ? error.message
                        : 'Unable to load employees.'
                )
            } finally {
                if (!cancelled) {
                    setIsLoading(false)
                }
            }
        }

        // load the employees (call the func)
        void loadEmployees()

        // if filter/page changes previous requests gets ignored
        return () => {
            cancelled = true
        }

        // run the effect again if authenticated session or statusFilter changes again
    }, [session, statusFilter, currentPage])

    // while backend request is running display loading component (avoid blinking)
    if (isLoading && employees.length === 0) {
        return <LoadingState message="Loading employees..." />
    }

    // if call fails display error message
    if (error) {
        return <ErrorMessage message={error} />
    }

    // display the list of employees (Main Page)
    return (
        <PageContainer>
            {/* CREATE NEW EMPLOYEE LINK */}
            <div className="page-heading-row">

                <h1>Employees</h1>

                <Link
                    className="primary-link-button"
                    to="/admin/employees/new"
                >
                    + Create Employee
                </Link>

            </div>

            <label>
                Status:

                {/* URL filter param*/}
                <select
                    className="status-filter"
                    value={statusFilter}
                    onChange={(event) => {
                        const nextStatus =
                            event.target.value as EmployeeStatus | ''

                        // RESET page to 0 when filter changes
                        setCurrentPage(0)

                        if (nextStatus) {
                            setSearchParams({ status: nextStatus })
                        } else {
                            setSearchParams({})
                        }
                    }}
                >
                    <option value="">All</option>
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="TERMINATED">Terminated</option>
                </select>

            {/*
            Drop down
            */}
            </label>

            {/* EMPLOYEES TABLE */}
            {employees.length === 0 ? (
                <EmptyState message="No employees found." />
            ) : (
                <>
                    <SimpleTable
                        headers={[
                            'Name',
                            'Email',
                            'Status',
                            'Clock Status',
                            'Details'
                        ]}
                    >
                        {employees.map((employee) => (
                            <tr key={employee.employeeId}>
                                <td>
                                    <div className="employee-name-cell">
                                        <UserAvatar
                                            imageUrl={employee.profileImageUrl}
                                            name={`${employee.firstName} ${employee.lastName}`}
                                        />

                                        <span>
                                {employee.firstName} {employee.lastName}
                            </span>
                                    </div>
                                </td>

                                <td>{employee.email}</td>

                                <td>
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
                                </td>

                                <td>
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
                                </td>

                                <td>
                                    <Link
                                        className="view-link"
                                        to={`/admin/employees/${employee.employeeId}`}
                                    >
                                        View
                                    </Link>
                                </td>
                            </tr>
                        ))}
                    </SimpleTable>

                    {/* PAGINATION CONTROLS */}
                    <div className="pagination-controls">
                        <button
                            type="button"
                            // disable pagination buttons while request is loading
                            disabled={isFirstPage || isLoading}
                            onClick={() =>
                                setCurrentPage((page) => Math.max(0, page - 1))
                            }
                        >
                            Previous
                        </button>

                        <span>
                            Page {currentPage + 1} of {totalPages}
                         </span>

                        <button
                            type="button"
                            // disable pagination buttons while request is loading
                            disabled={isLastPage || isLoading}
                            onClick={() =>
                                setCurrentPage((page) =>
                                    Math.min(totalPages - 1, page + 1)
                                )
                            }
                        >
                            Next
                        </button>
                    </div>
                </>
            )}
        </PageContainer>
    )
}

export default AdminEmployeesPage