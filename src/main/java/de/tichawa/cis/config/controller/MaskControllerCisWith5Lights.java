package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.beans.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller parent class for {@link CisWith5Lights} that handles the user interface with respect to common functionality of all {@link CisWith5Lights}.
 * Common functionality includes
 * - light choice boxes for all 5 lights,
 * - checkboxes for cooling and cloudy day,
 * - camera link options and labels for camera link information on the user interface
 * - lens choice section
 *
 * @param <C> the actual subclass of the {@link CisWith5Lights}
 */
public abstract class MaskControllerCisWith5Lights<C extends CisWith5Lights> extends MaskController<C> implements PropertyChangeListener {

    @FXML
    protected ChoiceBox<CIS.LightColor> brightFieldLeftChoiceBox;
    @FXML
    protected ChoiceBox<CIS.LightColor> brightFieldRightChoiceBox;
    @FXML
    protected Label cameraLinkInfoLabel;
    @FXML
    protected CheckBox cloudyDayCheckBox;
    @FXML
    protected ChoiceBox<CIS.LightColor> coaxChoiceBox;
    @FXML
    protected CheckBox coolingLeftCheckBox;
    @FXML
    protected CheckBox coolingRightCheckBox;
    @FXML
    protected Label currentLineRatePercentLabel;
    @FXML
    protected ChoiceBox<CIS.LightColor> darkFieldLeftChoiceBox;
    @FXML
    protected ChoiceBox<CIS.LightColor> darkFieldRightChoiceBox;
    @FXML
    protected Label lensCodeLabel;
    @FXML
    protected CheckBox lensDofCheckBox;
    @FXML
    protected ChoiceBox<String> lensTypeChoiceBox;
    @FXML
    protected Label lightInfoLabel;
    @FXML
    protected ChoiceBox<String> moduloChoiceBox;
    @FXML
    protected Label pixelCountLabel;
    @FXML
    protected CheckBox reducedPixelClockCheckBox;
    @FXML
    protected Label tiViKeyLabel;

    /**
     * Returns a list of light color options for this CIS
     */
    protected abstract List<CIS.LightColor> getLightColorOptions();

    /**
     * Returns a list of light color options that can be used with coax lighting
     */
    protected abstract List<CIS.LightColor> getLightColorOptionsCoax();

    /**
     * Returns the light name corresponding to the given choice box id. The light name will be used as changed property
     *
     * @param choiceBoxId the id of the choice box of the light
     * @return the light (property) name
     */
    protected String getLightNameFromChoiceBoxId(String choiceBoxId) {
        switch (choiceBoxId) {
            case "brightFieldLeftChoiceBox":
                return CisWith5Lights.PROPERTY_BRIGHT_FIELD_LEFT;
            case "brightFieldRightChoiceBox":
                return CisWith5Lights.PROPERTY_BRIGHT_FIELD_RIGHT;
            case "darkFieldLeftChoiceBox":
                return CisWith5Lights.PROPERTY_DARK_FIELD_LEFT;
            case "darkFieldRightChoiceBox":
                return CisWith5Lights.PROPERTY_DARK_FIELD_RIGHT;
            case "coaxChoiceBox":
                return CisWith5Lights.PROPERTY_COAX;
            default:
                throw new IllegalArgumentException("unknown choiceBoxId: " + choiceBoxId);
        }
    }

    /**
     * Returns a list of scan width options for this CIS
     */
    protected abstract List<Integer> getScanWidthOptions();

    protected void handleCloudyDayChange(boolean newValue) {
        cloudyDayCheckBox.setSelected(newValue);
    }

    /**
     * Handles the change of the coax light
     *
     * @param oldValue the old coax light color
     * @param newValue the new coax light color
     */
    protected abstract void handleCoaxChange(CIS.LightColor oldValue, CIS.LightColor newValue);

    /**
     * Handles the change of the line rate.
     * Sets the label texts of the current line rate and percentage.
     */
    protected void handleLineRateChange(int lineRate) {
        currentLineRateLabel.setText(lineRate / 1000.0 + " kHz");
        currentLineRatePercentLabel.setText(Math.round(lineRate / CIS_DATA.getMaxLineRate() * 100) + " %");
    }

    /**
     * Handles changes to the phase count in the model: shows the corresponding text in the choice box and adjusts the line rate
     */
    protected void handlePhasesChange(Phases selectedPhase) {
        // set choice box selection
        phasesChoiceBox.getSelectionModel().select(selectedPhase.getDisplayText());
        // adjust line rate outputs
        updateMaxLineRateChange();
    }

    /**
     * Handles the resolution change by calling helper functions to update the maximum line rate and pixel size labels
     */
    private void handleResolutionChange(CIS.Resolution resolution) {
        // cannot easily set resolution in GUI since this would require mapping the resolution values to the fxml texts
        // however, since resolution only changes by user input, this does not really matter for now

        // set output texts
        updateMaxLineRateChange();
        updatePixelAndDefectSize();
    }

    /**
     * Handles the transport speed change by updating the corresponding label texts
     */
    protected void handleTransportSpeedChange(int transportSpeed) {
        double transportSpeedByThousands = transportSpeed / 1000.0;
        speedmmsLabel.setText(Util.round(transportSpeedByThousands, 3) + " mm/s");
        speedmsLabel.setText(Util.round(transportSpeedByThousands / 1000, 3) + " m/s");
        speedmminLabel.setText(Util.round(transportSpeedByThousands * 0.06, 3) + " m/min");
        speedipsLabel.setText(Util.round(transportSpeedByThousands * 0.03937, 3) + " ips");
    }

    /**
     * Initializes the cloudy day checkbox
     */
    private void initCloudyDay() {
        cloudyDayCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCloudyDay(newValue));
    }

    /**
     * Initializes the color and light sources section (light preset, phases, cloudy day, bright and dark fields, coax, cooling)
     */
    protected void initColorAndLightSources() {
        initPhases();
        initCloudyDay();
        initLights();
        initCooling();
    }

    /**
     * Initializes the cooling checkboxes
     */
    private void initCooling() {
        coolingLeftCheckBox.setSelected(CIS_DATA.hasCoolingLeft());
        coolingRightCheckBox.setSelected(CIS_DATA.hasCoolingRight());
        coolingLeftCheckBox.setDisable(true);
        coolingRightCheckBox.setDisable(true);
        coolingLeftCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCoolingLeft(newValue));
        coolingRightCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCoolingRight(newValue));
    }

    /**
     * Initializes the interface section
     */
    protected void initInterface() {
        // disable interface choice
        interfaceChoiceBox.getSelectionModel().selectFirst(); // disabled for now (only CameraLink)
        // listener and text output for reduced pixel clock
        reducedPixelClockCheckBox.setText(reducedPixelClockCheckBox.getText() + " (" + (CisWith5Lights.PIXEL_CLOCK_REDUCED / 1000000) + "\u200aMHz)");
        reducedPixelClockCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setReducedPixelClock(newValue));
        reducedPixelClockCheckBox.setSelected(CIS_DATA.isReducedPixelClock());
        // setup choice box for valid mod options
        moduloChoiceBox.getItems().clear();
        moduloChoiceBox.getItems().addAll(CisWith5Lights.VALID_MODS.stream().map(Object::toString).collect(Collectors.toList()));
        moduloChoiceBox.getSelectionModel().select(String.valueOf(CIS_DATA.getMod()));
        moduloChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setMod(Integer.parseInt(newValue)));
    }

    /**
     * Initializes a single given light choice box. If it is not the coax box, all light options for VUCIS will be present. For Coax only non shape from shading ones.
     */
    private void initLight(ChoiceBox<CIS.LightColor> lightBox, CIS.LightColor initialValue, boolean isCoaxBox) {
        // create string converter to convert the LightColor enum to and from String
        lightBox.setConverter(new StringConverter<CIS.LightColor>() {
            /**
             * Creates a {@link String} representation of the given light color for GUI light choice boxes:
             * Shows the description and the light short code separated by ' - '
             * @param lightColor the light color that is shown
             * @return the string representation of the light color
             */
            @Override
            public String toString(CIS.LightColor lightColor) {
                return lightColor.getDescription() + " - " + lightColor.getCode();
            }

            /**
             * Creates the corresponding light color object from the string representation created by {@link #toString(CIS.LightColor)}
             * @param string the string representation of the light color
             * @return the light color
             */
            @Override
            public CIS.LightColor fromString(String string) {
                // search all light colors by short code - the short code is (one character) after the separating ' - '
                return CIS.LightColor.findByCode(string.split(" - ")[1].charAt(0)).orElseThrow(() -> new IllegalStateException("invalid light code found at light choice box"));
            }
        });
        //set items to choice box
        lightBox.getItems().clear();
        lightBox.getItems().addAll(isCoaxBox ? getLightColorOptionsCoax() : getLightColorOptions());
        // select initial value
        lightBox.getSelectionModel().select(initialValue);
        //add listener to handle light changes
        lightBox.valueProperty().addListener((property, oldValue, newValue) -> {
            if (newValue != null) { // when clearing and resetting choice boxes this may be null
                //set light color in model on light change
                String choiceBoxId = ((ChoiceBox<?>) ((ObjectProperty<?>) property).getBean()).getId();
                CIS_DATA.setLightColor(getLightNameFromChoiceBoxId(choiceBoxId), newValue);
            }
        });
    }

    /**
     * Initializes the lights: left/right dark/bright field and coax
     */
    private void initLights() {
        initLight(brightFieldLeftChoiceBox, CIS_DATA.getBrightFieldLeft(), false);
        initLight(brightFieldRightChoiceBox, CIS_DATA.getBrightFieldRight(), false);
        initLight(darkFieldLeftChoiceBox, CIS_DATA.getDarkFieldLeft(), false);
        initLight(darkFieldRightChoiceBox, CIS_DATA.getDarkFieldRight(), false);
        initLight(coaxChoiceBox, CIS_DATA.getCoaxLight(), true);
    }

    /**
     * Initializes the line rate and transport speed section
     */
    private void initLineRateAndTransportSpeed() {
        handleTransportSpeedChange(CIS_DATA.getTransportSpeed());
        updateMaxLineRateChange();
        handleLineRateChange(CIS_DATA.getSelectedLineRate());
        selectedLineRateSlider.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setSelectedLineRate(newValue.intValue()));
    }

    /**
     * Initializes the optics section (lens type, DOF)
     */
    protected abstract void initOptics();

    /**
     * Initializes the phases choice box
     */
    private void initPhases() {
        //set items to choice box
        phasesChoiceBox.getItems().clear();
        phasesChoiceBox.getItems().addAll(Arrays.stream(Phases.values()).filter(this::isValidPhaseOption)
                .map(Phases::getDisplayText).collect(Collectors.toList()));
        //select initial value
        Phases initialPhase = Phases.findByPhaseCount(CIS_DATA.getPhaseCount()).orElseThrow(() -> new IllegalArgumentException("selected phases do not exist"));
        phasesChoiceBox.getSelectionModel().select(initialPhase.getDisplayText());
        //add listener to set phase count on changes
        phasesChoiceBox.valueProperty().addListener((property, oldValue, newValue) -> {
            Phases selectedPhase = Phases.findByDisplayText(newValue).orElseThrow(() -> new IllegalArgumentException("selected phases do not exist"));
            //set phase count in model when choice box value changed
            CIS_DATA.setPhaseCount(selectedPhase.phaseCount);
        });
    }

    /**
     * Initializes the resolution and scan width section
     */
    private void initResolutionAndScanWidth() {
        // select initial resolution
        resolutionChoiceBox.getSelectionModel().selectFirst();
        // listener for resolution choice box that sets the new resolution in the model
        resolutionChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setSelectedResolution(getResolutions().get(resolutionChoiceBox.getSelectionModel().getSelectedIndex())));
        // set items to scan width choice box
        scanWidthChoiceBox.getItems().clear();
        scanWidthChoiceBox.getItems().addAll(getScanWidthOptions());
        // set initial value
        scanWidthChoiceBox.getSelectionModel().select((Integer) CIS_DATA.getScanWidth());
        // listener for scan width choice box that sets the new scan width in the model
        scanWidthChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && newValue != null)
                CIS_DATA.setScanWidth(newValue);
        });
    }

    /**
     * Initializes the user interface for this CIS
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // initialize light options (first box)
        initColorAndLightSources();
        // initialize optics (second box)
        initOptics();
        // initialize resolution and scan width section (left box)
        initResolutionAndScanWidth();
        // initialize pixel and defect size section
        updatePixelAndDefectSize();
        // initialize line rate and transport speed
        initLineRateAndTransportSpeed();

        // initialize other stuff
        initInterface();
        updateCameraLinkInfo();
        updateTiViKey();
    }

    /**
     * Returns whether the given phase is a valid phase option
     */
    protected abstract boolean isValidPhaseOption(Phases phase);

    /**
     * Handles an observed property change from the model and updates the GUI accordingly
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println(CIS_DATA.cisName + " MaskController: observed change for " + evt.getPropertyName() + " to " + evt.getNewValue());
        switch (evt.getPropertyName()) {
            // lights
            case CisWith5Lights.PROPERTY_DARK_FIELD_LEFT:
                selectLightChoiceBox(darkFieldLeftChoiceBox, (CIS.LightColor) evt.getNewValue());
                break;
            case CisWith5Lights.PROPERTY_BRIGHT_FIELD_LEFT:
                selectLightChoiceBox(brightFieldLeftChoiceBox, (CIS.LightColor) evt.getNewValue());
                break;
            case CisWith5Lights.PROPERTY_COAX:
                handleCoaxChange((CIS.LightColor) evt.getOldValue(), (CIS.LightColor) evt.getNewValue());
                selectLightChoiceBox(coaxChoiceBox, (CIS.LightColor) evt.getNewValue());
                break;
            case CisWith5Lights.PROPERTY_BRIGHT_FIELD_RIGHT:
                selectLightChoiceBox(brightFieldRightChoiceBox, (CIS.LightColor) evt.getNewValue());
                break;
            case CisWith5Lights.PROPERTY_DARK_FIELD_RIGHT:
                selectLightChoiceBox(darkFieldRightChoiceBox, (CIS.LightColor) evt.getNewValue());
                break;
            case CIS.PROPERTY_PHASE_COUNT:
                Phases selectedPhase = Phases.findByPhaseCount((int) evt.getNewValue()).orElseThrow(() -> new IllegalArgumentException("selected phase count not found"));
                handlePhasesChange(selectedPhase);
                updateCameraLinkInfo();
                break;
            case CisWith5Lights.PROPERTY_CLOUDY_DAY:
                handleCloudyDayChange((boolean) evt.getNewValue());
                break;
            case CisWith5Lights.PROPERTY_COOLING_LEFT:
                coolingLeftCheckBox.setSelected((boolean) evt.getNewValue());
                break;
            case CisWith5Lights.PROPERTY_COOLING_RIGHT:
                coolingRightCheckBox.setSelected((boolean) evt.getNewValue());
                break;
            // resolution and scan width
            case CIS.PROPERTY_RESOLUTION:
                handleResolutionChange((CIS.Resolution) evt.getNewValue());
                updateCameraLinkInfo();
                break;
            case CIS.PROPERTY_SCAN_WIDTH:
                scanWidthChoiceBox.getSelectionModel().select((Integer) evt.getNewValue());
                updateCameraLinkInfo();
                break;
            // line rate and transport speed
            case CIS.PROPERTY_LINE_RATE:
                handleLineRateChange((int) evt.getNewValue());
                updateCameraLinkInfo();
                break;
            case CIS.PROPERTY_TRANSPORT_SPEED:
                handleTransportSpeedChange((int) evt.getNewValue());
                break;
            // interface
            case CisWith5Lights.PROPERTY_REDUCED_PIXEL_CLOCK:
                reducedPixelClockCheckBox.setSelected((boolean) evt.getNewValue());
                updateCameraLinkInfo();
                break;
            // mod
            case CisWith5Lights.PROPERTY_MOD:
                moduloChoiceBox.setValue(String.valueOf((int) evt.getNewValue()));
                updateCameraLinkInfo();
                break;
        }
        updateTiViKey();
    }

    /**
     * Selects the given light color for the given choice box.
     * Also calls {@link #updateCoolingCheckboxes()} to enable/disable the checkboxes after light change
     */
    protected void selectLightChoiceBox(ChoiceBox<CIS.LightColor> box, CIS.LightColor lightColor) {
        box.getSelectionModel().select(lightColor);
        updateCoolingCheckboxes();
    }

    /**
     * Updates the camera link info text.
     * Calculates the camera link configuration and displays needed boards, cables and number of used ports.
     * Also updates the total pixel count.
     */
    protected void updateCameraLinkInfo() {
        try {
            List<CPUCLink> clcalc = CIS_DATA.getCLCalc(CIS_DATA.calcNumOfPix(), null);
            StringJoiner displayText = new StringJoiner("\n");
            int boardNumber = 1;
            for (CPUCLink c : clcalc) {
                String boardText = "Board " + boardNumber++ + " " // board number
                        // camera link format (base, medium, full, deca)
                        + c.getCameraLinks().stream().map(CPUCLink.CameraLink::getCLFormat).collect(Collectors.joining("+", "(", ")"))
                        // needed cables and ports
                        + ": " + c.getCableCount() + " cable, " + c.getPortCount() + " ports, " + c.getPortCount() / CIS_DATA.getPhaseCount() + " taps";
                displayText.add(boardText);
            }
            cameraLinkInfoLabel.setText(displayText + "\n"
                    + "Actual scan width: "
                    + String.format(Locale.US, "%.2f", CIS_DATA.getActualSupportedScanWidth(clcalc))
                    + "\u200amm");
            //pixel count
            long pixelCount = clcalc.stream().mapToLong(CPUCLink::getPixelCount).sum();
            pixelCountLabel.setText(pixelCount + "");
        } catch (CISException e) {
            cameraLinkInfoLabel.setText(e.getMessage());
        }
    }

    /**
     * Disables/enables the cooling checkboxes
     */
    private void updateCoolingCheckboxes() {
        coolingLeftCheckBox.setDisable(CIS_DATA.hasCoolingLeft());
        coolingRightCheckBox.setDisable(CIS_DATA.hasCoolingRight());
    }

    /**
     * Handles a change to the max line rate: sets the new max value of the slider and updates the text label
     */
    protected void updateMaxLineRateChange() {
        maxLineRateLabel.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
        selectedLineRateSlider.setMax(CIS_DATA.getMaxLineRate());
        selectedLineRateSlider.setValue(CIS_DATA.getMaxLineRate());
        selectedLineRateSlider.setBlockIncrement(CIS_DATA.getMaxLineRate() / 100); // we can choose increments of 1%
    }

    /**
     * Updates the pixel and defect size text labels
     */
    protected void updatePixelAndDefectSize() {
        pixelSizeLabel.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
        defectSizeLabel.setText(Util.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
    }

    /**
     * Updates the {@link #tiViKeyLabel} by setting the current tivi key of the cis
     */
    protected void updateTiViKey() {
        tiViKeyLabel.setText(CIS_DATA.getTiViKey());
    }

    /**
     * Enumeration of possible phases (to map the phase count to the display text in the user interface)
     */
    public enum Phases {
        ONE(1, "One phase (Monochrome)"),
        TWO(2, "Two phases"),
        THREE(3, "Three phases (e.g. RGB)"),
        FOUR(4, "Four phases"),
        FIVE(5, "Five phases"),
        SIX(6, "Six phases");

        private final String displayText;
        private final int phaseCount;

        Phases(int phaseCount, String displayText) {
            this.phaseCount = phaseCount;
            this.displayText = displayText;
        }

        /**
         * Returns an optional of the phase with the given text or empty if no phase with this text exists
         */
        public static Optional<Phases> findByDisplayText(String displayText) {
            return Arrays.stream(Phases.values()).filter(t -> t.displayText.equals(displayText)).findFirst();
        }

        /**
         * Returns an optional of the phase with the given phase count or empty if no phase with this phase count exists
         */
        public static Optional<Phases> findByPhaseCount(int phaseCount) {
            return Arrays.stream(Phases.values()).filter(p -> p.phaseCount == phaseCount).findFirst();
        }

        public String getDisplayText() {
            return displayText;
        }
    }
}
