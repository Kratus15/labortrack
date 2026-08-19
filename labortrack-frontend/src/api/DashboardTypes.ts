export type WorkSessionStatus =
    | 'OPEN'
    | 'CLOSED'
    | 'ADJUSTED'

export type WorkSessionSummaryResponse = {
    workSessionId: number
    employeeId: number
    employeeName: string
    clockInTime: string
    clockOutTime: string | null
    status: WorkSessionStatus
    workedMinutes: number | null
}

export type AdminDashboardResponse = {
    totalEmployees: number
    activeEmployees: number
    inactiveEmployees: number
    currentlyClockedInEmployees: number
    todayWorkedMinutes: number
    recentWorkSessions: WorkSessionSummaryResponse[]
}

export type EmployeeStatus =
    | 'ACTIVE'
| 'INACTIVE'
| 'TERMINATED'

export type AdminEmployeeListItemResponse = {
    employeeId: number
    firstName: string
    lastName: string
    email: string
    phone: string | null
    hourlyRate: number
    status: EmployeeStatus
    profileImageUrl: string | null
    currentlyClockedIn: boolean
}

export type AdminEmployeeDetailResponse = {
    employeeId: number
    userId: number
    firstName: string
    lastName: string
    email: string
    phone: string | null
    hourlyRate: number
    hireDate: string
    status: EmployeeStatus
    profileImageUrl: string | null
    currentlyClockedIn: boolean
    currentOpenSession: WorkSessionSummaryResponse | null
    recentWorkSessions: WorkSessionSummaryResponse[]
}

export type EmployeeDashboardResponse = {
    employeeId: number
    firstName: string
    lastName: string
    email: string
    phone: string | null
    hourlyRate: number
    hireDate: String
    status: EmployeeStatus
    profileImageUrl: string | null
    currentlyClockedIn: boolean
    currentOpenSession: WorkSessionSummaryResponse | null
    todayWorkedMinutes: number
    recentWorkSessions: WorkSessionSummaryResponse[]
}

export type ClockInResponse = {
    workSessionId: number
    employeeId: number
    clockInTime: string
    status: WorkSessionStatus
    message: string
}

export type ClockOutResponse = {
    workSessionId: number
    employeeId: number
    clockInTime: string
    clockOutTime: string
    status: WorkSessionStatus
    workedMinutes: number
    message: string
}

export type PageResponse<T> = {
    content: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}


