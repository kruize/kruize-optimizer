/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.kruize.optimizer.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic API response wrapper for all REST endpoints.
 * <p>
 * Provides a consistent response structure across all APIs with:
 * <ul>
 *   <li>success: boolean indicating if the operation succeeded</li>
 *   <li>message: optional message for additional context</li>
 *   <li>data: the actual response payload (only included on success)</li>
 *   <li>error: error details (only included on failure)</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of the data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String error;

    /**
     * Private constructor to enforce use of factory methods.
     */
    private ApiResponse() {}

    /**
     * Creates a successful response with data.
     *
     * @param data the response data
     * @param <T> the type of data
     * @return ApiResponse with success=true and the provided data
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    /**
     * Creates a successful response with data and a message.
     *
     * @param data the response data
     * @param message success message
     * @param <T> the type of data
     * @return ApiResponse with success=true, data, and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = message;
        return response;
    }

    /**
     * Creates a successful response with only a message (no data).
     *
     * @param message success message
     * @param <T> the type of data
     * @return ApiResponse with success=true and message
     */
    public static <T> ApiResponse<T> successMessage(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        return response;
    }

    /**
     * Creates an error response with an error message.
     *
     * @param error the error message
     * @param <T> the type of data
     * @return ApiResponse with success=false and error message
     */
    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        return response;
    }

    /**
     * Creates an error response with an error message and additional context message.
     *
     * @param error the error message
     * @param message additional context
     * @param <T> the type of data
     * @return ApiResponse with success=false, error, and message
     */
    public static <T> ApiResponse<T> error(String error, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        response.message = message;
        return response;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

