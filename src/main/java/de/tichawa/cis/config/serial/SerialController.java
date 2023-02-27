package de.tichawa.cis.config.serial;

import com.fazecast.jSerialComm.SerialPort;
import de.tichawa.cis.config.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.util.List;

/**
 * controller for Serial.fxml
 */
public class SerialController {
    private CIS cis;
    @FXML
    private CheckBox saveParametersCheckBox;
    @FXML
    private ChoiceBox<SerialCommands.ShiftOptions> xyShiftChoiceBox;
    @FXML
    private ChoiceBox<SerialPort> serialPortChoiceBox;
    @FXML
    private ChoiceBox<de.tichawa.cis.config.CPUCLink> cpucLinkChoiceBox;
    @FXML
    private TextArea commandsTextArea;
    @FXML
    private TextField delayTextField;

    private long totalNumberOfPix;

    /**
     * initializes the controller
     *
     * @param cis the cis that is currently configured
     */
    public void initialize(CIS cis) {
        this.cis = cis;
        List<CPUCLink> cpucLinks = cis.getCLCalc(cis.getNumOfPix(), cis.calculate());
        totalNumberOfPix = cpucLinks.stream().mapToLong(de.tichawa.cis.config.CPUCLink::getPixelCount).sum();

        // setup choice boxes
        // - serial port
        serialPortChoiceBox.getItems().clear();
        serialPortChoiceBox.getItems().addAll(com.fazecast.jSerialComm.SerialPort.getCommPorts());
        serialPortChoiceBox.getSelectionModel().select(0);
        serialPortChoiceBox.getItems().stream().filter(port -> port.getSystemPortName().equals("COM10")).findFirst().ifPresent(port -> serialPortChoiceBox.getSelectionModel().select(port));
        serialPortChoiceBox.setConverter(new StringConverter<com.fazecast.jSerialComm.SerialPort>() {
            @Override
            public String toString(com.fazecast.jSerialComm.SerialPort port) {
                return port.getSystemPortName();
            }

            @Override
            public com.fazecast.jSerialComm.SerialPort fromString(String port) {
                return com.fazecast.jSerialComm.SerialPort.getCommPort(port);
            }
        });
        // - cpuc link
        cpucLinkChoiceBox.getItems().clear();
        cpucLinkChoiceBox.getItems().addAll(cpucLinks);
        cpucLinkChoiceBox.setConverter(new StringConverter<de.tichawa.cis.config.CPUCLink>() {
            @Override
            public String toString(de.tichawa.cis.config.CPUCLink object) {
                return "Board " + (1 + cpucLinks.indexOf(object));
            }

            @Override
            public de.tichawa.cis.config.CPUCLink fromString(String string) {
                return cpucLinks.get(Integer.parseInt(string.split(" ")[1]) - 1);
            }
        });
        cpucLinkChoiceBox.getSelectionModel().select(0);
        cpucLinkChoiceBox.setDisable(cpucLinks.size() <= 1);
        cpucLinkChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> updateCommandsText()));
        // - xy shift
        xyShiftChoiceBox.getItems().clear();
        xyShiftChoiceBox.setConverter(new StringConverter<SerialCommands.ShiftOptions>() {
            @Override
            public String toString(SerialCommands.ShiftOptions shiftOption) {
                return shiftOption.displayString;
            }

            @Override
            public SerialCommands.ShiftOptions fromString(String string) {
                return SerialCommands.ShiftOptions.fromString(string);
            }
        });
        xyShiftChoiceBox.getItems().addAll(SerialCommands.ShiftOptions.values());
        xyShiftChoiceBox.getSelectionModel().select(SerialCommands.ShiftOptions.DEFAULT);
        xyShiftChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> updateCommandsText()));
        // setup checkbox
        saveParametersCheckBox.setSelected(false);
        saveParametersCheckBox.selectedProperty().addListener(((observable, oldValue, newValue) -> updateCommandsText()));
        // show commands
        updateCommandsText();
        // delay
        delayTextField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, change -> {
            String newText = change.getControlNewText();
            if (newText.matches("0|[1-9][0-9]*"))
                return change;
            return null;
        }));
        delayTextField.setText(String.valueOf(SerialConnection.DEFAULT_DELAY));
    }

    /**
     * updates the commands text label by generating the commands for the current selection
     */
    private void updateCommandsText() {
        commandsTextArea.setText(String.join("\n", getCurrentCommands().generateCommands()));
    }

    /**
     * generates a serial commands object for cpuclinks with the current selection
     *
     * @return a {@link CPUCLinkSerialCommands} object for the current selection
     */
    private SerialCommands getCurrentCommands() {
        return new CPUCLinkSerialCommands(
                cis,
                xyShiftChoiceBox.getValue(),
                saveParametersCheckBox.isSelected(),
                cpucLinkChoiceBox.getValue(),
                totalNumberOfPix);
    }

    /**
     * handles the create commands button press. Creates the commands and sends it to the selected serial port.
     */
    @FXML
    private void handleCreateCommands() {
        SerialCommands commands = getCurrentCommands();
        SerialConnection connection = new SerialConnection(serialPortChoiceBox.getValue(), Integer.parseInt(delayTextField.getText()), (Stage) xyShiftChoiceBox.getScene().getWindow());
        connection.sendToSerialPort(commands.generateCommands());
    }
}