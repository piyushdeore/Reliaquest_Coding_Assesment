package com.reliaquest.api.controller;

import com.reliaquest.api.constants.ErrorConstants;
import com.reliaquest.api.dto.EmployeeDTO;
import com.reliaquest.api.dto.request.CreateEmployeeRequest;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.TooManyRequestsException;
import com.reliaquest.api.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeControllerTest {

    @InjectMocks
    private EmployeeController employeeController;

    @Mock
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // getAllEmployees
    @Test
    @DisplayName("GET /employees - Should return all employees successfully")
    void testGetAllEmployees_Positive() {
        EmployeeDTO emp1 = EmployeeDTO.builder().id("1").name("Rahul Sharma").salary(50000).build();
        EmployeeDTO emp2 = EmployeeDTO.builder().id("2").name("Priya Singh").salary(60000).build();
        List<EmployeeDTO> employees = Arrays.asList(emp1, emp2);

        when(employeeService.getAllEmployees()).thenReturn(employees);

        ResponseEntity<List<EmployeeDTO>> response = employeeController.getAllEmployees();
        List<EmployeeDTO> result = response.getBody();

        assertEquals(employees.size(), result.size());
        assertEquals(emp1.getName(), result.get(0).getName());
    }

    @Test
    @DisplayName("GET /employees - Should return empty list when no employees exist")
    void testGetAllEmployees_Negative_EmptyList() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        ResponseEntity<List<EmployeeDTO>> response = employeeController.getAllEmployees();
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /employees - Should throw 429 Too Many Requests")
    void testGetAllEmployees_TooManyRequests() {
        when(employeeService.getAllEmployees())
                .thenThrow(new TooManyRequestsException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> employeeController.getAllEmployees());
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, exception.getMessage());
    }

    //getEmployeeById
    @Test
    @DisplayName("GET /employees/{id} - Should return employee by ID successfully")
    void testGetEmployeeById_Positive() {
        EmployeeDTO emp = EmployeeDTO.builder().id("3").name("Anjali Mehta").salary(55000).build();
        when(employeeService.getEmployeeById("3")).thenReturn(emp);

        ResponseEntity<EmployeeDTO> response = employeeController.getEmployeeById("3");
        EmployeeDTO result = response.getBody();

        assertEquals(emp.getId(), result.getId());
        assertEquals(emp.getName(), result.getName());
    }


    @Test
    @DisplayName("GET /employees/{id} - Should throw EmployeeNotFoundException for invalid ID")
    void testGetEmployeeById_Negative_NotFound() {
        when(employeeService.getEmployeeById("10"))
                .thenThrow(new EmployeeServiceException(ErrorConstants.EMPLOYEE_NOT_FOUND));

        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeController.getEmployeeById("10"));
        assertEquals(ErrorConstants.EMPLOYEE_NOT_FOUND, exception.getMessage());
    }

    @Test
    @DisplayName("GET /employees/{id} - should throw 429 Too Many Requests")
    void testGetEmployeeById_TooManyRequests() {
        when(employeeService.getEmployeeById("3"))
                .thenThrow(new TooManyRequestsException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> employeeController.getEmployeeById("3"));
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, exception.getMessage());
    }



    // ------------------- getEmployeesByNameSearch -------------------
    @Test
    @DisplayName("GET /employees/search?name= - Should return employees matching search string")
    void testGetEmployeesByNameSearch_Positive() {
        EmployeeDTO emp = EmployeeDTO.builder().id("4").name("Vikram Patel").salary(65000).build();
        when(employeeService.getEmployeesByNameSearch("Vikram")).thenReturn(List.of(emp));

        ResponseEntity<List<EmployeeDTO>> response = employeeController.getEmployeesByNameSearch("Vikram");
        List<EmployeeDTO> result = response.getBody();

        assertEquals(1, result.size());
        assertEquals(emp.getName(), result.get(0).getName());
    }

    @Test
    @DisplayName("GET /employees/search?name= - Should return empty list when no match found")
    void testGetEmployeesByNameSearch_Negative_NoMatch() {
        when(employeeService.getEmployeesByNameSearch("Unknown")).thenReturn(List.of());

        ResponseEntity<List<EmployeeDTO>> response = employeeController.getEmployeesByNameSearch("Unknown");
        assertTrue(response.getBody().isEmpty());
    }



    @Test
    @DisplayName("GET /employees/search?name= - Should throw 429 Too Many Requests")
    void testGetEmployeesByNameSearch_TooManyRequests() {
        when(employeeService.getEmployeesByNameSearch("Vikram"))
                .thenThrow(new TooManyRequestsException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> employeeController.getEmployeesByNameSearch("Vikram"));
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, exception.getMessage());
    }



    // ------------------- getHighestSalaryOfEmployees -------------------
    @Test
    @DisplayName("GET /employees/highest-salary - Should return highest salary among employees")
    void testGetHighestSalaryOfEmployees_Positive() {
        when(employeeService.getHighestSalaryOfEmployees()).thenReturn(120000);

        ResponseEntity<Integer> response = employeeController.getHighestSalaryOfEmployees();
        assertEquals(120000, response.getBody());
    }

    @Test
    @DisplayName("GET /employees/highest-salary - Should return 0 when no employees exist")
    void testGetHighestSalaryOfEmployees_Negative_NoEmployees() {
        when(employeeService.getHighestSalaryOfEmployees()).thenReturn(0);

        ResponseEntity<Integer> response = employeeController.getHighestSalaryOfEmployees();
        assertEquals(0, response.getBody());
    }

    @Test
    @DisplayName("GET /employees/highest-salary - Should throw 429 Too Many Requests")
    void testGetHighestSalaryOfEmployees_TooManyRequests() {
        when(employeeService.getHighestSalaryOfEmployees())
                .thenThrow(new TooManyRequestsException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> employeeController.getHighestSalaryOfEmployees());
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, exception.getMessage());
    }

    // getTopTenHighestEarningEmployeeNames
    @Test
    @DisplayName("GET /employees/top-ten - Should return top ten highest earning employee names")
    void testGetTopTenHighestEarningEmployeeNames_Positive() {
        List<String> topEarners = Arrays.asList("Rahul Sharma", "Priya Singh", "Vikram Patel");
        when(employeeService.getTopTenHighestEarningEmployeeNames()).thenReturn(topEarners);

        ResponseEntity<List<String>> response = employeeController.getTopTenHighestEarningEmployeeNames();
        assertIterableEquals(topEarners, response.getBody());
    }

    @Test
    @DisplayName("GET /employees/top-ten - Should return empty list when no employees exist")
    void testGetTopTenHighestEarningEmployeeNames_Negative_EmptyList() {
        when(employeeService.getTopTenHighestEarningEmployeeNames()).thenReturn(List.of());

        ResponseEntity<List<String>> response = employeeController.getTopTenHighestEarningEmployeeNames();
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("GET /employees/top-ten - Should throw 429 Too Many Requests")
    void testGetTopTenHighestEarningEmployeeNames_TooManyRequests() {
        when(employeeService.getTopTenHighestEarningEmployeeNames())
                .thenThrow(new TooManyRequestsException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> employeeController.getTopTenHighestEarningEmployeeNames());
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, exception.getMessage());
    }

    // createEmployee
    @Test
    @DisplayName("POST /employees - Should create a new employee successfully")
    void testCreateEmployee_Positive() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("Sanya Kapoor").salary(70000).build();
        EmployeeDTO createdEmp = EmployeeDTO.builder()
                .id("5").name(request.getName()).salary(request.getSalary()).build();

        when(employeeService.createEmployee(request)).thenReturn(createdEmp);

        ResponseEntity<EmployeeDTO> response = employeeController.createEmployee(request);
        EmployeeDTO result = response.getBody();

        assertEquals(createdEmp.getName(), result.getName());
        assertEquals(createdEmp.getSalary(), result.getSalary());
    }

    @Test
    @DisplayName("POST /employees - Should throw 429 Too Many Requests when rate limit exceeded")
    void testCreateEmployee_Negative_TooManyRequests() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .name("Vikram Patel").salary(80000).build();

        when(employeeService.createEmployee(request))
                .thenThrow(new TooManyRequestsException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> employeeController.createEmployee(request));
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, exception.getMessage());
    }

    // deleteEmployeeById
    @Test
    @DisplayName("DELETE /employees/{id} - Should delete employee successfully")
    void testDeleteEmployeeById_Positive() {
        String deletedName = "Arjun Reddy";

        when(employeeService.deleteEmployeeById("6")).thenReturn(deletedName);

        ResponseEntity<String> response = employeeController.deleteEmployeeById("6");
        assertEquals(deletedName, response.getBody());
    }

    @Test
    @DisplayName("DELETE /employees/{id} - Should throw EmployeeNotFoundException for invalid ID")
    void testDeleteEmployeeById_Negative_NotFound() {
        when(employeeService.deleteEmployeeById("15"))
                .thenThrow(new EmployeeServiceException(ErrorConstants.EMPLOYEE_NOT_FOUND));

        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeController.deleteEmployeeById("15"));
        assertEquals(ErrorConstants.EMPLOYEE_NOT_FOUND, exception.getMessage());
    }

    @Test
    @DisplayName("DELETE /employees/{id} - Should throw 429 Too Many Requests")
    void testDeleteEmployeeById_TooManyRequests() {
        when(employeeService.deleteEmployeeById("6"))
                .thenThrow(new TooManyRequestsException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE));

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> employeeController.deleteEmployeeById("6"));
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, exception.getMessage());
    }
}
