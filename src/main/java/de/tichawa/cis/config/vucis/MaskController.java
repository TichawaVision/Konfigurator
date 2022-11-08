package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;
import java.util.stream.*;

public class MaskController extends de.tichawa.cis.config.MaskController<VUCIS> {

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

    public enum LightPresets {
        MANUAL(CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.NONE, 1),
        SFS(CIS.LightColor.WHITE_SFS, CIS.LightColor.WHITE_SFS, CIS.LightColor.WHITE_SFS, CIS.LightColor.WHITE_SFS, CIS.LightColor.NONE, 4),
        COAX(CIS.LightColor.NONE, CIS.LightColor.NONE, CIS.LightColor.NONE, CIS.LightColor.NONE, CIS.LightColor.RED, 1),
        BF_RGB(CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.RED, CIS.LightColor.NONE, CIS.LightColor.NONE, 1),
        BF_RGB_S(CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.NONE, 3),
        CLOUDY_DAY(CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.RGB_S, CIS.LightColor.NONE, CIS.LightColor.NONE, 3);

        private final CIS.LightColor leftBrightField, leftDarkField, rightBrightField, rightDarkField, coax;
        private final int phaseCount;

        LightPresets(CIS.LightColor leftBrightField, CIS.LightColor leftDarkField, CIS.LightColor rightBrightField, CIS.LightColor rightDarkField, CIS.LightColor coax,
                     int phaseCount) {
            this.leftBrightField = leftBrightField;
            this.leftDarkField = leftDarkField;
            this.rightBrightField = rightBrightField;
            this.rightDarkField = rightDarkField;
            this.coax = coax;
            this.phaseCount = phaseCount;
        }

        //TODO go on here
    }

    public MaskController() {
        CIS_DATA = new VUCIS();
    }

    @Override
    public List<CIS.Resolution> setupResolutions() {
        return Arrays.asList(
                new CIS.Resolution(1200, 1200, true, 0.25, 0.02115),
                new CIS.Resolution(1200, 1200, false, 0.5, 0.02115),
                new CIS.Resolution(600, 600, false, 1.0, 0.0423),
                new CIS.Resolution(400, 1200, false, 1.0, 0.0635),
                new CIS.Resolution(300, 300, false, 1.5, 0.0847),
                new CIS.Resolution(200, 600, false, 2.0, 0.125),
                new CIS.Resolution(150, 300, false, 3.0, 0.167),
                new CIS.Resolution(100, 300, false, 4.0, 0.25),
                new CIS.Resolution(75, 300, false, 6.0, 0.339),
                new CIS.Resolution(50, 300, false, 8.0, 0.5),
                new CIS.Resolution(25, 300, false, 10.0, 1.0));
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        CIS_DATA.setSelectedResolution(getResolutions().get(0));
        CIS_DATA.setScanWidth(1200);
        CIS_DATA.setExternalTrigger(false);
        CIS_DATA.setPhaseCount(1);
        CIS_DATA.setCooling(CIS.Cooling.LICO);

        Color.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            switch (newValue) {
                case "One phase (Monochrome)": {
                    CIS_DATA.setPhaseCount(1);
                    break;
                }
                case "Two phases": {
                    CIS_DATA.setPhaseCount(2);
                    break;
                }
                case "Three phases (RGB)": {
                    CIS_DATA.setPhaseCount(3);
                    break;
                }
                case "Four phases": {
                    CIS_DATA.setPhaseCount(4);
                    break;
                }
                case "Five phases": {
                    CIS_DATA.setPhaseCount(5);
                    break;
                }
                case "Six phases": {
                    CIS_DATA.setPhaseCount(6);
                    break;
                }
            }

            MaxLineRate.setText(CIS_DATA.getMaxLineRate() / 1000.0 + " kHz");
            SelLineRate.setMax(CIS_DATA.getMaxLineRate());
            SelLineRate.setValue(CIS_DATA.getMaxLineRate());
            SelLineRate.setBlockIncrement(CIS_DATA.getMaxLineRate() / 100);

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);
        });

        //set light color options
        BrightFieldLeft.getItems().clear();
        BrightFieldLeft.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
        BrightFieldLeft.getSelectionModel().select(1);
        // add listener for value changes
        BrightFieldLeft.valueProperty().addListener((observable, oldValue, newValue) -> {

            VUCIS.LightColor.findByDescription(newValue)
                    .ifPresent(CIS_DATA::setLeftBrightField);
            //update cooling
            updateCooling(CIS_DATA.getLeftDarkField().getDescription(), CIS_DATA.getLeftDarkField().getDescription(), // left dark field
                    oldValue, BrightFieldLeft.getSelectionModel().getSelectedItem(),                                  // left bright field
                    CIS_DATA.getRightDarkField().getDescription(), CIS_DATA.getRightDarkField().getDescription(),     // right dark field
                    CIS_DATA.getRightBrightField().getDescription(), CIS_DATA.getRightBrightField().getDescription(), // right bright field
                    CIS_DATA.getCoaxLight().getDescription(), CIS_DATA.getCoaxLight().getDescription());              // coax
            updateCoolingCheckboxes();

            handleShapeFromShading();
        });

        //set light color options
        Coax.getItems().clear();
        Coax.getItems().addAll(LIGHT_COLOR_OPTIONS_COAX_WITHOUT_SFS);
        Coax.getSelectionModel().selectFirst();
        // add listener for value changes
        Coax.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            VUCIS.LightColor.findByDescription(newValue)
                    .ifPresent(CIS_DATA::setCoaxLight);

            //update cooling
            updateCooling(CIS_DATA.getLeftDarkField().getDescription(), CIS_DATA.getLeftDarkField().getDescription(), // left dark field
                    CIS_DATA.getLeftBrightField().getDescription(), CIS_DATA.getLeftBrightField().getDescription(),   // left bright field
                    CIS_DATA.getRightDarkField().getDescription(), CIS_DATA.getRightDarkField().getDescription(),     // right dark field
                    CIS_DATA.getRightBrightField().getDescription(), CIS_DATA.getRightBrightField().getDescription(), // right bright field
                    oldValue, Coax.getSelectionModel().getSelectedItem());                                            // coax
            updateCoolingCheckboxes();

            if (CIS_DATA.hasCoax()) {
                //if coax: only allow valid scan widths
                String selected; //save currently selected scan width (if valid afterwards)
                if (SCAN_WIDTH_OPTIONS_WITH_COAX.contains(CIS_DATA.getScanWidth())) {
                    selected = CIS_DATA.getScanWidth() + " mm";
                } else {
                    selected = SCAN_WIDTH_OPTIONS_WITH_COAX.get(1) + " mm";
                    CIS_DATA.setScanWidth(SCAN_WIDTH_OPTIONS_WITH_COAX.get(1));
                }
                ScanWidth.getItems().clear(); // remove and add new scan widths for coax
                ScanWidth.getItems().addAll(SCAN_WIDTH_OPTIONS_WITH_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
                ScanWidth.getSelectionModel().select(selected); //restore currently selected scan width

                //if coax -> no left sided shape from shading
                if (CIS_DATA.getLeftBrightField().isShapeFromShading()) {
                    CIS_DATA.setLeftBrightField(CIS.LightColor.NONE);
                }
                // set choice box options to ones without shape from shading
                selected = CIS_DATA.getLeftBrightField().getDescription(); //save currently selected left side (only bright since no dark and coax is possible)
                BrightFieldLeft.getItems().clear(); //remove and add new left side color options (without shade from shading colors)
                BrightFieldLeft.getItems().addAll(LIGHT_COLOR_OPTIONS_WITHOUT_SFS);
                BrightFieldLeft.getSelectionModel().select(selected);
                CIS_DATA.setLeftDarkField(CIS.LightColor.NONE); // set dark field to NONE since there cannot be dark field and coax
                DarkFieldLeft.getSelectionModel().select(CIS.LightColor.NONE.getDescription());
            } else { // deselected coax
                //-> bigger scan widths possible
                String selected = CIS_DATA.getScanWidth() + " mm";
                ScanWidth.getItems().clear();
                ScanWidth.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
                ScanWidth.getSelectionModel().select(selected);
                // -> shape from shading colors possible
                selected = CIS_DATA.getLeftBrightField().getDescription();
                BrightFieldLeft.getItems().clear();
                BrightFieldLeft.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
                BrightFieldLeft.getSelectionModel().select(selected);
            }
            // enable/disable dark field (no coax + dark field)
            DarkFieldLeft.setDisable(CIS_DATA.hasCoax());
        });

        //set light color options
        BrightFieldRight.getItems().clear();
        BrightFieldRight.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
        BrightFieldRight.getSelectionModel().select(1);
        // add listener for value changes
        BrightFieldRight.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            VUCIS.LightColor.findByDescription(newValue)
                    .ifPresent(CIS_DATA::setRightBrightField);

            //update cooling
            updateCooling(CIS_DATA.getLeftDarkField().getDescription(), CIS_DATA.getLeftDarkField().getDescription(), // left dark field
                    CIS_DATA.getLeftBrightField().getDescription(), CIS_DATA.getLeftBrightField().getDescription(),   // left bright field
                    CIS_DATA.getRightDarkField().getDescription(), CIS_DATA.getRightDarkField().getDescription(),     // right dark field
                    oldValue, BrightFieldRight.getSelectionModel().getSelectedItem(),                                 // right bright field
                    CIS_DATA.getCoaxLight().getDescription(), CIS_DATA.getCoaxLight().getDescription());              // coax
            updateCoolingCheckboxes();

            handleShapeFromShading();
        });

        //set light color options
        DarkFieldLeft.getItems().clear();
        DarkFieldLeft.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
        DarkFieldLeft.getSelectionModel().selectFirst();
        // add listener for value changes
        DarkFieldLeft.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            VUCIS.LightColor.findByDescription(newValue)
                    .ifPresent(CIS_DATA::setLeftDarkField);
            //update cooling
            updateCooling(oldValue, DarkFieldLeft.getSelectionModel().getSelectedItem(),                              // left dark field
                    CIS_DATA.getLeftBrightField().getDescription(), CIS_DATA.getLeftBrightField().getDescription(),   // left bright field
                    CIS_DATA.getRightDarkField().getDescription(), CIS_DATA.getRightDarkField().getDescription(),     // right dark field
                    CIS_DATA.getRightBrightField().getDescription(), CIS_DATA.getRightBrightField().getDescription(), // right bright field
                    CIS_DATA.getCoaxLight().getDescription(), CIS_DATA.getCoaxLight().getDescription());              // coax
            updateCoolingCheckboxes();

            handleShapeFromShading();
        });

        //set light color options
        DarkFieldRight.getItems().clear();
        DarkFieldRight.getItems().addAll(LIGHT_COLOR_OPTIONS_WITH_SFS);
        DarkFieldRight.getSelectionModel().selectFirst();
        // add listener for value changes
        DarkFieldRight.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            VUCIS.LightColor.findByDescription(newValue)
                    .ifPresent(CIS_DATA::setRightDarkField);

            //update cooling
            updateCooling(CIS_DATA.getLeftDarkField().getDescription(), CIS_DATA.getLeftDarkField().getDescription(), // left dark field
                    CIS_DATA.getLeftBrightField().getDescription(), CIS_DATA.getLeftBrightField().getDescription(),   // left bright field
                    oldValue, DarkFieldRight.getSelectionModel().getSelectedItem(),                                   // right dark field
                    CIS_DATA.getRightBrightField().getDescription(), CIS_DATA.getRightBrightField().getDescription(), // right bright field
                    CIS_DATA.getCoaxLight().getDescription(), CIS_DATA.getCoaxLight().getDescription());              // coax
            updateCoolingCheckboxes();

            handleShapeFromShading();
        });
        LensType.valueProperty().addListener((observable, oldValue, newValue) ->
                handleLensTypeChange(oldValue, newValue, LensDOF.isSelected(), LensDOF.isSelected()));
        LensDOF.selectedProperty().addListener((observable, oldValue, newValue) ->
                handleLensTypeChange(LensType.getValue(), LensType.getValue(), oldValue, newValue));
        CoolingLeft.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCoolingLeft(newValue));
        CoolingRight.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setCoolingRight(newValue));


        Resolution.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedResolution(getResolutions().get(Resolution.getSelectionModel().getSelectedIndex()));

            MaxLineRate.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
            SelLineRate.setMax(CIS_DATA.getMaxLineRate());
            SelLineRate.setValue(CIS_DATA.getMaxLineRate());
            SelLineRate.setBlockIncrement(CIS_DATA.getMaxLineRate() / 100);

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

            PixelSize.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
            DefectSize.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
            Speedmms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            Speedms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            Speedmmin.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            Speedips.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
            //TODO maybe always round down so we don't offer too much
        });

        ScanWidth.getItems().clear();
        ScanWidth.getItems().addAll(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.stream().map(o -> o + " mm").collect(Collectors.toList()));
        ScanWidth.getSelectionModel().select(1);
        CIS_DATA.setScanWidth(SCAN_WIDTH_OPTIONS_WITHOUT_COAX.get(1));
        ScanWidth.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null && newValue != null)
                CIS_DATA.setScanWidth(Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim()));
        });

        SelLineRate.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedLineRate(newValue.intValue());

            CurrLineRate.setText(newValue.intValue() / 1000.0 + " kHz");

            Speedmms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            Speedms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            Speedmmin.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            Speedips.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });

        Interface.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setGigeInterface(Interface.getSelectionModel().getSelectedIndex() == 1));

        //only liquid cooling for now -> disable cooling
        CIS_DATA.setCooling(CIS.Cooling.LICO);
        Cooling.setVisible(false);

        CloudyDay.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { //selected cloudy day
                // no dark field
                // - set to NONE in model
                CIS_DATA.setLeftDarkField(CIS.LightColor.NONE);
                CIS_DATA.setRightDarkField(CIS.LightColor.NONE);
                // - update GUI
                DarkFieldRight.getSelectionModel().select(CIS.LightColor.NONE.getDescription());
                DarkFieldLeft.getSelectionModel().select(CIS.LightColor.NONE.getDescription());
            }
            //set value in model
            CIS_DATA.setCloudyDay(true);
            //enable/disable fields
            DarkFieldRight.setDisable(newValue);
            DarkFieldLeft.setDisable(newValue);
        });

        Color.getSelectionModel().selectFirst();
        Resolution.getSelectionModel().selectFirst();
        Interface.getSelectionModel().selectFirst();
        Cooling.getSelectionModel().select(1);
        LensType.getSelectionModel().selectFirst();
        LensDOF.setSelected(false);
        CoolingLeft.setSelected(true);
        CoolingRight.setSelected(true);
        CoolingLeft.setDisable(true);
        CoolingRight.setDisable(true);
        CloudyDay.setSelected(false);
    }

    /**
     * handles shape from shading light color: enforces 10mm working distance selection
     */
    private void handleShapeFromShading() {
        //if shape from shading -> enforce 10mm working distance
        if (CIS_DATA.isShapeFromShading())
            handleLensTypeChange(LensType.getValue(), "10mm", LensDOF.isSelected(), LensDOF.isSelected());
        LensType.setDisable(CIS_DATA.isShapeFromShading());
    }

    /**
     * if there is a LED on one sight it gets cooling. If you change from LED to NONE resulting in no LEDs on this side default value will be no cooling
     * "new" parameters must be what will get actually set (so don't feed it here if it gets reset immediately after)
     */
    private void updateCooling(String oldDarkFieldLeft, String newDarkFieldLeft, String oldBrightFieldLeft, String newBrightFieldLeft,
                               String oldDarkFieldRight, String newDarkFieldRight, String oldBrightFieldRight, String newBrightFieldRight,
                               String oldCoax, String newCoax) {
        /* if changes left */
        if (oldDarkFieldLeft != null && oldBrightFieldLeft != null && oldCoax != null && newDarkFieldLeft != null && newBrightFieldLeft != null && newCoax != null) {
            if (!oldDarkFieldLeft.equals(newDarkFieldLeft) || !oldBrightFieldLeft.equals(newBrightFieldLeft) || !oldCoax.equals(newCoax)) {
                // all NONE -> default no cooling
                // there is an LED now -> cooling
                CIS_DATA.setCoolingLeft(!newDarkFieldLeft.equals(CIS.LightColor.NONE.getDescription())
                        || !newBrightFieldLeft.equals(CIS.LightColor.NONE.getDescription())
                        || !newCoax.equals(CIS.LightColor.NONE.getDescription()));
            }
        }
        /* if changes right */
        if (oldDarkFieldRight != null && oldBrightFieldRight != null && newDarkFieldRight != null && newBrightFieldRight != null) {
            if (!oldDarkFieldRight.equals(newDarkFieldRight) || !oldBrightFieldRight.equals(newBrightFieldRight)) {
                // both NONE -> default no cooling
                // there is an LED now -> cooling
                CIS_DATA.setCoolingRight(!newDarkFieldRight.equals(CIS.LightColor.NONE.getDescription())
                        || !newBrightFieldRight.equals(CIS.LightColor.NONE.getDescription()));
            }
        }
    }

    /**
     * Sets and disables/enables the cooling checkboxes
     */
    private void updateCoolingCheckboxes() {
        if (CIS_DATA.hasCoolingLeft()) {
            CoolingLeft.setSelected(true);
            if (CIS_DATA.getLeftDarkField() != CIS.LightColor.NONE || CIS_DATA.getLeftBrightField() != CIS.LightColor.NONE || CIS_DATA.getCoaxLight() != CIS.LightColor.NONE)
                CoolingLeft.setDisable(true);
        } else {
            CoolingLeft.setSelected(false);
            CoolingLeft.setDisable(false);
        }
        if (CIS_DATA.hasCoolingRight()) {
            CoolingRight.setSelected(true);
            if (CIS_DATA.getRightDarkField() != CIS.LightColor.NONE || CIS_DATA.getRightBrightField() != CIS.LightColor.NONE)
                CoolingRight.setDisable(true);
        } else {
            CoolingRight.setSelected(false);
            CoolingRight.setDisable(false);
        }
    }

    /**
     * Helper method to handle changes to the lens options. Creates an alert and resets values if shape from shading is selected with 23mm.
     * Internally creates a description String from the given new values that should match VUCIS.LensType
     *
     * @param oldDistanceValue old value for working distance
     * @param newDistanceValue new value for working distance, must be 10mm or 23mm
     * @param oldDOFValue      old boolean value for DOF
     * @param newDOFValue      new boolean value for DOF
     */
    private void handleLensTypeChange(String oldDistanceValue, String newDistanceValue, boolean oldDOFValue, boolean newDOFValue) {
        String description = "";
        switch (newDistanceValue) {
            case "10mm":
                description += "TC54";
                LensType.getSelectionModel().select(newDistanceValue);
                break;
            case "23mm":
                // if shape from shading: 23mm not valid -> change to other lens (should not be possible to reach this anyway)
                if (CIS_DATA.isShapeFromShading()) {
                    System.err.println("Cannot choose 23mm with shape from shading. This line should not have been reached.");
                    description += "TC54";
                    LensType.getSelectionModel().select(oldDistanceValue);
                } else
                    description += "TC80";
                break;
            default:
                throw new CISException("Unsupported lens type");
        }
        if (newDOFValue) {
            description += " with long DOF";
        }
        Optional<VUCIS.LensType> lensType = VUCIS.LensType.findByDescription(description);
        lensType.ifPresent(CIS_DATA::setLensType);
    }
}
