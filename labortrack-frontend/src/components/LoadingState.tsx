type LoadingStateProps = {
    message?: string // ? means optional
}

/**
 * Reusable loading state component.
 * Displays a loading message while
 * content or data is being fetched.
 * Props is optional, if message
 * receives it prints out, if not
 * then loading... gets printed.
 */
function LoadingState({
    message = 'Loading...'
}: LoadingStateProps) {
    return (
        <div>
            <p>{message}</p>
        </div>
    )
}

export default LoadingState