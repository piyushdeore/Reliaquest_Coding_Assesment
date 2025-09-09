package com.reliaquest.api.service.impl;

import com.reliaquest.api.client.EmployeeApiClient;
import com.reliaquest.api.constants.ErrorConstants;
import com.reliaquest.api.dto.EmployeeDTO;
import com.reliaquest.api.dto.external.ExternalApiResponse;
import com.reliaquest.api.dto.external.ExternalEmployeeDTO;
import com.reliaquest.api.dto.request.CreateEmployeeRequest;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.service.EmployeeService;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeApiClient employeeApiClient;

    //  Core API Calls
    @Retry(name = "employee-api")
    private ExternalApiResponse<List<ExternalEmployeeDTO>> fetchEmployeesFromApi() {
        log.debug("Starting API call to fetch all employees using Feign client");

        try {
            ExternalApiResponse<List<ExternalEmployeeDTO>> response = employeeApiClient.getAllEmployees();

            // If response null return empty list
            if (response == null) {
                log.warn("External API returned null response, creating empty response object");
                response = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                        .data(Collections.emptyList())
                        .status("success")
                        .build();
            } else if (response.getData() == null) {
                log.warn("External API returned response with null data field, setting empty list");
                response.setData(Collections.emptyList());
            }

            log.info("Successfully fetched {} employees from external api", response.getData().size());
            return response;

        } catch (Exception e) {
            log.error("error occured while fetching employees from external API: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Retry(name = "employee-api")  // retry logic with exponential backoff
    private ExternalApiResponse<ExternalEmployeeDTO> fetchEmployeeByIdFromApi(String id) {
        log.debug("Initiating API call to fetch employee details for ID: {}", id);
        
        try {
            ExternalApiResponse<ExternalEmployeeDTO> response = employeeApiClient.getEmployeeById(id);
            log.info("Successfully retrieved employee data for ID: {}", id);
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch employee with ID: {}. Error: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Retry(name = "employee-api")   // retry logic with exponential backoff
    private ExternalApiResponse<ExternalEmployeeDTO> createEmployeeInApi(CreateEmployeeRequest request) {
        log.debug("Attempting to create new employee: {} with salary: {}", request.getName(), request.getSalary());
        
        try {
            ExternalApiResponse<ExternalEmployeeDTO> response = employeeApiClient.createEmployee(request);
            log.info("Successfully created employee: {} in external system", request.getName());
            return response;
        } catch (Exception e) {
            log.error("Failed to create employee: {}. Error details: {}", request.getName(), e.getMessage(), e);
            throw e;
        }
    }

    @Retry(name = "employee-api")  // retry logic with exponential backoff
    private void deleteEmployeeInApi(String name) {
        log.debug("Initiating deletion request for employee: {}", name);
        
        try {
            employeeApiClient.deleteEmployee(name);
            log.info("Successfully deleted employee: {} from external system", name);
        } catch (Exception e) {
            log.error("Failed to delete employee: {}. Error: {}", name, e.getMessage(), e);
            throw e;
        }
    }

    // Utility Methods

    private List<EmployeeDTO> mapToEmployeeDTOList(List<ExternalEmployeeDTO> externalEmployees) {
        log.debug("Converting {} external employee records to internal format", externalEmployees.size());
        
        List<EmployeeDTO> mappedEmployees = externalEmployees.stream()
                .filter(Objects::nonNull)
                .map(this::mapToEmployeeDTO)
                .collect(Collectors.toList());
                
        log.debug("Successfully converted {} employee records", mappedEmployees.size());
        return mappedEmployees;
    }

    private EmployeeDTO mapToEmployeeDTO(ExternalEmployeeDTO emp) {
        log.trace("Mapping employee: {} (ID: {})", emp.getEmployeeName(), emp.getId());
        
        return EmployeeDTO.builder()
                .id(emp.getId())
                .name(emp.getEmployeeName())
                .salary(emp.getEmployeeSalary())
                .age(emp.getEmployeeAge())
                .title(emp.getEmployeeTitle())
                .email(emp.getEmployeeEmail())
                .build();
    }

    //Service methods
    @Override
    public List<EmployeeDTO> getAllEmployees() {
        log.info("fetching all employees from service");
        List<EmployeeDTO> employees = mapToEmployeeDTOList(fetchEmployeesFromApi().getData());
        log.info("successfully retrieved {} employees", employees.size());
        return employees;
    }

    @Override
    public List<EmployeeDTO> getEmployeesByNameSearch(String searchString) {
        log.info("Received request to search employees by name pattern: '{}'", searchString);
        
        // Input validation
        if (Objects.isNull(searchString) || searchString.isBlank()) {
            log.warn("Search request rejected - invalid search string provided: '{}'", searchString);
            throw new EmployeeServiceException(ErrorConstants.INVALID_SEARCH_STRING);
        }

        log.debug("Fetching all employees to perform name-based filtering");
        List<EmployeeDTO> allEmployees = getAllEmployees();
        
        // for case-insensitive search
        List<EmployeeDTO> matchingEmployees = allEmployees.stream()
                .filter(emp -> emp.getName().toLowerCase().contains(searchString.toLowerCase()))
                .collect(Collectors.toList());
                
        log.info("Search completed - found {} employees matching pattern '{}' out of {} total employees", 
                matchingEmployees.size(), searchString, allEmployees.size());
        return matchingEmployees;
    }

    @Override
    public EmployeeDTO getEmployeeById(String id) {
        log.info("Fetching employee by ID: {}", id);
        if (Objects.isNull(id) || id.isBlank()) {
            log.warn("Invalid employee ID provided: {}", id);
            throw new EmployeeServiceException(ErrorConstants.INVALID_EMPLOYEE_ID);
        }

        EmployeeDTO employee = mapToEmployeeDTO(fetchEmployeeByIdFromApi(id).getData());
        log.info("Succesfully retrieved employee with id: {} and name: {}", id, employee.getName());
        return employee;
    }

    @Override
    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        log.info("Creating new employee: {}", request.getName());
        EmployeeDTO createdEmployee = mapToEmployeeDTO(createEmployeeInApi(request).getData());
        log.info("Successfully created employee with ID: {} and name: {}", createdEmployee.getId(), createdEmployee.getName());
        return createdEmployee;
    }

    @Override
    public String deleteEmployeeById(String id) {
        log.info("Deleting employee by ID: {}", id);
        if (Objects.isNull(id) || id.isBlank()) {
            log.warn("Invalid employee ID provided for deletion: {}", id);
            throw new EmployeeServiceException(ErrorConstants.INVALID_EMPLOYEE_ID);
        }
        
        EmployeeDTO employee = getEmployeeById(id);
        String name = employee.getName();
        log.debug("Found employee to delete - ID: {}, Name: {}", id, name);

        deleteEmployeeInApi(name);
        String result = "Employee with ID " + id + " deleted successfully.";
        log.info("Successfully deleted employee with ID: {} and name: {}", id, name);
        return result;
    }

    @Override
    public Integer getHighestSalaryOfEmployees() {
        log.info("Calculating highest salary among all employees");
        Integer highestSalary = getAllEmployees().stream()
                .filter(emp -> emp.getSalary() != null)
                .map(EmployeeDTO::getSalary)
                .max(Integer::compareTo)
                .orElseThrow(() -> {
                    log.warn("No salary data available for employees");
                    return new EmployeeServiceException(ErrorConstants.EMPLOYEE_NO_DATA);
                });
        log.info("Successfully calculated highest salary: {}", highestSalary);
        return highestSalary;
    }

    @Override
    public List<String> getTopTenHighestEarningEmployeeNames() {
        log.info("Retrieving top ten highest earning employee names");
        List<EmployeeDTO> employees = getAllEmployees();
        if (CollectionUtils.isEmpty(employees)) {
            log.warn("No employee data available for top earners calculation");
            throw new EmployeeServiceException(ErrorConstants.EMPLOYEE_NO_DATA);
        }

        List<String> topEarners = employees.stream()
                .filter(emp -> emp.getSalary() != null)
                .sorted((e1, e2) -> e2.getSalary().compareTo(e1.getSalary()))
                .limit(10)
                .map(EmployeeDTO::getName)
                .collect(Collectors.toList());
        log.info("Successfully retrieved {} top earning employee names", topEarners.size());
        return topEarners;
    }
}
