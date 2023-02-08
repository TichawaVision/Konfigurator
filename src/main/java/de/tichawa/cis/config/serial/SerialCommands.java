package de.tichawa.cis.config.serial;

import de.tichawa.cis.config.CIS;

import java.util.*;

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
        RAW("Raw"), DEFAULT("Default"), NO_SHIFT("Don't send shift commands");
        final String displayString;

        ShiftOptions(String displayString) {
            this.displayString = displayString;
        }

        /**
         * returns the {@link ShiftOptions} value corresponding to the given display string
         *
         * @param displayString the display string of the shift option
         * @return the shift option with the given display string
         */
        public static ShiftOptions fromString(String displayString) {
            return Arrays.stream(ShiftOptions.values()).filter(shiftOption -> shiftOption.displayString.equalsIgnoreCase(displayString)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("shift option does not exist"));
        }
    }
}
