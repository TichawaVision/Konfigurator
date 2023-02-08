package de.tichawa.cis.config.serial;

import de.tichawa.cis.config.CIS;

import java.util.List;

/**
 * Class that generates commands for the serial interface for the CPUCLinkA.
 * <p>
 * To be completed once the A method is in place.
 */
public class CPUCLinkASerialCommands extends SerialCommands {
    public CPUCLinkASerialCommands(CIS cis) {
        super(cis);
    }

    @Override
    public List<String> generateCommands() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
