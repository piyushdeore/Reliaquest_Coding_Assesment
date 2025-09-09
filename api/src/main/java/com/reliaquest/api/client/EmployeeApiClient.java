package com.reliaquest.api.client;

import com.reliaquest.api.dto.external.ExternalApiResponse;
import com.reliaquest.api.dto.external.ExternalEmployeeDTO;
import com.reliaquest.api.dto.request.CreateEmployeeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "employee-api",
    url = "${employee.api.base-url}",
    configuration = {com.reliaquest.api.config.FeignConfiguration.class}
)
public interface EmployeeApiClient {

    @GetMapping
    ExternalApiResponse<List<ExternalEmployeeDTO>> getAllEmployees();

    @GetMapping("/{id}")
    ExternalApiResponse<ExternalEmployeeDTO> getEmployeeById(@PathVariable("id") String id);

    @PostMapping
    ExternalApiResponse<ExternalEmployeeDTO> createEmployee(@RequestBody CreateEmployeeRequest request);

    @DeleteMapping("/{name}")
    void deleteEmployee(@PathVariable("name") String name);
}
