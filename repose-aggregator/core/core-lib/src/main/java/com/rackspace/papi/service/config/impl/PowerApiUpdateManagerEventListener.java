package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.commons.config.manager.InvalidConfigurationException;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import com.rackspace.papi.service.healthcheck.*;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author fran
 */
public class PowerApiUpdateManagerEventListener implements EventListener<ConfigurationEvent, ConfigurationResource> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(PowerApiUpdateManagerEventListener.class);

    private final Map<String, Map<Integer, ParserListenerPair>> listenerMap;
    private final HealthCheckService healthCheckService;

    private String healthCheckUID;

    public PowerApiUpdateManagerEventListener(Map<String, Map<Integer, ParserListenerPair>> listenerMap,
                                              HealthCheckService healthCheckService) {
        this.listenerMap = listenerMap;
        this.healthCheckService = healthCheckService;
        try {
            healthCheckUID = healthCheckService.register(this.getClass());
        } catch (InputNullException e) {
            LOG.error("Unable to register to health check service");
        }
    }

    @Override
    public void onEvent(Event<ConfigurationEvent, ConfigurationResource> e) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader previousClassLoader = currentThread.getContextClassLoader();
        final String payloadName = e.payload().name();
        Map<Integer, ParserListenerPair> listeners = getListenerMap(payloadName);

        LOG.info("Configuration event triggered for: " + payloadName);
        LOG.info("Notifying " + listeners.values().size() + " listeners");

        for (ParserListenerPair parserListener : listeners.values()) {
            UpdateListener updateListener = parserListener.getListener();

            if (updateListener != null) {
                LOG.info("Notifying " + updateListener.getClass().getName());

                currentThread.setContextClassLoader(parserListener.getClassLoader());
                try {
                    configUpdate(updateListener, parserListener.getParser().read(e.payload()));
                    if (parserListener.getFilterName() != null && !parserListener.getFilterName().isEmpty() && updateListener.isInitialized()) {
                        parserListener.getConfigurationInformation().setFilterLoadingInformation(parserListener.getFilterName(), updateListener.isInitialized(), e.payload());
                    } else {
                        parserListener.getConfigurationInformation().setFilterLoadingFailedInformation(parserListener.getFilterName(), e.payload(), "Failed loading File");
                    }
                } catch (Exception ex) {
                    if (parserListener.getFilterName() != null && !parserListener.getFilterName().isEmpty()) {
                        parserListener.getConfigurationInformation().setFilterLoadingFailedInformation(parserListener.getFilterName(), e.payload(), ex.getMessage());
                    }
                    LOG.error("Configuration update error. Reason: " + ex.getMessage(), ex);

                } finally {
                    currentThread.setContextClassLoader(previousClassLoader);
                }
            } else {
                LOG.warn("Update listener is null for " + payloadName);
            }
        }
    }

    public synchronized Map<Integer, ParserListenerPair> getListenerMap(String resourceName) {
        final Map<Integer, ParserListenerPair> mapReference = new HashMap<Integer, ParserListenerPair>(listenerMap.get(resourceName));

        return Collections.unmodifiableMap(mapReference);
    }

    private void configUpdate(UpdateListener upd, Object cfg) {
        final String configClass = cfg.getClass().getSimpleName();
        final String issueId = "InvalidConfig:" + configClass; //todo: make this a better id, unique per filter, if possible

        try {
            upd.configurationUpdated(cfg);

            // Solving an issue is idempotent, so it doesn't hurt to always attempt to solve. We don't want to log
            // when we aren't resolving an issue, though.
            LOG.debug("Configuration Updated: " + configClass);
            try {
                if (healthCheckService.getReports(healthCheckUID).containsKey(issueId)) {
                    try{
                        LOG.debug("Resolving issue with Health Checker Service: " + issueId);
                        healthCheckService.solveIssue(healthCheckUID, issueId);
                    } catch (InputNullException e) {
                        LOG.error("Unable to solve issue " + issueId + "from " + healthCheckUID);
                    } catch (NotRegisteredException e) {
                        LOG.error("Unable to solve issue " + issueId + "from " + healthCheckUID);
                    }
                }
            } catch (InputNullException ine) {
                // Do nothing
            } catch (NotRegisteredException nre) {
                // Do nothing
            }
        } catch (InvalidConfigurationException ice) {
            LOG.error("Invalid configuration content in " + configClass + ". Reason: " + ice.getMessage(), ice);
            LOG.debug("Reporting issue to Health Checker Service: " + issueId);
            try {
                healthCheckService.reportIssue(healthCheckUID, issueId,
                        new HealthCheckReport("Configuration Error on " + configClass, Severity.BROKEN));
            } catch (InputNullException ine) {
                LOG.error("Unable to report Issues to Health Check Service");
            } catch (NotRegisteredException nre) {
                LOG.error("Unable to report Issues to Health Check Service");
            }
        }
    }
}
