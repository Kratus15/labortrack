type ErrorMessageProps = {
    message: string
}

/**
 * Reusable error message component.
 * Displays an error message when an
 * operation fails or when something
 * goes wrong. Message props is required.
 */
function ErrorMessage({ message }: ErrorMessageProps) {
    return (
        <div>
            <p>{message}</p>
        </div>
    )

}

export default ErrorMessage