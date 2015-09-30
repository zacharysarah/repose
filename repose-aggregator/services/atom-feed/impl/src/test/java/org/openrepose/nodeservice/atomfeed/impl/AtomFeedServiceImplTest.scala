/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.nodeservice.atomfeed.impl

import java.net.URL

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.mockito.Matchers.{eq => isEq, _}
import org.mockito.Mockito.verify
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.systemmodel.{ReposeCluster, Service, ServicesList, SystemModel}
import org.openrepose.docs.repose.atom_feed_service.v1.AtomFeedServiceConfigType
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

class AtomFeedServiceImplTest extends FunSpec with Matchers with MockitoSugar with BeforeAndAfter {

  val ctx = LogManager.getContext(false).asInstanceOf[LoggerContext]
  val serviceListAppender = ctx.getConfiguration.getAppender("serviceList").asInstanceOf[ListAppender]

  var mockConfigService: ConfigurationService = _

  before {
    mockConfigService = mock[ConfigurationService]
    serviceListAppender.clear()
  }

  describe("init") {
    it("should register configuration listeners") {
      val atomFeedService = new AtomFeedServiceImpl("", mockConfigService)

      atomFeedService.init()

      verify(mockConfigService).subscribeTo(
        isEq("system-model.cfg.xml"),
        any[UpdateListener[SystemModel]](),
        isA(classOf[Class[SystemModel]])
      )
      verify(mockConfigService).subscribeTo(
        isEq("atom-feed-service.cfg.xml"),
        any[URL](),
        isA(classOf[UpdateListener[AtomFeedServiceConfigType]]),
        isA(classOf[Class[AtomFeedServiceConfigType]])
      )
    }

    it("should not start the service if configuration files have not yet been read") {
      val systemModel = new SystemModel()
      val cluster = new ReposeCluster()
      val services = new ServicesList()
      val service = new Service()
      services.getService.add(service)
      cluster.setId("clusterId")
      cluster.setServices(services)
      systemModel.getReposeCluster.add(cluster)

      val atomFeedService = new AtomFeedServiceImpl("clusterId", mockConfigService)
      atomFeedService.init()

      val logEvents = serviceListAppender.getEvents
      logEvents.size() shouldEqual 1
      logEvents.get(0).getMessage.getFormattedMessage should include("Initializing")
    }
  }

  describe("configurationUpdated") {
    it("should not start the service if it is not listed in the system model for this node") {
      val systemModel = new SystemModel()
      val cluster = new ReposeCluster()
      val service = new Service()
      cluster.setId("clusterId")
      cluster.getServices.getService.add(service)
      systemModel.getReposeCluster.add(cluster)

      val atomFeedService = new AtomFeedServiceImpl("clusterId", mockConfigService)
      atomFeedService.init()
      atomFeedService.SystemModelConfigurationListener.configurationUpdated(systemModel)
      atomFeedService.AtomFeedServiceConfigurationListener.configurationUpdated(new AtomFeedServiceConfigType())

      val logEvents = serviceListAppender.getEvents
      logEvents.size() shouldEqual 1
      logEvents.get(0).getMessage.getFormattedMessage should include("Initializing")
    }

    it("should start the service if it is listed in the system model for this node, and a valid config is provided") {
      pending
    }

    it("should stop the service is it was, but is no longer, listed in the system model for this node") {
      pending
    }
  }

  describe("registerListener") {
    pending
  }

  describe("unregisterListener") {
    pending
  }
}
