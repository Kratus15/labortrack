type EmptyStateProps = {
    message: string
}

/**
 * Reusable empty state component.
 * Displays a message when there is
 * no data or content available to
 * show. Of course when the request
 * was successful, an error will use
 * ErrorMessage instead of EmptyState.
 */
function EmptyState({ message }: EmptyStateProps) {
    return (
        <div>
            <p>{message}</p>
        </div>
    )
}

export default EmptyState