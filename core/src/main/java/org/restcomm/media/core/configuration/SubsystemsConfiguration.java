package org.restcomm.media.core.configuration;

import org.apache.commons.lang3.StringUtils;

import javax.naming.ConfigurationException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anikiforov on 6/14/2017.
 */
public class SubsystemsConfiguration {
    private final Map<String, SubsystemConfiguration> subsystems = new HashMap<>();

    public Collection<DriverConfiguration> getDrivers(final String subsystemName) {
        if (subsystems.containsKey(subsystemName)) {
            final SubsystemConfiguration subsystem = subsystems.get(subsystemName);
            if (subsystem != null) {
                return subsystem.getDrivers();
            }
        }
        return null;
    }

    public DriverConfiguration getDriver(final String subsystemName, final String driverName) {
        if (subsystems.containsKey(subsystemName)) {
            final SubsystemConfiguration subsystem = subsystems.get(subsystemName);
            if (subsystem != null) {
                return subsystem.getDriver(driverName);
            }
        }
        return null;
    }

    public void addSubsystem(final String subsystemName, final SubsystemConfiguration subsystemConfig)
            throws ConfigurationException {
        if (StringUtils.isEmpty(subsystemName)) {
            throw new IllegalArgumentException("Subsystem name shouldn't be empty");
        } else if (subsystemConfig == null) {
            throw new IllegalArgumentException("Driver config shouldn't be null");
        } else if (subsystems.containsKey(subsystemName)) {
            throw new ConfigurationException("Subsystem '" + subsystemName + "' is already configured");
        } else {
            subsystems.put(subsystemName, subsystemConfig);
        }
    }
}