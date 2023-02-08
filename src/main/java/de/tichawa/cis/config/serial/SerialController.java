package de.tichawa.cis.config.serial;

import com.fazecast.jSerialComm.SerialPort;
import de.tichawa.cis.config.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.util.List;

/**
 * controller for Serial.fxml
 */
public class SerialController {
    private CIS cis;
    @FXML
    private CheckBox SaveParameters;
    @FXML
    private ChoiceBox<SerialCommands.ShiftOptions> XYShift;
    @FXML
    private ChoiceBox<SerialPort> SerialPort;
    @FXML
    private ChoiceBox<de.tichawa.cis.config.CPUCLink> CPUCLink;
    @FXML
    private TextArea Commands;
    @FXML
    private TextField Delay;

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
        SerialPort.getItems().clear();
        SerialPort.getItems().addAll(com.fazecast.jSerialComm.SerialPort.getCommPorts());
        SerialPort.getSelectionModel().select(0);
        SerialPort.getItems().stream().filter(port -> port.getSystemPortName().equals("COM10")).findFirst().ifPresent(port -> SerialPort.getSelectionModel().select(port));
        SerialPort.setConverter(new StringConverter<com.fazecast.jSerialComm.SerialPort>() {
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
        CPUCLink.getItems().clear();
        CPUCLink.getItems().addAll(cpucLinks);
        CPUCLink.setConverter(new StringConverter<de.tichawa.cis.config.CPUCLink>() {
            @Override
            public String toString(de.tichawa.cis.config.CPUCLink object) {
                return "Board " + (1 + cpucLinks.indexOf(object));
            }

            @Override
            public de.tichawa.cis.config.CPUCLink fromString(String string) {
                return cpucLinks.get(Integer.parseInt(string.split(" ")[1]) - 1);
            }
        });
        CPUCLink.getSelectionModel().select(0);
        CPUCLink.setDisable(cpucLinks.size() <= 1);
        CPUCLink.valueProperty().addListener(((observable, oldValue, newValue) -> updateCommandsText()));
        // - xy shift
        XYShift.getItems().clear();
        XYShift.setConverter(new StringConverter<SerialCommands.ShiftOptions>() {
            @Override
            public String toString(SerialCommands.ShiftOptions shiftOption) {
                return shiftOption.displayString;
            }

            @Override
            public SerialCommands.ShiftOptions fromString(String string) {
                return SerialCommands.ShiftOptions.fromString(string);
            }
        });
        XYShift.getItems().addAll(SerialCommands.ShiftOptions.values());
        XYShift.getSelectionModel().select(SerialCommands.ShiftOptions.DEFAULT);
        XYShift.valueProperty().addListener(((observable, oldValue, newValue) -> updateCommandsText()));
        // setup checkbox
        SaveParameters.setSelected(false);
        SaveParameters.selectedProperty().addListener(((observable, oldValue, newValue) -> updateCommandsText()));
        // show commands
        updateCommandsText();
        // delay
        Delay.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), 0, change -> {
            String newText = change.getControlNewText();
            if (newText.matches("0|[1-9][0-9]*"))
                return change;
            return null;
        }));
        Delay.setText("250");
    }

    /**
     * updates the commands text label by generating the commands for the current selection
     */
    private void updateCommandsText() {
        Commands.setText(String.join("\n", getCurrentCommands().generateCommands()));
    }

    /**
     * generates a serial commands object for cpuclinks with the current selection
     *
     * @return a {@link CPUCLinkSerialCommands} object for the current selection
     */
    private SerialCommands getCurrentCommands() {
        return new CPUCLinkSerialCommands(
                cis,
                XYShift.getValue(),
                SaveParameters.isSelected(),
                CPUCLink.getValue(),
                totalNumberOfPix);
    }

    /**
     * handles the create commands button press. Creates the commands and sends it to the selected serial port.
     */
    @FXML
    private void handleCreateCommands() {
        SerialCommands commands = getCurrentCommands();
        SerialConnection connection = new SerialConnection(SerialPort.getValue(), Integer.parseInt(Delay.getText()));
        connection.sendToSerialPort(commands.generateCommands());
    }
}
