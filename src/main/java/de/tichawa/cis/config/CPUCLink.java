package de.tichawa.cis.config;

import lombok.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * class that represents a single CPUCLink.
 * A CPUCLink consists of cameraLinks.
 * For backward compatibility also handles general camera link configuration outputs.
 */
@Value
@AllArgsConstructor
public class CPUCLink {
    long dataRate;
    long pixelCount;
    long pixelClock;
    String notes;

    @Getter
    List<CameraLink> cameraLinks = new LinkedList<>();

    public CPUCLink(long dataRate, long pixelCount, long pixelClock) {
        this(dataRate, pixelCount, pixelClock, null);
    }

    /**
     * adds the given camera link to the camera links list and sets its id
     */
    public void addCameraLink(CameraLink cameraLink) {
        int nextCLId = this.getCameraLinks().isEmpty() ? CameraLink.DEFAULT_ID : cameraLinks.get(cameraLinks.size() - 1).getId() + 1;
        this.getCameraLinks().add(cameraLink.withId(nextCLId));
    }

    /**
     * calculates the needed cables by adding the needed cables for each camera link
     */
    public int getCableCount() {
        return cameraLinks.stream().mapToInt(CameraLink::getCableCount).sum();
    }

    /**
     * returns the sum of ports for all camera links of this CPUCLink
     */
    public int getPortCount() {
        return cameraLinks.stream()
                .mapToInt(CameraLink::getPortCount)
                .sum();
    }

    /**
     * returns the sum of ports in use for all camera links of this CPUCLink
     */
    public int getPortNumber() {
        return cameraLinks.stream()
                .mapToInt(CameraLink::getPortNumber)
                .sum();
    }

    @Override
    public String toString() {
        return toString("");
    }

    /**
     * returns the String representation of this CPUCLink:
     * - datarate
     * - number of pixels
     * - number of needed cables
     * - number of used ports
     * - pixel clock
     * - notes (if not empty)
     * Indents each line by the given indentation (prepends it to the line)
     */
    public String toString(String indentation) {
        StringBuilder output = new StringBuilder(indentation)
                .append(Util.getString("dataRate")).append(": ")
                .append(Math.round(getDataRate() / 100000.0) / 10.0)
                .append("\u200aMByte/s\n");
        output.append(indentation)
                .append(Util.getString("nomPix"))
                .append(getPixelCount())
                .append("\n");
        output.append(indentation)
                .append(Util.getString("numberClCables")).append(": ")
                .append(getCableCount())
                .append("\n");
        output.append(indentation)
                .append(Util.getString("numberClPorts")).append(": ")
                .append(getPortNumber())
                .append("\n");
        output.append(indentation)
                .append("Pixel clock: ")
                .append(getPixelClock() / 1000000)
                .append("\u200aMHz\n");
        if (getNotes() != null) {
            output.append(indentation)
                    .append(getNotes())
                    .append("\n");
        }

        output.append(this.getCameraLinks().stream()
                .map(CameraLink::toString)
                .collect(Collectors.joining("\n")));
        return output.toString();
    }

    /**
     * class that represents a single camera link.
     * A camera link has an id and can consist of max 10 ports.
     */
    @With
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CameraLink {
        public static final String CL_FORMAT_DECA = "Deca";
        public static final String CL_FORMAT_BASE = "Base";
        public static final String CL_FORMAT_MEDIUM = "Medium";
        public static final String CL_FORMAT_FULL = "Full";

        public static final int DEFAULT_ID = 1;
        public static final int MAX_PORT_COUNT = 10;

        int id;
        char defaultPort;
        @Getter(AccessLevel.PRIVATE)
        LinkedList<Port> ports;
        boolean forcedDeca;

        public CameraLink() {
            this(DEFAULT_ID, Port.DEFAULT_NAME);
        }

        public CameraLink(int id, boolean forcedDeca) {
            this(id, Port.DEFAULT_NAME, forcedDeca);
        }

        public CameraLink(int id, char port, boolean forcedDeca) {
            this(id, port, new LinkedList<>(), forcedDeca);
        }

        public CameraLink(int id, char port) {
            this(id, port, new LinkedList<>(), false);
        }

        /**
         * adds the given ports if there is space (see {@link #MAX_PORT_COUNT})
         * Also sets the port name for each port to the next letter
         */
        public void addPorts(Port... ports) {
            if (getPorts().size() + ports.length <= MAX_PORT_COUNT) {
                for (Port port : ports) {
                    char nextPortName = getPorts().isEmpty() ? Port.DEFAULT_NAME : (char) (getPorts().getLast().getName() + 1);
                    getPorts().add(port.withName(nextPortName));
                }
            }
        }

        /**
         * returns the number of ports in the ports list
         */
        public int getPortCount() {
            return getPorts().size();
        }

        /**
         * returns the number of ports in the ports list where the end pixel is not 0
         */
        public int getPortNumber() { //is this needed?
            return (int) getPorts().stream().filter(p -> p.getEndPixel() != 0).count();
        }

        /**
         * returns the string representation of this camera link:
         * - "CameraLink" number with format (BASE, MEDIUM, FULL, DECA)
         * - each port in one line
         */
        @Override
        public String toString() {
            return "CameraLink " + getId() + " (" + getCLFormat() + "):\n" + getPorts().stream()
                    .map(Port::toString)
                    .map(s -> "    " + s)
                    .collect(Collectors.joining("\n"));
        }

        /**
         * returns the last pixel of this camera link
         */
        public int getEndPixel() {
            return ports.getLast().getEndPixel();
        }

        /**
         * returns the camera link format string depending on the number of ports if it is not forced deca mode:
         * - "Base" if <=3
         * - "Medium" if <=6
         * - "Full" if <=8
         * - "Deca" otherwise
         */
        public String getCLFormat() {
            if (forcedDeca)
                return CL_FORMAT_DECA;
            if (getPortCount() <= 3)
                return CL_FORMAT_BASE;
            if (getPortCount() <= 6)
                return CL_FORMAT_MEDIUM;
            if (getPortCount() <= 8)
                return CL_FORMAT_FULL;
            return CL_FORMAT_DECA;
        }

        /**
         * calculates the needed cables for this camera link.
         * If there are more than three ports, 2 cables are needed otherwise 1.
         */
        public int getCableCount() {
            return getPortCount() > 3 ? 2 : 1;
        }
    }

    /**
     * class that represents a single port.
     * A port has a name (char), start and end pixel.
     */
    @With
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Port {
        public static char DEFAULT_NAME = 'A';

        char name;
        int startPixel;
        int endPixel;
        String note;

        public Port(int startPixel, int endPixel) {
            this(startPixel, endPixel, null);
        }

        public Port(int startPixel, int endPixel, String note) {
            this(DEFAULT_NAME, startPixel, endPixel, note);
        }

        public long getPixelCount() {
            return getEndPixel() - getStartPixel() + 1;
        }

        /**
         * returns the string representation of this port:
         * - "Port" portName: start and end pixel
         */
        @Override
        public String toString() {
            String noteString = getNote() == null ? "" : " (" + getNote() + ")";
            if (getEndPixel() == 0) {
                return "Port " + getName() + ":\t\t" + "          " + " - " + " ";
            }
            return "Port " + getName() + noteString + ":"
                    + (getName() == 'H' ? "" : "\t") // small letters (all but H) get an extra tab for alignment...
                    + "\t" + String.format("%05d", getStartPixel()) + " - " + String.format("%05d", getEndPixel());
        }//TODO if noteString not empty change number of tabs
    }
}