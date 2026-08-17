export type CompanyRegistrationRequest = {
    companyName: string;
    adminEmail: string
    adminPassword: string
}

export type CompanyRegistrationResponse = {
    companyId: number
    companyName: string
    adminUserId: number
    adminEmail: string
    timezone: string
    active: boolean
    createdAt: string
}