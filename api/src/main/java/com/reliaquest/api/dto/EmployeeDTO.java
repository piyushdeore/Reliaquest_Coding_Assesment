package com.reliaquest.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {

    private String id;
    private String name;
    private Integer salary;
    private Integer age;
    private String title;
    private String email;
}
