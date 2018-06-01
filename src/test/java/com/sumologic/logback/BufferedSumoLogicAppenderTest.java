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
import com.sumologic.logback.server.MaterializedHttpRequest;
import com.sumologic.logback.server.MockHttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertEquals;

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class BufferedSumoLogicAppenderTest {

    private static final int PORT = 10010;
    private static final String ENDPOINT_URL = "http://localhost:" + PORT;

    private MockHttpServer server;
    private AggregatingHttpHandler handler;
    private Logger loggerInTest;
    private BufferedSumoLogicAppender appender;


    private void setUpLogger(BufferedSumoLogicAppender appender) {
        loggerInTest = (Logger) LoggerFactory.getLogger("BufferedSumoLogicAppenderTest");
        loggerInTest.detachAndStopAllAppenders();
        loggerInTest.addAppender(appender);
    }

    private void setUpLogger(int batchSize, int windowSize, int precision) {

        appender = new BufferedSumoLogicAppender();
        appender.setUrl(ENDPOINT_URL);
        appender.setMessagesPerRequest(batchSize);
        appender.setMaxFlushInterval(windowSize);
        appender.setFlushingAccuracy(precision);
        appender.setSourceCategory("TestCategory");
        appender.setSourceName("Test Application");
        appender.setSourceHost("10.128.10.1");

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%message%n");
        layout.start();

        appender.setLayout(layout);
        appender.start();

        setUpLogger(appender);
    }


    @Before
    public void setUp() throws Exception {
        handler = new AggregatingHttpHandler();
        server = new MockHttpServer(PORT, handler);

        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (loggerInTest != null)
            loggerInTest.detachAndStopAllAppenders();
        if (server != null)
            server.stop();
    }

    @Test
    public void testSingleMessage() throws Exception {
        setUpLogger(1, 10000, 10);

        loggerInTest.info("This is a message");

        Thread.sleep(100);
        assertEquals(1, handler.getExchanges().size());
        assertEquals("This is a message\n", handler.getExchanges().get(0).getBody());
    }

    @Test
    public void testMultipleMessages() throws Exception {
        setUpLogger(1, 10000, 10);

        int numMessages = 20;
        for (int i = 0; i < numMessages; i ++) {
            loggerInTest.info("info " + i);
            Thread.sleep(100);
        }

        assertEquals(numMessages, handler.getExchanges().size());
    }


    @Test
    public void testBatchingBySize() throws Exception {
        // Huge window, ensure all messages get batched into one
        setUpLogger(100, 10000, 10);

        int numMessages = 100;
        for (int i = 0; i < numMessages; i ++) {
            loggerInTest.info("info " + i);
        }


        Thread.sleep(2000);
        assertEquals(handler.getExchanges().size(), 1);
    }

    @Test
    public void testBatchingByWindow() throws Exception {
        // Small window, ensure all messages get batched by time
        setUpLogger(10000, 500, 10);

        loggerInTest.info("message1");
        loggerInTest.info("message2");
        loggerInTest.info("message3");
        loggerInTest.info("message4");
        loggerInTest.info("message5");

        Thread.sleep(520);

        loggerInTest.info("message1");
        loggerInTest.info("message2");
        loggerInTest.info("message3");
        loggerInTest.info("message4");
        loggerInTest.info("message5");

        Thread.sleep(520);


        assertEquals(2, handler.getExchanges().size());
        MaterializedHttpRequest request1 = handler.getExchanges().get(0);
        MaterializedHttpRequest request2 = handler.getExchanges().get(1);
        assertEquals("Test Application", request1.getHeaders().get("X-sumo-name").get(0));
        assertEquals("TestCategory", request1.getHeaders().get("X-sumo-category").get(0));
        assertEquals("10.128.10.1", request1.getHeaders().get("X-sumo-host").get(0));
        assertEquals("sumo-logback-appender", request1.getHeaders().get("X-sumo-client").get(0));
        System.out.println(request1.getBody());
    }


    @Test
    // Start with an appender without its URL set. THEN set the property and
    // make sure everything's still there.
    public void testNoUrlSetInitially() throws Exception {
//        LogLog.setInternalDebugging(true);

        appender = new BufferedSumoLogicAppender();
        appender.setMessagesPerRequest(1000);
        appender.setMaxFlushInterval(100);
        appender.setFlushingAccuracy(1);
        appender.setRetryInterval(1);


        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("-- %message%n");
        layout.start();

        appender.setLayout(layout);
        appender.start();

        setUpLogger(appender);


        for (int i = 0; i < 100; i++) {
            loggerInTest.info("message " + i);
        }


        appender.setUrl(ENDPOINT_URL);
        appender.start();

        Thread.sleep(1000);
        assertEquals(1, handler.getExchanges().size());

    }


}
