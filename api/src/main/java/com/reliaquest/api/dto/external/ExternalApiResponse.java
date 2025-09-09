package com.reliaquest.api.dto.external;

import lombok.Builder;
import lombok.Data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalApiResponse<T> {
    private T data;
    private String status;
}
