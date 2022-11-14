package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.CIS;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.beans.*;
import java.net.URL;
import java.util.*;
import java.util.stream.*;

public class MaskController extends de.tichawa.cis.config.MaskController<VUCIS> implements PropertyChangeListener {

    private static final List<String> LIGHT_COLOR_OPTIONS_WITH_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISLightColor)
            .map(CIS.LightColor::getDescription).collect(Collectors.toList());
    private static final List<String> LIGHT_COLOR_OPTIONS_WITHOUT_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISLightColor).filter(c -> !c.isShapeFromShading())
            .map(CIS.LightColor::getDescription).collect(Collectors.toList());
    private static final List<String> LIGHT_COLOR_OPTIONS_COAX_WITHOUT_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISCoaxLightColor).filter(c -> !c.isShapeFromShading())
            .map(CIS.LightColor::getDescription).collect(Collectors.toList()); //coax is always without sfs
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITH_COAX = Stream.of(260, 520, 780, 1040).collect(Collectors.toList());
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITHOUT_COAX = Stream.of(260, 520, 780, 1040, 1300, 1560, 1820, 2080).collect(Collectors.toList());

    @FXML
    public ChoiceBox<String> LensType;
    @FXML
    public CheckBox LensDOF;
    @FXML
    private ChoiceBox<String> BrightFieldLeft;
    @FXML
    private ChoiceBox<String> Coax;
    @FXML
    private ChoiceBox<String> BrightFieldRight;
    @FXML
    private ChoiceBox<String> DarkFieldLeft;
    @FXML
    private ChoiceBox<String> DarkFieldRight;
    @FXML
    private CheckBox CoolingLeft;
    @FXML
    private CheckBox CoolingRight;
    @FXML
    private CheckBox CloudyDay;
    @FXML
    private ChoiceBox<String> LightPreset;

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
        CLOUDY_DAY(CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.NONE, 3, "Cloudy Day (RGB)", true);

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
        CIS_DATA.setLeftDarkField(lightPreset.leftDarkField);
        CIS_DATA.setLeftBrightField(lightPreset.leftBrightField);
        CIS_DATA.setRightBrightField(lightPreset.rightBrightField);
        CIS_DATA.setRightDarkField(lightPreset.rightDarkField);
        // set phase count in model and view
        CIS_DATA.setPhaseCount(lightPreset.phaseCount);
    }

    /**
     * initializes the light preset choice box (adds items, selects initial item, adds listener for changes)
     */
    private void initLightPresets() {
        //set items of choice box
        LightPreset.getItems().clear();
        LightPreset.getItems().addAll(Arrays.stream(LightPresets.values()).map(LightPresets::getDisplayText).collect(Collectors.toList()));
        //select initial value
        LightPreset.getSelectionModel().select(LightPresets.MANUAL.getDisplayText());
        //add listener to change lights on change
        LightPreset.valueProperty().addListener((observable, oldValue, newValue) -> {
            LightPresets selected = LightPresets.findByDisplayText(newValue).orElseThrow(() -> new IllegalArgumentException("selected light preset does not exist"));
            applyLightPreset(selected);
            //set to selected again because a change will set it to manual
            LightPreset.getSelectionModel().select(newValue);
        });
    }

    /**
     * initializes the phases choice box
     */
    private void initPhases() {
        //set items to choice box
        Color.getItems().clear();
        Color.getItems().addAll(Arrays.stream(Phases.values()).map(Phases::getDisplayText).collect(Collectors.toList()));
        //select initial value
        Phases initialPhase = Phases.findByPhaseCount(CIS_DATA.getPhaseCount()).orElseThrow(() -> new IllegalArgumentException("selected phases do not exist"));
        Color.getSelectionModel().select(initialPhase.getDisplayText());
        //add listener to set phase count on changes
        Color.valueProperty().addListener((property, oldValue, newValue) -> {
            Phases selectedPhase = Phases.findByDisplayText(newValue).orElseThrow(() -> new IllegalArgumentException("selected phases do not exist"));
            //set phase count in model when choice box value changed
            CIS_DATA.setPhaseCount(selectedPhase.phaseCount);
        });
    }

    /**
     * initializes a single given light choice box. If it is not the coax box, all light options for VUCIS will be present. For Coax only non shape from shading ones.
     */
    private void initLight(ChoiceBox<String> lightBox, String initialValue, boolean isCoaxBox) {
        //set items to choice box
        lightBox.getItems().clear();
        lightBox.getItems().addAll(isCoaxBox ? LIGHT_COLOR_OPTIONS_COAX_WITHOUT_SFS : LIGHT_COLOR_OPTIONS_WITH_SFS);
        // select initial value
        lightBox.getSelectionModel().select(initialValue);
        //add listener to handle light changes
        lightBox.valueProperty().addListener((property, oldValue, newValue) -> {
            if (newValue != null) { // when clearing and resetting choice boxes this may be null
                //set light color in model on light change
                String lightName = ((ChoiceBox) ((ObjectProperty) property).getBean()).getId();
                CIS_DATA.setLightColor(lightName,
                        CIS.LightColor.findByDescription(newValue).orElseThrow(() -> new IllegalArgumentException("selected light color for light " + lightName + " does not exist: " + newValue)));
            }
        });
    }

    /**
     * initializes the lights: left/right dark/bright field and coax
     */
    private void initLights() {
        initLight(BrightFieldLeft, CIS_DATA.getLeftBrightField().getDescription(), false);
        initLight(BrightFieldRight, CIS_DATA.getRightBrightField().getDescription(), false);
        initLight(DarkFieldLeft, CIS_DATA.getLeftDarkField().getDescription(), false);
        initLight(DarkFieldRight, CIS_DATA.getRightDarkField().getDescription(), false);
        initLight(Coax, CIS_DATA.getCoaxLight().getDescription(), true);
    }

    /**
     * initializes the cloudy day checkbox
     */
    private void initCloudyDay() {
        CloudyDay.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCloudyDay(newValue));
    }

    /**
     * initializes the cooling checkboxes
     */
    private void initCooling() {
        CoolingLeft.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCoolingLeft(newValue));
        CoolingRight.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCoolingRight(newValue));
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
    }

    /**
     * determines the lens String for the given distance
     */
    private static String getLensStringFromDistanceValue(String distance) {
        switch (distance) {
            case "10mm":
                return "TC54";
            case "23mm":
                return "TC80";
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
        // set initial value
        LensType.getSelectionModel().select("10mm"); //not pretty to have this fixed value here... could rework LensType enum to have 10mm as display text and make lens type handling easier
        // listener for lens type choice box that sets the new lens in the model
        LensType.valueProperty().addListener((observable, oldValue, newValue) -> {
            String description = getLensStringFromDistanceValue(newValue);
            description += getLensStringSuffixFromDOFValue(LensDOF.isSelected());
            CIS_DATA.setLensType(VUCIS.LensType.findByDescription(description).orElseThrow(() -> new IllegalArgumentException("selected lens does not exist")));
        });
        // set initial value
        LensDOF.setSelected(false); //not pretty to have this fixed value here... (see comment above)
        // listener for lens DOF checkbox that sets the new lens in the model
        LensDOF.selectedProperty().addListener((observable, oldValue, newValue) -> {
            String description = getLensStringFromDistanceValue(LensType.getValue());
            description += getLensStringSuffixFromDOFValue(newValue);
            CIS_DATA.setLensType(VUCIS.LensType.findByDescription(description).orElseThrow(() -> new IllegalArgumentException("selected lens does not exist")));
        });
    }

    /**
     * initializes the resolution and scan width section
     */
    private void initResolutionAndScanWidth() {
        // select initial resolution
        Resolution.getSelectionModel().selectFirst();
        // listener for resolution choice box that sets the new resolution in the model
        Resolution.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setSelectedResolution(getResolutions().get(Resolution.getSelectionModel().getSelectedIndex())));
        // set items to scan width choice box
        ScanWidth.getItems().clear();
        ScanWidth.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
        // set initial value
        ScanWidth.getSelectionModel().select(CIS_DATA.getScanWidth() + " mm");
        // listener for scan width choice box that sets the new scan width in the model
        ScanWidth.valueProperty().addListener((observable, oldValue, newValue) -> {
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
        SelLineRate.valueProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setSelectedLineRate(newValue.intValue()));
    }

    /**
     * initializes the pixel and defect size section by setting the text labels to the initial values
     */
    private void initPixelAndDefectSize() {
        updatePixelAndDefectSize();
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
        Cooling.setVisible(false); //make invisible as this is not used for VUCIS for now (but comes from config/MaskController)
        Interface.getSelectionModel().selectFirst(); // disabled for now (only CameraLink)
    }

    /**
     * disables/enables the cooling checkboxes
     */
    private void updateCoolingCheckboxes() {
        CoolingLeft.setDisable(CIS_DATA.hasCoolingLeft());
        CoolingRight.setDisable(CIS_DATA.hasCoolingRight());
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
                selectLightChoiceBox(DarkFieldLeft, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "leftBrightField":
                selectLightChoiceBox(BrightFieldLeft, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "coaxLight":
                handleCoaxChange((CIS.LightColor) evt.getOldValue(), (CIS.LightColor) evt.getNewValue());
                selectLightChoiceBox(Coax, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "rightBrightField":
                selectLightChoiceBox(BrightFieldRight, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "rightDarkField":
                selectLightChoiceBox(DarkFieldRight, (CIS.LightColor) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "phaseCount":
                Phases selectedPhase = Phases.findByPhaseCount((int) evt.getNewValue()).orElseThrow(() -> new IllegalArgumentException("selected phase count not found"));
                handlePhasesChange(selectedPhase);
                setLightPresetToManual();
                return;
            case "cloudyDay":
                CloudyDay.setSelected((boolean) evt.getNewValue());
                updateDarkFieldChoiceBoxes((boolean) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "coolingLeft":
                CoolingLeft.setSelected((boolean) evt.getNewValue());
                setLightPresetToManual();
                return;
            case "coolingRight":
                CoolingRight.setSelected((boolean) evt.getNewValue());
                setLightPresetToManual();
                return;
            // optics
            case "lensType":
                handleLensChange((VUCIS.LensType) evt.getNewValue());
                return;
            // resolution and scan width
            case "resolution":
                handleResolutionChange((CIS.Resolution) evt.getNewValue());
                return;
            case "lineRate":
                handleLineRateChange((int) evt.getNewValue());
                return;
            case "transportSpeed":
                handleTransportSpeedChange((int) evt.getNewValue());
                return;
        }
    }

    /**
     * sets the light preset to manual (for changes from a light preset)
     */
    private void setLightPresetToManual() {
        LightPreset.getSelectionModel().select(LightPresets.MANUAL.getDisplayText());
    }

    private void handleTransportSpeedChange(int transportSpeed) {
        double transportSpeedByThousands = transportSpeed / 1000.0;
        Speedmms.setText(CIS.round(transportSpeedByThousands, 3) + " mm/s");
        Speedms.setText(CIS.round(transportSpeedByThousands / 1000, 3) + " m/s");
        Speedmmin.setText(CIS.round(transportSpeedByThousands * 0.06, 3) + " m/min");
        Speedips.setText(CIS.round(transportSpeedByThousands * 0.03937, 3) + " ips");
    }

    /**
     * calculates speeds depending on the given line rate and sets labels accordingly
     */
    private void handleLineRateChange(int lineRate) {
        CurrLineRate.setText(lineRate / 1000.0 + " kHz");
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
        PixelSize.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
        DefectSize.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
    }

    /**
     * handles a change to the max line rate: sets the new max value of the slider and updates the text label
     */
    private void updateMaxLineRateChange() {
        MaxLineRate.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
        SelLineRate.setMax(CIS_DATA.getMaxLineRate());
        SelLineRate.setValue(CIS_DATA.getMaxLineRate());
        SelLineRate.setBlockIncrement(CIS_DATA.getMaxLineRate() / 100); // we can choose increments of 1%
    }

    /**
     * handles a lens change by setting the corresponding boxes
     */
    private void handleLensChange(VUCIS.LensType lens) {
        switch (lens) {
            case TC54L: // 10mm with DOF
                setLensBoxes("10mm", true);
                return;
            case TC54:
                setLensBoxes("10mm", false);
                return;
            case TC80L:
                setLensBoxes("23mm", true);
                return;
            case TC80:
                setLensBoxes("23mm", false);
                return;
            default:
                throw new IllegalArgumentException("lens does not exist");
        }
    }

    /**
     * sets the lens boxes to the given values
     */
    private void setLensBoxes(String distance, boolean DOF) {
        LensType.getSelectionModel().select(distance);
        LensDOF.setSelected(DOF);
    }

    /**
     * updates the dark field checkboxes depending on the cloudy day. Left dark field will only get enabled if there is no cloudy day and no coax
     */
    private void updateDarkFieldChoiceBoxes(boolean cloudyDay) {
        // right dark field disabled <=> there is cloudy day
        DarkFieldRight.setDisable(cloudyDay);
        // left dark field disabled when cloudy day or coax
        DarkFieldLeft.setDisable(cloudyDay || CIS_DATA.hasCoax());
    }

    /**
     * handles changes to the phase count in the model: shows the corresponding text in the choice box and adjusts the line rate
     */
    private void handlePhasesChange(Phases selectedPhase) {
        // set choice box selection
        Color.getSelectionModel().select(selectedPhase.getDisplayText());
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
            ScanWidth.getItems().clear();
            BrightFieldLeft.getItems().clear();
            if (hasCoax) { //coax selected
                //set items of scan width choice box to ones without coax
                ScanWidth.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
                //set items of left bf to ones without sfs (no coax and left sided sfs)
                BrightFieldLeft.getItems().addAll(LIGHT_COLOR_OPTIONS_WITHOUT_SFS);
            } else { // coax deselected
                //set items of scan width choice box to ones with coax
                ScanWidth.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
                //set items of left bf to ones with sfs
                BrightFieldLeft.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
            } //in either case:
            //enable/disable left dark field (no dark field + coax)
            DarkFieldLeft.setDisable(hasCoax);
            //select current values
            selectLightChoiceBox(BrightFieldLeft, CIS_DATA.getLeftBrightField());
            ScanWidth.getSelectionModel().select(CIS_DATA.getScanWidth() + " mm");
        }
    }

    /**
     * selects the given light color for the given choice box.
     * Also calls {@link #updateCoolingCheckboxes()} to enable/disable the checkboxes after light change
     * and {@link #updateLensTypeChoiceBox()} to enable/disable lens type changes if there is shape from shading
     */
    private void selectLightChoiceBox(ChoiceBox<String> box, CIS.LightColor lightColor) {
        box.getSelectionModel().select(lightColor.getDescription());
        updateCoolingCheckboxes();
        updateLensTypeChoiceBox();
    }

    /**
     * enables/disables lens type choice box depending on whether there is shape from shading (only 10mm distance allowed)
     */
    private void updateLensTypeChoiceBox() {
        LensType.setDisable(CIS_DATA.isShapeFromShading());
    }
}