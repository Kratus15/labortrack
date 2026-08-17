import { NavLink } from "react-router";


type SidebarProps = {
    role: "ADMIN" | "EMPLOYEE"
}

/**
 * Sidebar navigation component. Displays
 * navigation links based on the user's
 * role. Admin users can access the
 * dashboard and employees pages, while
 * Employee users can access their employee
 * dashboard. This is a UI restriction,
 * backend must still enforce security.
 */
export function Sidebar({ role }: SidebarProps) {
    return (
        <aside className="app-sidebar">
            <nav className="sidebar-nav">
                {role === "ADMIN" && (
                    <>
                        {/*
                        Nav links are better than <a> because it changes the
                        page without reloading the entire application.
                        */}
                        <NavLink to="/admin/dashboard">Dashboard</NavLink>
                        <NavLink to="/admin/employees">Employees</NavLink>
                    </>
                )}

                {role === "EMPLOYEE" && (
                    <NavLink to="/employee/dashboard">Dashboard</NavLink>
                )}
            </nav>
        </aside>
    )
}