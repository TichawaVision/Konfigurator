package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.util.MathEval;

import java.util.*;
import java.util.stream.Collectors;

public class VUCIS extends CIS {

    public static final int MAX_SCAN_WIDTH_WITH_COAX = 1040;

    private LightColor leftBrightField;
    private LightColor coaxLight;
    private LightColor rightBrightField;
    private LightColor leftDarkField;
    private LightColor rightDarkField;
    private LensType lensType;
    private boolean coolingLeft;
    private boolean coolingRight;
    private boolean cloudyDay;

    /**
     * returns whether the given light color is a valid option for VUCIS
     */
    public static boolean isVUCISLightColor(LightColor lightColor) {
        switch (lightColor) {
            case NONE:
            case RED:
            case GREEN:
            case BLUE:
            case YELLOW:
            case WHITE:
            case IR:
                //case IR950: //maybe later
            case UVA:
                //case VERDE: //maybe later
            case RGB:
            case RGB_S:
            case RGB8:
            case IRUV:
            case WHITE_SFS:
            case RED_SFS:
                return true;
            default:
                return false;
        }
    }

    /**
     * returns whether the given light color is a valid option for coax lighting for VUCIS
     */
    public static boolean isVUCISCoaxLightColor(LightColor lightColor) {
        switch (lightColor) {
            case RGB8: // no rebz 8 for coax
                return false;
            default: // rest same as for other positions
                return isVUCISLightColor(lightColor);
        }
    }

    /**
     * determines if there is a shape from shading by checking if any light color is a shape from shading one
     */
    public boolean isShapeFromShading() {
        return leftBrightField.isShapeFromShading() || leftDarkField.isShapeFromShading()
                || rightBrightField.isShapeFromShading() || rightDarkField.isShapeFromShading()
                || coaxLight.isShapeFromShading(); //coax sfs should not be possible anyway
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

        this.setExternalTrigger(false);

        this.leftBrightField = LightColor.RED;
        this.coaxLight = LightColor.NONE;
        this.rightBrightField = LightColor.RED;
        this.leftDarkField = LightColor.NONE;
        this.rightDarkField = LightColor.NONE;
        this.lensType = LensType.TC54;
        this.coolingLeft = true;
        this.coolingRight = true;
        this.setScanWidth(520);
        this.setPhaseCount(1);
        this.setCooling(Cooling.LICO);
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

    /**
     * creates the lights code for the current selection by concatenation of single light codes from left dark field, left bright field, coax, right bright field, right dark field.
     * Special code for cloudy day: 'D' replacing both dark field light codes.
     * Special code for shape from shading: 'S' in middle if there is no coax light, on left dark field otherwise (coax is left so there can be no left dark field)
     */
    @Override
    public String getLights() {
        String key = "";
        if (cloudyDay) {
            key += "D";
            key += getLeftBrightField().getCode();
            key += getCoaxLight().getCode();
            key += getRightBrightField().getCode();
            key += "D";
            return key;
        }
        if (isShapeFromShading()) {
            // if no coax -> S in middle
            if (!hasCoax()) {
                key += getLeftDarkField().getCode();
                key += getLeftBrightField().getCode();
                key += 'S';
                key += getRightBrightField().getCode();
                key += getRightDarkField().getCode();
                return key;
            }
            //else: (there is coax light) -> (sfs only on right side) -> S on dark field left (no coax and dark field so dark is free)
            key += 'S';
            key += getLeftBrightField().getCode();
            key += getCoaxLight().getCode();
            key += getRightBrightField().getCode();
            key += getRightDarkField().getCode();
            return key;
        }
        //default: just take the code from the lights
        key += getLeftDarkField().getCode();
        key += getLeftBrightField().getCode();
        key += getCoaxLight().getCode();
        key += getRightBrightField().getCode();
        key += getRightDarkField().getCode();
        return key;
    }

    /**
     * determines the L value for the current light selection (sum of individual L values)
     */
    @Override
    public int getLedLines() {
        return getLights().chars().mapToObj(c -> LightColor.findByCode((char) c))
                .filter(Optional::isPresent).map(Optional::get)
                .mapToInt(this::getLValue).sum();
    }

    /**
     * determine L value for each light: RGB = 3, IRUV = 2, Rebz8 = 8, other = 1
     */
    private int getLValue(LightColor lightColor) {
        switch (lightColor) {
            case NONE:
                return 0;
            case IRUV:
                return 2;
            case RGB:
            case RGB_S:
                return 3;
            case RGB8:
                return 8;
            default:
                return 1;
        }
    }

    /**
     * replaces parts of the mecha factor for VUCIS calculation:
     * - n(?!X) replaced with number of LEDs in the middle
     * - C replaced with number of coolings
     */
    @Override
    protected String prepareMechaFactor(String factor) {
        //small n: "LED in middle...", !X: "...that exists" (is not X)
        return factor.replace("n(?!X)", getLEDsInMiddle() + "")
                .replace("C", getCoolingCount() + "");
    }

    /**
     * replaces parts of the elect factor for VUCIS calculation:
     * - Light Code with the number of lights (e.g. AM with number of red lights)
     * - shape from shading light special factors to count all bright / dark field lights
     */
    @Override
    protected String prepareElectFactor(String factor) {
        if (Arrays.stream(LightColor.values()).map(LightColor::getShortHand).anyMatch(factor::equals)) {
            return getLights().chars().mapToObj(c -> LightColor.findByCode((char) c)).filter(LightColor.findByShortHand(factor)::equals).count() + "";
        }
        switch (factor) {
            case "RB":
                return getBrightFieldLightsWithColor(LightColor.RED_SFS) + "";
            case "WB":
                return getBrightFieldLightsWithColor(LightColor.WHITE_SFS) + "";
            case "RD":
                return getDarkFieldLightsWithColor(LightColor.RED_SFS) + "";
            case "WD":
                return getDarkFieldLightsWithColor(LightColor.WHITE_SFS) + "";
            default:
                return factor;
        }
    }

    /**
     * determines the number of lights with the given color on bright field
     */
    private int getBrightFieldLightsWithColor(LightColor lightColor) {
        return (getLights().charAt(1) == lightColor.getCode() ? 1 : 0) +
                (getLights().charAt(3) == lightColor.getCode() ? 1 : 0);
    }

    /**
     * determines the number of lights with the given color on dark field
     */
    private int getDarkFieldLightsWithColor(LightColor lightColor) {
        return (getLights().charAt(0) == lightColor.getCode() ? 1 : 0) +
                (getLights().charAt(4) == lightColor.getCode() ? 1 : 0);
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

    public LightColor getLeftBrightField() {
        return leftBrightField;
    }

    private LensType getLowDistanceLens(LensType lensType) {
        switch (lensType) {
            case TC54:
            case TC80:
                return LensType.TC54;
            case TC54L:
            case TC80L:
                return LensType.TC54L;
            default:
                throw new IllegalArgumentException("unsupported lens type");
        }
    }

    /**
     * sets the left bright field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the left cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the left
     */
    public void setLeftBrightField(LightColor leftBrightField) {
        // set/reset left cooling
        if (leftBrightField != LightColor.NONE)
            setCoolingLeft(true);
        else if (coaxLight == LightColor.NONE && leftDarkField == LightColor.NONE)
            setCoolingLeft(false); //set no left cooling if not needed
        // set to 10mm working distance if shape from shading
        if (leftBrightField.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update value and notify observers
        LightColor oldValue = this.leftBrightField;
        this.leftBrightField = leftBrightField;
        observers.firePropertyChange("leftBrightField", oldValue, leftBrightField);
    }

    public LightColor getCoaxLight() {
        return coaxLight;
    }

    /**
     * sets the coax light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the left dark field light to NONE (there can be no coax with left dark field)
     * - setting the left bright field to a non-shape-from-shading color if it isn't already (no coax and left sided shape from shading)
     * - setting the scan width to the max allowed value for coax if it is bigger
     * - setting the left cooling to true (coax needs left cooling)
     * or resets the cooling if coax is deselected and there is no light on the left
     */
    public void setCoaxLight(LightColor coaxLight) {
        if (coaxLight != LightColor.NONE) { // selected coax light
            // set left dark field to NONE (no coax + left dark field)
            setLeftDarkField(LightColor.NONE);
            // set left bright field to NONE if it is shape from shading (no left sided sfs + coax)
            if (leftBrightField.isShapeFromShading())
                setLeftBrightField(LightColor.NONE);
            // set scan width to max allowed value if it is bigger
            if (getScanWidth() > MAX_SCAN_WIDTH_WITH_COAX)
                setScanWidth(MAX_SCAN_WIDTH_WITH_COAX);
            // set left cooling
            setCoolingLeft(true);
        } else if (leftBrightField == LightColor.NONE && leftDarkField == LightColor.NONE)
            setCoolingLeft(false); // set no left cooling if not needed
        //update value and notify observers
        LightColor oldValue = this.coaxLight;
        this.coaxLight = coaxLight;
        observers.firePropertyChange("coaxLight", oldValue, coaxLight);
    }

    public LightColor getRightBrightField() {
        return rightBrightField;
    }

    /**
     * sets the right bright field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the right cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the right
     */
    public void setRightBrightField(LightColor rightBrightField) {
        // set/reset cooling
        if (rightBrightField != LightColor.NONE)
            setCoolingRight(true);
        else if (rightDarkField == LightColor.NONE)
            setCoolingRight(false); // set no right cooling if not needed
        // set to 10mm working distance if shape from shading
        if (rightBrightField.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update value and notify observers
        LightColor oldValue = this.rightBrightField;
        this.rightBrightField = rightBrightField;
        observers.firePropertyChange("rightBrightField", oldValue, rightBrightField);
    }

    public LightColor getLeftDarkField() {
        return leftDarkField;
    }

    /**
     * sets the left dark field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the left cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the left
     */
    public void setLeftDarkField(LightColor leftDarkField) {
        // set/reset left cooling
        if (leftDarkField != LightColor.NONE)
            setCoolingLeft(true);
        else if (coaxLight == LightColor.NONE && leftBrightField == LightColor.NONE)
            setCoolingLeft(false); //set no left cooling if not needed
        // set to 10mm working distance if shape from shading
        if (leftDarkField.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update value and notify observers
        LightColor oldValue = this.leftDarkField;
        this.leftDarkField = leftDarkField;
        observers.firePropertyChange("leftDarkField", oldValue, leftDarkField);
    }

    public LightColor getRightDarkField() {
        return rightDarkField;
    }

    /**
     * sets the right dark field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the right cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the right
     */
    public void setRightDarkField(LightColor rightDarkField) {
        // set/reset cooling
        if (rightDarkField != LightColor.NONE)
            setCoolingRight(true);
        else if (rightBrightField == LightColor.NONE)
            setCoolingRight(false); // set no right cooling if not needed
        // set to 10mm working distance if shape from shading
        if (rightDarkField.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update value and notify observers
        LightColor oldValue = this.rightDarkField;
        this.rightDarkField = rightDarkField;
        observers.firePropertyChange("rightDarkField", oldValue, rightDarkField);
    }

    public LensType getLensType() {
        return lensType;
    }

    public void setLensType(LensType lensType) {
        LensType oldValue = this.lensType;
        this.lensType = lensType;
        observers.firePropertyChange("lensType", oldValue, lensType);
    }

    public boolean hasCoolingLeft() {
        return coolingLeft;
    }

    public void setCoolingLeft(boolean coolingLeft) {
        boolean oldValue = this.coolingLeft;
        this.coolingLeft = coolingLeft;
        observers.firePropertyChange("coolingLeft", oldValue, coolingLeft);
    }

    public boolean hasCoolingRight() {
        return coolingRight;
    }

    public void setCoolingRight(boolean coolingRight) {
        boolean oldValue = this.coolingRight;
        this.coolingRight = coolingRight;
        observers.firePropertyChange("coolingRight", oldValue, coolingRight);
    }

    public boolean isCloudyDay() {
        return cloudyDay;
    }

    public void setCloudyDay(boolean cloudyDay) {
        if (cloudyDay) { // cloudy day has no dark field
            setLeftDarkField(LightColor.NONE);
            setRightDarkField(LightColor.NONE);
        }
        // update value and notify observers
        boolean oldValue = this.cloudyDay;
        this.cloudyDay = cloudyDay;
        observers.firePropertyChange("cloudyDay", oldValue, cloudyDay);
    }

    public boolean hasCoax() {
        return coaxLight != LightColor.NONE;
    }

    /**
     * calculates the number of coolings (one left and one right if it is selected)
     */
    public int getCoolingCount() {
        return (coolingLeft ? 1 : 0) + (coolingRight ? 1 : 0);
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
        return isValidLCode(code) || isValidCCode(code) || isValidMathExpression(code);
        //expand if necessary
    }

    /**
     * checks whether the given math code matches the current selection.
     * currently only checks for <
     */
    private boolean isValidMathExpression(String code) {
        String expression = code.replace("N", getScanWidth() + "")
                .replace("C", getCoolingCount() + "");
        //expand if necessary
        String[] splitted = expression.split("<");
        if (splitted.length < 2)
            return false;
        for (int i = 0; i < splitted.length - 1; i++) {
            try {
                if (MathEval.evaluate(splitted[i]) > MathEval.evaluate(splitted[i + 1]))
                    return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * checks whether the given code matches the current cooling selection
     */
    private boolean isValidCCode(String code) {
        switch (code) {
            case "C":
                return getCoolingCount() > 0;
            case "C_L":
                return hasCoolingLeft();
            case "C_R":
                return hasCoolingRight();
            default:
                return false;
        }
    }

    /**
     * checks whether the given code matches the current led selection
     */
    private boolean isValidLCode(String code) {
        if (code.charAt(0) != 'L')
            return false;
        try {
            switch (code.charAt(1)) {
                case '>':
                    return getLedLines() > Integer.parseInt(code.split(">")[1]);
                case '<':
                    return getLedLines() < Integer.parseInt(code.split("<")[1]);
                case '=': // -> '=='
                    return getLedLines() == Integer.parseInt(code.split("==")[1]);
                default: // unknown code
                    System.err.println("unknown L clause: " + code);
                    return false;
            }
        } catch (NumberFormatException e) { //not a number behind the math operator
            return false;
        }
    }

    /**
     * sets the given light color to the given light by calling the corresponding Setter
     */
    public void setLightColor(String light, LightColor value) {
        switch (light) {
            case "DarkFieldLeft":
                setLeftDarkField(value);
                return;
            case "BrightFieldLeft":
                setLeftBrightField(value);
                return;
            case "Coax":
                setCoaxLight(value);
                return;
            case "BrightFieldRight":
                setRightBrightField(value);
                return;
            case "DarkFieldRight":
                setRightDarkField(value);
                return;
            default:
                throw new IllegalArgumentException("light does not exist: " + light);
        }
    }

    @Override
    public void setSelectedResolution(Resolution resolution) {
        super.setSelectedResolution(resolution);
        this.setTransportSpeed((int) (this.getSelectedResolution().getPixelSize() * this.getSelectedLineRate()) * 1000);
    }
}
