package com.example.foreverhome.moderation.client.dto;

import java.util.List;

/**
 * Paginated response from Forever Home API.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public boolean hasMore() {
        return page < totalPages - 1;
    }
}
