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


        List<String> commands = new LinkedList<>();
        // main menu commands first
        commands.add("<"); // back to main menu (in case we are not there already)
        commands.addAll(mainMenuCommands);

        // set menu next
        commands.add("SET"); // switch to set menu
        commands.addAll(setMenuCommands);

        // save if we want to
        if (storeParameters) { // save in set menu
            commands.add("PPS"); // store parameters
        }

        commands.add("<"); // back to main menu
        commands.add("PCC"); // clear pixel correction
        if (storeParameters) // save in main menu
            commands.add("PCS"); // store pixel correction

        return commands;
    }

    /**
     * generates all commands for the main menu
     */
    private void generateMainMenuCommands() {
        generateVideoModeCommands();
        generatePixelCorrectionCommand();
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
        generateStartStopModeCommand();
        generateDynamicSensorBalanceCommand();
    }

    /**
     * generates the main menu commands for the video mode ("V")
     */
    private void generateVideoModeCommands() {
        mainMenuCommands.add("V 0,0,1");
        mainMenuCommands.add("V 1,0,1");
    }

    /**
     * generates the main menu command for the pixel correction.
     * Enables pixel correction for all phases (sends value 1).
     */
    private void generatePixelCorrectionCommand() {
        mainMenuCommands.add("PCP 1,1,1,1,1,1");
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
        }
        setMenuCommands.add("YG 999,0,0,0,0");
    }

    /**
     * generates the set menu command for the number of chips ("W")
     */
    private void generateNumberOfChipsCommand() {
        setMenuCommands.add("W " + chips);
    }

    /**
     * generates the set menu command for the global gain ("GLG")
     */
    private void generateGlobalGainCommand() {
        setMenuCommands.add("GLG 72");
    }

    /**
     * generates the set menu command for the camera link patch ("CLP")
     */
    private void generateCLPatchCommand() {
        setMenuCommands.add("CLP 0");
    }

    /**
     * generates the set menu command for the unit type ("UTP")
     */
    private void generateUnitTypeCommand() {
        setMenuCommands.add("UTP 0");
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
     * generates the set menu command for the auto correlator ("AC")
     */
    private void generateAutoCorrCommand() {
        setMenuCommands.add("AC 1,50,3,250");
    }

    /**
     * generates the set menu command for the max temperature ("TMP")
     */
    private void generateMaxTempCommand() {
        setMenuCommands.add("TMP 65,35,35");
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
     * <p>
     * Command parameters:
     * - phase number,
     * - target type (1 - video, 5 - autocorrection),
     * - FPGA channel number,
     * - video flag (0 - read video data, 1 - return video data),
     * - return flag (0 - continue, 1 - return),
     * - color mix table number
     */
    private void generateSequenceCommands() {
        int phases = cis.getPhaseCount();
        for (int i = 0; i < phases - 1; i++) {
            setMenuCommands.add("SEQ " + i + ",1," + i + ",0,0," + i);
        }
        setMenuCommands.add("SEQ " + (phases - 1) + ",1," + (phases - 1) + ",1,1," + (phases - 1));
    }

    /**
     * generates the set menu commands for the sensor chip
     */
    private void generateSensorChipCommands() {
        CIS.Resolution resolution = cis.getSelectedResolution();

        // RES command (sensor chip dpi)
        String resCommand = "RES ";
        switch (resolution.getActualResolution()) {
            case 25:
                resCommand += "0";
                break;
            case 50:
                resCommand += "1";
                break;
            case 75:
                resCommand += "2";
                break;
            case 100:
                resCommand += "3";
                break;
            case 150:
                resCommand += "4";
                break;
            case 200:
                resCommand += "5";
                break;
            case 300:
                resCommand += "6";
                break;
            case 400:
                resCommand += "7";
                break;
            case 600:
                resCommand += "8";
                break;
            case 1200:
                resCommand += "9";
                break;
            default:
                throw new UnsupportedOperationException("unsupported resolution: " + resolution.getActualResolution());
        }
        setMenuCommands.add(resCommand);
        // RES will also handle binning
    }

    /**
     * generates the set menu command to enable switching DPI ("SWI")
     */
    private void generateSetDPICommand() {
        setMenuCommands.add("SWI 1");
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
        setMenuCommands.add("SD 0,0");
    }

    /**
     * generates the set menu command for the trigger pulses
     */
    private void generateTriggerPulsesCommand() {
        setMenuCommands.add("TP 1");
    }

    /**
     * generates the set menu command for the analog offset ("AOF").
     * The command is a broadcast command with the value 0.
     */
    private void generateAnalogOffsetCommand() {
        setMenuCommands.add("AOF 999,0"); //TODO weg?
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
        setMenuCommands.add("LFR 1000");
    }

    /**
     * generates the set menu command for the exposure
     */
    private void generateExposureCommand() {
        setMenuCommands.add("EXP 999,999,0,0");
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

    /**
     * generates the set menu start/stop mode command.
     */
    private void generateStartStopModeCommand() {
        setMenuCommands.add("SSM 0");
    }

    /**
     * generates the set menu dynamic sensor balance ("Streifenregelung") command.
     * Disables the dynamic sensor balance by sending 0 as first parameter.
     * The second parameter (low pass length/depth) will then not be used.
     * Second parameter values range from 0 to 7 with 0 representing an extra half of the line before that results in a limit of 2 lines while 7 represents 7/8 of the line before resulting in a limit of 2^8=256 lines.
     */
    private void generateDynamicSensorBalanceCommand() {
        setMenuCommands.add("DSB 0,7");
    }
}
/*
    Taps CLT -> werden automatisch berechnet -> nix
    Humidity HUM -> nein
    Registers? REG -> nix
    Cycles -> nix (einfach lassen wie vorher)
    BS (binning skal) -> nix (einfach lassen wie vorher), default wäre 1
*/