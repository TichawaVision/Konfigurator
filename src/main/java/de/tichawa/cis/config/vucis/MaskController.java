package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.controller.DataSheetController;
import de.tichawa.cis.config.serial.SerialController;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.*;

import java.beans.*;
import java.net.URL;
import java.util.*;
import java.util.stream.*;

public class MaskController extends de.tichawa.cis.config.controller.MaskController<VUCIS> implements PropertyChangeListener {

    private static final List<CIS.LightColor> LIGHT_COLOR_OPTIONS_WITH_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISLightColor)
            .collect(Collectors.toList());
    private static final List<CIS.LightColor> LIGHT_COLOR_OPTIONS_WITHOUT_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISLightColor).filter(c -> !c.isShapeFromShading())
            .collect(Collectors.toList());
    private static final List<CIS.LightColor> LIGHT_COLOR_OPTIONS_COAX_WITHOUT_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISCoaxLightColor).filter(c -> !c.isShapeFromShading())
            .collect(Collectors.toList()); //coax is always without sfs
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITH_COAX = Stream.of(260, 520, 780, 1040).collect(Collectors.toList());
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITHOUT_COAX = Stream.of(260, 520, 780, 1040, 1300, 1560, 1820/*, 2080 (removed for now)*/).collect(Collectors.toList());
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITH_SFS = Stream.of(260, 520, 780, 1040, 1300, 1560, 1820).collect(Collectors.toList());

    private static long lastMinFreq = 0;

    @FXML
    private Label warningSelectedLineRateLabel;
    @FXML
    private ChoiceBox<String> lensTypeChoiceBox;
    @FXML
    private CheckBox lensDofCheckBox;
    @FXML
    private Label lensCodeLabel;
    @FXML
    private CheckBox reducedPixelClockCheckBox;
    @FXML
    private Label cameraLinkInfoLabel;
    @FXML
    private Label lightInfoLabel;
    @FXML
    private ChoiceBox<String> moduloChoiceBox;
    @FXML
    private Label pixelCountLabel;
    @FXML
    private Label lightFrequencyLimitLabel;
    @FXML
    private ChoiceBox<CIS.LightColor> brightFieldLeftChoiceBox;
    @FXML
    private ChoiceBox<CIS.LightColor> coaxChoiceBox;
    @FXML
    private ChoiceBox<CIS.LightColor> brightFieldRightChoiceBox;
    @FXML
    private ChoiceBox<CIS.LightColor> darkFieldLeftChoiceBox;
    @FXML
    private ChoiceBox<CIS.LightColor> darkFieldRightChoiceBox;
    @FXML
    private CheckBox coolingLeftCheckBox;
    @FXML
    private CheckBox coolingRightCheckBox;
    @FXML
    private CheckBox cloudyDayCheckBox;
    @FXML
    private ChoiceBox<String> lightPresetChoiceBox;
    @FXML
    private Label currentLineRatePercentLabel;

    public MaskController() {
        CIS_DATA = new VUCIS();
        CIS_DATA.addObserver(this); // add this as observer to listen for changes to the model
    }

    public enum Phases {
        ONE(1, "One phase (Monochrome)"),
        TWO(2, "Two phases"),
        THREE(3, "Three phases (e.g. RGB)"),
        FOUR(4, "Four phases"),
        FIVE(5, "Five phases"),
        SIX(6, "Six phases");

        private final int phaseCount;
        private final String displayText;

        Phases(int phaseCount, String displayText) {
            this.phaseCount = phaseCount;
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }

        public static Optional<Phases> findByPhaseCount(int phaseCount) {
            return Arrays.stream(Phases.values()).filter(p -> p.phaseCount == phaseCount).findFirst();
        }

        public static Optional<Phases> findByDisplayText(String displayText) {
            return Arrays.stream(Phases.values()).filter(t -> t.displayText.equals(displayText)).findFirst();
        }
    }

    public enum LightPresets {
        MANUAL(CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.NONE, 1, "Manual", false),
        SFS(CIS.LightColor.WHITE_SFS, CIS.LightColor.WHITE_SFS, CIS.LightColor.WHITE_SFS, CIS.LightColor.WHITE_SFS, CIS.LightColor.NONE, 4, "Shape from Shading (White)", false),
        COAX(CIS.LightColor.NONE, CIS.LightColor.NONE, CIS.LightColor.NONE, CIS.LightColor.NONE, CIS.LightColor.RED, 1, "Coax (Red)", false),
        BF_RGB(CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.NONE, 1, "BrightField (Red)", false),
        BF_RGB_S(CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.NONE, 3, "BrightField (RGB)", false),
        CLOUDY_DAY(CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.NONE, 3, "Cloudy Day (RGB)", true),
        DF_RED(CIS.LightColor.NONE, CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.RED, CIS.LightColor.NONE, 1, "DarkField (Red)", false),
        WAFER(CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.RGB_S, 3, "Wafer (RGB strong)", false);

        private final CIS.LightColor leftBrightField, leftDarkField, rightBrightField, rightDarkField, coax;
        private final int phaseCount;
        private final boolean cloudyDay;
        private final String displayText;

        LightPresets(CIS.LightColor leftBrightField, CIS.LightColor leftDarkField, CIS.LightColor rightBrightField, CIS.LightColor rightDarkField, CIS.LightColor coax,
                     int phaseCount, String displayText, boolean cloudyDay) {
            this.leftBrightField = leftBrightField;
            this.leftDarkField = leftDarkField;
            this.rightBrightField = rightBrightField;
            this.rightDarkField = rightDarkField;
            this.coax = coax;
            this.phaseCount = phaseCount;
            this.cloudyDay = cloudyDay;
            this.displayText = displayText;
        }

        public String getDisplayText() {
            return displayText;
        }

        public static Optional<LightPresets> findByDisplayText(String displayText) {
            return Arrays.stream(LightPresets.values()).filter(l -> l.getDisplayText().equals(displayText)).findFirst();
        }
    }

    @Override
    public List<CIS.Resolution> setupResolutions() {
        return VUCIS.getResolutions();
    }

    /**
     * applies the given light preset by setting the corresponding values in the model
     */
    private void applyLightPreset(LightPresets lightPreset) {
        if (lightPreset == LightPresets.MANUAL)
            return; // don't change anything when manual is selected
        // set cloudy day (first, so that all following changes won't reach an illegal state)
        CIS_DATA.setCloudyDay(lightPreset.cloudyDay);
        // set lights in model and view
        CIS_DATA.setCoaxLight(lightPreset.coax); // coax first so left dark field won't reach an illegal state (no coax + left df)
        CIS_DATA.setDarkFieldLeft(lightPreset.leftDarkField);
        CIS_DATA.setBrightFieldLeft(lightPreset.leftBrightField);
        CIS_DATA.setBrightFieldRight(lightPreset.rightBrightField);
        CIS_DATA.setDarkFieldRight(lightPreset.rightDarkField);
        // set phase count in model and view
        CIS_DATA.setPhaseCount(lightPreset.phaseCount);
    }

    /**
     * initializes the light preset choice box (adds items, selects initial item, adds listener for changes)
     */
    private void initLightPresets() {
        //set items of choice box
        lightPresetChoiceBox.getItems().clear();
        lightPresetChoiceBox.getItems().addAll(Arrays.stream(LightPresets.values()).map(LightPresets::getDisplayText).collect(Collectors.toList()));
        //select initial value
        lightPresetChoiceBox.getSelectionModel().select(LightPresets.MANUAL.getDisplayText());
        //add listener to change lights on change
        lightPresetChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            LightPresets selected = LightPresets.findByDisplayText(newValue).orElseThrow(() -> new IllegalArgumentException("selected light preset does not exist"));
            applyLightPreset(selected);
            //set to selected again because a change will set it to manual
            lightPresetChoiceBox.getSelectionModel().select(newValue);
        });
    }

    /**
     * initializes the phases choice box
     */
    private void initPhases() {
        //set items to choice box
        phasesChoiceBox.getItems().clear();
        phasesChoiceBox.getItems().addAll(Arrays.stream(Phases.values()).map(Phases::getDisplayText).collect(Collectors.toList()));
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
     * initializes a single given light choice box. If it is not the coax box, all light options for VUCIS will be present. For Coax only non shape from shading ones.
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
        lightBox.getItems().addAll(isCoaxBox ? LIGHT_COLOR_OPTIONS_COAX_WITHOUT_SFS : LIGHT_COLOR_OPTIONS_WITH_SFS);
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
     * returns the light name corresponding to the given choice box id. The light name will be used as changed property
     *
     * @param choiceBoxId the id of the choice box of the light
     * @return the light (property) name
     */
    private String getLightNameFromChoiceBoxId(String choiceBoxId) {
        switch (choiceBoxId) {
            case "brightFieldLeftChoiceBox":
                return VUCIS.PROPERTY_BRIGHT_FIELD_LEFT;
            case "brightFieldRightChoiceBox":
                return VUCIS.PROPERTY_BRIGHT_FIELD_RIGHT;
            case "darkFieldLeftChoiceBox":
                return VUCIS.PROPERTY_DARK_FIELD_LEFT;
            case "darkFieldRightChoiceBox":
                return VUCIS.PROPERTY_DARK_FIELD_RIGHT;
            case "coaxChoiceBox":
                return VUCIS.PROPERTY_COAX;
            default:
                throw new IllegalArgumentException("unknown choiceBoxId: " + choiceBoxId);
        }
    }

    /**
     * initializes the lights: left/right dark/bright field and coax
     */
    private void initLights() {
        initLight(brightFieldLeftChoiceBox, CIS_DATA.getBrightFieldLeft(), false);
        initLight(brightFieldRightChoiceBox, CIS_DATA.getBrightFieldRight(), false);
        initLight(darkFieldLeftChoiceBox, CIS_DATA.getDarkFieldLeft(), false);
        initLight(darkFieldRightChoiceBox, CIS_DATA.getDarkFieldRight(), false);
        initLight(coaxChoiceBox, CIS_DATA.getCoaxLight(), true);
    }

    /**
     * initializes the cloudy day checkbox
     */
    private void initCloudyDay() {
        cloudyDayCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCloudyDay(newValue));
    }

    /**
     * initializes the cooling checkboxes
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
     * initializes the color and light sources section (light preset, phases, cloudy day, bright and dark fields, coax, cooling)
     */
    private void initColorAndLightSources() {
        initLightPresets();
        initPhases();
        initCloudyDay();
        initLights();
        initCooling();
        updateLightFrequencyLimit();
    }

    /**
     * determines the lens String for the given distance
     */
    private static String getLensStringFromDistanceValue(String distance) {
        switch (distance) {
            case "10mm":
                return "TC54";
            case "23mm":
                return "TC100";
            default:
                throw new IllegalArgumentException("selected lens type does not exist");
        }
    }

    /**
     * determines the String suffix if DOF is selected
     */
    private static String getLensStringSuffixFromDOFValue(boolean isDOF) {
        return isDOF ? " with long DOF" : "";
    }

    /**
     * initializes the optics section (lens type, DOF)
     */
    private void initOptics() {
        VUCIS.LensType defaultLens = VUCIS.LensType.TC54;

        // set initial value
        lensTypeChoiceBox.getSelectionModel().select("10mm"); //not pretty to have this fixed value here... could rework LensType enum to have 10mm as display text and make lens type handling easier
        // listener for lens type choice box that sets the new lens in the model
        lensTypeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            String description = getLensStringFromDistanceValue(newValue);
            description += getLensStringSuffixFromDOFValue(description.contains("TC100") || lensDofCheckBox.isSelected());
            CIS_DATA.setLensType(VUCIS.LensType.findByDescription(description).orElseThrow(() -> new IllegalArgumentException("selected lens does not exist")));
        });
        // set initial value
        lensDofCheckBox.setSelected(defaultLens.isLongDOF());
        // listener for lens DOF checkbox that sets the new lens in the model
        lensDofCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            String description = getLensStringFromDistanceValue(lensTypeChoiceBox.getValue());
            description += getLensStringSuffixFromDOFValue(description.contains("TC100") || newValue);
            CIS_DATA.setLensType(VUCIS.LensType.findByDescription(description).orElseThrow(() -> new IllegalArgumentException("selected lens does not exist")));
        });
        // set initial lens code label
        lensCodeLabel.setText(defaultLens.getCode());
    }

    /**
     * initializes the resolution and scan width section
     */
    private void initResolutionAndScanWidth() {
        // select initial resolution
        resolutionChoiceBox.getSelectionModel().selectFirst();
        // listener for resolution choice box that sets the new resolution in the model
        resolutionChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setSelectedResolution(getResolutions().get(resolutionChoiceBox.getSelectionModel().getSelectedIndex())));
        // set items to scan width choice box
        scanWidthChoiceBox.getItems().clear();
        scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
        // set initial value
        scanWidthChoiceBox.getSelectionModel().select(CIS_DATA.getScanWidth() + " mm");
        // listener for scan width choice box that sets the new scan width in the model
        scanWidthChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && newValue != null)
                CIS_DATA.setScanWidth(Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim()));
        });
    }

    /**
     * initializes the line rate and transport speed section
     */
    private void initLineRateAndTransportSpeed() {
        handleTransportSpeedChange(CIS_DATA.getTransportSpeed());
        updateMaxLineRateChange();
        handleLineRateChange(CIS_DATA.getSelectedLineRate());
        selectedLineRateSlider.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setSelectedLineRate(newValue.intValue()));
    }

    /**
     * initializes the pixel and defect size section by setting the text labels to the initial values
     */
    private void initPixelAndDefectSize() {
        updatePixelAndDefectSize();
    }

    /**
     * initializes the interface section
     */
    private void initInterface() {
        // disable interface choice
        interfaceChoiceBox.getSelectionModel().selectFirst(); // disabled for now (only CameraLink)
        // listener and text output for reduced pixel clock
        reducedPixelClockCheckBox.setText(reducedPixelClockCheckBox.getText() + " (" + (VUCIS.PIXEL_CLOCK_REDUCED / 1000000) + "\u200aMHz)");
        reducedPixelClockCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setReducedPixelClock(newValue));
        reducedPixelClockCheckBox.setSelected(CIS_DATA.isReducedPixelClock());
        // setup choice box for valid mod options
        moduloChoiceBox.getItems().clear();
        moduloChoiceBox.getItems().addAll(VUCIS.VALID_MODS.stream().map(Object::toString).collect(Collectors.toList()));
        moduloChoiceBox.getSelectionModel().select(String.valueOf(CIS_DATA.getMod()));
        moduloChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setMod(Integer.parseInt(newValue)));
    }

    /**
     * initializes all choice boxes and checkboxes
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // initialize color and light sources section (upper box)
        initColorAndLightSources();
        // initialize optics (second box)
        initOptics();
        // initialize resolution and scan width section (left box)
        initResolutionAndScanWidth();
        // initialize pixel and defect size section
        initPixelAndDefectSize();
        // initialize line rate and transport speed
        initLineRateAndTransportSpeed();

        // initialize other stuff
        initInterface();
        updateCameraLinkInfo();
    }

    /**
     * disables/enables the cooling checkboxes
     */
    private void updateCoolingCheckboxes() {
        coolingLeftCheckBox.setDisable(CIS_DATA.hasCoolingLeft());
        coolingRightCheckBox.setDisable(CIS_DATA.hasCoolingRight());
    }

    /**
     * handles an observed property change from the model and updates the GUI accordingly
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("observed change for " + evt.getPropertyName() + " to " + evt.getNewValue());
        switch (evt.getPropertyName()) {
            // lights
            case "leftDarkField":
                selectLightChoiceBox(darkFieldLeftChoiceBox, (CIS.LightColor) evt.getNewValue());
                updateScanWidthOptions();
                setLightPresetToManual();
                updateLightInfo();
                updateLightFrequencyLimit();
                return;
            case "leftBrightField":
                selectLightChoiceBox(brightFieldLeftChoiceBox, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                updateScanWidthOptions();
                updateLightInfo();
                updateLightFrequencyLimit();
                return;
            case "coaxLight":
                handleCoaxChange((CIS.LightColor) evt.getOldValue(), (CIS.LightColor) evt.getNewValue());
                selectLightChoiceBox(coaxChoiceBox, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                updateLightInfo();
                updateLightFrequencyLimit();
                return;
            case "rightBrightField":
                selectLightChoiceBox(brightFieldRightChoiceBox, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                updateScanWidthOptions();
                updateLightInfo();
                updateLightFrequencyLimit();
                return;
            case "rightDarkField":
                selectLightChoiceBox(darkFieldRightChoiceBox, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                updateScanWidthOptions();
                updateLightInfo();
                updateLightFrequencyLimit();
                return;
            case "phaseCount":
                Phases selectedPhase = Phases.findByPhaseCount((int) evt.getNewValue()).orElseThrow(() -> new IllegalArgumentException("selected phase count not found"));
                handlePhasesChange(selectedPhase);
                setLightPresetToManual();
                updateCameraLinkInfo();
                updateLightFrequencyLimit();
                return;
            case "cloudyDay":
                cloudyDayCheckBox.setSelected((boolean) evt.getNewValue());
                updateDarkFieldChoiceBoxes((boolean) evt.getNewValue());
                setLightPresetToManual();
                updateLightFrequencyLimit();
                return;
            case "coolingLeft":
                coolingLeftCheckBox.setSelected((boolean) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "coolingRight":
                coolingRightCheckBox.setSelected((boolean) evt.getNewValue());
                setLightPresetToManual();
                return;
            // optics
            case "lensType":
                handleLensChange((VUCIS.LensType) evt.getNewValue());
                updateLightFrequencyLimit();
                return;
            // resolution and scan width
            case "resolution":
                handleResolutionChange((CIS.Resolution) evt.getNewValue());
                updateCameraLinkInfo();
                updateLightFrequencyLimit();
                return;
            case "scanWidth":
                scanWidthChoiceBox.getSelectionModel().select(evt.getNewValue() + " mm");
                updateCameraLinkInfo();
                return;
            // line rate and transport speed
            case "lineRate":
                handleLineRateChange((int) evt.getNewValue());
                updateCameraLinkInfo();
                updateSelectedLineRateWarning();
                updateLightFrequencyLimit();
                return;
            case "transportSpeed":
                handleTransportSpeedChange((int) evt.getNewValue());
                return;
            // interface
            case "reducedPixelClock":
                reducedPixelClockCheckBox.setSelected((boolean) evt.getNewValue());
                updateCameraLinkInfo();
                return;
            // mod
            case "mod":
                moduloChoiceBox.setValue(String.valueOf((int) evt.getNewValue()));
                updateCameraLinkInfo();
                return;
        }
    }

    /**
     * sets the light preset to manual (for changes from a light preset)
     */
    private void setLightPresetToManual() {
        lightPresetChoiceBox.getSelectionModel().select(LightPresets.MANUAL.getDisplayText());
    }

    private void handleTransportSpeedChange(int transportSpeed) {
        double transportSpeedByThousands = transportSpeed / 1000.0;
        speedmmsLabel.setText(CIS.round(transportSpeedByThousands, 3) + " mm/s");
        speedmsLabel.setText(CIS.round(transportSpeedByThousands / 1000, 3) + " m/s");
        speedmminLabel.setText(CIS.round(transportSpeedByThousands * 0.06, 3) + " m/min");
        speedipsLabel.setText(CIS.round(transportSpeedByThousands * 0.03937, 3) + " ips");
    }

    /**
     * calculates speeds depending on the given line rate and sets labels accordingly
     */
    private void handleLineRateChange(int lineRate) {
        currentLineRateLabel.setText(lineRate / 1000.0 + " kHz");
        currentLineRatePercentLabel.setText(Math.round(lineRate / CIS_DATA.getMaxLineRate() * 100) + " %");
    }

    /**
     * calculates the displayed values and sets them to the corresponding texts
     */
    private void handleResolutionChange(CIS.Resolution resolution) {
        // cannot easily set resolution in GUI since this would require mapping the resolution values to the fxml texts
        // however, since resolution only changes by user input, this does not really matter for now

        // set output texts
        updateMaxLineRateChange();
        updatePixelAndDefectSize();
    }

    /**
     * updates the pixel and defect size text labels
     */
    private void updatePixelAndDefectSize() {
        pixelSizeLabel.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
        defectSizeLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
    }

    /**
     * handles a change to the max line rate: sets the new max value of the slider and updates the text label
     */
    private void updateMaxLineRateChange() {
        maxLineRateLabel.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
        selectedLineRateSlider.setMax(CIS_DATA.getMaxLineRate());
        selectedLineRateSlider.setValue(CIS_DATA.getMaxLineRate());
        selectedLineRateSlider.setBlockIncrement(CIS_DATA.getMaxLineRate() / 100); // we can choose increments of 1%
    }

    /**
     * handles a lens change by setting the corresponding boxes
     */
    private void handleLensChange(VUCIS.LensType lens) {
        switch (lens) {
            case TC54L: // 10mm with DOF
                setLensBoxes("10mm", true);
                break;
            case TC54:
                setLensBoxes("10mm", false);
                break;
            case TC80L:
            case TC100L:
                setLensBoxes("23mm", true);
                break;
            case TC80:
                setLensBoxes("23mm", false);
                break;
            default:
                throw new IllegalArgumentException("lens does not exist");
        }
        // disable the checkbox if there is only one version
        lensDofCheckBox.setDisable(!lens.hasShortDOFVersion());
        lensCodeLabel.setText(lens.getCode());
    }

    /**
     * sets the lens boxes to the given values
     */
    private void setLensBoxes(String distance, boolean DOF) {
        lensTypeChoiceBox.getSelectionModel().select(distance);
        lensDofCheckBox.setSelected(DOF);
    }

    /**
     * updates the dark field checkboxes depending on the cloudy day. Left dark field will only get enabled if there is no cloudy day and no coax
     */
    private void updateDarkFieldChoiceBoxes(boolean cloudyDay) {
        // right dark field disabled <=> there is cloudy day
        darkFieldRightChoiceBox.setDisable(cloudyDay);
        // left dark field disabled when cloudy day or coax
        darkFieldLeftChoiceBox.setDisable(cloudyDay || CIS_DATA.hasCoax());
    }

    /**
     * handles changes to the phase count in the model: shows the corresponding text in the choice box and adjusts the line rate
     */
    private void handlePhasesChange(Phases selectedPhase) {
        // set choice box selection
        phasesChoiceBox.getSelectionModel().select(selectedPhase.getDisplayText());
        // adjust line rate outputs
        updateMaxLineRateChange();
    }

    /**
     * handles a coax light change by
     * - disabling left dark field choice box (no coax + left dark field)
     * - setting the light color choices for left bright field to ones without shape from shading (no left sided shape from shading + coax)
     * - setting the scan width choices to valid ones for coax
     * or resets these changes if coax is deselected
     */
    private void handleCoaxChange(CIS.LightColor oldValue, CIS.LightColor newValue) {
        boolean hasCoax = newValue != CIS.LightColor.NONE;
        boolean hadCoax = oldValue != CIS.LightColor.NONE;
        if (hasCoax != hadCoax) {//only do something if we switch from coax to non-coax or other way around
            //clear scan width and left bright field because these options will be changed
            scanWidthChoiceBox.getItems().clear();
            brightFieldLeftChoiceBox.getItems().clear();
            if (hasCoax) { //coax selected
                //set items of scan width choice box to ones without coax
                scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
                //set items of left bf to ones without sfs (no coax and left sided sfs)
                brightFieldLeftChoiceBox.getItems().addAll(LIGHT_COLOR_OPTIONS_WITHOUT_SFS);
            } else { // coax deselected
                //set items of scan width choice box to ones with coax
                if (CIS_DATA.isShapeFromShading()) {
                    scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_SFS.stream().map(o -> o + " mm").collect(Collectors.toList()));
                } else {
                    scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
                }
                //set items of left bf to ones with sfs
                brightFieldLeftChoiceBox.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
            } //in either case:
            //enable/disable left dark field (no dark field + coax)
            darkFieldLeftChoiceBox.setDisable(hasCoax || CIS_DATA.isCloudyDay());
            //select current values
            selectLightChoiceBox(brightFieldLeftChoiceBox, CIS_DATA.getBrightFieldLeft());
            scanWidthChoiceBox.getSelectionModel().select(CIS_DATA.getScanWidth() + " mm");
        }
    }

    /**
     * selects the given light color for the given choice box.
     * Also calls {@link #updateCoolingCheckboxes()} to enable/disable the checkboxes after light change
     * and {@link #updateLensTypeChoiceBox()} to enable/disable lens type changes if there is shape from shading
     */
    private void selectLightChoiceBox(ChoiceBox<CIS.LightColor> box, CIS.LightColor lightColor) {
        box.getSelectionModel().select(lightColor);
        updateCoolingCheckboxes();
        updateLensTypeChoiceBox();
    }

    /**
     * updates scan width options depending on current shape from shading selection
     */
    private void updateScanWidthOptions() {
        if (!CIS_DATA.hasCoax()) { // with coax already handled
            //sfs ->
            scanWidthChoiceBox.getItems().clear();
            if (CIS_DATA.isShapeFromShading()) {
                scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_SFS.stream().map(o -> o + " mm").collect(Collectors.toList()));
            } else {
                scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
            }
            // set initial value
            scanWidthChoiceBox.getSelectionModel().select(CIS_DATA.getScanWidth() + " mm");
        }
    }

    /**
     * enables/disables lens type choice box depending on whether there is shape from shading (only 10mm distance allowed)
     */
    private void updateLensTypeChoiceBox() {
        lensTypeChoiceBox.setDisable(CIS_DATA.isShapeFromShading());
    }

    /**
     * updates the camera link info text.
     * Calculates the camera link configuration and displays needed boards, cables and number of used ports.
     * Also updates the total pixel count.
     */
    private void updateCameraLinkInfo() {
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
     * updates the light info textbox that shows a warning if the needed light channels are >10.
     * Also shows options to combine bright or dark fields if possible.
     */
    private void updateLightInfo() {
        if (CIS_DATA.getLedLines() < 10) { // smaller than 10 is fine, 10 might not be fine if it's actually more
            lightInfoLabel.setText("");
            return;
        } //else 10 or more
        int ledLines = CIS_DATA.getLights().chars().mapToObj(c -> CIS.LightColor.findByCode((char) c))
                .filter(Optional::isPresent).map(Optional::get)
                .mapToInt(VUCIS::getLValue).sum();
        if (ledLines == 10) { // might be actually 10 and 10 is fine
            lightInfoLabel.setText("");
            return;
        } //else more than 10
        StringBuilder displayText = new StringBuilder("Too many LED channels. ");
        //might be able to combine bright fields
        if (CIS_DATA.getBrightFieldLeft() == CIS_DATA.getBrightFieldRight() && CIS_DATA.getBrightFieldLeft() != CIS.LightColor.NONE
                && ledLines - VUCIS.getLValue(CIS_DATA.getBrightFieldLeft()) <= 10)
            displayText.append("\tBright Field channels from both sides might be combined.");
        //might be able to combine dark fields
        if (CIS_DATA.getDarkFieldLeft() == CIS_DATA.getDarkFieldRight() && CIS_DATA.getDarkFieldLeft() != CIS.LightColor.NONE
                && ledLines - VUCIS.getLValue(CIS_DATA.getDarkFieldLeft()) <= 10)
            displayText.append("\tDark Field channels from both sides might be combined.");
        //might be able to combine all
        //TODO check condition on when we can combine all 88XXX ? 88X88 ? 88XAA ? ...
        lightInfoLabel.setText(displayText.toString());
    }

    /**
     * updates the light frequency limit label
     */
    private void updateLightFrequencyLimit() {
        long minFreq = Math.round(1000 * CIS_DATA.getMinFreq(CIS_DATA.calculate())) / 1000;
        lastMinFreq = minFreq;
        lightFrequencyLimitLabel.setText(minFreq < 0 // if < 0 there are values missing in database -> give error msg
                ? Util.getString("missing photo values") + "\n"
                : "~" + minFreq + "\u200akHz\n"
        );
        updateSelectedLineRateWarning();
    }

    /**
     * updates the selected line rate warning label. The warning is shown if the minimum light frequency is smaller than twice the selected line rate.
     */
    private void updateSelectedLineRateWarning() {
        double lineRate = Math.round(CIS_DATA.getSelectedLineRate() / 100.0) / 10.0;
        if (lastMinFreq >= 0 && lastMinFreq < lineRate) // if missing values in database lastMinFreq is <0 -> don't show warning as this would overlap with other warning
            warningSelectedLineRateLabel.setText(Util.getString("warning minfreq linerate").replace('\n', ' '));
        else
            warningSelectedLineRateLabel.setText("");
    }

    /**
     * returns the datasheet controller for VUCIS
     *
     * @return a new {@link de.tichawa.cis.config.vucis.DataSheetController} object
     */
    @Override
    protected DataSheetController getNewDatasheetController() {
        return new de.tichawa.cis.config.vucis.DataSheetController();
    }

    /**
     * handles the serial button press by opening the serial settings window
     */
    @FXML
    private void handleSerial() {
        Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("Serial.fxml", "Serial Interface parameters");
        ((SerialController) stageWithLoader.getValue().getController()).initialize(CIS_DATA);
        stageWithLoader.getKey().show();
    }
}