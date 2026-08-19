package com.labortrack.labortrack_backend.common.dto.response;
import java.util.List;

/**
 * This DTO represents a reusable paginated API response.
 * It provides a common structure for endpoints that return
 * paginated data along with pagination metadata.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
