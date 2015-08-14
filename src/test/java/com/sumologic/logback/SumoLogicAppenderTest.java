/**
 *    _____ _____ _____ _____    __    _____ _____ _____ _____
 *   |   __|  |  |     |     |  |  |  |     |   __|     |     |
 *   |__   |  |  | | | |  |  |  |  |__|  |  |  |  |-   -|   --|
 *   |_____|_____|_|_|_|_____|  |_____|_____|_____|_____|_____|
 *
 *                UNICORNS AT WARP SPEED SINCE 2010
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.logback;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import com.sumologic.logback.server.AggregatingHttpHandler;
import com.sumologic.logback.server.MockHttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertEquals;

public class SumoLogicAppenderTest {

    private static final int PORT = 10010;
    private static final String ENDPOINT_URL = "http://localhost:" + PORT;

    private MockHttpServer server;
    private AggregatingHttpHandler handler;
    private Logger loggerInTest;


    private void setUpLogger() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        SumoLogicAppender sla = new SumoLogicAppender();
        sla.setUrl(ENDPOINT_URL);
        // TODO: Shouldn't there be a default layout?
        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("-- %message%n");
        layout.start();
        sla.setLayout(layout);
        sla.start();

        loggerInTest = (Logger) LoggerFactory.getLogger("logger");
        loggerInTest.detachAndStopAllAppenders();
        loggerInTest.addAppender(sla);

    }


    @Before
    public void setUp() throws Exception {
        handler = new AggregatingHttpHandler();
        server = new MockHttpServer(PORT, handler);

        server.start();

        setUpLogger();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testSingleMessage() throws Exception {

        loggerInTest.info("This is a message");

        assertEquals(1, handler.getExchanges().size());
        assertEquals("-- This is a message\n", handler.getExchanges().get(0).getBody());
    }

    @Test
    public void testMultipleMessages() throws Exception {

        int numMessages = 20;
        for (int i = 0; i < numMessages / 2; i ++) {
            loggerInTest.info("info " + i);
            loggerInTest.error("error " + i);
        }

        assertEquals(numMessages, handler.getExchanges().size());
    }
}
