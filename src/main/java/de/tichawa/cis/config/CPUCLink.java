package de.tichawa.cis.config;

import lombok.*;

import java.util.LinkedList;
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

    @Getter(AccessLevel.PRIVATE)
    LinkedList<CameraLink> cameraLinks = new LinkedList<>();

    public CPUCLink(long dataRate, long pixelCount, long pixelClock) {
        this(dataRate, pixelCount, pixelClock, null);
    }

    public void addCameraLink(CameraLink cameraLink) {
        int nextCLId = this.getCameraLinks().isEmpty() ? CameraLink.DEFAULT_ID : this.getCameraLinks().getLast().getId() + 1;
        this.getCameraLinks().add(cameraLink.withId(nextCLId));
    }

    public int getCameraLinkCount() {
        int kabelCount;
        if (getPortCount() / this.getCameraLinks().size() <= 2) {
            kabelCount = this.getCameraLinks().size();
        } else if (getPortCount() > 3 && this.getCameraLinks().size() == 1) {
            kabelCount = 2;
        } else {
            kabelCount = getPortCount() / 3;
        }
        return kabelCount;

    }

    public int getPortCount() {
        return this.getCameraLinks().stream()
                .mapToInt(CameraLink::getPortCount)
                .sum();
    }

    public int getPortNumber() {
        return this.getCameraLinks().stream()
                .mapToInt(CameraLink::getPortNumber)
                .sum();
    }

    @Override
    public String toString() { //TODO don't use this for VUCIS output but individual methods
        StringBuilder output = new StringBuilder(Util.getString("datarate"))
                .append(Math.round(getDataRate() / 100000.0) / 10.0)
                .append(" MByte/s\n");
        output.append(Util.getString("nomPix"))
                .append(getPixelCount())
                .append("\n");
        output.append(Util.getString("numofcons"))
                .append(getCameraLinkCount())
                .append("\n");
        output.append(Util.getString("numofport"))
                .append(getPortNumber())
                .append("\n");
        output.append("Pixel clock: ")
                .append(getPixelClock() / 1000000)
                .append("MHz\n");
        if (getNotes() != null) {
            output.append(getNotes())
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
        public static final int DEFAULT_ID = 1;
        public static final int MAX_PORT_COUNT = 10;

        int id;
        char defaultPort;
        @Getter(AccessLevel.PRIVATE)
        LinkedList<Port> ports;

        public CameraLink() {
            this(DEFAULT_ID, Port.DEFAULT_NAME);
        }

        public CameraLink(int id) {
            this(id, Port.DEFAULT_NAME);
        }

        public CameraLink(int id, char defaultPort) {
            this(id, defaultPort, new LinkedList<>());
        }

        public boolean addPorts(Port... ports) {
            if (getPorts().size() + ports.length <= MAX_PORT_COUNT) {
                for (Port port : ports) {
                    char nextPortName = getPorts().isEmpty() ? Port.DEFAULT_NAME : (char) (getPorts().getLast().getName() + 1);
                    getPorts().add(port.withName(nextPortName));
                }

                return true;
            } else {
                return false;
            }
        }

        public int getPortCount() {
            return getPorts().size();
        }

        public int getPortNumber() {
            int num = 0;
            for (int i = 0; i < getPortCount(); ++i) {
                if (!(getPorts().get(i).getEndPixel() == 0)) {
                    num++;
                }
            }
            return num;
        }

        @Override
        public String toString() {
            return "CameraLink " + getId() + ":\n" + getPorts().stream()
                    .map(Port::toString)
                    .map(s -> "    " + s)
                    .collect(Collectors.joining("\n"));
        }

        public int getEndPixel() {
            return ports.getLast().getEndPixel();
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

        @Override
        public String toString() {
            String noteString = getNote() == null ? "" : " (" + getNote() + ")";
            if (getEndPixel() == 0) {
                return "Port " + getName() + ": " + "          " + " - " + " ";
            }
            return "Port " + getName() + noteString + ": "
                    + String.format("%05d", getStartPixel()) + " - " + String.format("%05d", getEndPixel());
        }
    }
}
