package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.*;

import java.net.URL;
import java.util.*;

/**
 * Controller for EquipmentSelection.fxml
 */
public class EquipmentSelectionController implements Initializable {
    private static final String SELECT_CODE_POWER_SUPPLY_2_5 = "PowerSupply2.5";
    private static final String SELECT_CODE_POWER_SUPPLY_4 = "PowerSupply4";
    private static final String SELECT_CODE_POWER_SUPPLY_6 = "PowerSupply6";
    private static final String SELECT_CODE_POWER_SUPPLY_13 = "PowerSupply13";
    private static final String SELECT_CODE_POWER_SUPPLY_20 = "PowerSupply20";
    private static final String SELECT_CODE_POWER_CABLE_3 = "PowerCable3";
    private static final String SELECT_CODE_POWER_CABLE_5 = "PowerCable5";
    private static final String SELECT_CODE_POWER_CABLE_10 = "PowerCable10";
    private static final String SELECT_CODE_TRIGGER_CABLE_10 = "TriggerCable10";
    private static final String SELECT_CODE_TRIGGER_CABLE_15 = "TriggerCable15";
    private static final String SELECT_CODE_TRIGGER_CABLE_20 = "TriggerCable20";
    private static final String SELECT_CODE_TRIGGER_CABLE_25 = "TriggerCable25";
    private static final String SELECT_CODE_TRIGGER_CABLE_30 = "TriggerCable30";
    private static final String SELECT_CODE_TRIGGER_CABLE_35 = "TriggerCable35";
    private static final String SELECT_CODE_TRIGGER_CABLE_40 = "TriggerCable40";
    private static final String SELECT_CODE_TRIGGER_CABLE_45 = "TriggerCable45";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_3 = "MDR/MDR3";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_5 = "MDR/MDR5";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_7_5 = "MDR/MDR7.5";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_10 = "MDR/MDR10";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_3 = "SDR/SDR3";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_5 = "SDR/SDR5";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_7_5 = "SDR/SDR7.5";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_10 = "SDR/SDR10";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_3 = "MDR/SDR3";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_5 = "MDR/SDR5";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_7_5 = "MDR/SDR7.5";
    private static final String SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_10 = "MDR/SDR10";

    @FXML
    private ChoiceBox<PowerSupplyOptions> powerSupplyChoiceBox;
    @FXML
    private ChoiceBox<PowerCableOptions> powerCableChoiceBox;
    @FXML
    private ChoiceBox<TriggerCableOptions> triggerCableChoiceBox;
    @FXML
    private RadioButton mdrRadioButton;
    @FXML
    private ChoiceBox<CameraLinkLengthOptions> cameraLinkCableLengthChoiceBox;

    private CIS cisData;
    private CIS ldstdData;

    /**
     * Initializes the form elements. Sets the choice box items and converter and initial values
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // power supply
        powerSupplyChoiceBox.setConverter(new StringConverter<PowerSupplyOptions>() {
            @Override
            public String toString(PowerSupplyOptions powerSupplyOption) {
                return powerSupplyOption.displayString;
            }

            @Override
            public PowerSupplyOptions fromString(String displayString) {
                return PowerSupplyOptions.forDisplayString(displayString);
            }
        });
        powerSupplyChoiceBox.getItems().clear();
        powerSupplyChoiceBox.getItems().addAll(PowerSupplyOptions.values());
        powerSupplyChoiceBox.getSelectionModel().select(PowerSupplyOptions.NONE); //TODO replace with what was calculated

        // power cable
        powerCableChoiceBox.setConverter(new StringConverter<PowerCableOptions>() {
            @Override
            public String toString(PowerCableOptions powerCableOption) {
                return powerCableOption.displayString;
            }

            @Override
            public PowerCableOptions fromString(String displayString) {
                return PowerCableOptions.forDisplayString(displayString);
            }
        });

        powerCableChoiceBox.getItems().clear();
        powerCableChoiceBox.getItems().addAll(PowerCableOptions.values());
        powerCableChoiceBox.getSelectionModel().select(PowerCableOptions.THREE); //TODO maybe replace with something else

        // camera link cable
        cameraLinkCableLengthChoiceBox.setConverter(new StringConverter<CameraLinkLengthOptions>() {
            @Override
            public String toString(CameraLinkLengthOptions cameraLinkLengthOption) {
                return cameraLinkLengthOption.displayString;
            }

            @Override
            public CameraLinkLengthOptions fromString(String displayString) {
                return CameraLinkLengthOptions.forDisplayString(displayString);
            }
        });
        cameraLinkCableLengthChoiceBox.getItems().clear();
        cameraLinkCableLengthChoiceBox.getItems().addAll(CameraLinkLengthOptions.values());
        cameraLinkCableLengthChoiceBox.getSelectionModel().select(CameraLinkLengthOptions.NONE);

        // trigger cable
        triggerCableChoiceBox.setConverter(new StringConverter<TriggerCableOptions>() {
            @Override
            public String toString(TriggerCableOptions powerCableOption) {
                return powerCableOption.displayString;
            }

            @Override
            public TriggerCableOptions fromString(String displayString) {
                return TriggerCableOptions.forDisplayString(displayString);
            }
        });

        triggerCableChoiceBox.getItems().clear();
        triggerCableChoiceBox.getItems().addAll(TriggerCableOptions.values());
        triggerCableChoiceBox.getSelectionModel().select(TriggerCableOptions.NONE);
    }

    /**
     * Initializes the attributes with the given data.
     *
     * @param cisData   the CIS that needs additional equipment
     * @param ldstdData the {@link de.tichawa.cis.config.ldstd.LDSTD} that comes with the given CIS
     */
    public void passData(CIS cisData, CIS ldstdData) {
        this.cisData = cisData;
        this.ldstdData = ldstdData;
    }

    /**
     * Handles the submit button press by passing the selected data to the next equipment overview
     */
    @FXML
    private void handleSubmit() {
        Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("Equipment.fxml", "Additional Equipment");
        ((EquipmentController) stageWithLoader.getValue().getController()).passData(cisData, ldstdData,
                powerSupplyChoiceBox.getValue().selectCodeString,
                powerCableChoiceBox.getValue().selectCodeString,
                CameraLinkCableOptions.getOption(cisData.usesMdrCameraLinkOnCisSide(), mdrRadioButton.isSelected(),
                        cameraLinkCableLengthChoiceBox.getValue()).selectCodeString,
                triggerCableChoiceBox.getValue().selectCodeString);
        stageWithLoader.getKey().show();
    }

    /**
     * Enumeration of power supply options.
     * Used for the {@link #powerSupplyChoiceBox}.
     */
    private enum PowerSupplyOptions {
        NONE("None", null),
        TWO_POINT_FIVE("2.5 A", SELECT_CODE_POWER_SUPPLY_2_5),
        FOUR("4 A", SELECT_CODE_POWER_SUPPLY_4),
        SIX("6 A", SELECT_CODE_POWER_SUPPLY_6),
        THIRTEEN("13 A", SELECT_CODE_POWER_SUPPLY_13),
        TWENTY("20 A", SELECT_CODE_POWER_SUPPLY_20);

        private final String displayString;
        private final String selectCodeString;

        PowerSupplyOptions(String displayString, String selectCodeString) {
            this.displayString = displayString;
            this.selectCodeString = selectCodeString;
        }

        /**
         * Returns the power supply option corresponding to the given display string. Used by the {@link StringConverter} of the {@link #powerSupplyChoiceBox}.
         */
        public static PowerSupplyOptions forDisplayString(String displayString) {
            return Arrays.stream(PowerSupplyOptions.values()).filter(option -> option.displayString.equals(displayString)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Power supply option not found for display string: " + displayString));
        }
    }

    /**
     * Enumeration of power cable options.
     * Used for the {@link #powerCableChoiceBox}.
     */
    private enum PowerCableOptions {
        NONE("None", null),
        THREE("3 m", SELECT_CODE_POWER_CABLE_3),
        FIVE("5 m", SELECT_CODE_POWER_CABLE_5),
        TEN("10 m", SELECT_CODE_POWER_CABLE_10);

        private final String displayString;
        private final String selectCodeString;

        PowerCableOptions(String displayString, String selectCodeString) {
            this.displayString = displayString;
            this.selectCodeString = selectCodeString;
        }

        /**
         * Returns the power cable option corresponding to the given display string. Used by the {@link StringConverter} of the {@link #powerCableChoiceBox}.
         */
        public static PowerCableOptions forDisplayString(String displayString) {
            return Arrays.stream(PowerCableOptions.values()).filter(option -> option.displayString.equals(displayString)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Power supply option not found for display string: " + displayString));
        }
    }

    /**
     * Enumeration of camera link length options.
     * Used for the {@link #cameraLinkCableLengthChoiceBox}.
     */
    private enum CameraLinkLengthOptions {
        NONE("None"), THREE("3 m"), FIVE("5 m"), SEVEN_FIVE("7.5 m"), TEN("10 m");

        private final String displayString;

        CameraLinkLengthOptions(String displayString) {
            this.displayString = displayString;
        }

        /**
         * Returns the camera link cable length option corresponding to the given display string. Used by the {@link StringConverter} of the {@link #cameraLinkCableLengthChoiceBox}.
         */
        public static CameraLinkLengthOptions forDisplayString(String displayString) {
            return Arrays.stream(CameraLinkLengthOptions.values()).filter(option -> option.displayString.equals(displayString)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Power supply option not found for display string: " + displayString));
        }
    }

    /**
     * Enumeration of camera link cable options.
     * Used to pass the correct select code to the {@link EquipmentController}.
     */
    private enum CameraLinkCableOptions {
        NONE(null),
        MDR_MDR_3(SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_3),
        MDR_MDR_5(SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_5),
        MDR_MDR_7_5(SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_7_5),
        MDR_MDR_10(SELECT_CODE_CAMERA_LINK_CABLE_MDR_MDR_10),
        MDR_SDR_3(SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_3),
        MDR_SDR_5(SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_5),
        MDR_SDR_7_5(SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_7_5),
        MDR_SDR_10(SELECT_CODE_CAMERA_LINK_CABLE_MDR_SDR_10),
        SDR_SDR_3(SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_3),
        SDR_SDR_5(SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_5),
        SDR_SDR_7_5(SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_7_5),
        SDR_SDR_10(SELECT_CODE_CAMERA_LINK_CABLE_SDR_SDR_10);

        private final String selectCodeString;

        CameraLinkCableOptions(String selectCodeString) {
            this.selectCodeString = selectCodeString;
        }

        public static CameraLinkCableOptions getOption(boolean isMdrCisSide, boolean isMdrUserSide, CameraLinkLengthOptions length) {
            if (length == CameraLinkLengthOptions.NONE)
                return NONE;
            if (isMdrCisSide != isMdrUserSide)
                switch (length) {
                    case THREE:
                        return MDR_SDR_3;
                    case FIVE:
                        return MDR_SDR_5;
                    case SEVEN_FIVE:
                        return MDR_SDR_7_5;
                    case TEN:
                        return MDR_SDR_10;
                }
            // else: cis=user side
            if (isMdrCisSide)
                switch (length) {
                    case THREE:
                        return MDR_MDR_3;
                    case FIVE:
                        return MDR_MDR_5;
                    case SEVEN_FIVE:
                        return MDR_MDR_7_5;
                    case TEN:
                        return MDR_MDR_10;
                }
            // else: both sdr
            switch (length) {
                case THREE:
                    return SDR_SDR_3;
                case FIVE:
                    return SDR_SDR_5;
                case SEVEN_FIVE:
                    return SDR_SDR_7_5;
                case TEN:
                    return SDR_SDR_10;
            }
            throw new IllegalStateException("this should not be reached");
        }
    }

    /**
     * Enumeration of trigger cable options.
     * Used for the {@link #triggerCableChoiceBox}.
     */
    private enum TriggerCableOptions {
        NONE("None", null),
        TEN("10 m", SELECT_CODE_TRIGGER_CABLE_10),
        FIFTEEN("15 m", SELECT_CODE_TRIGGER_CABLE_15),
        TWENTY("20 m", SELECT_CODE_TRIGGER_CABLE_20),
        TWENTY_FIVE("25 m", SELECT_CODE_TRIGGER_CABLE_25),
        THIRTY("30 m", SELECT_CODE_TRIGGER_CABLE_30),
        THIRTY_FIVE("35 m", SELECT_CODE_TRIGGER_CABLE_35),
        FORTY("40 m", SELECT_CODE_TRIGGER_CABLE_40),
        FORTY_FIVE("45 m", SELECT_CODE_TRIGGER_CABLE_45);

        private final String displayString;
        private final String selectCodeString;

        TriggerCableOptions(String displayString, String selectCodeString) {
            this.displayString = displayString;
            this.selectCodeString = selectCodeString;
        }

        /**
         * Returns the trigger cable option corresponding to the given display string. Used by the {@link StringConverter} of the {@link #triggerCableChoiceBox}.
         */
        public static TriggerCableOptions forDisplayString(String displayString) {
            return Arrays.stream(TriggerCableOptions.values()).filter(option -> option.displayString.equals(displayString)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Power supply option not found for display string: " + displayString));
        }
    }
}
