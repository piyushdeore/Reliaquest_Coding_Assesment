package com.reliaquest.api.service;

import com.reliaquest.api.dto.EmployeeDTO;
import com.reliaquest.api.dto.request.CreateEmployeeRequest;
import java.util.List;

public interface EmployeeService {
    List<EmployeeDTO> getAllEmployees();

    List<EmployeeDTO> getEmployeesByNameSearch(String name);

    EmployeeDTO getEmployeeById(String id);

    Integer getHighestSalaryOfEmployees();

    List<String> getTopTenHighestEarningEmployeeNames();

    EmployeeDTO createEmployee(CreateEmployeeRequest request);

    String deleteEmployeeById(String id);
}
