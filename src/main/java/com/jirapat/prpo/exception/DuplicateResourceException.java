package com.jirapat.prpo.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final String fieldValue;

    public DuplicateResourceException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}
