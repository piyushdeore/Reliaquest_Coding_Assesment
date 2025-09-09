    package com.reliaquest.api.constants;

public final class ErrorConstants {

        public static final String EMPLOYEE_FETCH_FAILED = "Failed to fetch the employees";
        public static final String EMPLOYEE_NOT_FOUND = "Employee not found";
        public static final String EMPLOYEE_DELETE_FAILED = "Failed to delete employee";
        public static final String EMPLOYEE_CREATION_FAILED = "Failed to create employee";
        public static final String EMPLOYEE_NO_DATA = "No employee data returned from API";
        public static final String EMPLOYEE_API_UNAVAILABLE = "Employee API is currently unavailable";
        public static final String INVALID_SEARCH_STRING = "Employee name search string is invalid";
        public static final String INVALID_EMPLOYEE_ID = "Invalid Employee Id";
        public static final String EMPLOYEE_NOT_FOUND_WITH_ID = "Employee not found with id: ";
        public static final String TOO_MANY_REQUESTS = "Too many requests – please try again later";

        private ErrorConstants() {
        }
}