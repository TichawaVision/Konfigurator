package de.tichawa.cis.config.serial;

import com.fazecast.jSerialComm.SerialPort;

import java.io.*;
import java.util.List;

/**
 * Class for connection to a serial port
 */
public class SerialConnection {
    private final SerialPort port;

    /**
     * creates a new connection object with the given serial port
     *
     * @param port the serial port to write to
     */
    public SerialConnection(SerialPort port) {
        this.port = port;
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

        port.openPort();
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 300, 0);

        BufferedWriter outputStreamWriter = new BufferedWriter(new OutputStreamWriter(port.getOutputStream()));
        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(port.getInputStream()));
        commands.forEach(command -> {
            try {
                System.out.println("serial connection: writing " + command);
                outputStreamWriter.write(command);
                outputStreamWriter.newLine();
                outputStreamWriter.flush();
                System.out.println("serial connection: reading " + inputStreamReader.readLine()); // output result message
            } catch (IOException e) {
                System.err.println("failed to write command " + command + ": " + e);
            }
        });

        port.closePort();
    }
}
