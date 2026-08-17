import type { ReactNode } from 'react'

type PageContainerProps = {
    children: ReactNode
}

/**
 * Reusable page container component. Wraps
 * page content in a shared container so
 * common layout or styling can be applied
 * consistently.
 */
function PageContainer({ children }: PageContainerProps) {
    return (
        <div className="page-container">
            {children}
        </div>
    )
}

export default PageContainer