package com.rackspace.papi.commons.config.manager;

/*
    An exception used to indicate that the contents of a configuration file were not as expected
    even though the configuration file passed validation.
*/
public class InvalidConfigurationException extends Exception {
    public InvalidConfigurationException(String message) { super(message); }

    public InvalidConfigurationException(String message, Throwable cause) { super(message, cause); }
}
