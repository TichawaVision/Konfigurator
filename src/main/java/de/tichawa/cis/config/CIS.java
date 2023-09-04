package de.tichawa.cis.config;

import de.tichawa.cis.config.ldstd.LDSTD;
import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.cis.config.mxcis.MXCIS;
import de.tichawa.cis.config.vdcis.VDCIS;
import de.tichawa.cis.config.vucis.VUCIS;
import de.tichawa.util.MathEval;
import lombok.*;
import org.jooq.exception.DataAccessException;

import java.beans.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;
import java.util.stream.*;

import static de.tichawa.cis.config.model.Tables.*;

// Alle allgemeine CIS Funktionen
public abstract class CIS {
    // global constants
    public static final int BASE_LENGTH = 260;
    public static final String PRINTOUT_WARNING = "!!WARNING: ";
    protected static final int DEFAULT_MOD = 8;
    private static final Pattern LIGHT_PATTERN = Pattern.compile("(\\d+)D(\\d+)C");
    private static final Map<String, AdcBoardRecord> ADC_BOARDS;
    private static final Map<String, SensorChipRecord> SENSOR_CHIPS;
    private static final Map<String, SensorBoardRecord> SENSOR_BOARDS;
    private static final HashMap<Integer, Integer> MAX_RATE_FOR_HALF_MODE;

    static {
        // global information from database
        ADC_BOARDS = Util.getDatabase()
                .map(context -> context.selectFrom(ADC_BOARD).stream())
                .orElse(Stream.empty())
                .collect(Collectors.toMap(AdcBoardRecord::getName, Function.identity()));
        SENSOR_CHIPS = Util.getDatabase()
                .map(context -> context.selectFrom(SENSOR_CHIP).stream())
                .orElse(Stream.empty())
                .collect(Collectors.toMap(SensorChipRecord::getName, Function.identity()));
        SENSOR_BOARDS = Util.getDatabase()
                .map(context -> context.selectFrom(SENSOR_BOARD).stream())
                .orElse(Stream.empty())
                .collect(Collectors.toMap(SensorBoardRecord::getName, Function.identity()));
        // other constants
        MAX_RATE_FOR_HALF_MODE = new HashMap<>();
        MAX_RATE_FOR_HALF_MODE.put(25, 27);
        MAX_RATE_FOR_HALF_MODE.put(50, 27);
        MAX_RATE_FOR_HALF_MODE.put(75, 18);
        MAX_RATE_FOR_HALF_MODE.put(100, 27);
        MAX_RATE_FOR_HALF_MODE.put(150, 18);
        MAX_RATE_FOR_HALF_MODE.put(200, 27);
        MAX_RATE_FOR_HALF_MODE.put(300, 18);
        MAX_RATE_FOR_HALF_MODE.put(400, 13);
        MAX_RATE_FOR_HALF_MODE.put(600, 10);
    }

    // members
    protected transient PropertyChangeSupport observers; // part of 'observer' pattern (via property change) to communicate changes of the model (this class) e.g. to the GUI
    // observer currently only supported by VUCIS so only methods used by VUCIS will fire property changes (for now)

    /**
     * The name (type) of the CIS (e.g. VUCIS, VTCIS, ...)
     */
    public final String cisName;
    @Getter
    private final Set<LightColor> lightColors;
    @Getter
    private int phaseCount;
    @Getter
    private int scanWidth;
    @Getter
    private int selectedLineRate;
    @Getter
    @Setter
    private boolean gigeInterface;
    @Getter
    private Resolution selectedResolution;
    @Getter
    @Setter
    private boolean externalTrigger;
    @Getter
    @Setter
    private Cooling cooling;
    @Getter
    @Setter
    private int binning;
    @Getter
    private int transportSpeed;
    @Getter
    @Setter
    private int diffuseLightSources;
    @Getter
    @Setter
    private int coaxLightSources;

    protected CIS() {
        // init for observer pattern via property change
        observers = new PropertyChangeSupport(this); // create observer list

        // other inits (known at creation)
        cisName = getClass().getSimpleName();
        this.lightColors = new HashSet<>();
    }

    /**
     * Copy constructor that just sets all attributes
     */
    protected CIS(CIS cis) {
        this.binning = cis.binning;
        this.cisName = cis.cisName;
        this.coaxLightSources = cis.coaxLightSources;
        this.cooling = cis.cooling;
        this.diffuseLightSources = cis.diffuseLightSources;
        this.externalTrigger = cis.externalTrigger;
        this.gigeInterface = cis.gigeInterface;
        this.lightColors = new HashSet<>(cis.lightColors);
        this.phaseCount = cis.phaseCount;
        this.scanWidth = cis.scanWidth;
        this.selectedLineRate = cis.selectedLineRate;
        this.selectedResolution = cis.selectedResolution; //resolutions don't change so we can copy reference
        this.transportSpeed = cis.transportSpeed;
    }

    /**
     * creates a copy (to be able to handle multiple configurations at once)
     */
    public abstract CIS copy();

    /**
     * adds the given observer to the list of observers that will get notified by changes to the model (this class)
     */
    public void addObserver(PropertyChangeListener observer) {
        observers.addPropertyChangeListener(observer);
    }

    public void setLightColor(LightColor lightColor) {
        getLightColors().clear();
        getLightColors().add(lightColor);
    }

    public abstract double getMaxLineRate();

    public abstract List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation);

    public abstract String getTiViKey();

    public String getLightSources() {
        return getDiffuseLightSources() + "D" + getCoaxLightSources() + "C";
    }

    public abstract String getLights();

    public int getLedLines() {
        Matcher m = LIGHT_PATTERN.matcher(getLightSources());
        if (m.matches()) {
            return Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2));
        } else {
            throw new IllegalArgumentException(getLightSources() + " is not a valid light source pattern.");
        }
    }

    public Optional<AdcBoardRecord> getADC(String name) {
        return Optional.ofNullable(ADC_BOARDS.get(name));
    }

    public Optional<SensorBoardRecord> getSensorBoard(String name) {
        return Optional.ofNullable(SENSOR_BOARDS.get(name));
    }

    public Optional<SensorChipRecord> getSensorChip(String name) {
        return Optional.ofNullable(SENSOR_CHIPS.get(name));
    }

    public static double round(double value, int digits) {
        return Math.round(Math.pow(10.0, digits) * value) / Math.pow(10.0, digits);
    }

    /**
     * Calculates the components with prices for the current selection
     *
     * @return A {@link CISCalculation} calculation result object
     */
    public CISCalculation calculate() {
        // create the result object
        CISCalculation calculation = new CISCalculation();

        // map for prices, maps artNo -> priceRecord
        Map<Integer, PriceRecord> priceRecords = new HashMap<>();

        // look in database for records
        Util.getDatabase().ifPresent(context -> {
            // put all price entries into map
            context.selectFrom(PRICE).stream().forEach(priceRecord -> priceRecords.put(priceRecord.getArtNo(), priceRecord));

            /*
                Electronics
            */

            double lengthPerSens = BASE_LENGTH * getNumberOfSensorsPerFpga();
            calculation.numFPGA = (int) Math.ceil(getScanWidth() / lengthPerSens);
            try {
                // read applicable entries from electronic table
                context.selectFrom(ELECTRONIC)
                        // must be for this CIS
                        .where(ELECTRONIC.CIS_TYPE.eq(getClass().getSimpleName()))
                        // must be for current scan width
                        .and(ELECTRONIC.CIS_LENGTH.eq(getScanWidth())).stream()
                        // must match otherwise
                        .filter(electronicRecord -> isApplicable(electronicRecord.getSelectCode()))
                        // for each applicable record: add price
                        .forEach(electronicRecord -> {
                            // calculate needed amount
                            int amount = (int) (getElectronicsFactor(electronicRecord.getMultiplier()) *
                                    // replace codes with numbers and evaluate factor
                                    MathEval.evaluate(electronicRecord.getAmount()
                                            .replace(" ", "") // don't need empty spaces
                                            .replace("L", "" + getLedLines()) // L is replaced by light number
                                            .replace("F", "" + calculation.numFPGA) // F is replaced by FPGA number
                                            .replace("S", "" + getBoardCount()) // S is replaced by number of boards
                                            .replace("N", "" + getScanWidth()))); // N is replaced by scan width

                            // add to prices
                            calculateAndAddSinglePrice(electronicRecord.getArtNo(), amount, calculation.electConfig, calculation.electSums, priceRecords);

                            // add FPGA if there is an item with a code that contains it
                            if (amount > 0 && electronicRecord.getSelectCode() != null && electronicRecord.getSelectCode().contains("FPGA")) {
                                calculation.numFPGA += amount; //TODO check if this works if it's changed here but (also) read above
                            }
                        });
            } catch (DataAccessException e) {
                throw new CISException("Error in Electronics");
            }

            /*
                Mechanics
            */
            try {
                // read applicable entries from mechanics table
                context.selectFrom(MECHANIC)
                        // must be for this CIS
                        .where(MECHANIC.CIS_TYPE.eq(getClass().getSimpleName()))
                        // must be for current scan width
                        .and(MECHANIC.CIS_LENGTH.eq(getScanWidth()))
                        // light code must match (or be empty)
                        .and(MECHANIC.LIGHTS.eq(getLightSources()).or(MECHANIC.LIGHTS.isNull())).stream()
                        // must match otherwise
                        .filter(mechanicRecord -> {
                            try {
                                // check if this item is applicable
                                return isApplicable(mechanicRecord.getSelectCode());
                            } catch (CISNextSizeException e) { // will throw an exception if we need next size (that's ugly but works for now...)
                                // adds the next size entry instead of this one
                                calculateNextSizeMechanics(calculation, mechanicRecord, priceRecords);
                                return false; //don't take this size if we need next larger one
                            }
                        })
                        // for each applicable entry: add to prices
                        .forEach(mechanicRecord -> {
                            int amount = getMechanicsFactor(mechanicRecord.getAmount(), calculation);
                            calculateAndAddSinglePrice(mechanicRecord.getArtNo(), amount, calculation.mechaConfig, calculation.mechaSums, priceRecords);
                        });
            } catch (DataAccessException e) {
                throw new CISException("Error in Mechanics");
            }
        });
        return calculation;
    }

    /**
     * Calculates and returns the number of sensors per FPGA based on the current resolution and line rate
     *
     * @return the number of sensors per fpga:
     * 1 - if the actual resolution is greater than 600 dpi
     * 4 - if the selected line rate is smaller than the maximum rate for half mode (i.e. the half mode can be used)
     * 2 - else
     */
    protected int getNumberOfSensorsPerFpga() {
        if (getSelectedResolution().getActualResolution() > 600)
            return 1;
        if (getMaxRateForHalfMode(getSelectedResolution()).map(rate -> getSelectedLineRate() / 1000 <= rate).orElse(false))
            // half mode
            return 4;
        // else: full mode
        return 2;
    }

    /**
     * calculates the price for the given artNo and amount and adds it to the config map and sums array
     */
    private void calculateAndAddSinglePrice(Integer artNo, int amount, Map<PriceRecord, Integer> config, Double[] sums, Map<Integer, PriceRecord> priceRecords) {
        if (amount > 0) {
            Optional.ofNullable(priceRecords.get(artNo))
                    .ifPresent(priceRecord -> {
                        if (config.containsKey(priceRecord)) {
                            config.put(priceRecord, amount + config.get(priceRecord));
                        } else {
                            config.put(priceRecord, amount);
                        }
                        sums[0] += priceRecord.getPrice() * amount;
                        sums[1] += priceRecord.getAssemblyTime() * amount;
                        sums[2] += priceRecord.getPowerConsumption() * amount;
                        sums[3] += priceRecord.getWeight() * amount;

                        if (priceRecord.getPhotoValue() != 0) {
                            sums[4] = priceRecord.getPhotoValue(); //TODO is this right?
                            // only one record will have a photo value, so it can be overwritten ?!
                            // but is this actually the case?
                        }
                    });
        }
    }

    /**
     * calculates and adds the article given in next_size_art_no for the given mechanical record if the remaining select code is applicable.
     * If the next article number is the same as the current one, instead the amount will get one size bigger
     */
    private void calculateNextSizeMechanics(CISCalculation calculation, MechanicRecord mechanicRecord, Map<Integer, PriceRecord> priceRecords) {
        String selectCodeWithoutSpaces = mechanicRecord.getSelectCode().replaceAll("\\s", "");
        String selectCodeWithoutS = "S".equals(selectCodeWithoutSpaces) ? "" : selectCodeWithoutSpaces.replace("S&", "");
        if (isApplicable(selectCodeWithoutS)) { // only add if select code without S works
            int amount = getMechanicsFactor(mechanicRecord.getAmount(), calculation);
            if (mechanicRecord.getNextSizeArtNo().equals(mechanicRecord.getArtNo())) // same number -> depends on N -> make N next size (+260)
                amount = getMechanicsFactor(mechanicRecord.getAmount().replace("N", "" + (getScanWidth() + BASE_LENGTH)), calculation);
            calculateAndAddSinglePrice(mechanicRecord.getNextSizeArtNo(), amount, calculation.mechaConfig, calculation.mechaSums, priceRecords);
        }
    }

    public String getVersion() {
        return ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version");
    }

    public String getMechanicVersion() {
        return "_" + Util.getDatabase().flatMap(context -> context.selectFrom(CONFIG)
                        .where(CONFIG.CIS_TYPE.eq(getClass().getSimpleName()))
                        .and(CONFIG.KEY.eq("VERSION"))
                        .fetchOptional(CONFIG.VALUE))
                .orElse("2.0") + "_";
    }

    public String getVersionHeader() {
        return "Version: " + getVersion() + "; " + SimpleDateFormat.getInstance().format(new Date());
    }

    private Optional<Integer> getMaxRateForHalfMode(Resolution res) {
        if (MAX_RATE_FOR_HALF_MODE.containsKey(res.getActualResolution())) {
            return Optional.of(MAX_RATE_FOR_HALF_MODE.get(res.getActualResolution()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * returns the line rate factor that gets multiplied with the max line rate.
     * Default is 1 if not overwritten by subclass.
     */
    protected double getMaxLineRateFactor() {
        return 1;
    }

    /**
     * returns the internal light description
     */
    protected String getInternalLightsForPrintOut() {
        String printout = "";
        String color;
        if ((getPhaseCount() == 3 && !(this instanceof VDCIS)) || ((this instanceof VDCIS || this instanceof MXCIS) && getPhaseCount() == 4)) {
            color = "RGB";
        } else {
            color = Util.getString(getLightColors().stream()
                    .findAny().orElse(LightColor.NONE)
                    .getDescription());
        }

        switch (getLightSources()) {
            case "0D0C":
                printout += Util.getString("None");
                break;
            case "1D0C":
                printout += color + ", " + Util.getString("oneSided");
                break;
            case "2D0C":
                printout += color + ", " + Util.getString("twoSided");
                break;
            case "1D1C":
                printout += color + ", " + Util.getString("oneSidedCoax");
                break;
            case "2D1C":
                printout += color + ", " + Util.getString("twoSidedCoax");
                break;
            case "0D1C":
                printout += color + ", " + Util.getString("coax");
                break;
            default:
                printout += "Unknown";
        }
        return printout;
    }

    /**
     * returns the transport speed factor for print out
     */
    protected double getTransportSpeedFactor() {
        return (1.0 * getSelectedLineRate() / getMaxLineRate());
    }

    /**
     * returns the geometry correction string for print out
     */
    protected String getGeometryCorrectionString() {
        if (getSelectedResolution().getActualResolution() > 400) {
            return Util.getString("xYCorrection");
        } else {
            return Util.getString("xCorrection");
        }
    }

    /**
     * returns the scan distance string that is shown in print out.
     * Default is 9-12mm unless overwritten by subclass
     */
    protected String getScanDistanceString() {
        return "9-12mm " + Util.getString("warningExactScanDistance");
    }

    /**
     * returns the depth of field for print out.
     * Default is the depth of field from the selected resolution if not overwritten by subclass
     */
    protected double getDepthOfField() {
        return getSelectedResolution().getDepthOfField();
    }

    /**
     * returns the base case length.
     * Default is the scan width if not overwritten by subclass
     */
    protected int getBaseCaseLength() {
        return getScanWidth();
    }

    /**
     * returns the extra case length needed in addition to the base case length.
     * Default is 100 if not overwritten by subclass
     */
    protected int getExtraCaseLength() {
        return 100;
    }

    /**
     * returns the case profile for print out.
     * Default is unknown aluminum case
     */
    protected String getCaseProfile() {
        return Util.getString("aluminumCaseUnknown");
    }

    /**
     * returns a string that gets appended to the end of the spec section of print out.
     * Default is the empty string unless overwritten by subclass
     */
    protected String getEndOfSpecs() {
        return "";
    }

    /**
     * returns a string that gets appended to the end of the camera link section of print out.
     * Default is the empty string unless overwritten by subclass
     */
    protected String getEndOfCameraLinkSection() {
        return "";
    }

    /**
     * returns the resolution string for print out.
     * Default is the actual resolution unless overwritten by subclass
     */
    protected String getResolutionString() {
        return "~ " + getSelectedResolution().getActualResolution() + "dpi";
    }

    /**
     * returns an appendix string for print out that is shown after the case length.
     * Default is the empty string unless overwritten by subclass
     */
    protected String getCaseLengthAppendix() {
        return "";
    }

    /**
     * returns the case printout.
     * Default is case length in single line, profile (H x T) in next line and glass pane in third line unless overwritten by subclass
     */
    protected String getCasePrintout() {
        String printout = "";
        // - case length
        printout += Util.getString("caseLength") + ": ~ " + (getBaseCaseLength() + getExtraCaseLength()) + " mm" + getCaseLengthAppendix() + "\n";
        // - case profile
        printout += getCaseProfile() + "\n";
        // - glass pane
        printout += Util.getString("glassPane") + "\n";
        return printout;
    }

    /**
     * returns the beginning of the camera link printout.
     * Default is the empty string unless overwritten by subclass
     */
    protected String getStartOfCLPrintOut() {
        return "";
    }

    /**
     * returns the trigger printout.
     * Default is CC1 if not external unless overwritten by subclass (that has no options)
     */
    protected String getTriggerPrintout() {
        return "Trigger: " + (isExternalTrigger() ? "extern (RS422)" : "CC1") + ", ";
    }

    /**
     * returns whether the trigger printout should be in the beginning.
     * Default is true (for subclasses with options) unless overwritten by subclass.
     */
    protected boolean hasEarlyTriggerPrintout() {
        return true;
    }

    /**
     * returns the interface printout.
     * Default is GigE or CameraLink max 5m unless overwritten by subclass
     */
    protected String getInterfacePrintout() {
        return "Interface: " + (isGigeInterface() ? "GigE" : "CameraLink (max. 5m) " + Util.getString("interfaceSdr"));
    }

    /**
     * returns the weight printout
     */
    protected String getWeightString(CISCalculation calculation) {
        return (" " + Math.round((((calculation.electSums[3] == null) ? 0.0 : calculation.electSums[3])
                + ((calculation.mechaSums[3] == null) ? 0.0 : calculation.mechaSums[3])) * 10) / 10.0 + "kg")
                .replace(" 0kg", " ???");
    }

    /**
     * this method creates a print out for the datasheet
     */
    public String createPrntOut() {
        CISCalculation calculation = calculate();

        // header section: key
        StringBuilder printout = new StringBuilder(getTiViKey());
        printout.append("\n\t\n");

        // specs section
        // - scan width, trigger, phase count, max line rate
        printout.append(getScanWidth()).append("mm, ").append(hasEarlyTriggerPrintout() ? getTriggerPrintout() : "");
        printout.append(Util.getString("phases")).append(": ").append(getPhaseCount()).append(", ");
        printout.append("max. ").append((getMaxLineRate() / 1000) * getMaxLineRateFactor()).append("kHz\n");

        // - resolution
        printout.append("\n").append(Util.getString("resolution")).append(": ");
        printout.append(getResolutionString()).append("\n");

        // - internal lights
        printout.append(Util.getString("internalLight")).append(": ");
        printout.append(getInternalLightsForPrintOut());
        printout.append("\n\n");

        // - scan width
        printout.append(Util.getString("scanWidth")).append(": ").append(getScanWidth()).append("mm\n");
        // - selected line rate
        printout.append(Util.getString("selectedLineRate")).append(": ").append(Util.getNumberAsOutputString(getSelectedLineRate() / 1000., 1)).append("kHz\n");
        // - transport speed
        printout.append(Util.getString("transportSpeed")).append(": ").append(Util.getNumberAsOutputString(getTransportSpeed() / 1000., 1)).append("mm/s\n");
        // - geometry correction
        printout.append(getGeometryCorrectionString()).append("\n");
        // - trigger (if late printout)
        printout.append(hasEarlyTriggerPrintout() ? "" : getTriggerPrintout());
        // - trigger pulse
        printout.append(Util.getString("triggerPulse"))
                .append(":\n\t").append(Util.getString("triggerPulseDouble")).append(": 1 ").append(Util.getString("impulseEvery")).append(" ")
                .append(String.format(Locale.US, "%.2f", selectedResolution.pixelSize / phaseCount * 2000)).append("µm")
                .append("\n\t").append(Util.getString("triggerPulse4Times")).append(": 1 ").append(Util.getString("impulseEvery")).append(" ")
                .append(String.format(Locale.US, "%.2f", selectedResolution.pixelSize / phaseCount * 4000)).append("µm\n");
        // - scan distance
        printout.append(Util.getString("scanDistance")).append(": ").append(getScanDistanceString()).append("\n");
        // - depth of field (replaced +/- with *2)
        printout.append(Util.getString("depthOfField")).append(": ~ ").append(getDepthOfField() * 2).append("mm\n");
        // - line width
        printout.append(Util.getString("lineWidthLight")).append(": > 1mm\n");
        // - case printout (L x W x H, with glass pane)
        printout.append(getCasePrintout());
        // - shading
        printout.append(Util.getString("shading")).append("\n");
        // - power
        printout.append(Util.getString("powerSource")).append(": (24 +/- 1)VDC\n");
        printout.append(Util.getString("maxPower")).append((": " + calculateNeededPower(calculation) + "A")
                .replace(" 0A", " ???")).append(" +/- 20%\n");
        printout.append(Util.getString("averagePower"))
                .append((": " + Util.getNumberAsOutputString(calculateNeededPower(calculation) / phaseCount, 1) + "A")
                        .replace(" 0A", " ???")).append(" +/- 20%\n");
        // - frequency limit
        if (hasLEDs()) // only print this if there are lights
            printout.append(Util.getString("frequencyLimit")).append(": ").append(getMinFreq(calculation) < 0 // if < 0 there are values missing in database -> give error msg
                    ? Util.getString("missingPhotoValues") + "\n"
                    : "~" + Util.getNumberAsOutputString(getMinFreq(calculation), 0) + "kHz\n");
        // - cooling
        printout.append(Util.getString(getCooling().getShortHand())).append("\n");
        // - weight
        printout.append(Util.getString("weight")).append(": ~ ").append(getWeightString(calculation)).append("\n");

        // - interface
        printout.append(getInterfacePrintout()).append("\n");
        // - end of specs
        printout.append(getEndOfSpecs());
        // - add warning if necessary (if min freq is less than 2 * selected line rate)
        if (getMinFreq(calculation) < getSelectedLineRate() / 1000.)
            printout.append(PRINTOUT_WARNING).append(Util.getString("warningFrequencyLimit")).append("\n");

        //CL Config
        printout.append("\n\t\n");
        printout.append(getStartOfCLPrintOut());
        int numOfPix = calcNumOfPix();
        if (isGigeInterface()) {
            printout.append("Pixel Clock: 40MHz\n");
            printout.append(Util.getString("numberOfPixels")).append(": ").append(numOfPix).append("\n");
        } else {
            List<CPUCLink> clCalc = getCLCalc(numOfPix, calculation);
            if (!clCalc.isEmpty()) {
                int i = 1;
                for (CPUCLink CPUCLink : clCalc) {
                    printout.append("Board ").append(i++).append(":\n");
                    printout.append(CPUCLink.toString("  ")).append("\n\n\n");
                }
            } else {
                return null;
            }
            printout.append(getEndOfCameraLinkSection());
        }
        return printout.toString();
    }

    /**
     * Extracts the needed power from the given calculation
     *
     * @param calculation the calculation
     * @return the power needed, rounded to 1 decimal, or 0 if the corresponding field is null
     */
    public double calculateNeededPower(CISCalculation calculation) {
        return (calculation.electSums[2] == null) ? 0.0 : (Math.round(10.0 * calculation.electSums[2]) / 10.0);
    }

    /**
     * Determines whether a given select code matches the current CIS setting.
     * The select code can consist of different alternatives separated by ||. If one alternate matches the whole select code is applicable.
     * Each alternative can consist of different multipliers separated by &. All multipliers must match for the alternate to match.
     * Every single multiplier will get checked by {@link #isApplicableMultiplier(String)}
     */
    private boolean isApplicable(String selectCode) {
        if (selectCode == null) {
            return true; // node code -> this matches
        }
        selectCode = selectCode.replaceAll("\\s", "");

        String[] alternates = selectCode.split("\\|\\|");
        alternate:
        //label for continue jump
        for (String alternate : alternates) { //handle alternates first (as || has priority over &)
            String[] multiplier = alternate.split("&");
            for (String m : multiplier) {// proceed inner calculation as long as every multiplier is applicable
                if (!isApplicableMultiplier(m)) { //not applicable -> whole alternate cannot be true (but other alternates still could be)
                    continue alternate; //continue with next alternate
                }
            } // all multipliers applicable -> alternate is true -> whole select code matches
            return true;
        } //found no alternate that matches
        return false;
    }

    /**
     * determines whether a single multiplier is applicable for the current CIS setting.
     * If a multiplier starts with ^ and ends with $, the current light code will be matched with the regex between ^ and $.
     * A multiplier can start with ! for negation.
     */
    private boolean isApplicableMultiplier(String multiplier) {
        boolean proceed;
        String key = getTiViKey();
        boolean invert = multiplier.startsWith("!");
        multiplier = (invert ? multiplier.substring(1) : multiplier);

        // Regex pattern, apply to light code
        if (multiplier.startsWith("^") && multiplier.endsWith("$")) {
            try {
                proceed = getLights().matches(multiplier);
            } catch (PatternSyntaxException ex) {
                //Not a valid expression, ignore and proceed
                proceed = !invert; //!invert ^ invert == true
            }
        } else {
            switch (multiplier) {
                case "": //No code
                case "FPGA": //Notifier for FPGA parts, ignore and proceed
                    proceed = !invert; //!invert ^ invert == true
                    break;
                case "RGB": //Color coding
                case "RGB8":
                case "RGB_S":
                    if (this instanceof VUCIS) {
                        proceed = LightColor.findByShortHand(multiplier)
                                .map(getLightColors()::contains)
                                .orElse(false);
                    } else {
                        proceed = key.contains(multiplier);
                    }
                    break;
                case "AM":
                case "BL":
                case "GR":
                case "IR":
                case "JR":
                case "UV":
                case "HI":
                case "YE":
                case "VE":
                case "WH":
                case "REBEL":
                case "REBZ8":
                    proceed = LightColor.findByShortHand(multiplier)
                            .map(getLightColors()::contains)
                            .orElse(false);
                    break;
                case "MONO": //Monochrome only
                    proceed = !getTiViKey().contains("RGB");
                    break;
                case "25dpi": //Specific resolution
                case "50dpi":
                case "75dpi":
                case "100dpi":
                case "150dpi":
                case "200dpi":
                case "300dpi":
                case "400dpi":
                case "600dpi":
                case "1200dpi":
                case "2400dpi":
                    proceed = multiplier.equals(getSelectedResolution().getBoardResolution() + "dpi");
                    break;
                case "GIGE": //GigE only
                    proceed = isGigeInterface();
                    break;
                case "CL": //CameraLink only
                    proceed = !isGigeInterface();
                    break;
                case "COAX": //At least one coaxial light
                    proceed = key.split("_")[4].endsWith("C") || (this instanceof MXCIS && key.split("_")[5].endsWith("C"));
                    break;
                case "DIFF":
                    proceed = !(key.split("_")[4].endsWith("C") || (this instanceof MXCIS && key.split("_")[5].endsWith("C"))) //No coaxial light
                            || (key.split("_")[4].startsWith("2") || (this instanceof MXCIS && key.split("_")[5].startsWith("2"))); //Twosided => at least one diffuse (2XX oder 2XXC)
                    break;
                case "NOCO": //Specific cooling
                case "FAIR":
                case "PAIR":
                case "LICO":
                    proceed = key.contains(multiplier);
                    break;
                case "default": //Default cooling
                    proceed = !key.contains("NOCO") && !key.contains("FAIR") && !key.contains("PAIR") && !key.contains("LICO");
                    break;
                case "NOEXT": //No external trigger
                    proceed = !isExternalTrigger();
                    break;
                case "EXT": //With external trigger
                    proceed = isExternalTrigger();
                    break;
                default: //Unknown modifier -> maybe find it in P code or subclass checking
                    proceed = isValidPCode(multiplier) || checkSpecificApplicability(multiplier);
                    break;
            } //end switch
        } // end else

        proceed = invert ^ proceed;
        return proceed;
    }

    /**
     * Method for CIS specific applicability check. To be overwritten by subclasses for specific checks.
     * Default implementation returns false as the given code is unknown and therefore could not be matched.
     */
    protected boolean checkSpecificApplicability(String code) {
        return false;
    }

    /**
     * checks whether the given code matches the current phase count
     */
    private boolean isValidPCode(String code) {
        if (code.charAt(0) != 'P')
            return false;
        try {
            switch (code.charAt(1)) {
                case '>':
                    return getPhaseCount() > Integer.parseInt(code.split(">")[1]);
                case '<':
                    return getPhaseCount() < Integer.parseInt(code.split("<")[1]);
                case '=': // -> '=='
                    return getPhaseCount() == Integer.parseInt(code.split("==")[1]);
                default: // unknown code
                    System.err.println("unknown P clause: " + code);
                    return false;
            }
        } catch (NumberFormatException e) { //not a number behind the math operator
            return false;
        }
    }

    private int getMechanicsFactor(String factor, CISCalculation calculation) {
        if (isInteger(factor)) {
            return Integer.parseInt(factor);
        } else {
            String ev = factor.replace("F", "" + calculation.numFPGA)
                    .replace("S", "" + getScanWidth() / BASE_LENGTH)
                    .replace("N", "" + getScanWidth())
                    .replace(" ", "")
                    .replace("L", "" + getLedLines());
            ev = prepareMechanicsFactor(ev); // do CIS specific calculations
            return (int) MathEval.evaluate(ev);
        }
    }

    /**
     * Method for CIS specific Mecha calculations. To be overwritten by subclasses if specific calculations are required.
     * Default implementation returns the given String unchanged.
     */
    protected String prepareMechanicsFactor(String factor) {
        return factor;
    }

    /**
     * Method for CIS specific Elect calculations. To be overwritten by subclasses if specific calculations are required.
     * Default implementation returns the given String unchanged.
     */
    protected String prepareElectronicsFactor(String factor) {
        return factor;
    }

    /**
     * Determines the int value of the given String factor by replacing Variables with the corresponding values.
     * Currently, replaces 'L' with the number of led lines.
     *
     * @param factor the factor String that might contain variables
     * @return the calculated int value corresponding to the given String. If there is a condition, the return value is 1 if the condition is met or -1 otherwise
     */
    private int getElectronicsFactor(String factor) {
        factor = prepareElectronicsFactor(factor); // do CIS specific calculations
        if (isInteger(factor)) {
            // just a number -> convert to int
            return Integer.parseInt(factor);
        } else if (factor.equals("L")) {
            // just 'L' -> return number of led lines
            return getLedLines();
        } else if (factor.contains("==")) {
            // condition with L==...
            String[] split = factor.split("==");
            if (split[0].equals("L") && isInteger(split[1]) && getLedLines() == Integer.parseInt(split[1])) {
                return 1;
            }
        } else if (factor.contains(">")) {
            // condition with L>...
            String[] split = factor.split(">");
            if (split[0].equals("L") && isInteger(split[1]) && getLedLines() > Integer.parseInt(split[1])) {
                return 1;
            }
        } else if (factor.contains("<")) {
            // condition with L<...
            String[] split = factor.split("<");
            if (split[0].equals("L") && isInteger(split[1]) && getLedLines() < Integer.parseInt(split[1])) {
                return 1;
            }
        }
        // condition isn't met, or we have something else -> return -1
        return -1;
    }

    /**
     * Returns the board count (that is the scan width divided by the base length)
     */
    protected int getBoardCount() {
        return getScanWidth() / BASE_LENGTH;
    }

    public String createCalculation() {
        CISCalculation calculation = calculate();

        String printout = getTiViKey() + "\n\t\n";
        StringBuilder electOutput = new StringBuilder(Util.getString("electronics")).append(":").append("\n\n");
        StringBuilder mechaOutput = new StringBuilder(Util.getString("mechanics")).append(":").append("\n\n");
        StringBuilder totalOutput = new StringBuilder(Util.getString("totals")).append(":").append("\n\n");

        electOutput.append(Util.getString("component")).append("\t")
                .append(Util.getString("itemNumber")).append("\t")
                .append(Util.getString("amount")).append("\t")
                .append(Util.getString("priceEur")).append("\t")
                .append(Util.getString("weightKg")).append("\t")
                .append(Util.getString("timeH")).append("\t")
                .append(Util.getString("powerA")).append("\n");

        mechaOutput.append(Util.getString("component")).append("\t")
                .append(Util.getString("itemNumber")).append("\t")
                .append(Util.getString("amount")).append("\t")
                .append(Util.getString("priceEur")).append("\t")
                .append(Util.getString("weightKg")).append("\n");

        calculation.electConfig.forEach((priceRecord, amount) -> electOutput.append(priceRecord.getFerixKey()).append("\t")
                .append(String.format("%05d", priceRecord.getArtNo())).append("\t")
                .append(amount).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPrice())).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getWeight())).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getAssemblyTime())).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPowerConsumption())).append("\n"));

        electOutput.append("\n\t\n").append(Util.getString("totals")).append("\t")
                .append(" \t")
                .append("0\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[3])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[1])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[2])).append("\n");

        calculation.mechaConfig.forEach((priceRecord, amount) -> mechaOutput.append(priceRecord.getFerixKey()).append("\t")
                .append(String.format("%05d", priceRecord.getArtNo())).append("\t")
                .append(amount).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPrice())).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getWeight())).append("\n"));

        mechaOutput.append("\n\t\n").append(Util.getString("totals")).append("\t")
                .append(" \t")
                .append("0\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[0])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[3])).append("\n");

        try {
            Map<String, Double> calcMap = Util.getDatabase().map(Stream::of).orElse(Stream.empty())
                    .flatMap(context -> context.selectFrom(CONFIG)
                            .where(CONFIG.CIS_TYPE.eq(getClass().getSimpleName())).stream())
                    .collect(Collectors.toMap(ConfigRecord::getKey, configRecord -> Double.parseDouble(configRecord.getValue())));

            totalOutput.append(Util.getString("calcFor10")).append("\t \t \t \t ").append("\n");
            totalOutput.append(Util.getString("electronics")).append(":\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0])).append("\t \n");
            calculation.totalPrices[2] = calculation.electSums[0];
            totalOutput.append(Util.getString("electronicsOverhead")).append(" (").append(calcMap.get("A_ELEKTRONIK")).append("%):\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100))).append("\t \n");
            calculation.totalPrices[2] += calculation.electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100);
            totalOutput.append(Util.getString("electronicsTesting")).append(":\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[1] * calcMap.get("STUNDENSATZ"))).append("\t \n");
            calculation.totalPrices[2] += calculation.electSums[1] * calcMap.get("STUNDENSATZ");
            if (isGigeInterface()) {
                totalOutput.append(Util.getString("gigEOverhead")).append(" (").append(calcMap.get("Z_GIGE")).append("%):\t \t \t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0] * calcMap.get("Z_GIGE") / 100)).append("\t \n");
                calculation.totalPrices[2] += calculation.electSums[0] * (calcMap.get("Z_GIGE") / 100);
            }
            totalOutput.append(Util.getString("mechanics")).append(":\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[0])).append("\t \n");
            calculation.totalPrices[2] += calculation.mechaSums[0];
            totalOutput.append(Util.getString("mechanicsOverhead")).append(" (").append(calcMap.get("A_MECHANIK")).append("%):\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[0] * (calcMap.get("A_MECHANIK") / 100))).append("\t \n");
            calculation.totalPrices[2] += calculation.mechaSums[0] * (calcMap.get("A_MECHANIK") / 100);
            totalOutput.append(Util.getString("assembly")).append(":\t \t ")
                    .append(calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()).append(" h\t")
                    .append(String.format(Util.getLocale(), "%.2f", (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()) * calcMap.get("STUNDENSATZ"))).append("\t \n");
            calculation.totalPrices[2] += (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()) * calcMap.get("STUNDENSATZ");

            int addition = 0;
            double surcharge = 0.0;

            calculation.totalPrices[0] = calculation.totalPrices[2] * calcMap.get("F_1") / 100;
            calculation.totalPrices[1] = calculation.totalPrices[2] * calcMap.get("F_5") / 100;
            calculation.totalPrices[2] = calculation.totalPrices[2] * calcMap.get("F_10") / 100;
            calculation.totalPrices[3] = calculation.totalPrices[2] * calcMap.get("F_25") / 100;
            totalOutput.append(" \t(1 ").append(Util.getString("pc")).append(")\t")
                    .append("(5 ").append(Util.getString("pcs")).append(")\t")
                    .append("(10 ").append(Util.getString("pcs")).append(")\t")
                    .append("(25 ").append(Util.getString("pcs")).append(")\n");

            String format = "%.2f";
            double value = calcMap.get("Z_TRANSPORT") / 100;
            totalOutput.append(Util.getString("price")).append(":\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[0])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[1])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[2])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[3])).append("\n");
            totalOutput.append(Util.getString("transportSurcharge")).append(" (").append(value).append("%):\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[0] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[1] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[2] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[3] * value)).append("\n");
            surcharge += value;

            if (this instanceof MXCIS) {
                String cat = getTiViKey().split("_")[4];
                value = calcMap.get("Z_" + cat) / 100;
                totalOutput.append(Util.getString("surcharge")).append(" ").append(cat).append(" (").append(calcMap.get("Z_" + cat)).append("%):\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[0] * value)).append("\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[1] * value)).append("\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[2] * value)).append("\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[3] * value)).append("\n");
                surcharge += value;
            } else if (!(this instanceof LDSTD)) {
                value = calcMap.get(getDpiCode()) / 100.0;
                totalOutput.append(Util.getString("switchableDpiSurcharge")).append(" (").append(calcMap.get(getDpiCode())).append("%):\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[0] * value)).append("\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[1] * value)).append("\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[2] * value)).append("\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[3] * value)).append("\n");
                surcharge += value;
            }

            format = "%.2f";
            value = calcMap.get("LIZENZ");
            totalOutput.append(Util.getString("licence")).append(":\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\n");
            addition += value;

            format = "%.2f";
            value = calcMap.get("Z_DISCONT") / 100;
            totalOutput.append(Util.getString("discountSurcharge")).append(" (").append(value).append("%):\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[0] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[1] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[2] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[3] * value)).append("\n");
            surcharge += value;

            calculation.totalPrices[0] *= 1 + surcharge;
            calculation.totalPrices[1] *= 1 + surcharge;
            calculation.totalPrices[2] *= 1 + surcharge;
            calculation.totalPrices[3] *= 1 + surcharge;

            calculation.totalPrices[0] += addition;
            calculation.totalPrices[1] += addition;
            calculation.totalPrices[2] += addition;
            calculation.totalPrices[3] += addition;

            totalOutput.append(Util.getString("totals")).append(" (EUR):").append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[0])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[1])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[2])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[3])).append("\n");
        } catch (NullPointerException | IndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
            throw new CISException(Util.getString("errorMissingConfigTables"));
        }

        printout += electOutput.toString();
        printout += "\n\t\n";
        printout += mechaOutput.toString();
        printout += "\n\t\n";
        printout += totalOutput.toString();

        return printout;
    }


    private String getDpiCode() {
        if (getSelectedResolution().isSwitchable()) {
            return "Z_SWT_DPI";
        } else {
            return "Z_" + String.format("%04d", getSelectedResolution().getActualResolution()) + "_DPI";
        }
    }

    /**
     * calculates the number of pixels.
     * Unless overwritten by subclass the default implementation calculates with SMARAGD (single line):
     * chips * number of sensor boards * 0.72 * actual resolution
     */
    public int calcNumOfPix() {
        int sensorBoardCount = getBoardCount();
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        int numOfPix = (int) (sensorBoard.getChips() * sensorBoardCount * 0.72 * getSelectedResolution().getActualResolution());
        if (isGigeInterface() && getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000 > 80) {
            throw new CISException(Util.getString("errorGigE") + ": " + (getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000) + " MByte");
        }
        return numOfPix;
    }

    /**
     * calculates the minimum frequency.
     * Unless overwritten by subclass the calculation is:
     * 100 * electronics phase value * mechanics phase value * led lines * geometry factor * sensitivity / (1.5 * phases)
     * <p>
     * currently phases is 3 if RGB in key else 1
     * currently coax is tested by C_ in key
     * currently phase values are determined by the last read value -> this is probably wrong
     */
    protected double getMinFreq(CISCalculation calculation) {
        String key = getTiViKey();
        boolean coax = key.contains("C_");
        return 100 * calculation.electSums[4]
                * calculation.mechaSums[4]
                * (coax ? 1 : getLedLines())
                * getGeometryFactor(coax)
                * getSensitivityFactor() / (1.5 * (key.contains("RGB") ? 3 : 1));
    }

    protected abstract double getGeometryFactor(boolean coax);

    /**
     * returns the sensitivity value that is multiplied for the minimum frequency calculation
     * Default value is 1 unless overwritten by subclass
     */
    protected double getSensitivityFactor() {
        return 1;
    }

    public static boolean isInteger(String s) {
        return s != null && s.matches("[-+]?\\d+");
    }

    public static boolean isDouble(String s) {
        return s != null && s.matches("[-+]?\\d+[.,]?\\d*");
    }

    public static String getPortName(int x) {
        return Character.toString((char) (65 + x));
    }

    public void setPhaseCount(int phaseCount) {
        int oldValue = this.phaseCount;
        this.phaseCount = phaseCount;
        observers.firePropertyChange("phaseCount", oldValue, phaseCount);
    }

    public void setSelectedResolution(Resolution resolution) {
        Resolution oldValue = this.selectedResolution;
        this.selectedResolution = resolution;
        observers.firePropertyChange("resolution", oldValue, resolution);
    }

    public void setTransportSpeed(int transportSpeed) {
        int oldValue = this.transportSpeed;
        this.transportSpeed = transportSpeed;
        observers.firePropertyChange("transportSpeed", oldValue, transportSpeed);
    }

    public void setScanWidth(int scanWidth) {
        int oldValue = this.scanWidth;
        this.scanWidth = scanWidth;
        observers.firePropertyChange("scanWidth", oldValue, scanWidth);
    }

    public void setSelectedLineRate(int selectedLineRate) {
        int oldValue = this.selectedLineRate;
        this.selectedLineRate = selectedLineRate;
        observers.firePropertyChange("lineRate", oldValue, selectedLineRate);
    }

    /**
     * returns whether the CIS has LEDs or not.
     * Default is true unless overwritten by subclass
     */
    protected boolean hasLEDs() {
        return true;
    }

    /**
     * Exception for handling components that do not match but next size is needed.
     * (This is not a clean way to do this but a cleaner way would need major restructuring...)
     */
    public static class CISNextSizeException extends RuntimeException {
        public CISNextSizeException(String message) {
            super(message);
        }
    }

    /**
     * calculates the actual supported scan width that resulting from the pixel count of the cpucLinks.
     * Might be lower than the selected scan width because lvals should be dividable by 16 and therefore some pixels might be lost.
     */
    public double getActualSupportedScanWidth(List<CPUCLink> cpucLinks) {
        long pixelSum = cpucLinks.stream().mapToLong(CPUCLink::getPixelCount).sum();
        return pixelSum * (21 + 1. / 6) * 1200 / selectedResolution.actualResolution / 1000;
    }

    /**
     * returns whether the CIS requires the next size housing.
     * Default is false unless overwritten by subclass
     */
    public boolean requiresNextSizeHousing() {
        return false;
    }

    /**
     * returns whether this CIS uses an MDR camera link connection on the CIS side.
     * Default is true unless overwritten by subclass that might use SDR
     */
    public boolean usesMdrCameraLinkOnCisSide() {
        return true;
    }

    /**
     * enumeration of all available light colors
     */
    public enum LightColor {
        NONE("None", "NO", 'X'),
        RED("Red", "AM", 'A'),
        GREEN("Green", "GR", 'G'),
        BLUE("Blue", "BL", 'B'),
        YELLOW("Yellow", "YE", 'Y'),
        WHITE("White", "WH", 'L'),
        IR("IR", "IR", 'I'),
        IR950("IR 950nm", "JR", 'J'),
        UVA("UVA 365nm", "UV", 'U'),
        VERDE("Verde", "VE", 'V'),
        RGB_S("RGB (strong)", "RGB_S", 'C'),
        RGB("RGB", "RGB", 'P'),
        IRUV("LEDIRUV", "HI", 'H'),
        RGB8("RGB8", "REBZ8", '8'),
        REBELMIX("REBELMIX", "REBEL", 'E'),
        RED_SFS("Red (Shape from Shading)", "RS", 'R'),
        WHITE_SFS("White (Shape from Shading)", "WS", 'W');

        private final String description;
        private final String shortHand;
        private final char code;

        public String getDescription() {
            return description;
        }

        public char getCode() {
            return code;
        }

        @SuppressWarnings("unused")
        public String getShortHand() {
            return shortHand;
        }

        LightColor(String description, String shortHand, char code) {
            this.description = description;
            this.shortHand = shortHand;
            this.code = code;
        }

        /**
         * searches the light color with the given description out of all available light colors
         *
         * @param description the description of the light color to search
         * @return an {@link Optional} of the light color with the given description if it exists or an empty one otherwise
         */
        public static Optional<LightColor> findByDescription(String description) {
            return Arrays.stream(LightColor.values())
                    .filter(c -> c.getDescription().equals(description))
                    .findFirst();
        }

        /**
         * searches the light color with the given short hand out of all available light colors
         *
         * @param shortHand the shortHand of the light color to search
         * @return an {@link Optional} of the light color with the given shortHand if it exists or an empty one otherwise
         */
        @SuppressWarnings("unused")
        public static Optional<LightColor> findByShortHand(String shortHand) {
            return Arrays.stream(LightColor.values())
                    .filter(c -> c.getShortHand().equals(shortHand))
                    .findFirst();
        }

        /**
         * searches the light color with the given code out of all available light colors
         *
         * @param code the description of the light color to search
         * @return an {@link Optional} of the light color with the given code if it exists or an empty one otherwise
         */
        public static Optional<LightColor> findByCode(char code) {
            return Arrays.stream(LightColor.values())
                    .filter(c -> c.getCode() == code)
                    .findFirst();
        }

        /**
         * returns whether this light color is a shape from shading light color
         *
         * @return true for shape from shading light colors i.e. {@link #WHITE_SFS} and {@link #RED_SFS} or false otherwise
         */
        public boolean isShapeFromShading() {
            return this == WHITE_SFS || this == RED_SFS;
        }
    }

    /**
     * enumeration of all available cooling methods
     */
    public enum Cooling {
        NONE("NOCO", "None", "noCooling"),
        PAIR("PAIR", "Passive Air", "passiveAir"),
        FAIR("", "Int. Forced Air (Default)", "internalForcedAir"),
        EAIR("FAIR", "Ext. Forced Air", "externalForcedAir"),
        LICO("LICO", "Liquid Cooling", "liquidCooling");

        private final String code;
        private final String description;
        private final String shortHand;

        Cooling(String code, String description, String shortHand) {
            this.code = code;
            this.description = description;
            this.shortHand = shortHand;
        }

        public String getCode() {
            return code;
        }

        public String getShortHand() {
            return shortHand;
        }

        public String getDescription() {
            return description;
        }

        /**
         * searches the cooling method with the given description out of all available cooling methods
         *
         * @param description the description of the cooling method to search
         * @return an {@link Optional} of the cooling method with the given description if it exists or an empty one otherwise
         */
        public static Optional<Cooling> findByDescription(String description) {
            return Arrays.stream(Cooling.values())
                    .filter(c -> c.getDescription().equals(description))
                    .findFirst();
        }

        /**
         * searches the cooling method with the given code out of all available cooling methods
         *
         * @param code the code of the cooling method to search
         * @return an {@link Optional} of the cooling method with the given code if it exists or an empty one otherwise
         */
        @SuppressWarnings("unused")
        public static Optional<Cooling> findByCode(String code) {
            return Arrays.stream(Cooling.values())
                    .filter(c -> c.getCode().equals(code))
                    .findFirst();
        }
    }

    @Value
    public static class Resolution {
        int boardResolution;
        double pixelSize;
        boolean switchable;
        int actualResolution;
        double depthOfField;

        public Resolution(int actualResolution, int boardResolution, boolean switchable, double depthOfField, double pixelSize) {
            this.pixelSize = pixelSize;
            this.boardResolution = boardResolution;
            this.switchable = switchable;
            this.depthOfField = depthOfField;
            this.actualResolution = actualResolution;
        }
    }

    /**
     * class for a CIS calculation. Collects electronics and mechanics items and prices.
     */
    public static class CISCalculation {
        public Double[] electSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        public Double[] mechaSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        public int numFPGA;
        public Map<PriceRecord, Integer> electConfig = new HashMap<>();
        public Map<PriceRecord, Integer> mechaConfig = new HashMap<>();
        Double[] totalPrices = new Double[]{0.0, 0.0, 0.0, 0.0};
    }

    /**
     * returns whether this CIS uses a reduced pixel clock.
     * Default is false unless overwritten by subclass.
     *
     * @return false for general CIS
     */
    public boolean isReducedPixelClock() {
        return false;
    }

    /**
     * returns the camera link modulo for this CIS. The default is 8.
     *
     * @return the camera link modulo
     */
    public int getMod() {
        return DEFAULT_MOD;
    }
}