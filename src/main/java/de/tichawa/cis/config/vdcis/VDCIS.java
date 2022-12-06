package de.tichawa.cis.config.vdcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import lombok.*;

import java.util.*;

public class VDCIS extends CIS {
    @Getter
    @Setter
    private String cLMode;

    public VDCIS() {
        super();
    }

    protected VDCIS(VDCIS cis) {
        super(cis);
        this.cLMode = cis.cLMode;
    }

    @Override
    public String getTiViKey() {
        String key = "G_VDCIS";
        key += String.format("_%04d", getScanWidth());

        if (getSelectedResolution().isSwitchable()) //Switchable
        {
            key += "_XXXX";
        } else {
            key += String.format("_%04d", getSelectedResolution().getActualResolution());
        }

        key += "_2";

        if (getPhaseCount() == 4) {
            key += "RGB";
        } else {
            key += getLightColors().stream()
                    .findAny().orElse(LightColor.NONE)
                    .getShortHand();
        }

        if (!getLightSources().endsWith("0C")) {
            key += "C";
        }

        key += getMechaVersion();

        if (isGigeInterface()) {
            key += "GT";
        }

        if (key.endsWith("_")) {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }

    @Override
    public List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation) {
        int numOfPixNominal;
        int taps;
        int pixPerTap;
        int lval;

        int softwareBinning = (1200 / getSelectedResolution().getActualResolution());
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
        numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() / softwareBinning);
        long mhzLineRate = (long) numOfPixNominal * getSelectedLineRate() / 1000000;
        taps = (int) Math.ceil(1.01 * mhzLineRate / 85.0);
        pixPerTap = numOfPixNominal / taps;
        lval = pixPerTap - pixPerTap % 8;

        boolean mediumMode = !getCLMode().equals("Full80");
        long datarate = (long) (getPhaseCount() - 1) * numOfPixNominal * getSelectedLineRate();
        LinkedList<CPUCLink.CameraLink> cameraLinks = new LinkedList<>();
        int portLimit = mediumMode ? 8 : 10;

        int blockSize;
        if (getPhaseCount() - 1 == 1) {
            blockSize = 1;
        } else {
            blockSize = 3 * (getPhaseCount() / 3);
            if (blockSize < getPhaseCount() - 1) {
                blockSize += 3;
            }
        }

        for (int i = 0; i < taps; ) {
            cameraLinks.add(new CPUCLink.CameraLink(0, (char) (CPUCLink.Port.DEFAULT_NAME + cameraLinks.stream()
                    .mapToInt(CPUCLink.CameraLink::getPortCount)
                    .sum())));

            while (cameraLinks.getLast().getPortCount() <= portLimit - blockSize && i < taps) {
                for (int k = 0; k < blockSize; k++) {
                    if (k < getPhaseCount() - 1) {
                        cameraLinks.getLast().addPorts(new CPUCLink.Port(i * lval, (i + 1) * lval - 1));
                    } else {
                        cameraLinks.getLast().addPorts(new CPUCLink.Port(0, 0));
                    }
                }
                i++;
            }
        }

        String notes = "LVAL (Modulo 8): " + lval + "\n" +
                Util.getString("clMode") + (mediumMode ? "Base/Medium/Full" : "Full80") + "\n" +
                Util.getString("numPhases") + getPhaseCount() + "\n";

        CPUCLink CPUCLink = new CPUCLink(datarate, numOfPixNominal, 85000000, notes);
        cameraLinks.forEach(CPUCLink::addCameraLink);

        if (taps > (portLimit / blockSize) * 2) {
            throw new CISException("Number of required taps (" + taps * getPhaseCount() + ") is too high. Please reduce the data rate.");
        }
        if (getSelectedResolution().getActualResolution() >= 1200 && (numOfPix - 16 * getScanWidth() / BASE_LENGTH * 6 * 2) * getPhaseCount() * 2 > 327680) {
            throw new CISException("Out of Flash memory. Please reduce the scan width or resolution.");
        }
        return Collections.singletonList(CPUCLink);
    }

    @Override
    public double getGeometryFactor(boolean coax) {
        return coax ? 0.229 : 0.252;
    }

    @Override
    public double getMaxLineRate() {
        AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
        SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VD").orElseThrow(() -> new CISException("Unknown sensor chip"));
        return 1000 * Math.round(1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;

    }

    @Override
    public String getLights() {
        return "";
    }

    /**
     * returns the scan distance string for print out: ~55-70mm
     */
    @Override
    protected String getScanDistanceString() {
        return "~ 55 - 70 mm " + Util.getString("exactresolution");
    }

    /**
     * returns the case profile string for print out: 80x80mm
     */
    @Override
    protected String getCaseProfile() {
        return Util.getString("Aluminium case profile: 80x80mm (HxT) with bonded");
    }

    /**
     * returns a string that gets appended at the end of the specs section in print out: laser warning
     */
    @Override
    protected String getEndOfSpecs() {
        return Util.getString("laser") + "\n";
    }

    /**
     * returns a string that is appended to the end of the camera link section in print out: other cl config on request
     */
    @Override
    protected String getEndOfCameraLinkSection() {
        return Util.getString("configOnRequest");
    }

    /**
     * returns the sensitivity factor that is used for the minimum frequency calculation
     */
    @Override
    protected double getSensitivityFactor() {
        switch (getSelectedResolution().getBoardResolution()) {
            case 1000:
                return 500;
            case 500:
                return 1000;
            case 250:
                return 1800;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public CIS copy() {
        return new VDCIS(this);
    }
}
