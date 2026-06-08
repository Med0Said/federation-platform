package com.federation.news.dto;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TagResponse {
    private UUID   id;
    private String name;
    private String slug;
    private String color;
}
