import { useState } from 'react'

type UserAvatarProps = {
    imageUrl: string | null
    name: string
}

/**
 * This function displays the user's profile
 * avatar. Shows the profile image when one
 * is available, and falls back to the user's
 * first initial if the image is missing or
 * fails to load.
 */
function UserAvatar({
    imageUrl,
    name
}: UserAvatarProps) {

    const [imageFailed, setImageFailed] = useState(false)

    const shouldShowImage =
        Boolean(imageUrl) && !imageFailed

    return (
        <div className="user-avatar">

            {shouldShowImage ? (
                <img
                    src={imageUrl!}
                    alt={'${name} profile'}
                    onError={() => setImageFailed(true)}
                />
            ) : (
                <span aria-hidden="true">
                    {name.charAt(0).toUpperCase()}
                </span>
            )}
        </div>
    )
}

export default UserAvatar