package ru.bssg.lottabyte.core.model.searchQuery;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SearchQuery {
    private UUID id;
    private String query;
    private LocalDateTime created;
    private String creator;
}
