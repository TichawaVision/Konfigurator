package de.tichawa.cis.config.serial;

import com.fazecast.jSerialComm.SerialPort;
import de.tichawa.cis.config.Util;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.*;
import java.util.List;

/**
 * Class for connection to a serial port
 */
public class SerialConnection {
    private final SerialPort port;
    private final StringProperty displayString = new SimpleStringProperty("");

    /**
     * creates a new connection object with the given serial port
     *
     * @param port the serial port to write to
     */
    public SerialConnection(SerialPort port) {
        this.port = port;
        Stage stage = Util.createNewStage("SerialOutput.fxml", "Serial output");
        // bind text property to automatically update it later
        ((Label) ((ScrollPane) stage.getScene().getRoot()).getContent()).textProperty().bind(displayString);
        stage.show();
    }

    /**
     * sends the given commands to this object's serial port and tries to read the responses.
     * Each command will be appended by a newline character and sent to the port
     *
     * @param commands the commands to write
     */
    public void sendToSerialPort(List<String> commands) {

        if (port == null) {
            System.err.println("no port selected");
            return;
        }

        // add task for extra thread to concurrently show the output in the gui
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                port.openPort();
                port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);
                port.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

                BufferedWriter outputStreamWriter = new BufferedWriter(new OutputStreamWriter(port.getOutputStream()));
                BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(port.getInputStream()));
                commands.forEach(command -> {
                    try {
                        updateLabel("serial connection - writing: " + command);
                        outputStreamWriter.write(command);
                        outputStreamWriter.newLine();
                        outputStreamWriter.flush();
                        String line = inputStreamReader.readLine();
                        updateLabel("serial connection - reading: " + line);
                        System.out.println("serial connection - reading second line: " + inputStreamReader.readLine());
                    } catch (IOException e) {
                        String errorMsg = "failed to write command " + command + ": " + e;
                        updateLabel(errorMsg);
                    }
                });
                port.closePort();
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Updates the label in the SerialOutput.fxml by setting the {@link #displayString} property that is binded to it.
     *
     * @param newLine the new line to add to the output
     */
    private void updateLabel(String newLine) {
        System.out.println(newLine);
        Platform.runLater(() -> displayString.setValue(displayString.getValue().isEmpty() ? newLine : displayString.getValue() + "\n" + newLine));
    }
}
