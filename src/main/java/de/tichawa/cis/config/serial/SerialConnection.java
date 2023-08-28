package de.tichawa.cis.config.serial;

import com.fazecast.jSerialComm.*;
import de.tichawa.cis.config.Util;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Class for connection to a serial port
 */
public class SerialConnection {
    public static final int DEFAULT_DELAY = 100; // determined by testing, faster creates a connection error

    private final SerialPort port;
    private final StringProperty displayStringIn = new SimpleStringProperty(""); // for the input label (what we are reading)
    private final StringProperty displayStringOut = new SimpleStringProperty(""); // for the output label (what we are writing)
    private final int delay; // the delay in between two sent commands

    /**
     * creates a new connection object with the given serial port
     *
     * @param port the serial port to write to
     */
    public SerialConnection(SerialPort port, int delay, Stage serialStage) {
        this.port = port;
        this.delay = delay;
        // create stage for port log
        Stage stage = Util.createNewStage("SerialOutput.fxml", "Serial output");
        // bind text property to automatically update it later
        List<Node> labels = ((HBox) ((ScrollPane) stage.getScene().getRoot()).getContent()).getChildrenUnmodifiable();
        ((Label) labels.get(0)).textProperty().bind(displayStringOut);
        ((Label) labels.get(1)).textProperty().bind(displayStringIn);
        updateLabelIn("read messages:\n");
        updateLabelOut("written messages:");

        stage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            serialStage.close();
            stage.close();
        });
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

        // create task for writing commands for live gui update
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                port.openPort();
                port.addDataListener(new SerialPortDataListener() {
                    /**
                     * returns the data available listening event as this is the only thing we are looking for
                     * @return {@link SerialPort#LISTENING_EVENT_DATA_AVAILABLE}
                     */
                    @Override
                    public int getListeningEvents() {
                        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                    }

                    /**
                     * reads the available data and writes it to the label
                     * @param serialPortEvent the serial port event that should be {@link SerialPort#LISTENING_EVENT_DATA_AVAILABLE}
                     */
                    @Override
                    public void serialEvent(SerialPortEvent serialPortEvent) {
                        if (serialPortEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                            return;
                        // read response
                        byte[] newData = new byte[port.bytesAvailable()];
                        int numBytesRead = port.readBytes(newData, newData.length);
                        System.out.println(numBytesRead + " bytes read");
                        updateLabelIn(new String(newData, StandardCharsets.UTF_8).replace("\n", ""));
                    }
                });
                // write commands
                commands.stream().map(command -> command + "\r").map(command -> command.getBytes(StandardCharsets.UTF_8)).forEach(bytes -> {
                    port.writeBytes(bytes, bytes.length);
                    updateLabelOut(new String(bytes, StandardCharsets.UTF_8));
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                Thread.sleep(5000); // give some time for answers
                port.closePort();
                return null;
            }
        };
        // start task in new Thread for live gui update
        (new Thread(task)).start();
    }

    /**
     * appends the input label by the given new line
     *
     * @param newLine the line to append
     */
    private void updateLabelIn(String newLine) {
        updateLabel(newLine, displayStringIn, false);
    }

    /**
     * appends the output label by the given new line
     *
     * @param newLine the line to append
     */
    private void updateLabelOut(String newLine) {
        updateLabel(newLine, displayStringOut, true);
    }

    /**
     * updates a label via its bound {@link StringProperty} by adding the given new line to the given property
     *
     * @param newText       the text to append
     * @param displayString the {@link StringProperty} where the new line is appended
     * @param lineBreak     whether there should be inserted a new line character between the current and new text
     */
    private static void updateLabel(String newText, StringProperty displayString, boolean lineBreak) {
        System.out.println(newText);
        Platform.runLater(() -> displayString.setValue(displayString.getValue().isEmpty() ?
                newText :
                displayString.getValue() + (lineBreak ? "\n" : "") + newText));
    }
}