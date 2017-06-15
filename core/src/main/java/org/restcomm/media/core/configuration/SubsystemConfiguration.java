package org.restcomm.media.core.configuration;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anikiforov on 6/15/2017.
 */
public class SubsystemConfiguration {
    private final String subsystemName;
    private final Map<String, DriverConfiguration> drivers = new HashMap<>();

    public SubsystemConfiguration(final String subsystemName) {
        this.subsystemName = subsystemName;
    }

    public DriverConfiguration getDriver(final String driverName) {
        if (drivers.containsKey(driverName)) {
            return drivers.get(driverName);
        } else {
            return null;
        }
    }

    public Collection<DriverConfiguration> getDrivers() {
        return drivers.values();
    }

    public void addDriver(final String driverName, final DriverConfiguration driverConf) {
        if (StringUtils.isEmpty(driverName)) {
            throw new IllegalArgumentException("Driver name shouldn't be empty");
        } else if (driverConf == null) {
            throw new IllegalArgumentException("Driver configuration shouldn't be null");
        } else if (drivers.containsKey(driverName)) {
            throw new IllegalArgumentException("Driver " + driverName + " is already specified");
        } else {
            drivers.put(driverName, driverConf);
        }
    }
}
