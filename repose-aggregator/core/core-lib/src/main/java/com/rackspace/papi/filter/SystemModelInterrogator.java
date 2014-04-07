package com.rackspace.papi.filter;

import com.rackspace.papi.commons.config.manager.InvalidConfigurationException;
import com.rackspace.papi.commons.util.net.NetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.NetworkNameResolver;
import com.rackspace.papi.commons.util.net.StaticNetworkInterfaceProvider;
import com.rackspace.papi.commons.util.net.StaticNetworkNameResolver;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author franshua
 */
@Component("modelInterrogator")
public class SystemModelInterrogator {

    private static final Logger LOG = LoggerFactory.getLogger(SystemModelInterrogator.class);
    private final NetworkInterfaceProvider networkInterfaceProvider;
    private final NetworkNameResolver nameResolver;
    private final List<Port> ports;

    @Autowired
    public SystemModelInterrogator(@Qualifier("servicePorts") ServicePorts ports) {
        this(StaticNetworkNameResolver.getInstance(), StaticNetworkInterfaceProvider.getInstance(), ports);
    }

    public SystemModelInterrogator(NetworkNameResolver nameResolver, NetworkInterfaceProvider nip, ServicePorts ports) {
        this.nameResolver = nameResolver;
        this.networkInterfaceProvider = nip;
        this.ports = ports;
    }

    public ReposeCluster getLocalServiceDomain(SystemModel systemModel) throws InvalidConfigurationException {
        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            if (containsLocalNodeForPorts(cluster, ports)) {
                return cluster;
            }
        }

        throw new InvalidConfigurationException("Unable to identify the local host in the system model - please " +
                "check your system-model.cfg.xml");
    }

    public Node getLocalHost(SystemModel systemModel) throws InvalidConfigurationException {
        for (ReposeCluster cluster : systemModel.getReposeCluster()) {
            Node node = getLocalNodeForPorts(cluster, ports);

            if (node != null) {
                return node;
            }
        }

        throw new InvalidConfigurationException("Unable to identify the local host in the system model - please " +
                "check your system-model.cfg.xml");
    }

    public Destination getDefaultDestination(SystemModel systemModel) throws InvalidConfigurationException {
        ReposeCluster reposeCluster = getLocalServiceDomain(systemModel);

        return getDefaultDestination(reposeCluster);
    }

    private Destination getDefaultDestination(ReposeCluster reposeCluster) {
        if (reposeCluster == null) { throw new IllegalArgumentException("Domain cannot be null"); }

        List<Destination> destinations = new ArrayList<Destination>();

        destinations.addAll(reposeCluster.getDestinations().getEndpoint());
        destinations.addAll(reposeCluster.getDestinations().getTarget());

        for (Destination destination : destinations) {
            if (destination.isDefault()) {
                return destination;
            }
        }

        return null;
    }

    private boolean containsLocalNodeForPorts(ReposeCluster reposeCluster, List<Port> ports) {
        return getLocalNodeForPorts(reposeCluster, ports) != null;
    }

    private Node getLocalNodeForPorts(ReposeCluster reposeCluster, List<Port> ports) {
        if (reposeCluster == null) { throw new IllegalArgumentException("Domain cannot be null"); }

        if (ports.isEmpty()) {
            return null;
        }

        for (Node host : reposeCluster.getNodes().getNode()) {
            List<Port> hostPorts = getPortsList(host);

            if (hostPorts.equals(ports) && hasLocalInterface(host)) {
                return host;
            }
        }

        return null;
    }

    private boolean hasLocalInterface(Node node) {
        if (node == null) { throw new IllegalArgumentException("Node cannot be null"); }

        try {
            final InetAddress hostAddress = nameResolver.lookupName(node.getHostname());
            return networkInterfaceProvider.hasInterfaceFor(hostAddress);
        } catch (UnknownHostException uhe) {
            LOG.error("Unable to look up network host name. Reason: " + uhe.getMessage(), uhe);
        } catch (SocketException socketException) {
            LOG.error(socketException.getMessage(), socketException);
        }

        return false;
    }

    private List<Port> getPortsList(Node node) {
        if (node == null) { throw new IllegalArgumentException("Node cannot be null"); }

        List<Port> portList = new ArrayList<Port>();

        // TODO Model: use constants or enum for possible protocols
        if (node.getHttpPort() > 0) {
            portList.add(new Port("http", node.getHttpPort()));
        }

        if (node.getHttpsPort() > 0) {
            portList.add(new Port("https", node.getHttpsPort()));
        }

        return portList;
    }
}