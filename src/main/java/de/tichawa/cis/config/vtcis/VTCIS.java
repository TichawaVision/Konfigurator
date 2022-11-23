package de.tichawa.cis.config.vtcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.cis.config.vscis.VSCIS;
import lombok.*;

import java.util.*;

public class VTCIS extends VSCIS {

    @Getter
    @Setter
    private String cLMode;

    public VTCIS() {
        super();
    }

    @Override
    public List<CPUCLink> getCLCalc(int numOfPix) {
        int numOfPixNominal;
        int taps;
        int pixPerTap;
        int lval;

        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));
        long mhzLineRate = (long) numOfPixNominal * getSelectedLineRate() / 1000000;
        taps = (int) Math.ceil(1.01 * mhzLineRate / 85.0);
        pixPerTap = numOfPixNominal / taps;
        lval = pixPerTap - pixPerTap % 8;

        boolean mediumMode = !getCLMode().equals("Full80");
        long datarate = (long) getPhaseCount() * numOfPixNominal * getSelectedLineRate();
        LinkedList<CPUCLink.CameraLink> cameraLinks = new LinkedList<>();
        int portLimit = mediumMode ? 8 : 10;

        int blockSize;
        if (getPhaseCount() == 1) {
            blockSize = 1;
        } else {
            blockSize = 3 * (getPhaseCount() / 3);
            if (blockSize < getPhaseCount()) {
                blockSize += 3;
            }
        }

        for (int i = 0; i < taps; ) {
            cameraLinks.add(new CPUCLink.CameraLink(0, (char) (CPUCLink.Port.DEFAULT_NAME + cameraLinks.stream()
                    .mapToInt(CPUCLink.CameraLink::getPortCount)
                    .sum())));

            while (cameraLinks.getLast().getPortCount() <= portLimit - blockSize && i < taps) {
                for (int k = 0; k < blockSize; k++) {
                    if (k < getPhaseCount()) {
                        cameraLinks.getLast().addPorts(new CPUCLink.Port(i * lval, (i + 1) * lval - 1));
                    } else {
                        cameraLinks.getLast().addPorts(new CPUCLink.Port(0, 0));
                    }
                }
                i++;
            }
        }
        boolean flashExtension = getSelectedResolution().getActualResolution() >= 1200 && (numOfPix - 16 * getScanWidth() / BASE_LENGTH * 6 * 2) * getPhaseCount() * 2 > 327680;
        String notes = "LVAL (Modulo 8): " + lval + "\n" +
                Util.getString("clMode") + (mediumMode ? "Base/Medium/Full" : "Full80") + "\n" +
                Util.getString("numPhases") + getPhaseCount() + "\n" +
                "Flash Extension: " + (flashExtension ? "Required" : "Not required.") + "\n";

        CPUCLink CPUCLink = new CPUCLink(datarate, numOfPixNominal, 85000000, notes);
        cameraLinks.forEach(CPUCLink::addCameraLink);

        if (taps > (portLimit / blockSize) * 2) {
            throw new CISException("Number of required taps (" + taps * getPhaseCount() + ") is too high. Please reduce the data rate.");
        }
        return Collections.singletonList(CPUCLink);
    }

    @Override
    public double getMaxLineRate() {
        AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
        return 1000 * Math.round(1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getPixelPerSensor() + 100) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;

    }

    @Override
    public double getGeometryFactor(boolean coax) {
        return coax ? 0.229 : 0.252;
    }

    @Override
    public String getLights() {
        return "";
    }

    /**
     * returns the scan distance string for print out
     */
    @Override
    protected String getScanDistanceString() {
        return "10 mm";
    }

    /**
     * returns the case profile string for print out: 53x50 with coax, 86x80 without coax
     */
    @Override
    protected String getCaseProfile() {
        if (!getLightSources().endsWith("0C")) {
            return Util.getString("Aluminium case profile: 53x50mm (HxT) with bondedcoax");
        } else {
            return Util.getString("Aluminium case profile: 86x80mm (HxT) with bonded");
        }
    }

    /**
     * returns a string that is appended to the end of the camera link section in print out: other cl config on request
     */
    @Override
    protected String getEndOfCameraLinkSection() {
        return Util.getString("configOnRequest");
    }

    /**
     * returns the resolution string for print out: the actual resolution or the switchable resolution string
     */
    @Override
    protected String getResolutionString() {
        if (getSelectedResolution().isSwitchable())
            return Util.getString("binning200");
        return super.getResolutionString();
    }

    /**
     * returns the sensitivity factor that is used for the minimum frequency calculation
     */
    @Override
    protected double getSensitivityFactor() {
        switch (getSelectedResolution().getBoardResolution()) {
            case 1200:
                return 500;
            case 600:
                return 1000;
            case 300:
                return 1800;
            default:
                throw new UnsupportedOperationException("selected board resolution not supported");
        }
    }
}
