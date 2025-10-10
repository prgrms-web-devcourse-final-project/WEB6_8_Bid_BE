package com.backend.domain.product.dto.response;

import co.elastic.clients.elasticsearch.indices.reload_search_analyzers.ReloadDetails;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record ReloadAnalyzersResponse(
        @NotNull Boolean success,
        @NotNull List<ReloadDetails> reloadedNodes,
        @NotNull LocalDateTime timestamp
) {}
