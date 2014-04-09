package com.rackspace.papi.filter

import com.rackspace.papi.commons.config.manager.InvalidConfigurationException
import com.rackspace.papi.domain.Port
import com.rackspace.papi.domain.ServicePorts
import com.rackspace.papi.model.*
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue

public class SystemModelInterrogatorTest {
    private SystemModelInterrogator interrogator

    @Before
    public void setup() throws Exception {
        ServicePorts servicePorts = new ServicePorts()
        servicePorts.add(new Port("http", 8080))

        interrogator = new SystemModelInterrogator(servicePorts)
    }

    @Test
    public void getLocalServiceDomain_returnsMatchingCluster() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        ReposeCluster returnedCluster = interrogator.getLocalServiceDomain(sysModel)

        assertTrue(returnedCluster.getId().equals("cluster1"))
        assertTrue(returnedCluster.getNodes().getNode().get(0).getId().equals("node1"))
        assertTrue(returnedCluster.getNodes().getNode().get(0).getHostname().equals("localhost"))
        assertTrue(returnedCluster.getNodes().getNode().get(0).getId().equals("node1"))
        assertTrue(returnedCluster.getNodes().getNode().get(0).getHttpPort() == 8080)
    }

    @Test(expected = InvalidConfigurationException.class)
    public void getLocalServiceDomain_throwsInvalidConfigurationExceptionIfMatchingClusterNotFound() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        interrogator.getLocalServiceDomain(sysModel)
    }

    @Test
    public void getLocalHost_returnsMatchingNode() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Node returnedNode = interrogator.getLocalHost(sysModel)

        assertTrue(returnedNode.getId().equals("node1"))
        assertTrue(returnedNode.getHostname().equals("localhost"))
        assertTrue(returnedNode.getId().equals("node1"))
        assertTrue(returnedNode.getHttpPort() == 8080)
    }

    @Test(expected = InvalidConfigurationException.class)
    public void getLocalHost_throwsInvalidConfigurationExceptionIfMatchingNodeNotFound() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        interrogator.getLocalHost(sysModel)
    }

    @Test
    public void getDefaultDestination_returnsMatchingDestination() throws Exception {
        SystemModel sysModel = getValidSystemModel()

        Destination returnedDest = interrogator.getDefaultDestination(sysModel)

        assertTrue(returnedDest.getId().equals("dest1"))
        assertTrue(returnedDest.getProtocol().equals("http"))
        assertTrue(returnedDest instanceof DestinationEndpoint)
    }

    @Test(expected = InvalidConfigurationException.class)
    public void getDefaultDestination_throwsInvalidConfigurationExceptionIfMatchingNodeNotFound() throws Exception {
        SystemModel sysModel = getValidSystemModel()
        sysModel.getReposeCluster().get(0).getNodes().getNode().get(0).setHostname("www.example.com")

        interrogator.getDefaultDestination(sysModel)
    }

    /**
     * @return a valid system model
     */
    private SystemModel getValidSystemModel() {
        Node node = new Node()
        DestinationEndpoint dest = new DestinationEndpoint()
        ReposeCluster cluster = new ReposeCluster()
        SystemModel sysModel = new SystemModel()

        node.setId("node1")
        node.setHostname("localhost")
        node.setHttpPort(8080)

        dest.setHostname("localhost")
        dest.setPort(9090)
        dest.setDefault(true)
        dest.setId("dest1")
        dest.setProtocol("http")

        cluster.setId("cluster1")
        cluster.setNodes(new NodeList())
        cluster.getNodes().getNode().add(node)
        cluster.setDestinations(new DestinationList())
        cluster.getDestinations().getEndpoint().add(dest)

        sysModel.getReposeCluster().add(cluster)

        return sysModel
    }
}
