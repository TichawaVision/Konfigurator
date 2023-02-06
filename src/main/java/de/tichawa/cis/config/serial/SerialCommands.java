package de.tichawa.cis.config.serial;

import de.tichawa.cis.config.CIS;

import java.util.List;

/**
 * general class for serial commands
 */
public abstract class SerialCommands {

    protected final CIS cis;

    protected SerialCommands(CIS cis) {
        this.cis = cis;
    }

    /**
     * Generates all commands for the serial interface setup of this CIS.
     * Generates the common commands first and adds the specific ones afterwards.
     *
     * @return a list of commands for the serial interface
     */
    public abstract List<String> generateCommands();

    /**
     * enum for x- and y-shift options.
     * DEFAULT represents the default shift that is most commonly used,
     * RAW represents no shift, so the first pixel will be 0 and the last will be the maximum pixel,
     * NO_SHIFT is used if there should not be sent an x- or y-shift command (e.g. because it was set beforehand)
     */
    public enum ShiftOptions {
        RAW, DEFAULT, NO_SHIFT
    }
}
