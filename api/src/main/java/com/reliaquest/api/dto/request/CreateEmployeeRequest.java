package com.reliaquest.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmployeeRequest {

    @NotBlank(message = "Employee name must not be blank")
    private String name;

    @NotNull(message = "Salary is required") @Min(value = 1, message = "Salary must be greater than 0")
    private Integer salary;

    @NotNull(message = "Age is required") @Min(value = 18, message = "Age must be at least 16")
    @Max(value = 60, message = "Age must not be more than 75")
    private Integer age;

    @NotBlank(message = "Title must not be blank")
    private String title;
}
