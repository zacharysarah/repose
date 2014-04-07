package com.rackspace.papi.commons.config.manager;

public interface UpdateListener<T> {
    boolean isInitialized();

    void configurationUpdated(T configurationObject) throws InvalidConfigurationException;
}
