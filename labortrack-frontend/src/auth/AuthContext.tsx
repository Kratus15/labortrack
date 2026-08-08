import {
    createContext,
    useContext,
    useState,
    type ReactNode,
} from 'react'
import {
    clearAuthSession,
    getAuthSession,
    saveAuthSession
} from './AuthStorage'
import type { LoginResponse } from './AuthTypes'

/**
 * Data and functions that will be available
 * to components that use the authentication
 * context.
 */
type AuthContextValue = {
    session: LoginResponse | null
    isAuthenticated: boolean
    signIn: (session: LoginResponse) => void
    signOut: () => void
}

// authentication context. Undefined by default
const AuthContext = createContext<AuthContextValue | undefined>(undefined)

/**
 * Provides authentication information and
 * actions to all components placed inside
 * this provider.
 */
export function AuthProvider({ children }: { children: ReactNode }) {

    // store the current authentication session. It retrieves any previously saved session
    const [session, setSession] = useState<LoginResponse | null>(
        () => getAuthSession(),
    )

    // sings the user in.
    function signIn(newSession: LoginResponse): void {
        // save session in storage so it can service a page refresh
        saveAuthSession(newSession)
        // update react state so app knows that user is logged in
        setSession(newSession)
    }

    // signs the user out.
    function signOut(): void {
        // removes the saved session from storage
        clearAuthSession()
        // set the React session state to null
        setSession(null)
    }

    /**
     * Makes the authentication values available
     * to every child component inside AuthProvider
     */
    return (
        <AuthContext.Provider
        value={{
            session,
            isAuthenticated: session !== null,
            signIn,
            signOut,
        }}
        >
            {children}
        </AuthContext.Provider>
    )
}

// Custom hook used by components to access authentication data
export function useAuth(): AuthContextValue {
    // read the current value
    const context = useContext(AuthContext)

    if (!context) {
        throw new Error('useAuth must be used inside AuthProvider')
    }

    return context
}