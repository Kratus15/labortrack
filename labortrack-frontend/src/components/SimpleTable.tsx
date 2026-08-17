import type { ReactNode } from 'react'

type SimpleTableProps = {
    headers: string[]
    children: ReactNode
}

/**
 * Reusable table component. Renders
 * table headers from the provided
 * header list and displays custom
 * table rows passed in as children.
 * Basically handles the basic table
 * structure table - headers - rows.
 */
function SimpleTable({
    headers,
    children
}: SimpleTableProps) {
    return (
        <div className="table-container">
            <table className="simple-table">
                <thead>
                    <tr>
                        {headers.map((header) => (
                            <th key={header}>
                                {header}
                            </th>
                        ))}
                    </tr>
                </thead>

                <tbody>
                    {children}
                </tbody>
            </table>
        </div>
    )
}

export default SimpleTable