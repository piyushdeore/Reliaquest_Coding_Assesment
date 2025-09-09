package com.reliaquest.api.controller;

import com.reliaquest.api.dto.EmployeeDTO;
import com.reliaquest.api.dto.request.CreateEmployeeRequest;
import com.reliaquest.api.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/v1/employee")
@Slf4j
public class EmployeeController implements IEmployeeController<EmployeeDTO, CreateEmployeeRequest> {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        log.info("Received request to get all employees");
        List<EmployeeDTO> employees = employeeService.getAllEmployees();
        log.info("Successfully retrieved {} employees", employees.size());
        return ResponseEntity.ok(employees);
    }

    @Override
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByNameSearch(String searchString) {
        log.info("Received request to search employees by name: {}", searchString);
        List<EmployeeDTO> employees = employeeService.getEmployeesByNameSearch(searchString);
        log.info("Found {} employees matching search criteria: {}", employees.size(), searchString);
        return ResponseEntity.ok(employees);
    }

    @Override
    public ResponseEntity<EmployeeDTO> getEmployeeById(String id) {
        log.info("Received request to get employee by ID: {}", id);
        EmployeeDTO employee = employeeService.getEmployeeById(id);
        log.info("Successfully retrieved employee with ID: {}", id);
        return ResponseEntity.ok(employee);
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.info("Received request to get highest salary of employees");
        Integer highestSalary = employeeService.getHighestSalaryOfEmployees();
        log.info("Successfully retrieved highest salary: {}", highestSalary);
        return ResponseEntity.ok(highestSalary);
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.info("Received request to get top ten highest earning employee names");
        List<String> topEarners = employeeService.getTopTenHighestEarningEmployeeNames();
        log.info("Successfully retrieved {} top earning employee names", topEarners.size());
        return ResponseEntity.ok(topEarners);
    }

    @Override
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        log.info("Received request to create employee: {}", request.getName());
        EmployeeDTO createdEmployee = employeeService.createEmployee(request);
        log.info("Successfully created employee with ID: {} and name: {}", createdEmployee.getId(), createdEmployee.getName());
        return ResponseEntity.ok(createdEmployee);
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        log.info("Received request to delete employee by ID: {}", id);
        String result = employeeService.deleteEmployeeById(id);
        log.info("Successfully deleted employee with ID: {}", id);
        return ResponseEntity.ok(result);
    }
}