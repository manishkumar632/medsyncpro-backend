package com.medsyncpro.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Object errors;
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }
    
    public static <T> ApiResponse<T> error(String message, Object errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return error(message, null);
    }
}
