package com.example.foreverhome.moderation.client.dto;

import java.util.List;

/**
 * Paginated response from Forever Home API.
 * Must match the main app's PagedResponse format.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public boolean hasMore() {
        return !last;
    }

    public int pageSize() {
        return size;
    }
}
