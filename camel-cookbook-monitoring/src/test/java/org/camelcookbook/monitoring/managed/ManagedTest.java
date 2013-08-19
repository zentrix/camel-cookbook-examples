/*
 * Copyright (C) Scott Cranton and Jakub Korab
 * https://github.com/CamelCookbook
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
 */

package org.camelcookbook.monitoring.managed;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.management.DefaultManagementNamingStrategy;
import org.apache.camel.spi.ManagementAgent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedTest extends CamelTestSupport {
    private static Logger LOG = LoggerFactory.getLogger(ManagedTest.class);

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new ManagedRouteBuilder();
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        enableJMX();

        CamelContext context = super.createCamelContext();

        DefaultManagementNamingStrategy naming = (DefaultManagementNamingStrategy) context.getManagementStrategy().getManagementNamingStrategy();
        naming.setHostName("localhost");
        naming.setDomainName("org.apache.camel");

        return context;
    }

    @Test
    public void testManagedResource() throws Exception {
        final ManagementAgent managementAgent = context.getManagementStrategy().getManagementAgent();
        assertNotNull(managementAgent);

        final MBeanServer mBeanServer = managementAgent.getMBeanServer();
        assertNotNull(mBeanServer);

        final String mBeanServerDefaultDomain = managementAgent.getMBeanServerDefaultDomain();
        assertEquals("org.apache.camel", mBeanServerDefaultDomain);

        final String managementName = context.getManagementName();
        LOG.info("managementName = {}", managementName);

        // Send a couple of messages to get some route statistics
        template.sendBody("direct:start", "Hello Camel");
        template.sendBody("direct:start", "Camel Rocks!");

        // Get myManagedBean
        ObjectName onManagedBean = ObjectName.getInstance(mBeanServerDefaultDomain + ":context=localhost/" + managementName + ",type=processors,name=\"myManagedBean\"");
        assertTrue("Should be registered", mBeanServer.isRegistered(onManagedBean));

        // Get MBean attribute
        int camelsSeenCount = (Integer) mBeanServer.getAttribute(onManagedBean, "CamelsSeenCount");
        assertEquals(2, camelsSeenCount);

        // Stop the route via JMX
        mBeanServer.invoke(onManagedBean, "resetCamelsSeenCount", null, null);

        camelsSeenCount = (Integer) mBeanServer.getAttribute(onManagedBean, "CamelsSeenCount");
        assertEquals(0, camelsSeenCount);
    }
}