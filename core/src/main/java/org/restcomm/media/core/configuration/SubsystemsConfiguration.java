package org.restcomm.media.core.configuration;

import org.apache.commons.lang3.StringUtils;

import javax.naming.ConfigurationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anikiforov on 6/14/2017.
 */
public class SubsystemsConfiguration {
    private final Map<String, DriverConfiguration> subsystems = new HashMap<>();

    public Map<String, DriverConfiguration> getSubsystems() {
        return Collections.unmodifiableMap(subsystems);
    }

    public void addSubsystem(final String subsystemName, final DriverConfiguration driverConfig) throws ConfigurationException {
        if (StringUtils.isEmpty(subsystemName)) {
            throw new IllegalArgumentException("Subsystem name shouldn't be empty");
        } else if (driverConfig == null) {
            throw new IllegalArgumentException("Driver config shouldn't be null");
        } else if (subsystems.containsKey(subsystemName)) {
            throw new ConfigurationException("Subsystem '" + subsystemName + "' is already configured");
        } else {
            subsystems.put(subsystemName, driverConfig);
        }
    }
}