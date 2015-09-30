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

import javax.annotation.PostConstruct
import javax.inject.{Inject, Named}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.filter.SystemModelInterrogator
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.docs.repose.atom_feed_service.v1.AtomFeedServiceConfigType
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService}
import org.springframework.beans.factory.annotation.Value

@Named
class AtomFeedServiceImpl @Inject()(@Value(ReposeSpringProperties.NODE.CLUSTER_ID) clusterId: String,
                                    configurationService: ConfigurationService)
  extends AtomFeedService with LazyLogging {

  private final val SERVICE_NAME = "atom-feed-service"
  private final val DEFAULT_CONFIG = SERVICE_NAME + ".cfg.xml"
  private final val SYSTEM_MODEL_CONFIG = "system-model.cfg.xml"

  private var serviceConfig: AtomFeedServiceConfigType = _
  private var isServiceEnabled: Boolean = false

  @PostConstruct
  def init(): Unit = {
    logger.trace("Initializing and registering configuration listeners")
    val xsdURL = getClass.getResource("/META-INF/schema/config/atom-feed-service.xsd")

    configurationService.subscribeTo(
      DEFAULT_CONFIG,
      xsdURL,
      AtomFeedServiceConfigurationListener,
      classOf[AtomFeedServiceConfigType]
    )
    configurationService.subscribeTo(
      SYSTEM_MODEL_CONFIG,
      SystemModelConfigurationListener,
      classOf[SystemModel]
    )
  }

  override def registerListener(feedId: String, listener: AtomFeedListener): String = ???

  override def unregisterListener(listenerId: String): Unit = ???

  private def startService(): Unit = {
    if (isInitialized && isServiceEnabled) {
      logger.trace("Starting the service")
      ???
    } else if (!isServiceEnabled) {
      // TODO: Configuration has been read, but the service is not enabled
      ???
    } else {
      // TODO: Configuration has not been read
      ???
    }
  }

  private def stopService(): Unit = {
    logger.trace("Stopping the service")
    ???
  }

  def isInitialized: Boolean = {
    SystemModelConfigurationListener.isInitialized && AtomFeedServiceConfigurationListener.isInitialized
  }

  object AtomFeedServiceConfigurationListener extends UpdateListener[AtomFeedServiceConfigType] {
    private var initialized = false

    override def configurationUpdated(configurationObject: AtomFeedServiceConfigType): Unit = {
      initialized = true
      serviceConfig = configurationObject

      startService() // Try to start the service in case the system model was loaded first
    }

    override def isInitialized: Boolean = initialized
  }

  object SystemModelConfigurationListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      initialized = true

      val systemModelInterrogator = new SystemModelInterrogator(clusterId, null)
      isServiceEnabled = systemModelInterrogator.getServiceForCluster(configurationObject, SERVICE_NAME).isPresent

      if (isServiceEnabled) {
        startService()
      } else {
        stopService()
      }
    }

    override def isInitialized: Boolean = initialized
  }

}
