package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.*;
import de.tichawa.util.MathEval;

import java.util.*;
import java.util.stream.Collectors;

import static de.tichawa.cis.config.model.Tables.*;
import static org.jooq.impl.DSL.min;

/**
 * CIS subclass for all VUCIS objects
 */
public class VUCIS extends CisWith5Lights {

    public static final int MAX_SCAN_WIDTH_WITH_COAX = 1040;
    public static final int MAX_SCAN_WIDTH_WITH_SFS = 1820;

    /**
     * Available resolutions for VUCIS
     */
    private static final List<Resolution> resolutions;

    static {
        resolutions = Arrays.asList(
                new CIS.Resolution(1200, 1200, true, 0.25, 0.02115),
                new CIS.Resolution(1200, 1200, false, 0.5, 0.02115),
                new CIS.Resolution(600, 600, false, 1.0, 0.0423),
                new CIS.Resolution(400, 1200, false, 1.0, 0.0635),
                new CIS.Resolution(300, 300, false, 1.5, 0.0847),
                new CIS.Resolution(200, 600, false, 2.0, 0.125),
                new CIS.Resolution(150, 300, false, 3.0, 0.167),
                new CIS.Resolution(100, 300, false, 4.0, 0.25),
                new CIS.Resolution(75, 300, false, 6.0, 0.339),
                new CIS.Resolution(50, 300, false, 8.0, 0.5),
                new CIS.Resolution(25, 300, false, 10.0, 1.0));
    }

    private LensType lensType;

    public VUCIS() {
        this.lensType = LensType.TC54;
        this.setSelectedResolution(resolutions.get(0));
        this.setSelectedLineRate((int) getMaxLineRate());
    }

    protected VUCIS(VUCIS cis) {
        super(cis);
        this.lensType = cis.lensType;
    }

    /**
     * determine L value for each light: RGB = 3, IRUV = 2, Rebz8 = 8, other = 1
     */
    public static int getLValue(LightColor lightColor) {
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
     * returns the smaller distance lens (TC54 or TC54L) for the given lens type.
     */
    private static LensType getLowDistanceLens(LensType lensType) {
        switch (lensType) {
            case TC54:
                return LensType.TC54;
            case TC54L:
            case TC100L:
                return LensType.TC54L;
            default:
                throw new IllegalArgumentException("unsupported lens type");
        }
    }

    public static List<Resolution> getResolutions() {
        return resolutions;
    }

    /**
     * returns whether the given light color is a valid option for coax lighting for VUCIS
     */
    public static boolean isVUCISCoaxLightColor(LightColor lightColor) {
        if (lightColor == LightColor.RGB8) { // no rebz 8 for coax
            return false;
        }
        return isVUCISLightColor(lightColor);// rest same as for other positions
    }

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

    //should return false if code is unknown
    @Override
    protected boolean checkSpecificApplicability(String code) {
        return isValidLCode(code) || isValidCCode(code) || isValidMathExpression(code) || isValidSCode(code);
        //expand if necessary
    }

    /**
     * Returns a copy of this VUCIS
     */
    @Override
    public CIS copy() {
        return new VUCIS(this);
    }

    /**
     * returns the base case length: scan width or next bigger size if there is shape from shading on bright field
     */
    @Override
    protected int getBaseCaseLength() {
        return getScanWidth() + (hasBrightFieldShapeFromShading() ? BASE_LENGTH : 0);
    }

    /**
     * returns the printout for the case: aluminum case with width x height x depth and sealed glass pane
     */
    @Override
    protected String getCasePrintout() {
        return Util.getString("caseDimensions") + ":\n\t" +
                Util.getString("width") + ": ~ " + (getBaseCaseLength() + getExtraCaseLength()) + "mm +/-3mm\n\t" +
                Util.getString("height") + ": ~ 92mm +/-2mm\n\t" +
                Util.getString("depth") + ": ~ 80mm +/-2mm\n" +
                Util.getString("aluminumCaseGlassPane") + "\n";
    }

    /**
     * returns the case profile string for print out: 92x80mm
     */
    @Override
    protected String getCaseProfile() {
        return Util.getString("aluminumCaseVUCIS");
    }

    /**
     * returns the depth of field for print out.
     */
    @Override
    protected double getDepthOfField() {
        return lensType.isLongDOF() ? getSelectedResolution().getDepthOfField() * 2 : getSelectedResolution().getDepthOfField();
    }

    /**
     * returns the geometry correction string for print out
     */
    @Override
    protected String getGeometryCorrectionString() {
        return Util.getString("xYCorrection");
    }

    /**
     * determines the L value for the current light selection (sum of individual L values).
     * If the sum is greater than 10, 10 is returned instead since this is the maximum allowed LED channels
     */
    @Override
    public int getLedLines() {
        int sum = getLights().chars().mapToObj(c -> LightColor.findByCode((char) c))
                .filter(Optional::isPresent).map(Optional::get)
                .mapToInt(VUCIS::getLValue).sum();
        return Math.min(sum, 10); //max 10 allowed
    }

    @Override
    public String getLightSources() {
        return lensType.code;
    }

    /**
     * returns the minimum frequency by calculating for each light and taking the minimum of:
     * 100 * I_p * gamma * n * tau * S_v / (1.5 * m)
     * <p>
     * will return the double max value if there is no light
     */
    @Override
    protected double getMinFreq(CISCalculation calculation) {
        return Collections.min(Arrays.asList(
                getMinFreqForLight(darkFieldLeft, true, false, calculation),
                getMinFreqForLight(brightFieldLeft, false, false, calculation),
                getMinFreqForLight(coaxLight, false, true, calculation),
                getMinFreqForLight(brightFieldRight, false, false, calculation),
                getMinFreqForLight(darkFieldRight, true, false, calculation)));
    }

    /**
     * returns the scan distance string for print out: 10+-2mm or 23+-3 mm
     */
    @Override
    protected String getScanDistanceString() {
        return lensType.getWorkingDistanceString() + " +/- " + (lensType.workingDistance / 10 + 1) + "mm";
    }

    /**
     * returns a fixed weight string depending on CIS size since calculation is not perfect due to many missing weight values
     */
    @Override
    protected String getWeightString(CISCalculation calculation) {
        // sfs has next size, otherwise index -1
        return CisWith5Lights.WEIGHTS[getScanWidth() / BASE_LENGTH - (isShapeFromShading() ? 0 : 1)] + "kg";
    }

    /**
     * returns whether the VUCIS has LEDs or not
     */
    @Override
    protected boolean hasLEDs() {
        return getLedLines() > 0;
    }

    /**
     * replaces parts of the mecha factor for VUCIS calculation:
     * - n(?!X) replaced with number of LEDs in the middle
     * - C replaced with number of coolings
     */
    @Override
    protected String prepareMechanicsFactor(String factor) {
        //small n: "LED in middle...", !X: "...that exists" (is not X)
        return factor.replace("n(?!X)", getNonSfsLEDsInMiddle() + "")
                .replace("C", getCoolingCount() + "");
    }

    /**
     * returns whether a bigger size for the housing is required (that is for shape from shading)
     *
     * @return true if a bigger size housing is required i.e. if there is shape from shading or false otherwise
     */
    @Override
    public boolean requiresNextSizeHousing() {
        return isShapeFromShading();
    }

    /**
     * Returns whether this VUCIS uses an MDR camera link connection on the CIS side
     *
     * @return false because VUCIS uses SDR
     */
    @Override
    public boolean usesMdrCameraLinkOnCisSide() {
        return false; // VUCIS uses SDR not MDR
    }

    /**
     * determines the number of lights with the given color on bright field
     */
    private int getBrightFieldLightsWithColor(LightColor lightColor) {
        return (getLights().charAt(1) == lightColor.getCode() ? 1 : 0) +
                (getLights().charAt(3) == lightColor.getCode() ? 1 : 0);
    }

    /**
     * calculates the number of coolings (one left and one right if it is selected)
     */
    public int getCoolingCount() {
        return (coolingLeft ? 1 : 0) + (coolingRight ? 1 : 0);
    }

    /**
     * determines the number of lights with the given color on dark field
     */
    private int getDarkFieldLightsWithColor(LightColor lightColor) {
        return (getLights().charAt(0) == lightColor.getCode() ? 1 : 0) +
                (getLights().charAt(4) == lightColor.getCode() ? 1 : 0);
    }

    /**
     * returns the geometry lighting factor for the given VUCIS led:
     * if cloudy day is selected -> 0.04
     * if given LED is shape from shading -> 0.08
     * if given LED is coax -> 0.025
     * if given LED is darkfield -> 0.08
     * if given LED is brightfield -> 0.13
     */
    private double getGeometryFactor(LightColor lightColor, boolean isDarkfield, boolean isCoax) {
        if (isCloudyDay()) return 0.04; // cloudy day -> 0.04 for all LEDs
        if (lightColor.isShapeFromShading()) return 0.08; // LED is sfs -> 0.08
        if (isCoax) return 0.025; // LED is coax -> 0.025
        if (isDarkfield) return 0.08; // LED is on df -> 0.08
        return 0.13; // else: LED is on bf -> 0.13
    }

    /**
     * reads the I_p value from the database for the given light color
     */
    private double getIpValue(LightColor lightColor, boolean isDarkfield) {
        //special handling for sfs lights as these have different codes in the database on bright and dark field
        String shortHand = lightColor.isShapeFromShading() ?
                lightColor.getCode() + (isDarkfield ? "D" : "B") // add D for darkfield, B for brightfield to the short code
                : lightColor.getShortHand(); // normal short hand for all other lights
        return Util.getDatabase().orElseThrow(() -> new IllegalStateException("database not found"))
                .select(min(PRICE.PHOTO_VALUE))
                .from(PRICE.join(ELECTRONIC).on(PRICE.ART_NO.eq(ELECTRONIC.ART_NO)))
                .where(ELECTRONIC.CIS_TYPE.eq(getClass().getSimpleName()))
                .and(ELECTRONIC.CIS_LENGTH.eq(getScanWidth()))
                .and(ELECTRONIC.MULTIPLIER.eq(shortHand))
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("light color not found in database"))
                .value1();
    }

    public LensType getLensType() {
        return lensType;
    }

    public void setLensType(LensType lensType) {
        LensType oldValue = this.lensType;
        this.lensType = lensType;
        observers.firePropertyChange(PROPERTY_LENS_TYPE, oldValue, lensType);
    }

    /**
     * returns the minimum frequency for the given light by calculating:
     * 100 * I_p * gamma * n * tau * S_v / (1.5 * m)
     */
    private double getMinFreqForLight(LightColor lightColor, boolean isDarkfield, boolean isCoax, CISCalculation calculation) {
        if (lightColor == LightColor.NONE)
            return Double.MAX_VALUE;
        double I_p = getIpValue(lightColor, isDarkfield);
        double gamma = getGeometryFactor(lightColor, isDarkfield, isCoax);
        double n = getNFactor(isDarkfield, isCoax);
        double tau = calculation.mechanicSums[4]; // only Lens will have a photo value in mecha table
        double S_v = getSensitivityFactor();
        double m = getPhaseCount();
        return 100 * I_p * gamma * n * tau * S_v / (1.5 * m);
    }

    /**
     * returns the n factor for the given light depending on its position:
     * if it is the coax light -> n is always 1
     * cloudy day -> both bright field lights -> n=2
     * shape from shading -> if phase count 1 -> 4 else 1
     * if both dark/bright field -> 2
     * else -> 1
     */
    private double getNFactor(boolean isDarkfield, boolean isCoax) {
        if (isCoax)
            return 1; // coax light always factor 1
        if (isCloudyDay())
            return 2; // cloudy day only both or no lights on bright field -> 2 since no lights won't reach this method
        if (isShapeFromShading())
            if (getPhaseCount() == 1)
                return 4; // shape from shading and 1 phase -> multiply by 4
            else return 1; // shape from shading and not 1 phase -> 1
        if (isDarkfield && darkFieldLeft != LightColor.NONE && darkFieldLeft == darkFieldRight ||
                !isDarkfield && brightFieldLeft != LightColor.NONE && brightFieldLeft == brightFieldRight)
            // if this is darkfield and both dark fields same -> 2 (same for brightfield)
            return 2;
        return 1; // default is 1 for any other case
    }

    /**
     * calculates the number of LEDs in the middle (bright field + coax) that are not shape from shading lights
     */
    private int getNonSfsLEDsInMiddle() {
        int sum = 0;
        if (brightFieldLeft != LightColor.NONE && !brightFieldLeft.isShapeFromShading()) sum++;
        if (brightFieldRight != LightColor.NONE && !brightFieldRight.isShapeFromShading()) sum++;
        if (coaxLight != LightColor.NONE) sum++;
        return sum;
    }

    private boolean hasBrightFieldShapeFromShading() {
        return brightFieldLeft.isShapeFromShading() || brightFieldRight.isShapeFromShading();
    }

    /**
     * determines if there is a shape from shading by checking if any light color is a shape from shading one
     */
    public boolean isShapeFromShading() {
        return brightFieldLeft.isShapeFromShading() || darkFieldLeft.isShapeFromShading()
                || brightFieldRight.isShapeFromShading() || darkFieldRight.isShapeFromShading()
                || coaxLight.isShapeFromShading(); //coax sfs should not be possible anyway
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

    private boolean isValidSCode(String code) {
        if (!"S".equals(code))
            return false; // not a valid S code if it isn't 'S'
        // code is S here -> check if valid
        if (!hasBrightFieldShapeFromShading())
            return true; // if S code but no dark field sfs -> code is ok
        // if dark field sfs -> not valid -> next larger size needed
        throw new CISNextSizeException("this component needs one size larger");
    }

    /**
     * sets the left bright field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the left cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the left
     */
    public void setBrightFieldLeft(LightColor brightFieldLeft) {
        // set to 10mm working distance if shape from shading
        if (brightFieldLeft.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update other stuff and notify observers
        super.setBrightFieldLeft(brightFieldLeft);
    }

    /**
     * sets the right bright field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the right cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the right
     */
    public void setBrightFieldRight(LightColor brightFieldRight) {
        // set to 10mm working distance if shape from shading
        if (brightFieldRight.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update other stuff and notify observers
        super.setBrightFieldRight(brightFieldRight);
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
            // set left bright field to NONE if it is shape from shading (no left sided sfs + coax)
            if (brightFieldLeft.isShapeFromShading())
                setBrightFieldLeft(LightColor.NONE);
        }
        // update other stuff and notify observers
        super.setCoaxLight(coaxLight);
    }

    /**
     * sets the left dark field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the left cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the left
     */
    public void setDarkFieldLeft(LightColor darkFieldLeft) {
        // set to 10mm working distance if shape from shading
        if (darkFieldLeft.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update other stuff and notify observers
        super.setDarkFieldLeft(darkFieldLeft);
    }

    /**
     * sets the right dark field light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the right cooling to true (need cooling if there is a light)
     * - setting the lens working distance to 10mm if there is shape from shading
     * or resets the cooling if there is no light on the right
     */
    public void setDarkFieldRight(LightColor darkFieldRight) {
        // set to 10mm working distance if shape from shading
        if (darkFieldRight.isShapeFromShading())
            setLensType(getLowDistanceLens(lensType));
        // update other stuff and notify observers
        super.setDarkFieldRight(darkFieldRight);
    }

    @Override
    protected String getLensTypeCode() {
        return getLensType().getCode();
    }

    /**
     * creates the lights code for the current selection by concatenation of single light codes from left dark field, left bright field, coax, right bright field, right dark field.
     * Special code for cloudy day: 'D' replacing both dark field light codes.
     * Special code for shape from shading: 'S' in middle if there is no coax light, on left dark field otherwise (coax is left so there can be no left dark field)
     */
    @Override
    public String getLights() {
        if (isShapeFromShading()) {
            String key = "";
            // if no coax -> S in middle
            if (!hasCoax()) {
                key += getDarkFieldLeft().getCode();
                key += getBrightFieldLeft().getCode();
                key += 'S';
                key += getBrightFieldRight().getCode();
                key += getDarkFieldRight().getCode();
                return key;
            } // else: (there is coax light) -> (sfs only on right side) -> S on dark field left (no coax and dark field so dark is free)
            key += 'S';
            key += getBrightFieldLeft().getCode();
            key += getCoaxLight().getCode();
            key += getBrightFieldRight().getCode();
            key += getDarkFieldRight().getCode();
            return key;
        } // else: no shape from shading
        return super.getLights();
    }

    /**
     * replaces parts of the elect factor for VUCIS calculation:
     * - Light Code with the number of lights (e.g. AM with number of red lights)
     * - shape from shading light special factors to count all bright / dark field lights
     */
    @Override
    protected String prepareElectronicsMultiplier(String factor) {
        factor = super.prepareElectronicsMultiplier(factor); // replace light codes with amounts
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

    @Override
    protected int getMaxScanWidthWithCoax() {
        return MAX_SCAN_WIDTH_WITH_COAX;
    }

    /**
     * calculates the number of needed CPUCLinks.
     * Will represent the data shown in the specification table for now but this might change in the future (may need more CPUCLinks than in table).
     * Therefore, this won't read data from the database and there might be discrepancies!
     */
    protected int getNumOfCPUCLink() {
        switch (getPhaseCount()) {
            case 1:
            case 2:
                return getScanWidth() >= 1820 ? 2 : 1;
            case 3:
                return getScanWidth() >= 1560 ? 2 : 1;
            case 4:
                return getScanWidth() >= 1300 ? 2 : 1;
            case 5:
                return getScanWidth() >= 1820 ? 3 : (getScanWidth() >= 1040 ? 2 : 1);
            case 6:
                return getScanWidth() >= 1820 ? 4 : (getScanWidth() >= 1300 ? 3 : getScanWidth() >= 780 ? 2 : 1);
            default:
                throw new IllegalStateException("illegal phase count: " + getPhaseCount() + ", must be in [1,6]");
        }
    }

    /**
     * sets the given light color to the given light by calling the corresponding Setter
     */
    public void setLightColor(String light, LightColor value) {
        super.setLightColor(light, value);
        updateScanWidth();
    }

    private void updateScanWidth() {
        if (isShapeFromShading() && getScanWidth() > MAX_SCAN_WIDTH_WITH_SFS)
            setScanWidth(MAX_SCAN_WIDTH_WITH_SFS);
    }

    /**
     * enumeration of available lens types for VUCIS
     */
    public enum LensType {
        TC54("20", false, true, 10),
        TC54L("2L", true, true, 10),
        TC100L("3L", true, false, 23);

        private final String code;
        private final boolean hasShortDOFVersion;
        private final boolean longDOF;
        private final int workingDistance;

        /**
         * creates the lens type with the given parameters.
         *
         * @param code               the code of the lens
         * @param longDOF            whether the lens is the long DOF version
         * @param hasShortDOFVersion whether there is a short DOF version for this lens
         * @param workingDistance    the working distance of the lens
         */
        LensType(String code, boolean longDOF, boolean hasShortDOFVersion, int workingDistance) {
            this.code = code;
            this.longDOF = longDOF;
            this.hasShortDOFVersion = hasShortDOFVersion;
            this.workingDistance = workingDistance;
        }

        /**
         * Searches a lens type with the given working distance and longDOF value.
         * Filters all lens types by working distance first. If only one is left it is returned (disregarding the longDOF value).
         * If there are more than one lens types with the correct working distance, they are filtered by longDOF value and returned.
         *
         * @param workingDistanceString the working distance string of the lens type that is returned
         * @param longDOF               the longDOF value of the lens type that is returned if there is more than one lens type with the given working distance
         * @return the found lens type or an empty optional
         */
        public static Optional<LensType> findLens(String workingDistanceString, boolean longDOF) {
            int workingDistance = fromWorkingDistanceString(workingDistanceString);
            List<LensType> lensesWithCorrectWorkingDistance = Arrays.stream(LensType.values()).filter(l -> l.workingDistance == workingDistance).collect(Collectors.toList());
            // check if there is only one (e.g. TC100) -> return it disregarding DOF
            if (lensesWithCorrectWorkingDistance.size() == 1)
                return Optional.of(lensesWithCorrectWorkingDistance.get(0));
            // else: filter by DOF and return
            return lensesWithCorrectWorkingDistance.stream().filter(l -> l.longDOF == longDOF).findFirst();
        }

        /**
         * Extracts the working distance from the given working distance string
         *
         * @param workingDistanceString the working distance string of a lens created by {@link #getWorkingDistanceString()}
         * @return the working distance value
         */
        private static int fromWorkingDistanceString(String workingDistanceString) {
            return Integer.parseInt(workingDistanceString.substring(0, workingDistanceString.length() - 2));
        }

        public String getCode() {
            return code;
        }

        public String getWorkingDistanceString() {
            return workingDistance + "mm";
        }

        /**
         * returns whether a short version exists for this lens type
         *
         * @return true if there exists a short version of this lens type and false otherwise
         */
        public boolean hasShortDOFVersion() {
            return hasShortDOFVersion;
        }

        /**
         * returns whether this lens is the long version
         *
         * @return true if this lens is the long version and false otherwise
         */
        public boolean isLongDOF() {
            return longDOF;
        }
    }


}