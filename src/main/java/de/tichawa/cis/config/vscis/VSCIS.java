package de.tichawa.cis.config.vscis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.cis.config.vtcis.VTCIS;

import java.util.*;

public class VSCIS extends CIS {

    public VSCIS() {
        super();
    }

    @Override
    public String getTiViKey() {
        String key = "G_" + getClass().getSimpleName();
        key += String.format("_%04d", getScanWidth());

        if (getSelectedResolution().isSwitchable()) {
            key += "_XXXX";
        } else {
            key += String.format("_%04d", getSelectedResolution().getActualResolution());
        }

        key += "_";
        switch (getLightSources()) {
            case "0D0C":
                key += "NO";
                break;
            case "2D0C":
            case "2D1C":
                key += getLedLines();
                break;
        }

        if (!getLightSources().equals("0D0C")) {
            if (getPhaseCount() == 3) {
                key += "RGB";
            } else {
                key += getLightColors().stream()
                        .findAny().orElse(LightColor.NONE)
                        .getShortHand();
            }
        }

        if (!getLightSources().endsWith("0C")) {
            key += "C";
        }

        key += getMechaVersion();

        if (isGigeInterface()) {
            key += "GT";
        }
        if (!(this instanceof VTCIS)) {
            key += getCooling().getCode();
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
        int chipsPerTap;
        long portDataRate;
        int lval;
        int pixPerTap;
        double ppsbin;
        double binning;
        int pixelClock = 85000000;

        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VS").orElseThrow(() -> new CISException("Unknown sensor chip"));
        numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));

        taps = numOfPix * getSelectedLineRate() / pixelClock;
        taps--;

        do {
            taps++;
            chipsPerTap = (int) Math.ceil((sensorBoard.getChips() * getBoardCount()) / (double) taps);
            ppsbin = sensorChip.getPixelPerSensor() / ((double) getSelectedResolution().getBoardResolution() / (double) getSelectedResolution().getActualResolution());
            pixPerTap = (int) (chipsPerTap * ppsbin);
            portDataRate = (long) pixPerTap * getSelectedLineRate();
        }
        while (portDataRate > pixelClock);

        binning = 1 / (sensorChip.getBinning() * ((double) getSelectedResolution().getBoardResolution() / (double) getSelectedResolution().getActualResolution()));
        lval = (int) (chipsPerTap * (ppsbin - (sensorBoard.getOverlap() * binning) / sensorBoard.getChips()));
        lval -= lval % 8;
        portDataRate = (long) getPhaseCount() * numOfPix * getSelectedLineRate();
        CPUCLink cl = new CPUCLink(portDataRate, numOfPixNominal, pixelClock);
        LinkedList<CPUCLink.CameraLink> cameraLinks = new LinkedList<>();

        if ((getPhaseCount() == 3 && taps > 3)
                || (getPhaseCount() == 1 && taps > 8)) {
            System.out.println("Please select a lower line rate. Currently required number of taps (" + taps * getPhaseCount() + ") is too high.");
            return new LinkedList<>(); //should probably throw an exception but seems like this is never reached anyway
        }

        int x = 0;
        int oddPortLimit = 3;
        int evenPortLimit = 6;

        while (x < taps) {
            cameraLinks.add(new CPUCLink.CameraLink(0, (char) (CPUCLink.Port.DEFAULT_NAME + cameraLinks.stream()
                    .mapToInt(CPUCLink.CameraLink::getPortCount)
                    .sum())));
//      int portLimit = connections.size() % 2 == 0 ? evenPortLimit : oddPortLimit;
            int portLimit = 9;
            while (cameraLinks.getLast().getPortCount() + getPhaseCount() <= portLimit && x < taps) {
                if (getPhaseCount() == 1) {
                    cameraLinks.getLast().addPorts(new CPUCLink.Port(x * lval, (x + 1) * lval - 1));
                } else {
                    cameraLinks.getLast().addPorts(new CPUCLink.Port(x * lval, (x + 1) * lval - 1, "Red"),
                            new CPUCLink.Port(x * lval, (x + 1) * lval - 1, "Green"),
                            new CPUCLink.Port(x * lval, (x + 1) * lval - 1, "Blue"));
                }

                x++;
            }
        }
        cameraLinks.forEach(cl::addCameraLink);
        return Collections.singletonList(cl);
    }

    @Override
    public double getMaxLineRate() {
        AdcBoardRecord adcBoard = getADC("VARICISC").orElseThrow(() -> new CISException("Unknown ADC board"));
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VS").orElseThrow(() -> new CISException("Unknown sensor chip"));
        return 1000 * Math.round(1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
    }

    @Override
    public double getGeometryFactor(boolean coax) {
        return coax ? 0.105 : 0.128;
    }

    @Override
    public String getLights() {
        return "";
    }

    /**
     * returns the case profile string for print out: 53x50
     */
    @Override
    protected String getCaseProfile() {
        if (!getLightSources().endsWith("0C")) {
            return Util.getString("Aluminium case profile: 53x50mm (HxT) with bondedcoax");
        } else {
            return Util.getString("Aluminium case profile: 53x50mm (HxT) with bonded");
        }
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
}