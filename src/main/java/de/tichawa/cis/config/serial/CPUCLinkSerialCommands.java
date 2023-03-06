package de.tichawa.cis.config.serial;

import de.tichawa.cis.config.*;

import java.util.*;

/**
 * class that generates commands for the serial interface for the CPUCLink
 */
public class CPUCLinkSerialCommands extends SerialCommands {
    private final List<String> setMenuCommands;
    private final List<String> mainMenuCommands;
    private final ShiftOptions shiftOptions;
    private final boolean storeParameters;
    private final CPUCLink cpucLink;
    private final int chips;

    /**
     * creates a serial command object for cpuclinks
     *
     * @param cis                the cis that should be configured by the commands
     * @param shiftOptions       whether there should be an x- and y-shift
     * @param storeParameters    whether the S command should be sent do save the configuration
     * @param cpucLink           the current cpuc link that gets configured
     * @param totalNumberOfPixel the total number of pixel. This is used to calculate the number of chips on this cpuc link
     */
    public CPUCLinkSerialCommands(CIS cis, ShiftOptions shiftOptions, boolean storeParameters, CPUCLink cpucLink, long totalNumberOfPixel) {
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
        //TODO maybe we need password

        // generate all commands
        generateSetMenuCommands();
        generateMainMenuCommands();

        // main menu commands first, then go to set menu and add corresponding commands
        List<String> commands = new LinkedList<>(mainMenuCommands);
        commands.add(">");
        commands.addAll(setMenuCommands);
        commands.add("<"); // back to main menu
        if (storeParameters) // save if we want to
            commands.add("S"); // needs to be last
        return commands;
    }

    /**
     * generates all commands for the set menu
     */
    private void generateSetMenuCommands() {
        generateADCBoardCommand();
        generateSetDPICommand(); // needs to be before sensor
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
     * generates all commands for the main menu
     */
    private void generateMainMenuCommands() {
        generateBLDigitalCommand();
        generateDLDigitalCommand();
        generateFifoDirectionCommand();
        generateExposureCommand();
        generateLineFrequencyCommand();
        generateLTrigModeCommand();
        generateFTrigCommand();
        generateAnalogOffsetCommand();
        generateCyclesCommand();
        generateTriggerPulsesCommand();
        generateVideoModeCommands();
        generateSheetLenDelayCommand();
    }

    /**
     * Generates the command for the adc board.
     */
    private void generateADCBoardCommand() {
        setMenuCommands.add("A0");
    }

    /**
     * Generates the commands for the sensor chip.
     * Generates the set menu C-command depending on the board resolution.
     * Also generates the set menu I-command with the factor depending on the actual resolution.
     */
    private void generateSensorChipCommands() {
        CIS.Resolution resolution = cis.getSelectedResolution();

        // C command (sensor chip dpi)
        String cCommand = "C";
        switch (resolution.getBoardResolution()) {
            case 1200:
                cCommand += "2";
                break;
            case 600:
                cCommand += "1";
                break;
            case 300:
                cCommand += "0";
                break;
            default:
                throw new UnsupportedOperationException("unsupported resolution: " + resolution.getBoardResolution());
        }
        setMenuCommands.add(cCommand);

        // I command (binning)
        int binningFactor = resolution.getBoardResolution() / resolution.getActualResolution();
        String iCommand = "I" + binningFactor + ",0," + binningFactor;
        setMenuCommands.add(iCommand);
    }

    /**
     * generates the set menu D-command depending on the number of phases
     */
    private void generateSequenceCommands() {
        int phases = cis.getPhaseCount();
        for (int i = 0; i < phases - 1; i++) {
            setMenuCommands.add("D" + i + ",1," + i + ",0,0," + i);
        }
        setMenuCommands.add("D" + (phases - 1) + ",1," + (phases - 1) + ",1,1," + (phases - 1));
    }

    /**
     * generates the set menu F-command depending on whether there is a reduced pixel clock
     */
    private void generateCLOutFrequencyCommand() {
        setMenuCommands.add("F" + (cis.isReducedPixelClock() ? "53" : "85"));
    }

    /**
     * generates the set menu K-command that allows setting of the dpi
     */
    private void generateSetDPICommand() {
        setMenuCommands.add("K1");
    }

    /**
     * generates the set menu L-command based on the camera link mode
     */
    private void generateCLModeCommand() {
        // read from first camera link from the cpuclink
        setMenuCommands.add("L" + (cpucLink.getCameraLinks().get(0).getCLFormat().equals(CPUCLink.CameraLink.CL_FORMAT_DECA) ? 1 : 0));
    }

    /**
     * generates the set menu M-command to set the max temperature
     */
    private void generateMaxTempCommand() {
        setMenuCommands.add("M65,35,35");
    }

    /**
     * generates the set menu O-command to reset the automatic correction
     */
    private void generateAutoCorrCommand() {
        setMenuCommands.add("O0,0,0,0,0");
    }

    /**
     * generates the set menu Q-command depending on the camera link modulo of the CIS
     */
    private void generateCLModuloCommand() {
        setMenuCommands.add("Q" + cis.getMod());
    }

    /**
     * generates the set menu R-command depending on the selected line rate
     */
    private void generateLineRateCommand() {
        long lineRate = Math.round(cis.getSelectedLineRate() / cis.getMaxLineRate() * 100);
        setMenuCommands.add("R" + lineRate);
    }

    /**
     * generates the set menu S-command
     */
    private void generateSensorBoardCommand() {
        setMenuCommands.add("S0");
    }

    /**
     * generates the set menu T-command
     */
    private void generateUnitTypeCommand() {
        setMenuCommands.add("T0");
    }

    /**
     * generates the set menu U-command
     */
    private void generateCLPatchCommand() {
        setMenuCommands.add("U0");
    }

    /**
     * generates the set menu V-command
     */
    private void generateGlobalGainCommand() {
        setMenuCommands.add("V72");
    }

    /**
     * generates the set menu W-command
     */
    private void generateNumberOfChipsCommand() {
        setMenuCommands.add("W" + chips);
    }

    /**
     * generates the set menu X- and Y-commands based on the selected shift options
     */
    private void generateShiftCommands() {
        int maxPixel = 864 * cis.getSelectedResolution().getBoardResolution() / 1200;
        switch (shiftOptions) {
            case NO_SHIFT:
                return;
            case RAW:
                setMenuCommands.add("X999,0,0," + (maxPixel - 1) + ",0");
                break;
            case DEFAULT:
                int xShift = 48 * cis.getSelectedResolution().getBoardResolution() / 1200;
                setMenuCommands.add("X999," + xShift + ",0," + (maxPixel - xShift - 1) + ",0");
        }
        setMenuCommands.add("Y999,0,0,0,0");
    }

    /**
     * generates the set menu &-command
     */
    private void generateKinkCommand() {
        setMenuCommands.add("&999,0");
    }

    /**
     * generates the set menu &-command
     */
    private void generateLaserExpoCommands() {
        for (int i = 0; i <= 32; i++) {
            setMenuCommands.add("*" + i + ",0");
        }
    }

    /**
     * generates the main menu B-command
     */
    private void generateBLDigitalCommand() {
        for (int i = 0; i < 6; i++) {
            mainMenuCommands.add("B" + i + ",230");
        }
    }

    /**
     * generates the main menu D-command
     */
    private void generateDLDigitalCommand() {
        for (int i = 0; i < 6; i++) {
            mainMenuCommands.add("D" + i + ",10");
        }
    }

    /**
     * generates the main menu d-command
     */
    private void generateFifoDirectionCommand() {
        mainMenuCommands.add("d0");
    }

    /**
     * generates the main menu E-command
     */
    private void generateExposureCommand() {
        mainMenuCommands.add("E99,99,0");
    }

    /**
     * generates the main menu F-command
     */
    private void generateLineFrequencyCommand() {
        mainMenuCommands.add("F100");
    }

    /**
     * generates the main menu M-command
     */
    private void generateLTrigModeCommand() {
        mainMenuCommands.add("M0");
    }

    /**
     * generates the main menu N-command
     */
    private void generateFTrigCommand() {
        mainMenuCommands.add("N0");
    }

    /**
     * generates the main menu O-command
     */
    private void generateAnalogOffsetCommand() {
        mainMenuCommands.add("O999,0");
    }

    /**
     * generates the main menu Q-command
     */
    private void generateCyclesCommand() {
        mainMenuCommands.add("Q1024");
    }

    /**
     * generates the main menu T-command
     */
    private void generateTriggerPulsesCommand() {
        mainMenuCommands.add("T1");
    }

    /**
     * generates the main menu V-commands
     */
    private void generateVideoModeCommands() {
        mainMenuCommands.add("V0,0,1");
        mainMenuCommands.add("V1,0,1");
    }

    /**
     * generates the main menu *-command
     */
    private void generateSheetLenDelayCommand() {
        mainMenuCommands.add("*0,0");
    }
}
