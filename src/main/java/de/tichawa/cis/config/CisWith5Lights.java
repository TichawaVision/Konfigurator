package de.tichawa.cis.config;


import de.tichawa.cis.config.model.tables.records.*;

import java.util.*;
import java.util.stream.*;

/**
 * Subclass of CIS that represents all CIS with 5 lighting options (left and right dark and bright field + coax).
 * Currently, used by {@link de.tichawa.cis.config.vucis.VUCIS} and {@link de.tichawa.cis.config.bdcis.BDCIS}.
 */
public abstract class CisWith5Lights extends CIS {
    /**
     * Normal pixel clock speed which is 85MHz
     */
    public static final long PIXEL_CLOCK_NORMAL = 85000000;
    /**
     * Reduced pixel clock speed which is 53MHz
     */
    public static final long PIXEL_CLOCK_REDUCED = 53000000;
    public static final String PROPERTY_BRIGHT_FIELD_LEFT = "brightFieldLeft";
    public static final String PROPERTY_BRIGHT_FIELD_RIGHT = "brightFieldRight";
    public static final String PROPERTY_CLOUDY_DAY = "cloudyDay";
    public static final String PROPERTY_COAX = "coax";
    public static final String PROPERTY_COOLING_LEFT = "coolingLeft";
    public static final String PROPERTY_COOLING_RIGHT = "coolingRight";
    public static final String PROPERTY_DARK_FIELD_LEFT = "darkFieldLeft";
    public static final String PROPERTY_DARK_FIELD_RIGHT = "darkFieldRight";
    public static final String PROPERTY_LENS_TYPE = "lensType";
    public static final String PROPERTY_MOD = "mod";
    public static final String PROPERTY_REDUCED_PIXEL_CLOCK = "reducedPixelClock";
    /**
     * Valid camera link modulus
     */
    public static final List<Integer> VALID_MODS = Arrays.asList(1, 4, 8, 16, 32);
    protected static final String DEFAULT_ADC_BOARD = "VADCFPGA";
    protected static final String DEFAULT_SENSOR_BOARD = "SMARAGD";

    /**
     * estimated weights in kg by length (first value is for 260 mm, 2nd for 520 mm, ..., last for 2080 mm).
     * These weights were determined by weighing the VUCIS end product (and scaling by length).
     * These should be used as long as the weight values of the single components are not set in the database.
     */
    protected static final double[] WEIGHTS = {3.8, 5.6, 8.2, 11.1, 13.2, 15.4, 17.6, 20}; // total (estimated) weights of the VUCIS by length

    protected LightColor brightFieldLeft;
    protected LightColor brightFieldRight;
    protected boolean cloudyDay;
    protected LightColor coaxLight;
    protected boolean coolingLeft;
    protected boolean coolingRight;
    protected LightColor darkFieldLeft;
    protected LightColor darkFieldRight;
    protected int mod;
    protected boolean reducedPixelClock;

    /**
     * Default constructor that creates a CIS with default values.
     * Default lighting is red on both bright fields and no other lights.
     * Default scan width is 520mm.
     * Default phase count is 1.
     */
    public CisWith5Lights() {
        this.setExternalTrigger(false);
        this.brightFieldLeft = LightColor.RED;
        this.coaxLight = LightColor.NONE;
        this.brightFieldRight = LightColor.RED;
        this.darkFieldLeft = LightColor.NONE;
        this.darkFieldRight = LightColor.NONE;
        this.coolingLeft = true; // since default lighting is not NONE
        this.coolingRight = true; // since default lighting is not NONE
        this.setScanWidth(520);
        this.setPhaseCount(1);
        this.setCooling(Cooling.LICO);
        this.setMod(DEFAULT_MOD);
    }

    /**
     * Copy constructor that copies all attribute values
     *
     * @param cis the cis to copy
     */
    protected CisWith5Lights(CisWith5Lights cis) {
        super(cis);
        this.cloudyDay = cis.cloudyDay;
        this.coaxLight = cis.coaxLight;
        this.coolingLeft = cis.coolingLeft;
        this.coolingRight = cis.coolingRight;
        this.brightFieldLeft = cis.brightFieldLeft;
        this.darkFieldLeft = cis.darkFieldLeft;
        this.mod = cis.mod;
        this.reducedPixelClock = cis.reducedPixelClock;
        this.brightFieldRight = cis.brightFieldRight;
        this.darkFieldRight = cis.darkFieldRight;
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
     * creates a camera link with the given number of ports.
     */
    private static CPUCLink.CameraLink createCameraLink(int phases, int numberOfPorts, int lval, int id, int startLval, int startLvalCPUC, boolean forcedDeca) {
        CPUCLink.CameraLink cameraLink = new CPUCLink.CameraLink(id, forcedDeca);
        List<CPUCLink.Port> ports = new LinkedList<>();
        for (int i = 0; i < numberOfPorts; i += phases) {
            for (int j = 0; j < phases; j++) {
                CPUCLink.Port port = new CPUCLink.Port(i / phases * lval + startLval + startLvalCPUC, (i / phases + 1) * lval - 1 + startLval + startLvalCPUC);
                ports.add(port);
            }
        }
        ports.forEach(cameraLink::addPorts);
        return cameraLink;
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
     * returns a list containing of a single camera link created for the given phases, port number and lval
     */
    private static List<CPUCLink.CameraLink> fillSingleCameraLink(int phases, int ports, int lval, int startLvalCPUC) {
        return Stream.of(createCameraLink(phases, ports, lval, 1, 0, startLvalCPUC, false)).collect(Collectors.toList());
    }

    /**
     * returns a list of two camera links for the given phases and lval.
     * The first one will have ports 0 to portsForOne, the other one the remaining ports.
     */
    private static List<CPUCLink.CameraLink> fillTwoCameraLinks(int phases, int ports, int portsForOne, int lval, int startLvalCPUC) {
        boolean forcedDeca = portsForOne > 8 && ports > 8;
        CPUCLink.CameraLink cameraLink1 = createCameraLink(phases, portsForOne, lval, 1, 0, startLvalCPUC, forcedDeca);
        return Stream.of(cameraLink1, createCameraLink(phases, ports - portsForOne, lval, 2, cameraLink1.getEndPixel() + 1, 0, forcedDeca)).collect(Collectors.toList());
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
     * throws a new CIS exception. The message text is build according to the given parameters.
     */
    private static void throwTooManyPortsException(int maxPortsAllowed, int phases, int requiredPorts) {
        throw new CISException(Util.getString("errorTooManyPorts1") + " "
                + maxPortsAllowed
                + " " + Util.getString("errorTooManyPorts2") + " "
                + (phases < 3 ? 1 + " " + Util.getString("errorTooManyPortsOr") + " " + 2 : phases)
                + " " + Util.getString("errorTooManyPorts3") + " "
                + requiredPorts
                + " " + Util.getString("errorTooManyPorts4"));
    }

    public LightColor getBrightFieldLeft() {
        return brightFieldLeft;
    }

    /**
     * Sets the left bright field light to the given light color and notifies observers.
     * Also keeps a valid state by previously setting the left cooling to true (need cooling if there is a light)
     * or resets the cooling if there is no light on the left
     */
    public void setBrightFieldLeft(LightColor brightFieldLeft) {
        // set/reset left cooling
        if (brightFieldLeft != LightColor.NONE)
            setCoolingLeft(true);
        else if (coaxLight == LightColor.NONE && darkFieldLeft == LightColor.NONE)
            setCoolingLeft(false); //set no left cooling if not needed
        // update value and notify observers
        LightColor oldValue = this.brightFieldLeft;
        this.brightFieldLeft = brightFieldLeft;
        observers.firePropertyChange(PROPERTY_BRIGHT_FIELD_LEFT, oldValue, brightFieldLeft);
    }

    public LightColor getBrightFieldRight() {
        return brightFieldRight;
    }

    /**
     * Sets the right bright field light to the given light color and notifies observers.
     * Also keeps a valid state by setting the right cooling to true (need cooling if there is a light)
     * or resets the cooling if there is no light on the right
     */
    public void setBrightFieldRight(LightColor brightFieldRight) {
        // set/reset cooling
        if (brightFieldRight != LightColor.NONE)
            setCoolingRight(true);
        else if (darkFieldRight == LightColor.NONE)
            setCoolingRight(false); // set no right cooling if not needed
        // update value and notify observers
        LightColor oldValue = this.brightFieldRight;
        this.brightFieldRight = brightFieldRight;
        observers.firePropertyChange(PROPERTY_BRIGHT_FIELD_RIGHT, oldValue, brightFieldRight);
    }

    /**
     * calculates the camera link configuration.
     * Determines the maximum number of pixels per CPUCLink and calculates a lval that is dividable by 16 (some pixels are lost in the process)
     * Calculates port allocation accordingly.
     * calculation may be null!
     */
    @Override
    public List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation) {
        // calculate maximum number of pixels
        SensorBoardRecord sensorBoard = getSensorBoard(DEFAULT_SENSOR_BOARD).orElseThrow(() -> new CISException("Unknown sensor board"));
        int numSensorBoards = getBoardCount();
        int resolution = getSelectedResolution().getActualResolution();
        int numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() * resolution / 1200);
        // other data needed for calculations
        long pixelClock = reducedPixelClock ? PIXEL_CLOCK_REDUCED : PIXEL_CLOCK_NORMAL;
        int numCPUCLink = getNumOfCPUCLink();
        double lineRateKHz = getSelectedLineRate() / 1000.0;
        double firmwareDeadTimeFactor = 1.01;

        // calculate maximum possible number of pixels per CPUCLink and create an CLCalcCPUCLink object for further calculations
        List<Integer> pixelsCPU = roundPixel(numSensorBoards, numCPUCLink, numOfPixNominal);
        List<CLCalcCPUCLink> clcalcCPUCLinks = pixelsCPU.stream().map(CLCalcCPUCLink::new).collect(Collectors.toList());
        // calculate maximum possible datarate for each CPUCLink
        clcalcCPUCLinks.forEach(c -> c.datarateCPUPerPixelClockMax = c.pixelsMax * lineRateKHz * getPhaseCount() * firmwareDeadTimeFactor / (pixelClock / 1000));
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
        clcalcCPUCLinks.forEach(c -> c.datarateCPUPerPixelClock = c.pixels * lineRateKHz * getPhaseCount() * firmwareDeadTimeFactor / (pixelClock / 1000));
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

    /**
     * returns an appendix string for print out that is shown after the case length.
     */
    @Override
    protected String getCaseLengthAppendix() {
        return " (" + Util.getString("noCoolingPipe") + ")";
    }

    /**
     * returns a string that is appended to the end of the camera link section in print out: other cl config on request
     */
    @Override
    protected String getEndOfCameraLinkSection() {
        return "\n" + Util.getString("configOnRequest") + "\n";
    }

    /**
     * throws an error as these CIS have a different way to calculate the minimum frequency and therefore can't use this method
     */
    @Override
    protected double getGeometryFactor(boolean coax) {
        throw new UnsupportedOperationException("getGeometry calculations differs for this CIS");
    }

    @Override
    protected String getInterfacePrintout() {
        return "Interface: CameraLink (max. " + (reducedPixelClock ? "10" : "5") + "m) " + Util.getString("interfaceSdr");
    }

    /**
     * returns the lights string for print out: every light separately if existent
     */
    @Override
    protected String getInternalLightsForPrintOut() {
        if (!hasLEDs())
            return Util.getString("noLight");
        String lights = "\n";
        if (brightFieldLeft != LightColor.NONE)
            lights += "\t" + Util.getString("brightField") + " " + Util.getString("left") + ": " + Util.getString(brightFieldLeft.getDescription()) + "\n";
        if (brightFieldRight != LightColor.NONE)
            lights += "\t" + Util.getString("brightField") + " " + Util.getString("right") + ": " + Util.getString(brightFieldRight.getDescription()) + "\n";
        if (darkFieldLeft != LightColor.NONE)
            lights += "\t" + Util.getString("darkField") + " " + Util.getString("left") + ": " + Util.getString(darkFieldLeft.getDescription()) + "\n";
        if (darkFieldRight != LightColor.NONE)
            lights += "\t" + Util.getString("darkField") + " " + Util.getString("right") + ": " + Util.getString(darkFieldRight.getDescription()) + "\n";
        if (coaxLight != LightColor.NONE)
            lights += "\t" + Util.getString("coaxial") + ": " + Util.getString(coaxLight.getDescription()) + "\n";
        return lights.substring(0, lights.length() - 1); // remove last line break
    }

    /**
     * creates the lights code for the current selection by concatenation of single light codes from left dark field, left bright field, coax, right bright field, right dark field.
     * Special code for cloudy day: 'D' replacing both dark field light codes.
     */
    @Override
    public String getLights() {
        String key = "";
        if (cloudyDay) {
            key += "D";
            key += getBrightFieldLeft().getCode();
            key += getCoaxLight().getCode();
            key += getBrightFieldRight().getCode();
            key += "D";
            return key;
        } // else: no cloudy day
        key += getDarkFieldLeft().getCode();
        key += getBrightFieldLeft().getCode();
        key += getCoaxLight().getCode();
        key += getBrightFieldRight().getCode();
        key += getDarkFieldRight().getCode();
        return key;
    }

    /**
     * Calculates the maximum line rate for this CIS.
     * Divides the needed number of pixels per sensor by the clock speed (minimum clock speed of sensor chip and adc board).
     * The lines of the sensor board are divided by the previous result to get the maximum line rate
     *
     * @return the maximum line rate for the current configuration
     */
    @Override
    public double getMaxLineRate() {
        AdcBoardRecord adcBoard = getADC(DEFAULT_ADC_BOARD).orElseThrow(() -> new CISException("Unknown ADC board"));
        SensorBoardRecord sensorBoard = getSensorBoard(DEFAULT_SENSOR_BOARD).orElseThrow(() -> new CISException("Unknown sensor board"));
        SensorChipRecord sensorChip = getSensorChip(DEFAULT_SENSOR_BOARD + getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
        return 1000 * Math.round(1000 * sensorBoard.getLines() /
                (
                        getPhaseCount() * (sensorChip.getPixelPerSensor() + 100) * 1.0
                                / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed())
                )) / 1000.0;
    }

    public int getMod() {
        return mod;
    }

    /**
     * returns the resolution string for print out: the actual resolution or the switchable resolution string
     */
    @Override
    protected String getResolutionString() {
        if (getSelectedResolution().isSwitchable())
            return Util.getString("switchableResolution");
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

    @Override
    protected String getStartOfCLPrintOut() {
        return Util.getString("modForLval") + ": " + mod + "\n";
    }

    /**
     * generates the TiVi-key for this CIS in its current configuration.
     *
     * @return the TiVi-key consisting of:
     * - "G_"
     * - cis (class) name
     * - "_"
     * - scan width
     * - "_S_" (as Smaragd is the only sensor at the moment)
     * - light code
     * - "_"
     * - lens type code
     * - "_C"
     * - mechanic version (see {@link #getMechanicVersion()}
     */
    @Override
    public String getTiViKey() {
        String key = "G_";
        key += cisName;
        key += String.format("_%04d", getScanWidth());
        key += "_S";

        key += "_";
        key += getLights();

        key += "_" + getLensTypeCode();

        key += "_C";
        key += getMechanicVersion();

        if (key.endsWith("_")) {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }

    @Override
    protected String getTriggerPrintout() {
        return Util.getString("triggerCc1OrExtern") + "\n";
    }

    @Override
    protected boolean hasEarlyTriggerPrintout() {
        return false;
    }

    public boolean isReducedPixelClock() {
        return reducedPixelClock;
    }

    /**
     * Sets the given phase count and updates the transport speed accordingly
     */
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

    /**
     * Sets the given resolution and updates the transport speed accordingly
     */
    @Override
    public void setSelectedResolution(Resolution resolution) {
        super.setSelectedResolution(resolution);
        updateTransportSpeed();
    }

    /**
     * Sets whether the pixel clock uses the reduced frequency and notifies observers of the change
     */
    public void setReducedPixelClock(boolean reducedPixelClock) {
        boolean oldValue = this.reducedPixelClock;
        this.reducedPixelClock = reducedPixelClock;
        observers.firePropertyChange(PROPERTY_REDUCED_PIXEL_CLOCK, oldValue, reducedPixelClock);
    }

    /**
     * Sets the given modulus and notifies observers of the change
     */
    public void setMod(int mod) {
        int oldValue = this.mod;
        this.mod = mod;
        observers.firePropertyChange(PROPERTY_MOD, oldValue, mod);
    }

    public LightColor getCoaxLight() {
        return coaxLight;
    }

    /**
     * sets the coax light to the given light color and notifies observers. Also keeps a valid state by
     * - setting the left dark field light to NONE (there can be no coax with left dark field)
     * - setting the scan width to the max allowed value for coax if it is bigger
     * - setting the left cooling to true (coax needs left cooling)
     * or resets the cooling if coax is deselected and there is no light on the left
     */
    public void setCoaxLight(LightColor coaxLight) {
        if (coaxLight != LightColor.NONE) { // selected coax light
            // set left dark field to NONE (no coax + left dark field)
            setDarkFieldLeft(LightColor.NONE);
            // set scan width to max allowed value if it is bigger
            if (getScanWidth() > getMaxScanWidthWithCoax())
                setScanWidth(getMaxScanWidthWithCoax());
            // set left cooling
            setCoolingLeft(true);
        } else if (brightFieldLeft == LightColor.NONE && darkFieldLeft == LightColor.NONE)
            setCoolingLeft(false); // set no left cooling if not needed
        //update value and notify observers
        LightColor oldValue = this.coaxLight;
        this.coaxLight = coaxLight;
        observers.firePropertyChange(PROPERTY_COAX, oldValue, coaxLight);
    }

    public LightColor getDarkFieldLeft() {
        return darkFieldLeft;
    }

    /**
     * sets the left dark field light to the given light color and notifies observers.
     * Also keeps a valid state by setting the left cooling to true (need cooling if there is a light)
     * or resets the cooling if there is no light on the left
     */
    public void setDarkFieldLeft(LightColor darkFieldLeft) {
        // set/reset left cooling
        if (darkFieldLeft != LightColor.NONE)
            setCoolingLeft(true);
        else if (coaxLight == LightColor.NONE && brightFieldLeft == LightColor.NONE)
            setCoolingLeft(false); //set no left cooling if not needed
        // update value and notify observers
        LightColor oldValue = this.darkFieldLeft;
        this.darkFieldLeft = darkFieldLeft;
        observers.firePropertyChange(PROPERTY_DARK_FIELD_LEFT, oldValue, darkFieldLeft);
    }

    public LightColor getDarkFieldRight() {
        return darkFieldRight;
    }

    /**
     * sets the right dark field light to the given light color and notifies observers.
     * Also keeps a valid state by setting the right cooling to true (need cooling if there is a light)
     * or resets the cooling if there is no light on the right
     */
    public void setDarkFieldRight(LightColor darkFieldRight) {
        // set/reset cooling
        if (darkFieldRight != LightColor.NONE)
            setCoolingRight(true);
        else if (brightFieldRight == LightColor.NONE)
            setCoolingRight(false); // set no right cooling if not needed
        // update value and notify observers
        LightColor oldValue = this.darkFieldRight;
        this.darkFieldRight = darkFieldRight;
        observers.firePropertyChange(PROPERTY_DARK_FIELD_RIGHT, oldValue, darkFieldRight);
    }

    /**
     * Returns the lens code for the TiVi-key
     */
    protected abstract String getLensTypeCode();

    /**
     * Returns the maximum possible scan width with coax lighting
     */
    protected abstract int getMaxScanWidthWithCoax();

    /**
     * Returns the number of needed cpuc links
     */
    protected abstract int getNumOfCPUCLink();

    public boolean hasCoax() {
        return coaxLight != LightColor.NONE;
    }

    public boolean hasCoolingLeft() {
        return coolingLeft;
    }

    public boolean hasCoolingRight() {
        return coolingRight;
    }

    public boolean isCloudyDay() {
        return cloudyDay;
    }

    /**
     * Sets whether cloudy day is used and updates observers.
     * Also removes dark field lighting by setting the light value to {@link de.tichawa.cis.config.CIS.LightColor#NONE} because dark field and coax lighting cannot coexist
     */
    public void setCloudyDay(boolean cloudyDay) {
        if (cloudyDay) { // cloudy day has no dark field
            setDarkFieldLeft(LightColor.NONE);
            setDarkFieldRight(LightColor.NONE);
        }
        // update value and notify observers
        boolean oldValue = this.cloudyDay;
        this.cloudyDay = cloudyDay;
        observers.firePropertyChange(PROPERTY_CLOUDY_DAY, oldValue, cloudyDay);
    }

    /**
     * Sets whether cooling on the left side is used and notifies observers.
     */
    public void setCoolingLeft(boolean coolingLeft) {
        boolean oldValue = this.coolingLeft;
        this.coolingLeft = coolingLeft;
        observers.firePropertyChange(PROPERTY_COOLING_LEFT, oldValue, coolingLeft);
    }

    /**
     * Sets whether cooling on the right side is used and notifies observers.
     */
    public void setCoolingRight(boolean coolingRight) {
        boolean oldValue = this.coolingRight;
        this.coolingRight = coolingRight;
        observers.firePropertyChange(PROPERTY_COOLING_RIGHT, oldValue, coolingRight);
    }

    /**
     * sets the given light color to the given light by calling the corresponding Setter
     */
    public void setLightColor(String light, LightColor value) {
        switch (light) {
            case PROPERTY_DARK_FIELD_LEFT:
                setDarkFieldLeft(value);
                return;
            case PROPERTY_BRIGHT_FIELD_LEFT:
                setBrightFieldLeft(value);
                return;
            case PROPERTY_COAX:
                setCoaxLight(value);
                return;
            case PROPERTY_BRIGHT_FIELD_RIGHT:
                setBrightFieldRight(value);
                return;
            case PROPERTY_DARK_FIELD_RIGHT:
                setDarkFieldRight(value);
                return;
            default:
                throw new IllegalArgumentException("light does not exist: " + light);
        }
    }

    /**
     * Updates the transport speed. It is calculated by multiplying the pixel size and the currently selected line rate
     */
    private void updateTransportSpeed() {
        if (this.getSelectedResolution() != null)
            this.setTransportSpeed((int) (this.getSelectedResolution().getPixelSize() * this.getSelectedLineRate() * 1000));
    }

    /**
     * Class to collect data for camera link calculation for a single cpuc link
     */
    private static class CLCalcCPUCLink {

        double datarateCPUPerPixelClock;
        double datarateCPUPerPixelClockMax; // the maximum/wanted/raw datarate
        int lval; // actual lval we can use
        int lvalMax; // the maximum/wanted/raw lval
        int numberOfBlocks;
        int pixels; //actual pixel we have
        int pixelsMax; // the maximum/wanted/raw number of pixels for this CPUCLink
        int ports;
        int taps;

        public CLCalcCPUCLink(int pixelsMax) {
            this.pixelsMax = pixelsMax;
        }
    }
}
