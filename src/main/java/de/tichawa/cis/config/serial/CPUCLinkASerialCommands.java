package de.tichawa.cis.config.serial;

import de.tichawa.cis.config.*;

import java.util.*;

/**
 * Class that generates commands for the serial interface for the CPUCLinkA.
 * <p>
 * To be completed once the A method is in place.
 */
public class CPUCLinkASerialCommands extends SerialCommands {
    private final List<String> setMenuCommands;
    private final List<String> mainMenuCommands;
    private final ShiftOptions shiftOptions;
    private final boolean storeParameters;
    private final CPUCLink cpucLink;
    private final int chips;

    public CPUCLinkASerialCommands(CIS cis, ShiftOptions shiftOptions, boolean storeParameters, CPUCLink cpucLink, long totalNumberOfPixel) {
        super(cis);
        setMenuCommands = new LinkedList<>();
        mainMenuCommands = new LinkedList<>();
        this.shiftOptions = shiftOptions == null ? ShiftOptions.DEFAULT : shiftOptions;
        this.storeParameters = storeParameters;
        this.cpucLink = cpucLink;
        this.chips = (int) (16 * cis.getScanWidth() * cpucLink.getPixelCount() / totalNumberOfPixel / 260);
    }

    /**
     * generates the commands to configure the cpuc link based on the current selection
     *
     * @return a list of commands to configure the cpuc link
     */
    @Override
    public List<String> generateCommands() {
        // generate commands first
        generateMainMenuCommands();
        generateSetMenuCommands();

        // main menu commands first
        List<String> commands = new LinkedList<>(mainMenuCommands);
        // switch to set menu
        commands.add("SET");
        commands.addAll(setMenuCommands);
        commands.add("<"); // back to main menu
        if (storeParameters) // save if we want to
            commands.add("PPS"); // needs to be last
        return commands;
    }

    /**
     * generates all commands for the main menu
     */
    private void generateMainMenuCommands() {
        generateVideoModeCommands();
    }

    /**
     * generates all commands for the set menu
     */
    private void generateSetMenuCommands() {
        generateSetDPICommand(); // needs to be before sensor
        generateBLDigitalCommand();
        generateDLDigitalCommand();
        generateFifoDirectionCommand();
        generateExposureCommand();
        generateLineFrequencyCommand();
        generateLTrigModeCommand();
        generateFTrigModeCommand();
        generateAnalogOffsetCommand();
        generateCyclesCommand();
        generateTriggerPulsesCommand();
        generateSheetLenDelayCommand();
        generateADCBoardCommand();
        generateSensorChipCommands();
        generateSequenceCommands();
        generateCLOutFrequencyCommand();
        generateCLModeCommand();
        generateMaxTempCommand();
        generateAutoCorrCommand();
        generateCLModuloCommand();
        generateLineRateCommand();
        generateSensorBoardCommand();
        generateUnitTypeCommand();
        generateCLPatchCommand();
        generateGlobalGainCommand();
        generateNumberOfChipsCommand();
        generateShiftCommands();
        generateKinkCommand();
        generateLaserExpoCommands();
    }

    /**
     * generates the main menu commands for the video mode ("V")
     */
    private void generateVideoModeCommands() {
        //TODO
        throw new UnsupportedOperationException();
    }

    private void generateLaserExpoCommands() {
        //TODO
        throw new UnsupportedOperationException();
    }

    private void generateKinkCommand() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * generates the set menu commands for X- and Y-geo-correction ("XG, YG").
     * Depending on the currently selected {@link de.tichawa.cis.config.serial.SerialCommands.ShiftOptions}
     * - the commands are skipped ({@link de.tichawa.cis.config.serial.SerialCommands.ShiftOptions#NO_SHIFT}),
     * - there is a broadcast command to set the geo corrections to 0 ({@link de.tichawa.cis.config.serial.SerialCommands.ShiftOptions#RAW}) or
     * - the default values for the x-geo-correction are broadcasted as well as a 0-value for the y-geo-correction ({@link de.tichawa.cis.config.serial.SerialCommands.ShiftOptions#DEFAULT})
     */
    private void generateShiftCommands() {
        int maxPixel = 864 * cis.getSelectedResolution().getBoardResolution() / 1200;
        switch (shiftOptions) {
            case NO_SHIFT:
                return;
            case RAW:
                setMenuCommands.add("XG 999,0,0," + (maxPixel - 1));
                break;
            case DEFAULT:
                int xShift = 48 * cis.getSelectedResolution().getBoardResolution() / 1200;
                setMenuCommands.add("XG 999," + xShift + ",0," + (maxPixel - xShift - 1));
        } //TODO check if correct
        setMenuCommands.add("YG 999,0,0,0,0");
    }

    /**
     * generates the set menu command for the number of chips ("W")
     */
    private void generateNumberOfChipsCommand() {
        setMenuCommands.add("W" + chips); //TODO check if correct
    }

    /**
     * generates the set menu command for the global gain ("GLG")
     */
    private void generateGlobalGainCommand() {
        setMenuCommands.add("GLG 72"); //TODO check if correct
    }

    /**
     * generates the set menu command for the camera link patch ("CLP")
     */
    private void generateCLPatchCommand() {
        setMenuCommands.add("CLP 0"); //TODO check if correct
    }

    /**
     * generates the set menu command for the unit type ("UTP")
     */
    private void generateUnitTypeCommand() {
        setMenuCommands.add("UTP 0"); //TODO check if correct
    }

    /**
     * generates the set menu command for the sensor board ("SB")
     */
    private void generateSensorBoardCommand() {
        setMenuCommands.add("SB 0");
    }

    /**
     * generates the set menu command for the line rate limit ("LRL")
     */
    private void generateLineRateCommand() {
        long lineRate = Math.round(cis.getSelectedLineRate() / cis.getMaxLineRate() * 100);
        setMenuCommands.add("LRL " + lineRate);
    }

    /**
     * generates the set menu command for the camera link modulus ("CLM")
     */
    private void generateCLModuloCommand() {
        setMenuCommands.add("CLM " + cis.getMod());
    }

    /**
     * generates the set menu command for the autocorrection ("AC")
     */
    private void generateAutoCorrCommand() {
        setMenuCommands.add("AC 0,0,0,0"); //TODO check if correct
    }

    /**
     * generates the set menu command for the max temperature ("TMP")
     */
    private void generateMaxTempCommand() {
        setMenuCommands.add("TMP 65,35,35"); //TODO check if correct
        throw new UnsupportedOperationException();
    }

    /**
     * generates the set menu command for the camera link mode (medium/full80) ("CL8")
     */
    private void generateCLModeCommand() {
        // read from first camera link from the cpuclink
        setMenuCommands.add("CL8 " + (cpucLink.getCameraLinks().get(0).getCLFormat().equals(CPUCLink.CameraLink.CL_FORMAT_DECA) ? 1 : 0));
    }

    /**
     * generates the set menu command for the camera link output frequency (53 or 85 kHz) ("CLF")
     */
    private void generateCLOutFrequencyCommand() {
        setMenuCommands.add("CLF " + (cis.isReducedPixelClock() ? "53" : "85"));
    }

    /**
     * generates the set menu commands for the sequence/phases ("SEQ").
     * All but the last phase get a command with 0 as the third and second last element, the last phase gets a 1 instead.
     * Uses the phase index wherever possible otherwise.
     */
    private void generateSequenceCommands() {
        int phases = cis.getPhaseCount();
        for (int i = 0; i < phases - 1; i++) {
            setMenuCommands.add("SEQ " + i + ",1," + i + ",0,0," + i);
        }
        setMenuCommands.add("SEQ " + (phases - 1) + ",1," + (phases - 1) + ",1,1," + (phases - 1));
    }

    /**
     * generates the set menu command for the sensor chip
     */
    private void generateSensorChipCommands() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * generates the set menu command to enable switching DPI ("SWI")
     */
    private void generateSetDPICommand() {
        setMenuCommands.add("SWI 1"); //TODO GEO?
    }

    /**
     * generates the set menu command for the adc board ("ADC")
     */
    private void generateADCBoardCommand() {
        setMenuCommands.add("ADC 0");
    }

    /**
     * generates the set menu command for the sheet len delay
     */
    private void generateSheetLenDelayCommand() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * generates the set menu command for the trigger pulses
     */
    private void generateTriggerPulsesCommand() {
        setMenuCommands.add("TP 1"); //TODO check if correct
    }

    /**
     * generates the set menu command for the cycles
     */
    private void generateCyclesCommand() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * generates the set menu command for the analog offset ("AOF").
     * The command is a broadcast command with the value 0.
     */
    private void generateAnalogOffsetCommand() {
        setMenuCommands.add("AOF 999,0"); //TODO check if exists
    }

    /**
     * generates the set menu command for the frame trigger mode ("FTM")
     */
    private void generateFTrigModeCommand() {
        setMenuCommands.add("FTM 0");
    }

    /**
     * generates the set menu command for the line trigger mode ("LTM")
     */
    private void generateLTrigModeCommand() {
        setMenuCommands.add("LTM 0");
    }

    /**
     * generates the set menu command for the line frequency
     */
    private void generateLineFrequencyCommand() {
        //TODO LFR/FRQ??
        throw new UnsupportedOperationException();
    }

    /**
     * generates the set menu command for the exposure
     */
    private void generateExposureCommand() {
        //TODO
        throw new UnsupportedOperationException();
    }

    /**
     * generates the set menu command for the dark level ("DL").
     * Generates a command for each phase.
     */
    private void generateDLDigitalCommand() {
        for (int i = 0; i < 6; i++) {
            setMenuCommands.add("DL " + i + ",10");
        }
    }

    /**
     * generates the set menu commands for the bright level ("BL").
     * Generates a command for each phase.
     */
    private void generateBLDigitalCommand() {
        for (int i = 0; i < 6; i++) {
            setMenuCommands.add("BL " + i + ",230");
        }
    }

    /**
     * generates the set menu command for the FIFO direction ("FFD").
     */
    private void generateFifoDirectionCommand() {
        setMenuCommands.add("FFD 0");
    }

}
/* TODO go through serial commands from non ARM and bring them here
    status:
        - prev. main menu -> done
        - pref set menu -> done
        other
            Pixelcorrection PCS/PCP
            Taps CLT
            Exposure EXP
            Enable Geo GEO
            Humidity HUM
            Registers? REG
            Resolution RES
            Safe Factory Settings? SFS
            Sheet Delay? SHD
            Set Start/Stop Mode SSM
*/