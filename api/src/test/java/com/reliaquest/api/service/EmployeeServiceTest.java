package com.reliaquest.api.service;

import com.reliaquest.api.client.EmployeeApiClient;
import com.reliaquest.api.config.FeignErrorDecoder;
import com.reliaquest.api.constants.ErrorConstants;
import com.reliaquest.api.dto.EmployeeDTO;
import com.reliaquest.api.dto.external.ExternalApiResponse;
import com.reliaquest.api.dto.external.ExternalEmployeeDTO;
import com.reliaquest.api.dto.request.CreateEmployeeRequest;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.TooManyRequestsException;
import com.reliaquest.api.service.impl.EmployeeServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeApiClient employeeApiClient;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private ExternalEmployeeDTO createExternalEmployeeDTO(String id, String name, Integer salary, Integer age, String title, String email) {
        return ExternalEmployeeDTO.builder()
                .id(id)
                .employeeName(name)
                .employeeSalary(salary)
                .employeeAge(age)
                .employeeTitle(title)
                .employeeEmail(email)
                .build();
    }

    private CreateEmployeeRequest createEmployeeRequest(String name, Integer salary, Integer age, String title) {
        return CreateEmployeeRequest.builder()
                .name(name)
                .salary(salary)
                .age(age)
                .title(title)
                .build();
    }

    private Request createMockRequest() {
        return Request.create(Request.HttpMethod.GET, "http://localhost:8112/api/v1/employee",
                new HashMap<>(), null, StandardCharsets.UTF_8, new RequestTemplate());
    }

    // GetAllEmployees() Tests

    @Test
    @DisplayName("getAllEmployees: Success scenario with valid employee data")
    void getAllEmployees_Success() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Arjun Sharma", 75000, 29, "Software Engineer", "arjun.sharma@google.com"),
                createExternalEmployeeDTO("2", "Priya Nair", 85000, 32, "Senior Developer", "priya.nair@microsoft.com")

        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        // Mock
        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<EmployeeDTO> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result, "Employee list should not be null");
        assertEquals(2, result.size(), "Should return exactly 2 employees");
        assertEquals("Arjun Sharma", result.get(0).getName(), "First employee name should match");
        assertEquals("Priya Nair", result.get(1).getName(), "Second employee name should match");
        assertEquals(Integer.valueOf(75000), result.get(0).getSalary(), "First employee salary should match");
        assertEquals(Integer.valueOf(85000), result.get(1).getSalary(), "Second employee salary should match");


        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getAllEmployees: Null API response returns empty list")
    void getAllEmployees_NullResponse_ReturnsEmptyList() {


        when(employeeApiClient.getAllEmployees()).thenReturn(null);
        //act
        List<EmployeeDTO> result = employeeService.getAllEmployees();

        // assert
        assertNotNull(result, "Result should never be null, even when API returns null");
        assertTrue(result.isEmpty(), "Should return empty list when API response is null");
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getAllEmployees: Empty data list returns empty result")
    void getAllEmployees_EmptyDataList_ReturnsEmptyList() {
        // arrange
        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(Collections.emptyList())
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<EmployeeDTO> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getAllEmployees: Too many requests throws TooManyRequestsException")
    void getAllEmployees_TooManyRequests_ThrowsException() {
        // Arrange
        Response response = Response.builder()
                .status(429)
                .reason("Too Many Requests")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "/employees",
                        Collections.emptyMap(),
                        null,
                        Charset.defaultCharset(),
                        null
                ))
                .build();

        // Decodding with FeignErrorDecoder
        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception decodedException = decoder.decode("EmployeeApiClient#getAllEmployees", response);

        // Mock
        when(employeeApiClient.getAllEmployees()).thenThrow(decodedException);

        // Act and assert
        TooManyRequestsException thrown = assertThrows(TooManyRequestsException.class,
                () -> employeeService.getAllEmployees());

        assertEquals(ErrorConstants.TOO_MANY_REQUESTS, thrown.getMessage());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }


    @Test
    @DisplayName("getAllEmployees: Server error throws EmployeeServiceException")
    void getAllEmployees_ServerError_ThrowsException() {
        // Arrange
        Response response = Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "/employees",
                        Collections.emptyMap(),
                        null,
                        Charset.defaultCharset(),
                        null
                ))
                .build();


        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception decodedException = decoder.decode("EmployeeApiClient#getAllEmployees", response);


        when(employeeApiClient.getAllEmployees()).thenThrow(decodedException);


        EmployeeServiceException thrown = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getAllEmployees());

        //assert
        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, thrown.getMessage());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }


    // GetEmployeesByNameSearch() Tests

    @Test
    @DisplayName("getEmployeesByNameSearch: Success with matching employees")
    void getEmployeesByNameSearch_Success() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Rajesh Kumar", 65000, 30, "Backend Developer", "rajesh.kumar@amazon.com"),
                createExternalEmployeeDTO("2", "Sneha Patel", 70000, 28, "Frontend Developer", "sneha.patel@paypal.com"),
                createExternalEmployeeDTO("3", "Rajesh Gupta", 55000, 32, "Business Analyst", "rajesh.gupta@google.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<EmployeeDTO> result = employeeService.getEmployeesByNameSearch("Rajesh");

        // Assert
        assertNotNull(result, "Search result should not be null");
        assertEquals(2, result.size(), "Should find exactly 2 employees named Rajesh");
        assertTrue(result.stream().allMatch(emp -> emp.getName().contains("Rajesh")),
                "All returned employees should have 'Rajesh' in their name");
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getEmployeesByNameSearch: No matching employees returns empty list")
    void getEmployeesByNameSearch_NoMatches_ReturnsEmptyList() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Anita Desai", 68000, 30, "UI Designer", "anita.desai@microsoft.com"),
                createExternalEmployeeDTO("2", "Vikram Singh", 72000, 28, "DevOps Engineer", "vikram.singh@amazon.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // act
        List<EmployeeDTO> result = employeeService.getEmployeesByNameSearch("Ramesh");

        // Assert
        assertNotNull(result, "Result should not be null even when no matches found");
        assertTrue(result.isEmpty(), "Should return empty list when no employees match search criteria");
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getEmployeesByNameSearch: Null search string throws exception")
    void getEmployeesByNameSearch_NullSearchString_ThrowsException() {
        // act & assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getEmployeesByNameSearch(null),
                "Should throw exception when search string is null");
        assertEquals(ErrorConstants.INVALID_SEARCH_STRING, exception.getMessage());


        verify(employeeApiClient, never()).getAllEmployees();
    }

    @Test
    @DisplayName("getEmployeesByNameSearch: Blank search string throws exception")
    void getEmployeesByNameSearch_BlankSearchString_ThrowsException() {
        // Act & Assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getEmployeesByNameSearch("   "));
        assertEquals(ErrorConstants.INVALID_SEARCH_STRING, exception.getMessage());
        verify(employeeApiClient, never()).getAllEmployees();
    }

    // getEmployeeById() Tests
    @Test
    @DisplayName("getEmployeeById: Success with valid employee ID")
    void getEmployeeById_Success() {
        // Arrange
        ExternalEmployeeDTO externalEmployee = createExternalEmployeeDTO(
                "1", "Rahul Sharma", 50000, 30, "Developer", "rahul.sharma@google.com"
        );
        ExternalApiResponse<ExternalEmployeeDTO> apiResponse = ExternalApiResponse.<ExternalEmployeeDTO>builder()
                .data(externalEmployee)
                .status("success")
                .build();

        when(employeeApiClient.getEmployeeById("1")).thenReturn(apiResponse);

        // act
        EmployeeDTO result = employeeService.getEmployeeById("1");

        // asert
        assertNotNull(result);
        assertEquals(externalEmployee.getId(), result.getId());
        assertEquals(externalEmployee.getEmployeeName(), result.getName());
        assertEquals(Integer.valueOf(50000), result.getSalary());
        verify(employeeApiClient, times(1)).getEmployeeById("1");
    }

    @Test
    @DisplayName("getEmployeeById: Null ID throws exception")
    void getEmployeeById_NullId_ThrowsException() {
        // act & assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getEmployeeById(null));
        assertEquals(ErrorConstants.INVALID_EMPLOYEE_ID, exception.getMessage());
        verify(employeeApiClient, never()).getEmployeeById(anyString());
    }

    @Test
    @DisplayName("getEmployeeById: Blank ID throws exception")
    void getEmployeeById_BlankId_ThrowsException() {
        // Act and assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getEmployeeById("   "));
        assertEquals(ErrorConstants.INVALID_EMPLOYEE_ID, exception.getMessage());
        verify(employeeApiClient, never()).getEmployeeById(anyString());
    }

    @Test
    @DisplayName("getEmployeeById: Not found throws EmployeeNotFoundException")
    void getEmployeeById_NotFound_ThrowsException() {
        // Arrange
        when(employeeApiClient.getEmployeeById("999"))
                .thenThrow(new EmployeeNotFoundException(ErrorConstants.EMPLOYEE_NOT_FOUND));

        // Act
        EmployeeNotFoundException thrown = assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.getEmployeeById("999"));

        //assert
        assertEquals(ErrorConstants.EMPLOYEE_NOT_FOUND, thrown.getMessage());
        verify(employeeApiClient, times(1)).getEmployeeById("999");
    }


    //  createEmployee() Tests

    @Test
    @DisplayName("createEmployee: Success with valid employee data")
    void createEmployee_Success() {
        // Arrange
        CreateEmployeeRequest request = createEmployeeRequest("Amit Patel", 50000, 30, "Developer");
        ExternalEmployeeDTO externalEmployee = createExternalEmployeeDTO(
                "1", "Amit Patel", 50000, 30, "Developer", "amit.patel@microsoft.com"
        );
        ExternalApiResponse<ExternalEmployeeDTO> apiResponse = ExternalApiResponse.<ExternalEmployeeDTO>builder()
                .data(externalEmployee)
                .status("success")
                .build();

        when(employeeApiClient.createEmployee(request)).thenReturn(apiResponse);

        // Act
        EmployeeDTO result = employeeService.createEmployee(request);

        // Assert
        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("Amit Patel", result.getName());
        assertEquals(Integer.valueOf(50000), result.getSalary());
        verify(employeeApiClient, times(1)).createEmployee(request);
    }

    @Test
    @DisplayName("createEmployee: Too many requests throws TooManyRequestsException")
    void createEmployee_TooManyRequests_ThrowsException() {
        // Arrange
        CreateEmployeeRequest request = createEmployeeRequest("John Doe", 50000, 30, "Developer");
        when(employeeApiClient.createEmployee(request))
                .thenThrow(new TooManyRequestsException(ErrorConstants.TOO_MANY_REQUESTS));

        // Act & Assert
        TooManyRequestsException thrown = assertThrows(TooManyRequestsException.class,
                () -> employeeService.createEmployee(request));

        assertEquals(ErrorConstants.TOO_MANY_REQUESTS, thrown.getMessage());
        verify(employeeApiClient, times(1)).createEmployee(request);
    }


    @Test
    @DisplayName("createEmployee: Server error throws EmployeeServiceException")
    void createEmployee_ServerError_ThrowsException() {
        // Arrange
        CreateEmployeeRequest request = createEmployeeRequest("John Doe", 50000, 30, "Developer");

        Response response = Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(Request.create(
                        Request.HttpMethod.POST,
                        "/employees",
                        Collections.emptyMap(),
                        null,
                        Charset.defaultCharset(),
                        null
                ))
                .build();


        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception decodedException = decoder.decode("EmployeeApiClient#createEmployee", response);

        // Feign client throwing the decoded exception
        when(employeeApiClient.createEmployee(request)).thenThrow(decodedException);

        // Act & Assert
        EmployeeServiceException thrown = assertThrows(EmployeeServiceException.class,
                () -> employeeService.createEmployee(request));

        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, thrown.getMessage());
        verify(employeeApiClient, times(1)).createEmployee(request);
    }


    @Test
    @DisplayName("createEmployee: Bad request throws EmployeeServiceException")
    void createEmployee_BadRequest_ThrowsException() {
        // Arrange
        CreateEmployeeRequest request = createEmployeeRequest("", -100, 10, "");

        Response response = Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(Request.create(
                        Request.HttpMethod.POST,
                        "/employees",
                        Collections.emptyMap(),
                        null,
                        Charset.defaultCharset(),
                        null
                ))
                .build();

        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception decodedException = decoder.decode("EmployeeApiClient#createEmployee", response);

        when(employeeApiClient.createEmployee(request)).thenThrow(decodedException);

        // Act & Assert
        EmployeeServiceException thrown = assertThrows(EmployeeServiceException.class,
                () -> employeeService.createEmployee(request));

        assertEquals(ErrorConstants.INVALID_EMPLOYEE_ID, thrown.getMessage());
        verify(employeeApiClient, times(1)).createEmployee(request);
    }


    //  deleteEmployeeById() Tests

    @Test
    @DisplayName("deleteEmployeeById: Success with valid employee ID")
    void deleteEmployeeById_Success() {
        // Arrange
        ExternalEmployeeDTO externalEmployee = createExternalEmployeeDTO(
                "1", "Sanjay Kumar", 50000, 30, "Developer", "sanjay.kumar@amazon.com"
        );
        ExternalApiResponse<ExternalEmployeeDTO> apiResponse = ExternalApiResponse.<ExternalEmployeeDTO>builder()
                .data(externalEmployee)
                .status("success")
                .build();

        when(employeeApiClient.getEmployeeById("1")).thenReturn(apiResponse);
        doNothing().when(employeeApiClient).deleteEmployee("Sanjay Kumar");

        // Act
        String result = employeeService.deleteEmployeeById("1");

        // Assert
        assertEquals("Employee with ID 1 deleted successfully.", result);
        verify(employeeApiClient, times(1)).getEmployeeById("1");
        verify(employeeApiClient, times(1)).deleteEmployee("Sanjay Kumar");
    }

    @Test
    @DisplayName("deleteEmployeeById: Null ID throws exception")
    void deleteEmployeeById_NullId_ThrowsException() {
        // Act & Assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.deleteEmployeeById(null));
        assertEquals(ErrorConstants.INVALID_EMPLOYEE_ID, exception.getMessage());
        verify(employeeApiClient, never()).getEmployeeById(anyString());
        verify(employeeApiClient, never()).deleteEmployee(anyString());
    }

    @Test
    @DisplayName("deleteEmployeeById: Employee not found throws EmployeeNotFoundException")
    void deleteEmployeeById_EmployeeNotFound_ThrowsException() {
        // Arrange
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "/api/v1/employee/999",
                        Collections.emptyMap(),
                        null,
                        Charset.defaultCharset(),
                        null
                ))
                .build();

        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception decodedException = decoder.decode("EmployeeApiClient#getEmployeeById", response);

        when(employeeApiClient.getEmployeeById("999")).thenThrow(decodedException);

        // Act & Assert
        EmployeeNotFoundException thrown = assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.deleteEmployeeById("999"));

        assertEquals(ErrorConstants.EMPLOYEE_NOT_FOUND, thrown.getMessage());
        verify(employeeApiClient, times(1)).getEmployeeById("999");
        verify(employeeApiClient, never()).deleteEmployee(anyString());
    }


    @Test
    @DisplayName("deleteEmployeeById: Delete operation fails throws EmployeeNotFoundException")
    void deleteEmployeeById_DeleteFails_ThrowsException() {
        // Arrange
        ExternalEmployeeDTO externalEmployee = createExternalEmployeeDTO(
                "1", "Neha Singh", 50000, 30, "Developer", "neha.singh@paypal.com"
        );
        ExternalApiResponse<ExternalEmployeeDTO> apiResponse = ExternalApiResponse.<ExternalEmployeeDTO>builder()
                .data(externalEmployee)
                .status("success")
                .build();

        when(employeeApiClient.getEmployeeById("1")).thenReturn(apiResponse);

        // Throw the decoded exception instead of raw Feign exception
        doThrow(new EmployeeNotFoundException(ErrorConstants.EMPLOYEE_NOT_FOUND))
                .when(employeeApiClient).deleteEmployee("Neha Singh");

        // Act & Assert
        EmployeeNotFoundException thrown = assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.deleteEmployeeById("1"));

        assertEquals(ErrorConstants.EMPLOYEE_NOT_FOUND, thrown.getMessage());
        verify(employeeApiClient, times(1)).getEmployeeById("1");
        verify(employeeApiClient, times(1)).deleteEmployee("Neha Singh");
    }


    // GetHighestSalaryOfEmployees() Tests

    @Test
    @DisplayName("getHighestSalaryOfEmployees: Success with valid salary data")
    void getHighestSalaryOfEmployees_Success() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Ravi Gupta", 50000, 30, "Developer", "ravi.gupta@google.com"),
                createExternalEmployeeDTO("2", "Kavya Sharma", 80000, 28, "Senior Developer", "kavya.sharma@microsoft.com"),
                createExternalEmployeeDTO("3", "Arun Nair", 60000, 32, "Analyst", "arun.nair@amazon.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        Integer result = employeeService.getHighestSalaryOfEmployees();

        // Assert
        assertEquals(Integer.valueOf(80000), result);
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getHighestSalaryOfEmployees: No salary data throws exception")
    void getHighestSalaryOfEmployees_NoSalaryData_ThrowsException() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Manish Kumar", null, 30, "Developer", "manish.kumar@paypal.com"),
                createExternalEmployeeDTO("2", "Pooja Reddy", null, 28, "Senior Developer", "pooja.reddy@google.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act & Assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getHighestSalaryOfEmployees());
        assertEquals(ErrorConstants.EMPLOYEE_NO_DATA, exception.getMessage());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    // GetTopTenHighestEarningEmployeeNames() test

    @Test
    @DisplayName("getTopTenHighestEarningEmployeeNames: Success with valid employee data")
    void getTopTenHighestEarningEmployeeNames_Success() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Aditi Sharma", 90000, 30, "Senior Developer", "aditi.sharma@microsoft.com"),
                createExternalEmployeeDTO("2", "Rohit Patel", 80000, 28, "Developer", "rohit.patel@amazon.com"),
                createExternalEmployeeDTO("3", "Shreya Nair", 85000, 32, "Analyst", "shreya.nair@google.com"),
                createExternalEmployeeDTO("4", "Vikash Singh", 95000, 29, "Tech Lead", "vikash.singh@paypal.com"),
                createExternalEmployeeDTO("5", "Meera Gupta", 70000, 26, "Junior Developer", "meera.gupta@microsoft.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<String> result = employeeService.getTopTenHighestEarningEmployeeNames();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Vikash Singh", result.get(0)); // Highest salary: 95000
        assertEquals("Aditi Sharma", result.get(1)); // Second highest: 90000
        assertEquals("Shreya Nair", result.get(2)); // Third highest: 85000
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getTopTenHighestEarningEmployeeNames: More than 10 employees returns only top 10")
    void getTopTenHighestEarningEmployeeNames_MoreThan10Employees_ReturnsTop10() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Rajesh Khanna", 100000, 30, "CEO", "rajesh.khanna@google.com"),
                createExternalEmployeeDTO("2", "Priya Mehta", 95000, 29, "CTO", "priya.mehta@microsoft.com"),
                createExternalEmployeeDTO("3", "Arjun Reddy", 90000, 28, "VP", "arjun.reddy@amazon.com"),
                createExternalEmployeeDTO("4", "Sneha Agarwal", 85000, 27, "Director", "sneha.agarwal@paypal.com"),
                createExternalEmployeeDTO("5", "Kiran Kumar", 80000, 26, "Manager", "kiran.kumar@google.com"),
                createExternalEmployeeDTO("6", "Deepika Singh", 75000, 25, "Senior Dev", "deepika.singh@microsoft.com"),
                createExternalEmployeeDTO("7", "Varun Sharma", 70000, 24, "Developer", "varun.sharma@amazon.com"),
                createExternalEmployeeDTO("8", "Anita Nair", 65000, 23, "Junior Dev", "anita.nair@paypal.com"),
                createExternalEmployeeDTO("9", "Rohit Gupta", 60000, 22, "Intern", "rohit.gupta@google.com"),
                createExternalEmployeeDTO("10", "Kavita Patel", 55000, 21, "Trainee", "kavita.patel@microsoft.com"),
                createExternalEmployeeDTO("11", "Suresh Yadav", 50000, 20, "Assistant", "suresh.yadav@amazon.com"),
                createExternalEmployeeDTO("12", "Pooja Jain", 45000, 19, "Helper", "pooja.jain@paypal.com"),
                createExternalEmployeeDTO("13", "Manoj Tiwari", 40000, 18, "Support", "manoj.tiwari@google.com"),
                createExternalEmployeeDTO("14", "Ritu Verma", 35000, 17, "Clerk", "ritu.verma@microsoft.com"),
                createExternalEmployeeDTO("15", "Sanjay Das", 30000, 16, "Office Boy", "sanjay.das@amazon.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<String> result = employeeService.getTopTenHighestEarningEmployeeNames();

        // Assert
        assertNotNull(result);
        assertEquals(10, result.size());
        assertEquals("Rajesh Khanna", result.get(0));
        assertEquals("Kavita Patel", result.get(9));
        assertFalse(result.contains("Suresh Yadav"));
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getTopTenHighestEarningEmployeeNames: Employees with null salaries are filtered out")
    void getTopTenHighestEarningEmployeeNames_NullSalaries_FilteredOut() {
        // arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Lakshmi Iyer", 90000, 30, "Senior Developer", "lakshmi.iyer@google.com"),
                createExternalEmployeeDTO("2", "Ravi Malhotra", null, 28, "Developer", "ravi.malhotra@microsoft.com"),
                createExternalEmployeeDTO("3", "Divya Krishnan", 85000, 32, "Analyst", "divya.krishnan@amazon.com"),
                createExternalEmployeeDTO("4", "Ashok Bansal", null, 29, "Tech Lead", "ashok.bansal@paypal.com"),
                createExternalEmployeeDTO("5", "Sunita Rao", 70000, 26, "Junior Developer", "sunita.rao@google.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<String> result = employeeService.getTopTenHighestEarningEmployeeNames();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Lakshmi Iyer", result.get(0));
        assertEquals("Divya Krishnan", result.get(1));
        assertEquals("Sunita Rao", result.get(2));
        assertFalse(result.contains("Ravi Malhotra"));
        assertFalse(result.contains("Ashok Bansal"));
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getTopTenHighestEarningEmployeeNames: Empty employee list throws exception")
    void getTopTenHighestEarningEmployeeNames_EmptyList_ThrowsException() {
        // Arrange
        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(Collections.emptyList())
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act & Assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getTopTenHighestEarningEmployeeNames());
        assertEquals(ErrorConstants.EMPLOYEE_NO_DATA, exception.getMessage());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getAllEmployees: Response with null data field returns empty list")
    void getAllEmployees_NullDataField_ReturnsEmptyList() {
        // Arrange
        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(null)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<EmployeeDTO> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getAllEmployees: Response with null employees in list filters them out")
    void getAllEmployees_NullEmployeesInList_FiltersNulls() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "Vishal Agrawal", 50000, 30, "Developer", "vishal.agrawal@paypal.com"),
                null,
                createExternalEmployeeDTO("2", "Swati Joshi", 60000, 28, "Senior Developer", "swati.joshi@google.com"),
                null
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<EmployeeDTO> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Vishal Agrawal", result.get(0).getName());
        assertEquals("Swati Joshi", result.get(1).getName());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getEmployeesByNameSearch: Case insensitive search works correctly")
    void getEmployeesByNameSearch_CaseInsensitive_Success() {
        // Arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "RAJESH KUMAR", 50000, 30, "Developer", "rajesh.kumar@microsoft.com"),
                createExternalEmployeeDTO("2", "priya sharma", 60000, 28, "Senior Developer", "priya.sharma@amazon.com"),
                createExternalEmployeeDTO("3", "Arjun Nair", 55000, 32, "Analyst", "arjun.nair@paypal.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        List<EmployeeDTO> result = employeeService.getEmployeesByNameSearch("raj");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(emp -> emp.getName().equals("RAJESH KUMAR")));
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getEmployeesByNameSearch: Empty string throws exception")
    void getEmployeesByNameSearch_EmptyString_ThrowsException() {
        // Act & Assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getEmployeesByNameSearch(""));
        assertEquals(ErrorConstants.INVALID_SEARCH_STRING, exception.getMessage());
        verify(employeeApiClient, never()).getAllEmployees();
    }

    @Test
    @DisplayName("getEmployeeById: Empty string ID throws exception")
    void getEmployeeById_EmptyStringId_ThrowsException() {
        // Act & Assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getEmployeeById(""));
        assertEquals(ErrorConstants.INVALID_EMPLOYEE_ID, exception.getMessage());
        verify(employeeApiClient, never()).getEmployeeById(anyString());
    }

    @Test
    @DisplayName("deleteEmployeeById: Empty string ID throws exception")
    void deleteEmployeeById_EmptyStringId_ThrowsException() {
        // Act & Assert
        EmployeeServiceException exception = assertThrows(EmployeeServiceException.class,
                () -> employeeService.deleteEmployeeById(""));
        assertEquals(ErrorConstants.INVALID_EMPLOYEE_ID, exception.getMessage());
        verify(employeeApiClient, never()).getEmployeeById(anyString());
        verify(employeeApiClient, never()).deleteEmployee(anyString());
    }

    @Test
    @DisplayName("getHighestSalaryOfEmployees: Single employee with salary returns that salary")
    void getHighestSalaryOfEmployees_SingleEmployee_ReturnsCorrectSalary() {
        // Arange
        List<ExternalEmployeeDTO> externalEmployees = Collections.singletonList(
                createExternalEmployeeDTO("1", "John Doe", 75000, 30, "Developer", "john@example.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // Act
        Integer result = employeeService.getHighestSalaryOfEmployees();

        // Assert
        assertEquals(Integer.valueOf(75000), result);
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    @Test
    @DisplayName("getHighestSalaryOfEmployees: Mixed null and valid salaries returns highest valid")
    void getHighestSalaryOfEmployees_MixedNullAndValidSalaries_ReturnsHighestValid() {
        // arrange
        List<ExternalEmployeeDTO> externalEmployees = Arrays.asList(
                createExternalEmployeeDTO("1", "John Doe", null, 30, "Developer", "john@example.com"),
                createExternalEmployeeDTO("2", "Jane Smith", 80000, 28, "Senior Developer", "jane@example.com"),
                createExternalEmployeeDTO("3", "Bob Wilson", null, 32, "Analyst", "bob@example.com"),
                createExternalEmployeeDTO("4", "Alice Brown", 60000, 29, "Designer", "alice@example.com")
        );

        ExternalApiResponse<List<ExternalEmployeeDTO>> apiResponse = ExternalApiResponse.<List<ExternalEmployeeDTO>>builder()
                .data(externalEmployees)
                .status("success")
                .build();

        when(employeeApiClient.getAllEmployees()).thenReturn(apiResponse);

        // act
        Integer result = employeeService.getHighestSalaryOfEmployees();

        // Assert
        assertEquals(Integer.valueOf(80000), result);
        verify(employeeApiClient, times(1)).getAllEmployees();
    }

    //  Feign Exception Tests

    @Test
    @DisplayName("getAllEmployees: Gateway timeout throws EmployeeServiceException")
    void getAllEmployees_GatewayTimeout_ThrowsException() {
        // Arrange
        Response response = Response.builder()
                .status(504)
                .reason("Gateway Timeout")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "/employees",
                        Collections.emptyMap(),
                        null,
                        Charset.defaultCharset(),
                        null
                ))
                .build();


        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception decodedException = decoder.decode("EmployeeApiClient#getAllEmployees", response);


        when(employeeApiClient.getAllEmployees()).thenThrow(decodedException);

        //  Assert
        EmployeeServiceException thrown = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getAllEmployees());

        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, thrown.getMessage());
        verify(employeeApiClient, times(1)).getAllEmployees();
    }


    @Test
    @DisplayName("createEmployee: Null response data throws EmployeeServiceException")
    void createEmployee_NullResponseData_ThrowsException() {
        // Arrange
        CreateEmployeeRequest request = createEmployeeRequest("John Doe", 50000, 30, "Developer");
        ExternalApiResponse<ExternalEmployeeDTO> apiResponse = ExternalApiResponse.<ExternalEmployeeDTO>builder()
                .data(null)
                .status("success")
                .build();

        when(employeeApiClient.createEmployee(request)).thenReturn(apiResponse);

        // Act & assert
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> employeeService.createEmployee(request));
        verify(employeeApiClient, times(1)).createEmployee(request);
    }

    @Test
    @DisplayName("getEmployeeById: Service unavailable throws EmployeeServiceException")
    void getEmployeeById_ServiceUnavailable_ThrowsException() {
        // Arrange
        Response response = Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(Request.create(
                        Request.HttpMethod.GET,
                        "/employees/1",
                        Collections.emptyMap(),
                        null,
                        Charset.defaultCharset(),
                        null
                ))
                .build();

        //  FeignErrorDecoder mapping
        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception ex = decoder.decode("EmployeeApiClient#getEmployeeById", response);

        // Mock client
        when(employeeApiClient.getEmployeeById("1")).thenThrow(ex);

        // Act & Assert
        EmployeeServiceException thrown = assertThrows(EmployeeServiceException.class,
                () -> employeeService.getEmployeeById("1"));

        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, thrown.getMessage());
        verify(employeeApiClient, times(1)).getEmployeeById("1");
    }


    @Test
    @DisplayName("deleteEmployee: Bad gateway throws EmployeeServiceException")
    void deleteEmployee_BadGateway_ThrowsException() {
        // Arrange
        ExternalEmployeeDTO externalEmployee = createExternalEmployeeDTO(
                "1", "John Doe", 50000, 30, "Developer", "john@example.com"
        );
        ExternalApiResponse<ExternalEmployeeDTO> apiResponse = ExternalApiResponse.<ExternalEmployeeDTO>builder()
                .data(externalEmployee)
                .status("success")
                .build();

        when(employeeApiClient.getEmployeeById("1")).thenReturn(apiResponse);

        // fake Feign Response with 502
        Response response = Response.builder()
                .status(502)
                .reason("Bad Gateway")
                .request(Request.create(Request.HttpMethod.DELETE, "/employees/John Doe",
                        Collections.emptyMap(), null, Charset.defaultCharset(), null))
                .build();

        FeignErrorDecoder decoder = new FeignErrorDecoder();
        Exception ex = decoder.decode("EmployeeApiClient#deleteEmployee", response);

        doThrow(ex).when(employeeApiClient).deleteEmployee("John Doe");

        // Act & Assert
        EmployeeServiceException thrown = assertThrows(EmployeeServiceException.class,
                () -> employeeService.deleteEmployeeById("1"));

        assertEquals(ErrorConstants.EMPLOYEE_API_UNAVAILABLE, thrown.getMessage());
        verify(employeeApiClient, times(1)).getEmployeeById("1");
        verify(employeeApiClient, times(1)).deleteEmployee("John Doe");
    }

}
