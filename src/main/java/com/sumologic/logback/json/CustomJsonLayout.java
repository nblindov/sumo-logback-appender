package com.sumologic.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.Map;

public class CustomJsonLayout extends JsonLayout {

    @Override
    protected Map toJsonMap(ILoggingEvent event) {
        // Add default params to log.
        Map map = super.toJsonMap(event);

        // Add context params to log.
        map.putAll(event.getLoggerContextVO().getPropertyMap());
        // Add MDC parameters holding our header parameters.
        map.putAll(event.getMDCPropertyMap());

        return map;
    }
}
