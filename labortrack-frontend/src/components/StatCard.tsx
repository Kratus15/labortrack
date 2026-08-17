type StatCardProps = {
    title: string
    value: string | number
}

/**
 * Reusable statistic card component.
 * Displays a title and a corresponding
 * value for showing summary or dashboard
 * information.
 */
function StatCard({ title, value }: StatCardProps) {
    return (
        <div className="stat-card">
            <p className="stat-card-title">
                {title}
            </p>

            <p className="stat-card-value">
                {value}
            </p>
        </div>
    )
}

export default StatCard