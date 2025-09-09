package com.reliaquest.api.config;

import com.reliaquest.api.constants.ErrorConstants;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.EmployeeServiceException;
import com.reliaquest.api.exception.TooManyRequestsException;
import feign.Response;
import feign.codec.ErrorDecoder;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();



    // All the exception are handled by this Error decoder
    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());
        
        log.warn("Feign client error - Method: {}, Status: {}, Reason: {}", 
                methodKey, status, response.reason());

        switch (status) {
            case NOT_FOUND:
                if (methodKey.contains("getEmployeeById")) {
                    return new EmployeeNotFoundException(ErrorConstants.EMPLOYEE_NOT_FOUND);
                }
                return new EmployeeNotFoundException(ErrorConstants.EMPLOYEE_NOT_FOUND);
                
            case TOO_MANY_REQUESTS:
                return new TooManyRequestsException(ErrorConstants.TOO_MANY_REQUESTS);
                
            case BAD_REQUEST:
                return new EmployeeServiceException(ErrorConstants.INVALID_EMPLOYEE_ID);
                
            case INTERNAL_SERVER_ERROR:
            case BAD_GATEWAY:
            case SERVICE_UNAVAILABLE:
            case GATEWAY_TIMEOUT:
                return new EmployeeServiceException(ErrorConstants.EMPLOYEE_API_UNAVAILABLE);
                
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
