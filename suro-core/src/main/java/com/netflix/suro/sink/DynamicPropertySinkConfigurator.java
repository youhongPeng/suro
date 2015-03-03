package com.netflix.suro.sink;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.netflix.config.DynamicStringProperty;
import com.netflix.governator.annotations.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * {@link com.netflix.config.DynamicProperty} driven sink configuration.  Whenever a change is made to the
 * dynamic property, this module will parse the JSON and set a new configuration on the
 * main SinkManager.
 *
 * @author elandau
 */
public class DynamicPropertySinkConfigurator {
    private static final Logger log = LoggerFactory.getLogger(DynamicPropertySinkConfigurator.class);

    public static final String SINK_PROPERTY = "SuroServer.sinkConfig";

    private final SinkManager     sinkManager;
    private final ObjectMapper    jsonMapper;

    @Configuration(SINK_PROPERTY)
    private String initialSink;

    @Inject
    public DynamicPropertySinkConfigurator(
        SinkManager sinkManager,
        ObjectMapper jsonMapper) {
        this.sinkManager = sinkManager;
        this.jsonMapper  = jsonMapper;
    }

    @PostConstruct
    public void init() {
        DynamicStringProperty sinkDescription = new DynamicStringProperty(SINK_PROPERTY, initialSink) {
            @Override
            protected void propertyChanged() {
                buildSink(get(), false);
            }
        };

        buildSink(sinkDescription.get(), true);
    }

    private void buildSink(String sink, boolean initStart) {
        try {
            log.debug("building sink config: {}", sink);
            Map<String, Sink> newSinkMap = jsonMapper.readValue(sink, new TypeReference<Map<String, Sink>>(){});
            if ( !newSinkMap.containsKey("default") ) {
                throw new IllegalStateException("default sink should be defined");
            }

            if (initStart) {
                sinkManager.initialSet(newSinkMap);
            } else {
                sinkManager.set(newSinkMap);
            }
            if(initStart) {
                log.info("applied initial sink config: {}", sink);
            } else {
                log.warn("applied updated sink config: {}", sink);
            }
        } catch (Throwable e) {
            log.error("failed to build sink config", e);
        }
    }
}
