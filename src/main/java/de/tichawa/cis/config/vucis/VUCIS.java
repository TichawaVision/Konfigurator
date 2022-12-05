package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.util.MathEval;

import java.util.*;
import java.util.stream.*;

import static de.tichawa.cis.config.model.Tables.*;
import static org.jooq.impl.DSL.min;

public class VUCIS extends CIS {

    public static final int MAX_SCAN_WIDTH_WITH_COAX = 1040;
    public static final int MAX_SCAN_WIDTH_WITH_SFS = 1820;
    public static final long PIXEL_CLOCK_NORMAL = 85000000;
    public static final long PIXEL_CLOCK_REDUCED = 53000000;
    public static final List<Resolution> resolutions;
    public static final List<Integer> VALID_MODS;

    private LightColor leftBrightField;
    private LightColor coaxLight;
    private LightColor rightBrightField;
    private LightColor leftDarkField;
    private LightColor rightDarkField;
    private LensType lensType;
    private boolean coolingLeft;
    private boolean coolingRight;
    private boolean cloudyDay;
    private boolean reducedPixelClock;
    private int mod;

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
        VALID_MODS = Arrays.asList(1, 4, 8, 16, 32);
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
        this.setSelectedResolution(resolutions.get(0));
        this.setPhaseCount(1);
        this.setSelectedLineRate((int) getMaxLineRate());
        this.setCooling(Cooling.LICO);
        this.setMod(16);
    }

    public static List<Resolution> getResolutions() {
        return resolutions;
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
     * replaces parts of the mecha factor for VUCIS calculation:
     * - n(?!X) replaced with number of LEDs in the middle
     * - C replaced with number of coolings
     */
    @Override
    protected String prepareMechaFactor(String factor) {
        //small n: "LED in middle...", !X: "...that exists" (is not X)
        return factor.replace("n(?!X)", getNonSfsLEDsInMiddle() + "")
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
     * calculates the number of LEDs in the middle (bright field + coax) that are not shape from shading lights
     */
    private int getNonSfsLEDsInMiddle() {
        int sum = 0;
        if (leftBrightField != LightColor.NONE && !leftBrightField.isShapeFromShading()) sum++;
        if (rightBrightField != LightColor.NONE && !rightBrightField.isShapeFromShading()) sum++;
        if (coaxLight != LightColor.NONE) sum++;
        return sum;
    }

    /**
     * calculates the number of needed CPUCLinks.
     * Will represent the data shown in the specification table for now but this might change in the future (may need more CPUCLinks than in table).
     * Therefore, this won't read data from the database and there might be discrepancies!
     */
    private int getNumOfCPUCLink() {
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
     * calculates the pixel per CPUCLink.
     * If pixel number can't be divided equally the first CPUCLinks will get 1 more than the later ones.
     */
    private static List<Integer> roundPixel(int boardCount, int numCPUCLink, int pixel) {
        List<Integer> list = new LinkedList<>();
        int factor;
        for (int remainingBoardCount = boardCount; remainingBoardCount > 0; remainingBoardCount -= factor, numCPUCLink--) {
            factor = (int) Math.ceil(remainingBoardCount / (double) numCPUCLink); // always round up (first ones get 1 more, no rounding for later ones (no remainder))
            list.add((int) (factor / (double) boardCount * pixel)); //multiply by pixel number
        }
        return list;
    }

    /**
     * creates a camera link with the given number of ports
     */
    private static CPUCLink.CameraLink createCameraLink(int phases, int numberOfports, int lval, int id, int startLval, int startLvalCPUC) {
        CPUCLink.CameraLink cameraLink = new CPUCLink.CameraLink(id);
        List<CPUCLink.Port> ports = new LinkedList<>();
        for (int i = 0; i < numberOfports; i += phases) {
            for (int j = 0; j < phases; j++) {
                CPUCLink.Port port = new CPUCLink.Port(i / phases * lval + startLval + startLvalCPUC, (i / phases + 1) * lval - 1 + startLval + startLvalCPUC);
                ports.add(port);
            }
        }
        ports.forEach(cameraLink::addPorts);
        return cameraLink;
    }

    /**
     * returns a list containing of a single camera link created for the given phases, port number and lval
     */
    private static List<CPUCLink.CameraLink> fillSingleCameraLink(int phases, int ports, int lval, int startLvalCPUC) {
        return Stream.of(createCameraLink(phases, ports, lval, 1, 0, startLvalCPUC)).collect(Collectors.toList());
    }

    /**
     * returns a list of two camera links for the given phases and lval.
     * The first one will have ports 0 to portsForOne, the other one the remaining ports.
     */
    private static List<CPUCLink.CameraLink> fillTwoCameraLinks(int phases, int ports, int portsForOne, int lval, int startLvalCPUC) {
        CPUCLink.CameraLink cameraLink1 = createCameraLink(phases, portsForOne, lval, 1, 0, startLvalCPUC);
        return Stream.of(cameraLink1, createCameraLink(phases, ports - portsForOne, lval, 2, cameraLink1.getEndPixel() + 1, 0)).collect(Collectors.toList());
    }

    /**
     * throws a new CIS exception. The message text is build according to the given parameters.
     */
    private static void throwTooManyPortsException(int maxPortsAllowed, int phases, int requiredPorts) {
        throw new CISException(Util.getString("error too many ports 1")
                + maxPortsAllowed
                + Util.getString("error too many ports 2")
                + (phases < 3 ? 1 + Util.getString("error too many ports or") + 2 : phases)
                + Util.getString("error too many ports 3")
                + requiredPorts
                + Util.getString("error too many ports 4"));
    }

    /**
     * returns a list of camera links for 1 or 2 phases:
     * if we have 10 ports or fewer -> use single camera link (up to deca)
     * if we have 12 ports -> use two medium
     * if we have 16 or less -> use two full
     * if we have more -> use two deca (or 1 deca and 1 full)
     */
    private static List<CPUCLink.CameraLink> calculateCameraLinksFor1Or2Phases(int ports, int lval, boolean is1Phase, int startLvalCPUC) {
        if (ports > 20)
            throwTooManyPortsException(20, is1Phase ? 1 : 2, ports);
        if (ports <= 10)
            //fill one camera link (up to deca)
            return fillSingleCameraLink(is1Phase ? 1 : 2, ports, lval, startLvalCPUC);
        if (ports == 12) //TODO check whether this works or we go with full + medium
            //fill two camera links up to medium
            return fillTwoCameraLinks(is1Phase ? 1 : 2, ports, 6, lval, startLvalCPUC);
        if (ports <= 16)
            //fill two camera links up to full
            return fillTwoCameraLinks(is1Phase ? 1 : 2, ports, 8, lval, startLvalCPUC);
        //else: fill two camera links up to deca
        return fillTwoCameraLinks(is1Phase ? 1 : 2, ports, 10, lval, startLvalCPUC);
    }

    /**
     * returns a list of camera links for 3 phases:
     * if we have 10 ports or fewer -> use single camera link (up to deca)
     * if we have 12 ports -> use two medium
     * if we have more -> use two deca (or 1 deca and 1 full)
     */
    private static List<CPUCLink.CameraLink> calculateCameraLinksFor3Phases(int ports, int lval, int startLvalCPUC) {
        if (ports > 18)
            throwTooManyPortsException(18, 3, ports);
        if (ports <= 10) // 3, 6 or 9 ports
            //fill one camera link (up to deca)
            return fillSingleCameraLink(3, ports, lval, startLvalCPUC);
        if (ports == 12)
            //fill two camera links up to medium
            return fillTwoCameraLinks(3, ports, 6, lval, startLvalCPUC);
        //else: fill two camera links up to deca -> 15 or 18 ports
        return fillTwoCameraLinks(3, ports, 9, lval, startLvalCPUC);
    }

    /**
     * returns a list of camera links for 4 phases:
     * if we have 10 ports or fewer -> use single camera link (medium or full)
     * if we have more -> use two full (or 1 full and 1 medium)
     */
    private static List<CPUCLink.CameraLink> calculateCameraLinksFor4Phases(int ports, int lval, int startLvalCPUC) {
        if (ports > 16)
            throwTooManyPortsException(16, 4, ports);
        if (ports <= 10) // 4 or 8 ports
            //fill one camera link (up to deca)
            return fillSingleCameraLink(4, ports, lval, startLvalCPUC);
        // else: fill two camera links up to full -> 12 or 16 ports
        return fillTwoCameraLinks(4, ports, 8, lval, startLvalCPUC);
    }

    /**
     * returns a list of camera links for 5 phases:
     * if we have 10 ports or fewer -> use single camera link (up to deca)
     * if we have more -> use two deca (or 1 deca and 1 medium)
     */
    private static List<CPUCLink.CameraLink> calculateCameraLinksFor5Phases(int ports, int lval, int startLvalCPUC) {
        if (ports > 20)
            throwTooManyPortsException(20, 5, ports);
        if (ports <= 10) // 5 or 10 ports
            //fill one camera link (up to deca)
            return fillSingleCameraLink(5, ports, lval, startLvalCPUC);
        // else: fill two camera links up to deca -> 15 or 20 ports
        return fillTwoCameraLinks(5, ports, 10, lval, startLvalCPUC);
    }

    /**
     * returns a list of camera links for 6 phases:
     * if we have 10 ports or fewer -> use single camera link (up to medium)
     * if we have more -> use two medium
     */
    private static List<CPUCLink.CameraLink> calculateCameraLinksFor6Phases(int ports, int lval, int startLvalCPUC) {
        if (ports > 12)
            throwTooManyPortsException(12, 6, ports);
        if (ports <= 10) // 6 ports
            //fill one camera link (up to medium)
            return fillSingleCameraLink(6, ports, lval, startLvalCPUC);
        // else: fill two camera links (up to medium)
        return fillTwoCameraLinks(6, ports, 6, lval, startLvalCPUC);
    }

    /**
     * creates a single CPUCLink with its camera link configuration
     */
    private static CPUCLink createSingleCPUCLink(double dataratePerPixelClock, long pixel, int ports, int lval, int phaseCount, long pixelClock, int startLvalCPUC) {
        String notes = ""; // don't really need any extra info on print out
        // create CPUCLink object
        CPUCLink cpucLink = new CPUCLink((long) (dataratePerPixelClock * pixelClock), pixel, pixelClock, notes);

        // add camera links
        switch (phaseCount) {
            case 1:
            case 2:
                calculateCameraLinksFor1Or2Phases(ports, lval, phaseCount == 1, startLvalCPUC).forEach(cpucLink::addCameraLink);
                break;
            case 3:
                calculateCameraLinksFor3Phases(ports, lval, startLvalCPUC).forEach(cpucLink::addCameraLink);
                break;
            case 4:
                calculateCameraLinksFor4Phases(ports, lval, startLvalCPUC).forEach(cpucLink::addCameraLink);
                break;
            case 5:
                calculateCameraLinksFor5Phases(ports, lval, startLvalCPUC).forEach(cpucLink::addCameraLink);
                break;
            case 6:
                calculateCameraLinksFor6Phases(ports, lval, startLvalCPUC).forEach(cpucLink::addCameraLink);
                break;
            default:
                throw new IllegalStateException("unsupported phase count");
        }
        return cpucLink;
    }

    /**
     * class to collect data for camera link calculation for a single cpuc link
     */
    private static class CLCalcCPUCLink {

        public CLCalcCPUCLink(int pixelsMax) {
            this.pixelsMax = pixelsMax;
        }

        int pixelsMax; // the maximum/wanted/raw number of pixels for this CPUCLink
        double datarateCPUPerPixelClockMax; // the maximum/wanted/raw datarate
        int ports;
        int lvalMax; // the maximum/wanted/raw lval
        int taps;
        int numberOfBlocks;
        int lval; // actual lval we can use
        int pixels; //actual pixel we have
        double datarateCPUPerPixelClock;
    }

    /**
     * calculates the camera link configuration.
     * Determines the maximum number of pixels per CPUCLink and calculates a lval that is dividable by 16 (some pixels are lost in the process)
     * Calculates port allocation accordingly.
     * calculation may be null!
     */
    @Override
    public List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation) { //TODO maybe remove calculation (maybe overload method where it is needed - or use a getter?)
        // calculate maximum number of pixels
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        int numSensorBoards = getBoardCount();
        int resolution = getSelectedResolution().getActualResolution();
        int numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() * resolution / 1200);
        // other data needed for calculations
        long pixelClock = reducedPixelClock ? PIXEL_CLOCK_REDUCED : PIXEL_CLOCK_NORMAL;
        int numCPUCLink = getNumOfCPUCLink();
        double lineRateKHz = getSelectedLineRate() / 1000.0;

        // calculate maximum possible number of pixels per CPUCLink and create an CLCalcCPUCLink object for further calculations
        List<Integer> pixelsCPU = roundPixel(numSensorBoards, numCPUCLink, numOfPixNominal);
        List<CLCalcCPUCLink> clcalcCPUCLinks = pixelsCPU.stream().map(CLCalcCPUCLink::new).collect(Collectors.toList());
        // calculate maximum possible datarate for each CPUCLink
        clcalcCPUCLinks.forEach(c -> c.datarateCPUPerPixelClockMax = c.pixelsMax * lineRateKHz * getPhaseCount() / (pixelClock / 1000));
        // calculate maximum possible lval
        clcalcCPUCLinks.forEach(c -> c.lvalMax = (int) (c.pixelsMax / Math.ceil(c.datarateCPUPerPixelClockMax / getPhaseCount())));
        // calculate lval that is dividable by mod
        clcalcCPUCLinks.forEach(c -> c.numberOfBlocks = c.lvalMax / mod); // int division to round down
        clcalcCPUCLinks.forEach(c -> c.lval = c.numberOfBlocks * mod);
        // calculate needed taps
        clcalcCPUCLinks.forEach(c -> c.taps = c.pixelsMax / c.lvalMax);
        // calculate actual pixel number (with lval dividable by mod)
        clcalcCPUCLinks.forEach(c -> c.pixels = c.lval * c.taps);
        // calculate actual datarate
        clcalcCPUCLinks.forEach(c -> c.datarateCPUPerPixelClock = c.pixels * lineRateKHz * getPhaseCount() / (pixelClock / 1000));
        // calculate ports
        clcalcCPUCLinks.forEach(c -> c.ports = (int) (Math.ceil(c.datarateCPUPerPixelClockMax / getPhaseCount()) * getPhaseCount()));

        // create CPUCLink objects with calculated data
        List<CPUCLink> cpucLinks = new LinkedList<>();
        int startLvalCPUC = 0; // later CPUCLinks start with the end pixel of the previous one +1
        for (CLCalcCPUCLink c : clcalcCPUCLinks) {
            cpucLinks.add(createSingleCPUCLink(c.datarateCPUPerPixelClock, c.pixels, c.ports, c.lval, getPhaseCount(), pixelClock, startLvalCPUC));
            startLvalCPUC += c.pixels;
        }
        // just print this out here for now, may add it somewhere else later
        System.out.println("actual supported scan width: " + getActualSupportedScanWidth(cpucLinks));
        return cpucLinks;
    }

    public LightColor getLeftBrightField() {
        return leftBrightField;
    }

    /**
     * returns the smaller distance lens (TC54 or TC54L) for the given lens type.
     */
    private static LensType getLowDistanceLens(LensType lensType) {
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

    public boolean isReducedPixelClock() {
        return reducedPixelClock;
    }

    public void setReducedPixelClock(boolean reducedPixelClock) {
        boolean oldValue = this.reducedPixelClock;
        this.reducedPixelClock = reducedPixelClock;
        observers.firePropertyChange("reducedPixelClock", oldValue, reducedPixelClock);
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
        return isValidLCode(code) || isValidCCode(code) || isValidMathExpression(code) || isValidSCode(code);
        //expand if necessary
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

    private boolean hasBrightFieldShapeFromShading() {
        return leftBrightField.isShapeFromShading() || rightBrightField.isShapeFromShading();
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
                updateScanWidth();
                return;
            case "BrightFieldLeft":
                setLeftBrightField(value);
                updateScanWidth();
                return;
            case "Coax":
                setCoaxLight(value);
                return;
            case "BrightFieldRight":
                setRightBrightField(value);
                updateScanWidth();
                return;
            case "DarkFieldRight":
                setRightDarkField(value);
                updateScanWidth();
                return;
            default:
                throw new IllegalArgumentException("light does not exist: " + light);
        }
    }

    private void updateScanWidth() {
        if (isShapeFromShading() && getScanWidth() > MAX_SCAN_WIDTH_WITH_SFS)
            setScanWidth(MAX_SCAN_WIDTH_WITH_SFS);

    }

    @Override
    public void setSelectedResolution(Resolution resolution) {
        super.setSelectedResolution(resolution);
        updateTransportSpeed();
    }

    @Override
    public void setPhaseCount(int phaseCount) {
        super.setPhaseCount(phaseCount);
        updateTransportSpeed();
    }

    /**
     * sets the selected line rate.
     * Only allows 1% increments so the value is rounded to 1% of the possible maximum line rate.
     */
    @Override
    public void setSelectedLineRate(int selectedLineRate) {
        //manual round to 1% of max line rate
        double maxLineRatePercent = getMaxLineRate() / 100;
        int factor = (int) (selectedLineRate / maxLineRatePercent);
        int lower = (int) (factor * maxLineRatePercent);
        int higher = (int) ((factor + 1) * maxLineRatePercent);
        selectedLineRate = selectedLineRate - lower > higher - selectedLineRate ? higher : lower;
        //set the rounded line rate
        super.setSelectedLineRate(selectedLineRate);
        updateTransportSpeed();
    }

    private void updateTransportSpeed() {
        if (this.getSelectedResolution() != null)
            this.setTransportSpeed((int) (this.getSelectedResolution().getPixelSize() * this.getSelectedLineRate() * 1000));
    }

    /**
     * returns the lights string for print out: every light separately if existent
     */
    @Override
    protected String getInternalLightsForPrintOut() {
        if (!hasLEDs())
            return Util.getString("no light");
        String lights = "\n";
        if (leftBrightField != LightColor.NONE)
            lights += "\t" + Util.getString("Brightfield") + " " + Util.getString("left") + ": " + Util.getString(leftBrightField.getDescription()) + "\n";
        if (rightBrightField != LightColor.NONE)
            lights += "\t" + Util.getString("Brightfield") + " " + Util.getString("right") + ": " + Util.getString(rightBrightField.getDescription()) + "\n";
        if (leftDarkField != LightColor.NONE)
            lights += "\t" + Util.getString("Darkfield") + " " + Util.getString("left") + ": " + Util.getString(leftDarkField.getDescription()) + "\n";
        if (rightDarkField != LightColor.NONE)
            lights += "\t" + Util.getString("Darkfield") + " " + Util.getString("right") + ": " + Util.getString(rightDarkField.getDescription()) + "\n";
        if (coaxLight != LightColor.NONE)
            lights += "\t" + Util.getString("Coaxial") + ": " + Util.getString(coaxLight.getDescription()) + "\n";
        return lights.substring(0, lights.length() - 1); // remove last line break
    }

    /**
     * returns the scan distance string for print out: 10+-2mm or 23+-3 mm
     */
    @Override
    protected String getScanDistanceString() {
        switch (lensType) {
            case TC54:
            case TC54L:
                return "10\u200amm +/- 2\u200amm";
            case TC80:
            case TC80L:
                return "23\u200amm +/- 3\u200amm";
            default:
                throw new IllegalStateException("current lens not supported yet");
        }
    }

    /**
     * returns the case profile string for print out: 92x80mm
     */
    @Override
    protected String getCaseProfile() {
        return Util.getString("Aluminium case profile: 92x80\u200amm (HxT) with bonded");
    }

    /**
     * returns the printout for the case: aluminum case with width x height x depth and sealed glass pane
     */
    @Override
    protected String getCasePrintout() {
        return Util.getString("Case dimensions") + "\n\t" +
                Util.getString("Width") + ": ~ " + (getBaseCaseLength() + getExtraCaseLength()) + "\u200amm +/-3\u200amm\n\t" +
                Util.getString("Height") + ": ~ 92\u200amm +/-2\u200amm\n\t" +
                Util.getString("Depth") + ": ~ 80\u200amm +/-2\u200amm\n" +
                Util.getString("Alu case with bonded glass pane") + "\n";
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
     * returns the geometry correction string for print out
     */
    @Override
    protected String getGeometryCorrectionString() { //TODO adjust after discussion
        return Util.getString("Geometry correction: x and y");
    }

    /**
     * returns the depth of field for print out.
     */
    @Override
    protected double getDepthOfField() {
        return getSelectedResolution().getDepthOfField() * 2;
    }

    /**
     * returns the base case length: scan width or next bigger size if there is shape from shading on bright field
     */
    @Override
    protected int getBaseCaseLength() {
        return getScanWidth() + (hasBrightFieldShapeFromShading() ? BASE_LENGTH : 0);
    }

    /**
     * returns an appendix string for print out that is shown after the case length.
     */
    @Override
    protected String getCaseLengthAppendix() {
        return " (" + Util.getString("without cooling pipe") + ")";
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
                getMinFreqForLight(leftDarkField, true, false, calculation),
                getMinFreqForLight(leftBrightField, false, false, calculation),
                getMinFreqForLight(coaxLight, false, true, calculation),
                getMinFreqForLight(rightBrightField, false, false, calculation),
                getMinFreqForLight(rightDarkField, true, false, calculation)));
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
        double tau = calculation.mechaSums[4]; // only Lens will have a photo value in mecha table
        double S_v = getSensitivityFactor();
        double m = getPhaseCount();
        return 100 * I_p * gamma * n * tau * S_v / (1.5 * m);
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
     * throws an error as VUCIS has a different way to calculate the minimum frequency and therefore can't use this method
     */
    @Override
    protected double getGeometryFactor(boolean coax) {
        throw new UnsupportedOperationException("getGeometry calculations differs for VUCIS");
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
        if (isDarkfield && leftDarkField != LightColor.NONE && leftDarkField == rightDarkField ||
                !isDarkfield && leftBrightField != LightColor.NONE && leftBrightField == rightBrightField)
            // if this is darkfield and both dark fields same -> 2 (same for brightfield)
            return 2;
        return 1; // default is 1 for any other case
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

    /**
     * returns whether the VUCIS has LEDs or not
     */
    @Override
    protected boolean hasLEDs() {
        return getLedLines() > 0;
    }

    public void setMod(int mod) { //TODO change label for mod: mod for lval
        int oldValue = this.mod;
        this.mod = mod;
        observers.firePropertyChange("mod", oldValue, mod);
    }

    public int getMod() {
        return mod;
    }
}