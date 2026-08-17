/**
 * This function converts a total number of
 * minutes into a readable hours-and-minutes
 * format 125m -> 2h 5m
 */
export function formatMinutes(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60

    return `${hours}h ${remainingMinutes}m`;
}