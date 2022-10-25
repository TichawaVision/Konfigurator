package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;

import java.util.*;
import java.util.stream.Collectors;

public class VUCIS extends CIS {

    private LightPreset lightPreset;
    private LightColor leftBrightField;
    private LightColor coaxLight;
    private LightColor rightBrightField;
    private LightColor leftDarkField;
    private LightColor rightDarkField;
    private LensType lensType;
    private boolean coolingLeft;
    private boolean coolingRight;

    public enum LightPreset {
        MANUAL("Manual"),
        SHAPE_FROM_SHADING("Shape from Shading"),
        CLOUDY_DAY("Cloudy Day");

        private final String description;

        LightPreset(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }

        public static Optional<LightPreset> findByDescription(String description) {
            return Arrays.stream(LightPreset.values())
                    .filter(c -> c.getDescription().equals(description))
                    .findFirst();
        }
    }

    public enum LensType {
        TC48("TC48", "10"),
        TC48L("TC48 with long DOF", "1L"),
        TC54("TC54", "20"),
        TC54L("TC54 with long DOF", "2L"),
        TC80("TC80", "30"),
        TC80L("TC80 with long DOF", "3L"),
        TC99("TC99", "40"),
        TC147("TC147", "50"),
        OBJ("OL_OBJ_M12x0.5R_25mm_GLAS_0_ß=1:4", "64");

        private final String description;
        private final String code;


        public String getDescription() {
            return description;
        }

        public String getCode() {
            return code;
        }

        LensType(String description, String code) {
            this.description = description;
            this.code = code;
        }

        public static Optional<LensType> findByDescription(String description) {
            return Arrays.stream(LensType.values())
                    .filter(c -> c.getDescription().equals(description))
                    .findFirst();
        }

        @SuppressWarnings("unused")
        public static Optional<LensType> findByCode(String code) {
            return Arrays.stream(LensType.values())
                    .filter(c -> c.getCode().equals(code))
                    .findFirst();
        }
    }

    public VUCIS() {
        super();

        this.lightPreset = LightPreset.MANUAL;
        this.leftBrightField = LightColor.RED;
        this.coaxLight = LightColor.NONE;
        this.rightBrightField = LightColor.RED;
        this.leftDarkField = LightColor.NONE;
        this.rightDarkField = LightColor.NONE;
        this.lensType = LensType.TC54;
        this.coolingLeft = true;
        this.coolingRight = true;
    }

    @Override
    public String getTiViKey() {
        String key = "G_VUCIS";
        key += String.format("_%04d", getScanWidth());
        key += "_S";

        key += "_";
        key += getLights();

        key += "_" + getLensType().getCode();

        key += "_C";
        key += getMechaVersion();

        if (key.endsWith("_")) {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }

    @Override
    public Set<LightColor> getLightColors() {
        return getLights().chars()
                .mapToObj(c -> LightColor.findByCode((char) c))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    @Override
    public String getLightSources() {
        switch (getLensType()) {
            case TC54:
                return "20";
            case TC54L:
                return "2L";
            case TC80:
                return "30";
            case TC80L:
                return "3L";
            default:
                return "";
        }
    }

    @Override
    public String getLights() {
        String key = "";
        switch (getLightPreset()) {
            case CLOUDY_DAY: {
                key += "D";
                key += getLeftBrightField().getCode();
                key += getCoaxLight().getCode();
                key += getRightBrightField().getCode();
                key += "D";
                return key;
            }
            case SHAPE_FROM_SHADING: {
                if (getLeftBrightField().getCode() == 'R') {
                    return "RRSRR";
                }
                return "WWSWW";
            }
            default: {
                key += getLeftDarkField().getCode();
                key += getLeftBrightField().getCode();
                key += getCoaxLight().getCode();
                key += getRightBrightField().getCode();
                key += getRightDarkField().getCode();
                return key;
            }
        }
    }

    @Override
    public int getLedLines() {
        return (int) getLights().chars()
                .mapToObj(c -> LightColor.findByCode((char) c))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(l -> l != LightColor.NONE)
                .count();
    }

    /**
     * replaces parts of the mecha factor for VUCIS calculation:
     * - n(
     */
    @Override
    protected String prepareMechaFactor(String factor) {
        //small n: "LED in middle...", !X: "...that exists" (is not X)
        return factor.replace("n(?!X)", getLEDsInMiddle() + "");
    }

    /**
     * calculates the number of LEDs in the middle (bright field + coax)
     */
    private int getLEDsInMiddle() {
        int sum = 0;
        if (leftBrightField != LightColor.NONE) sum++;
        if (rightBrightField != LightColor.NONE) sum++;
        if (coaxLight != LightColor.NONE) sum++;
        return sum;
    }

    @Override
    public Optional<CameraLink> getCLCalc(int numOfPix) {
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

        long datarate = (long) getPhaseCount() * numOfPixNominal * getSelectedLineRate();
        LinkedList<CameraLink.Connection> connections = new LinkedList<>();
        int portLimit = 10;

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
            connections.add(new CameraLink.Connection(0, (char) (CameraLink.Port.DEFAULT_NAME + connections.stream()
                    .mapToInt(CameraLink.Connection::getPortCount)
                    .sum())));

            while (connections.getLast().getPortCount() <= portLimit - blockSize && i < taps) {
                for (int k = 0; k < blockSize; k++) {
                    if (k < getPhaseCount()) {
                        connections.getLast().addPorts(new CameraLink.Port(i * lval, (i + 1) * lval - 1));
                    } else {
                        connections.getLast().addPorts(new CameraLink.Port(0, 0));
                    }
                }
                i++;
            }
        }

        String notes = "LVAL(Modulo 8): " + lval + "\n" +
                getString("numPhases") + getPhaseCount() + "\n";

        CameraLink cameraLink = new CameraLink(datarate, numOfPixNominal, 85000000, notes);
        connections.forEach(cameraLink::addConnection);

        if (taps > (portLimit / blockSize) * 2) {
            throw new CISException("Number of required taps (" + taps * getPhaseCount() + ") is too high. Please reduce the data rate.");
        }
        if (getSelectedResolution().getActualResolution() >= 1200 && (numOfPix - 16 * getScanWidth() / BASE_LENGTH * 6 * 2) * getPhaseCount() * 2 > 327680) {
            throw new CISException("Out of Flash memory. Please reduce the scan width or resolution.");
        }

        return Optional.of(cameraLink);

    }

    @Override
    public double getGeometry(boolean coax) {
        return coax ? 0.229 : 0.252;
    }

    public LightPreset getLightPreset() {
        return this.lightPreset;
    }

    public void setLightPreset(LightPreset lightPreset) {
        this.lightPreset = lightPreset;
    }

    public LightColor getLeftBrightField() {
        return leftBrightField;
    }

    public void setLeftBrightField(LightColor leftBrightField) {
        this.leftBrightField = leftBrightField;
    }

    public LightColor getCoaxLight() {
        return coaxLight;
    }

    public void setCoaxLight(LightColor coaxLight) {
        this.coaxLight = coaxLight;
    }

    public LightColor getRightBrightField() {
        return rightBrightField;
    }

    public void setRightBrightField(LightColor rightBrightField) {
        this.rightBrightField = rightBrightField;
    }

    public LightColor getLeftDarkField() {
        return leftDarkField;
    }

    public void setLeftDarkField(LightColor leftDarkField) {
        this.leftDarkField = leftDarkField;
    }

    public LightColor getRightDarkField() {
        return rightDarkField;
    }

    public void setRightDarkField(LightColor rightDarkField) {
        this.rightDarkField = rightDarkField;
    }

    public LensType getLensType() {
        return lensType;
    }

    public void setLensType(LensType lensType) {
        this.lensType = lensType;
    }

    public boolean hasCoolingLeft() {
        return coolingLeft;
    }

    public void setCoolingLeft(boolean coolingLeft) {
        this.coolingLeft = coolingLeft;
    }

    public boolean hasCoolingRight() {
        return coolingRight;
    }

    public void setCoolingRight(boolean coolingRight) {
        this.coolingRight = coolingRight;
    }

    @Override
    public double getMaxLineRate() {
        AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
        return 1000 * Math.round(1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getPixelPerSensor() + 100) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
    }

    public int getColorCount() {
        List<LightColor> colors = Arrays.asList(getLeftBrightField(), getCoaxLight(), getRightBrightField(), getLeftDarkField(), getRightDarkField());
        return Math.max((int) colors.stream()
                .distinct()
                .filter(c -> c != LightColor.NONE)
                .count(), 1);
    }

    //should return false if code is unknown
    @Override
    protected boolean checkSpecificApplicability(String code) {
        return isValidLCode(code);
        //TODO expand
    }

    /**
     * checks whether the given code matches the current led selection
     */
    private boolean isValidLCode(String code) {
        switch (code.replaceAll("\\s", "")) {
            case "L>0":
                return getLedLines() > 0;
            case "L>0∧L<3":
                return getLedLines() > 0 && getLedLines() < 3;
            case "L==6":
                return getLedLines() == 6;
            case "L==4":
                return getLedLines() == 4;
            case "L==3":
                return getLedLines() == 3;
            default:
                return false;// no valid L code
        }
    }
}
