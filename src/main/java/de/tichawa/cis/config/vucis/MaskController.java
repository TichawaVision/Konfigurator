package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.controller.DataSheetController;
import de.tichawa.cis.config.controller.*;
import de.tichawa.cis.config.serial.SerialController;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.stream.*;

import static de.tichawa.cis.config.CisWith5Lights.*;

public class MaskController extends MaskControllerCisWith5Lights<VUCIS> {
    private static final List<CIS.LightColor> LIGHT_COLOR_OPTIONS_COAX_WITHOUT_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISCoaxLightColor).filter(c -> !c.isShapeFromShading())
            .collect(Collectors.toList()); //coax is always without sfs
    private static final List<CIS.LightColor> LIGHT_COLOR_OPTIONS_WITHOUT_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISLightColor).filter(c -> !c.isShapeFromShading())
            .collect(Collectors.toList());
    private static final List<CIS.LightColor> LIGHT_COLOR_OPTIONS_WITH_SFS = Stream.of(CIS.LightColor.values()).filter(VUCIS::isVUCISLightColor)
            .collect(Collectors.toList());
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITHOUT_COAX = Arrays.asList(260, 520, 780, 1040, 1300, 1560, 1820/*, 2080 (removed for now)*/);
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITH_COAX = Arrays.asList(260, 520, 780, 1040);
    private static final List<Integer> SCAN_WIDTH_OPTIONS_WITH_SFS = Arrays.asList(260, 520, 780, 1040, 1300, 1560, 1820);
    private static long lastMinFreq = 0;
    @FXML
    protected Label lightFrequencyLimitLabel;
    @FXML
    protected ChoiceBox<String> lightPresetChoiceBox;
    @FXML
    protected Label warningSelectedLineRateLabel;

    public MaskController() {
        CIS_DATA = new VUCIS();
        CIS_DATA.addObserver(this); // add this as observer to listen for changes to the model
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

    @Override
    protected List<CIS.LightColor> getLightColorOptions() {
        return LIGHT_COLOR_OPTIONS_WITH_SFS;
    }

    @Override
    protected List<CIS.LightColor> getLightColorOptionsCoax() {
        return LIGHT_COLOR_OPTIONS_COAX_WITHOUT_SFS;
    }

    @Override
    protected List<Integer> getScanWidthOptions() {
        return SCAN_WIDTH_OPTIONS_WITHOUT_COAX;
    }

    @Override
    protected void handleCloudyDayChange(boolean newValue) {
        super.handleCloudyDayChange(newValue);
        updateDarkFieldChoiceBoxes(newValue);
        setLightPresetToManual();
        updateLightFrequencyLimit();
    }

    /**
     * handles a coax light change by
     * - disabling left dark field choice box (no coax + left dark field)
     * - setting the light color choices for left bright field to ones without shape from shading (no left sided shape from shading + coax)
     * - setting the scan width choices to valid ones for coax
     * or resets these changes if coax is deselected
     */
    protected void handleCoaxChange(CIS.LightColor oldValue, CIS.LightColor newValue) {
        boolean hasCoax = newValue != CIS.LightColor.NONE;
        boolean hadCoax = oldValue != CIS.LightColor.NONE;
        if (hasCoax != hadCoax) {//only do something if we switch from coax to non-coax or other way around
            //clear scan width and left bright field because these options will be changed
            scanWidthChoiceBox.getItems().clear();
            brightFieldLeftChoiceBox.getItems().clear();
            if (hasCoax) { //coax selected
                //set items of scan width choice box to ones without coax
                scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_COAX);
                //set items of left bf to ones without sfs (no coax and left sided sfs)
                brightFieldLeftChoiceBox.getItems().addAll(LIGHT_COLOR_OPTIONS_WITHOUT_SFS);
            } else { // coax deselected
                //set items of scan width choice box to ones with coax
                if (CIS_DATA.isShapeFromShading()) {
                    scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_SFS);
                } else {
                    scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX);
                }
                //set items of left bf to ones with sfs
                brightFieldLeftChoiceBox.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
            } //in either case:
            //enable/disable left dark field (no dark field + coax)
            darkFieldLeftChoiceBox.setDisable(hasCoax || CIS_DATA.isCloudyDay());
            //select current values
            selectLightChoiceBox(brightFieldLeftChoiceBox, CIS_DATA.getBrightFieldLeft());
            scanWidthChoiceBox.getSelectionModel().select((Integer) CIS_DATA.getScanWidth());
        }
    }


    /**
     * initializes the color and light sources section (light preset, phases, cloudy day, bright and dark fields, coax, cooling)
     */
    protected void initColorAndLightSources() {
        super.initColorAndLightSources();
        initLightPresets();
        updateLightFrequencyLimit();
    }

    /**
     * initializes the optics section (lens type, DOF)
     */
    protected void initOptics() {
        lensTypeChoiceBox.getItems().clear();
        lensTypeChoiceBox.getItems().addAll(Arrays.stream(VUCIS.LensType.values()).distinct().map(VUCIS.LensType::getWorkingDistanceString).collect(Collectors.toSet()));
        // set initial value
        lensTypeChoiceBox.getSelectionModel().select(CIS_DATA.getLensType().getWorkingDistanceString());
        // listener for lens type choice box that sets the new lens in the model
        lensTypeChoiceBox.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setLensType(VUCIS.LensType.findLens(newValue, lensDofCheckBox.isSelected())
                        .orElseThrow(() -> new IllegalStateException("selected lens does not exist"))));
        // set initial value
        lensDofCheckBox.setSelected(CIS_DATA.getLensType().isLongDOF());
        // listener for lens DOF checkbox that sets the new lens in the model
        lensDofCheckBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setLensType(VUCIS.LensType.findLens(lensTypeChoiceBox.getValue(), newValue)
                        .orElseThrow(() -> new IllegalStateException("selected lens does not exist"))));
        // set initial lens code label
        lensCodeLabel.setText(CIS_DATA.getLensType().getCode());
    }

    @Override
    protected boolean isValidPhaseOption(MaskControllerCisWith5Lights.Phases phase) {
        return true; //all phases valid for VUCIS
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        switch (evt.getPropertyName()) {
            case PROPERTY_DARK_FIELD_LEFT:
            case PROPERTY_BRIGHT_FIELD_LEFT:
            case PROPERTY_BRIGHT_FIELD_RIGHT:
            case PROPERTY_DARK_FIELD_RIGHT:
                updateScanWidthOptions();
                setLightPresetToManual();
                updateLightInfo();
                updateLightFrequencyLimit();
                break;
            case PROPERTY_COAX:
                setLightPresetToManual();
                updateLightInfo();
                updateLightFrequencyLimit();
                break;
            case PROPERTY_PHASE_COUNT:
                setLightPresetToManual();
                updateLightFrequencyLimit();
                break;
            case PROPERTY_COOLING_LEFT:
            case PROPERTY_COOLING_RIGHT:
                setLightPresetToManual();
                break;
            case PROPERTY_LENS_TYPE:
                handleLensChange((VUCIS.LensType) evt.getNewValue());
                updateLightFrequencyLimit();
                break;
            case PROPERTY_RESOLUTION:
                updateLightFrequencyLimit();
                break;
            case PROPERTY_LINE_RATE:
                updateSelectedLineRateWarning();
                updateLightFrequencyLimit();
                break;
        }
    }

    /**
     * selects the given light color for the given choice box.
     * Also enables/disables the checkboxes after light change
     * and calls {@link #updateLensTypeChoiceBox()} to handle lens type changes if there is shape from shading
     */
    protected void selectLightChoiceBox(ChoiceBox<CIS.LightColor> box, CIS.LightColor lightColor) {
        super.selectLightChoiceBox(box, lightColor);
        updateLensTypeChoiceBox();
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

    @Override
    public List<CIS.Resolution> setupResolutions() {
        return VUCIS.getResolutions();
    }

    /**
     * handles a lens change by setting the corresponding boxes
     */
    private void handleLensChange(VUCIS.LensType lens) {
        setLensBoxes(lens.getWorkingDistanceString(), lens.isLongDOF());
        // disable the checkbox if there is only one version
        lensDofCheckBox.setDisable(!lens.hasShortDOFVersion());
        lensCodeLabel.setText(lens.getCode());
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
     * sets the lens boxes to the given values
     */
    private void setLensBoxes(String distance, boolean DOF) {
        lensTypeChoiceBox.getSelectionModel().select(distance);
        lensDofCheckBox.setSelected(DOF);
    }

    /**
     * sets the light preset to manual (for changes from a light preset)
     */
    private void setLightPresetToManual() {
        lightPresetChoiceBox.getSelectionModel().select(LightPresets.MANUAL.getDisplayText());
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
     * enables/disables lens type choice box depending on whether there is shape from shading (only 10mm distance allowed)
     */
    private void updateLensTypeChoiceBox() {
        lensTypeChoiceBox.setDisable(CIS_DATA.isShapeFromShading());
    }

    /**
     * updates the light frequency limit label
     */
    private void updateLightFrequencyLimit() {
        long minFreq = Math.round(1000 * CIS_DATA.getMinFreq(CIS_DATA.calculate())) / 1000;
        lastMinFreq = minFreq;
        lightFrequencyLimitLabel.setText(minFreq < 0 // if < 0 there are values missing in database -> give error msg
                ? Util.getString("missingPhotoValues") + "\n"
                : "~" + minFreq + "\u200akHz\n"
        );
        updateSelectedLineRateWarning();
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
     * updates scan width options depending on current shape from shading selection
     */
    private void updateScanWidthOptions() {
        if (!CIS_DATA.hasCoax()) { // with coax already handled
            //sfs ->
            scanWidthChoiceBox.getItems().clear();
            if (CIS_DATA.isShapeFromShading()) {
                scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_SFS);
            } else {
                scanWidthChoiceBox.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX);
            }
            // set initial value
            scanWidthChoiceBox.getSelectionModel().select((Integer) CIS_DATA.getScanWidth());
        }
    }

    /**
     * updates the selected line rate warning label. The warning is shown if the minimum light frequency is smaller than twice the selected line rate.
     */
    private void updateSelectedLineRateWarning() {
        double lineRate = Math.round(CIS_DATA.getSelectedLineRate() / 100.0) / 10.0;
        if (lastMinFreq >= 0 && lastMinFreq < lineRate) // if missing values in database lastMinFreq is <0 -> don't show warning as this would overlap with other warning
            warningSelectedLineRateLabel.setText(Util.getString("warningFrequencyLimit").replace('\n', ' '));
        else
            warningSelectedLineRateLabel.setText("");
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

        private final boolean cloudyDay;
        private final String displayText;
        private final CIS.LightColor leftBrightField, leftDarkField, rightBrightField, rightDarkField, coax;
        private final int phaseCount;

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

        public static Optional<LightPresets> findByDisplayText(String displayText) {
            return Arrays.stream(LightPresets.values()).filter(l -> l.getDisplayText().equals(displayText)).findFirst();
        }

        public String getDisplayText() {
            return displayText;
        }
    }
}