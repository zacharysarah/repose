package com.rackspace.papi.jmx.mbeans;

import com.rackspace.papi.model.DomainNode;
import com.rackspace.papi.model.Filter;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.model.ServiceDomain;

import java.util.ArrayList;
import java.util.List;

/**
 *  MBeans are needed for JMX instrumentation:
 *
 * "To manage resources using JMX technology, you must first instrument the resources in the Java programming language.
 *  You use Java objects known as MBeans to implement the access to the instrumentation of resources."
 *  source: http://docs.oracle.com/javase/6/docs/technotes/guides/jmx/overview/architecture.html#wp998959
 * 
 *  This is the implementation class of the SystemConfigurationMBean. An MBean is essentially a class and its
 *  interface which exposes some type of system instrumentation/management/monitoring.  This MBean allows a remote
 *  management system (like JConsole, VMWare, or another monitoring system) see what filters are registered
 *  for this instance of Repose.
 * 
 * @author fran
 */
public class SystemConfiguration implements SystemConfigurationMBean {

   private final DomainNode localHost;
   private final ServiceDomain domain;

   public SystemConfiguration(ServiceDomain domain, DomainNode localHost) {
      this.domain = domain;
      this.localHost = localHost;
   }

   @Override
   public List<String> getLoadedFilters() {
      List<String> loadedFilters = new ArrayList<String>();

      for (Filter filter : domain.getFilters().getFilter()) {
         loadedFilters.add(filter.getName());
      }       

      return loadedFilters;
   }

   @Override
   public PowerProxy updatePowerProxy(PowerProxy powerProxy) {
      System.out.print("In MBean " + powerProxy.toString());
      return powerProxy;
   }
}
