package org.restcomm.media.core.configuration;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anikiforov on 6/14/2017.
 */
public class DriverConfiguration {
    private final String driverName;
    private final String className;
    private final Map<String, String> parameters = new HashMap<>();

    public DriverConfiguration(final String driverName, final String className) {
        if (StringUtils.isEmpty(driverName)) {
            throw new IllegalArgumentException("Driver name shouldn't be empty");
        } else if (StringUtils.isEmpty(className)) {
            throw new IllegalArgumentException("Class name shouldn't be empty");
        } else {
            this.driverName = driverName;
            this.className = className;
        }
    }

    public String getDriverName() {
        return driverName;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void addParameter(final String paramName, final String paramValue) {
        if (StringUtils.isEmpty(paramName)) {
            throw new IllegalArgumentException("Parameter name shouldn't be empty");
        } else if (StringUtils.isEmpty(paramValue)) {
            throw new IllegalArgumentException("Parameter value shouldn't be empty");
        } else if (parameters.containsKey(paramName)) {
            throw new IllegalArgumentException("Parameter " + paramName + " is already specified");
        } else {
            parameters.put(paramName, paramValue);
        }
    }
}