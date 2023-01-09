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

    public final String cisName;
    @Getter
    private final Set<LightColor> lightColors;
    @Getter
    @Setter
    private int mode;
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
    @Setter
    private int numOfPix;
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
        String fullName = this.getClass().getName();
        cisName = fullName.substring(fullName.lastIndexOf('.') + 1);
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
        this.mode = cis.mode;
        this.numOfPix = cis.numOfPix;
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

    public CISCalculation calculate() {
        CISCalculation calculation = new CISCalculation();

        Map<Integer, PriceRecord> priceRecords = new HashMap<>();

        Util.getDatabase().ifPresent(context ->
        {
            context.selectFrom(PRICE).stream()
                    .forEach(priceRecord -> priceRecords.put(priceRecord.getArtNo(), priceRecord));

            //Electronics
            int sensPerFpga;

            if (getSelectedResolution().getActualResolution() > 600) {
                sensPerFpga = 1;
            } else if ((this instanceof VDCIS && getPhaseCount() > 1) || (this instanceof MXCIS && getPhaseCount() == 4)) {
                //FULL (RGB)
                sensPerFpga = 2;
            } else if (getMaxRateForHalfMode(getSelectedResolution())
                    .map(rate -> getSelectedLineRate() / 1000 <= rate)
                    .orElse(false)) {
                //HALF
                sensPerFpga = 4;
            } else {
                //FULL
                sensPerFpga = 2;
            }

            setMode(sensPerFpga);

            double lengthPerSens = BASE_LENGTH * sensPerFpga;
            calculation.numFPGA = (int) Math.ceil(getScanWidth() / lengthPerSens);

            try {
                // Einlesen der Elektronik-Tabelle
                context.selectFrom(ELECTRONIC)
                        .where(ELECTRONIC.CIS_TYPE.eq(getClass().getSimpleName()))
                        .and(ELECTRONIC.CIS_LENGTH.eq(getScanWidth()))
                        .stream()
                        .filter(electronicRecord -> isApplicable(electronicRecord.getSelectCode()))
                        .forEach(electronicRecord ->
                        {
                            int amount = (int) (getElectFactor(electronicRecord.getMultiplier())
                                    * MathEval.evaluate(electronicRecord.getAmount()
                                    .replace(" ", "")
                                    .replace("L", "" + getLedLines())
                                    .replace("F", "" + calculation.numFPGA)
                                    .replace("S", "" + getBoardCount())
                                    .replace("N", "" + getScanWidth())));

                            calculateAndAddSinglePrice(electronicRecord.getArtNo(), amount, calculation.electConfig, calculation.electSums, priceRecords);

                            if (amount > 0 && electronicRecord.getSelectCode() != null && electronicRecord.getSelectCode().contains("FPGA")) {
                                calculation.numFPGA += amount;
                            }
                        });
            } catch (DataAccessException e) {
                throw new CISException("Error in Electronics");
            }

            //Mechanics
            try {
                context.selectFrom(MECHANIC)
                        .where(MECHANIC.CIS_TYPE.eq(getClass().getSimpleName()))
                        .and(MECHANIC.CIS_LENGTH.eq(getScanWidth()))
                        .and(MECHANIC.LIGHTS.eq(getLightSources())
                                .or(MECHANIC.LIGHTS.isNull()))
                        .stream()
                        .filter(mechanicRecord -> {
                            try {
                                return isApplicable(mechanicRecord.getSelectCode());
                            } catch (CISNextSizeException e) { // need next size
                                calculateNextSizeMechanics(calculation, mechanicRecord, priceRecords);
                                return false; //don't take this size if we need next larger one
                            }
                        })
                        .forEach(mechanicRecord -> {
                            int amount = getMechaFactor(mechanicRecord.getAmount(), calculation);
                            calculateAndAddSinglePrice(mechanicRecord.getArtNo(), amount, calculation.mechaConfig, calculation.mechaSums, priceRecords);
                        });
            } catch (DataAccessException e) {
                throw new CISException("Error in Mechanics");
            }

            if (!(this instanceof LDSTD)) {
                setNumOfPix(calcNumOfPix());
            }
        });
        return calculation;
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
            int amount = getMechaFactor(mechanicRecord.getAmount(), calculation);
            if (mechanicRecord.getNextSizeArtNo().equals(mechanicRecord.getArtNo())) // same number -> depends on N -> make N next size (+260)
                amount = getMechaFactor(mechanicRecord.getAmount().replace("N", "" + (getScanWidth() + BASE_LENGTH)), calculation);
            calculateAndAddSinglePrice(mechanicRecord.getNextSizeArtNo(), amount, calculation.mechaConfig, calculation.mechaSums, priceRecords);
        }
    }

    public String getVersion() {
        return ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version");
    }

    public String getMechaVersion() {
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
                printout += color + Util.getString("onesided");
                break;
            case "2D0C":
                printout += color + Util.getString("twosided");
                break;
            case "1D1C":
                printout += color + Util.getString("onepluscoax");
                break;
            case "2D1C":
                printout += color + Util.getString("twopluscoax");
                break;
            case "0D1C":
                printout += color + Util.getString("coax");
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
            return Util.getString("Geometry correction: x and y");
        } else {
            return Util.getString("Geometry correction: x");
        }
    }

    /**
     * returns the scan distance string that is shown in print out.
     * Default is 9-12mm unless overwritten by subclass
     */
    protected String getScanDistanceString() {
        return "9-12\u200amm " + Util.getString("exactseetypesign");
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
        return Util.getString("Aluminium case profile: unknown with bonded");
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
        printout += Util.getString("case length") + ": ~ " + (getBaseCaseLength() + getExtraCaseLength()) + " mm" + getCaseLengthAppendix() + "\n";
        // - case profile
        printout += getCaseProfile() + "\n";
        // - glass pane
        printout += Util.getString("glass pane, see drawing") + "\n";
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
        return "Interface: " + (isGigeInterface() ? "GigE" : "CameraLink (max. 5\u200am)");
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
        printout.append(getScanWidth()).append("\u200amm, ").append(hasEarlyTriggerPrintout() ? getTriggerPrintout() : "");
        printout.append(Util.getString("numPhases")).append(getPhaseCount()).append(", ");
        printout.append("max. ").append((getMaxLineRate() / 1000) * getMaxLineRateFactor()).append("\u200akHz\n");

        // - resolution
        printout.append(Util.getString("Resolution: "));
        printout.append(getResolutionString()).append("\n");

        // - internal lights
        printout.append(Util.getString("internal light"));
        printout.append(getInternalLightsForPrintOut());
        printout.append("\n\n");

        // - scan width
        printout.append(Util.getString("scan width")).append(getScanWidth()).append("\u200amm\n");
        // - selected line rate
        double lineRate = Math.round(getSelectedLineRate() / 100.0) / 10.0;
        printout.append(Util.getString("sellinerate")).append(lineRate).append("\u200akHz\n");
        // - transport speed
        printout.append(Util.getString("transport speed")).append(": ").append(String.format("%.1f", (getTransportSpeed() / 1000.0))).append("\u200amm/s\n");
        // - geometry correction
        printout.append(getGeometryCorrectionString()).append("\n");
        // - trigger (if late printout)
        printout.append(hasEarlyTriggerPrintout() ? "" : getTriggerPrintout());
        // - scan distance
        printout.append(Util.getString("scan distance")).append(": ").append(getScanDistanceString()).append("\n");
        // - depth of field (replaced +/- with *2)
        printout.append(Util.getString("DepthofField")).append(": ~ ").append(getDepthOfField() * 2).append("\u200amm\n");
        // - line width
        printout.append(Util.getString("line width")).append(": > 1\u200amm\n");
        // - case printout (L x W x H, with glass pane)
        printout.append(getCasePrintout());
        // - shading
        printout.append(Util.getString("shading")).append("\n");
        // - power
        printout.append(Util.getString("powersource")).append("(24 +/- 1)\u200aVDC\n");
        printout.append(Util.getString("Needed power:")).append((" " + ((calculation.electSums[2] == null) ? 0.0 : (Math.round(10.0 * calculation.electSums[2]) / 10.0)) + "\u200aA").replace(" 0\u200aA", " ???")).append(" +/- 20%\n");
        // - frequency limit
        long minFreq = Math.round(1000 * getMinFreq(calculation)) / 1000;
        if (hasLEDs()) // only print this if there are lights
            printout.append(Util.getString("FrequencyLimit")).append(" ").append(getMinFreq(calculation) < 0 // if < 0 there are values missing in database -> give error msg
                    ? Util.getString("missing photo values") + "\n"
                    : "~" + minFreq + "\u200akHz\n");
        // - cooling
        printout.append(Util.getString(getCooling().getShortHand())).append("\n");
        // - weight
        printout.append(Util.getString("weight")).append(": ~ ")
                .append((" " + Math.round((((calculation.electSums[3] == null) ? 0.0 : calculation.electSums[3])
                        + ((calculation.mechaSums[3] == null) ? 0.0 : calculation.mechaSums[3])) * 10) / 10.0 + "\u200akg")
                        .replace(" 0\u200akg", " ???")).append("\n");
        // - interface
        printout.append(getInterfacePrintout()).append("\n");
        // - end of specs
        printout.append(getEndOfSpecs());
        // - add warning if necessary (if min freq is less than 2 * selected line rate)
        if (minFreq < 2 * lineRate)
            printout.append(PRINTOUT_WARNING).append(Util.getString("warning minfreq linerate")).append("\n");

        //CL Config
        printout.append("\n\t\n");
        printout.append(getStartOfCLPrintOut());
        if (isGigeInterface()) {
            printout.append("Pixel Clock: 40\u200aMHz\n");
            printout.append(Util.getString("numofpix")).append(numOfPix).append("\n");
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
                case "L": //MODE: LOW (MXCIS only)
                    proceed = this instanceof MXCIS && getMode() == 4;
                    break;
                case "H": //Mode: HIGH (MXCIS only)
                    proceed = this instanceof MXCIS && getMode() == 2;
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

    private int getMechaFactor(String factor, CISCalculation calculation) {
        if (isInteger(factor)) {
            return Integer.parseInt(factor);
        } else {
            String ev = factor.replace("F", "" + calculation.numFPGA)
                    .replace("S", "" + getScanWidth() / BASE_LENGTH)
                    .replace("N", "" + getScanWidth())
                    .replace(" ", "")
                    .replace("L", "" + getLedLines());
            ev = prepareMechaFactor(ev); // do CIS specific calculations
            return (int) MathEval.evaluate(ev);
        }
    }

    /**
     * Method for CIS specific Mecha calculations. To be overwritten by subclasses if specific calculations are required.
     * Default implementation returns the given String unchanged.
     */
    protected String prepareMechaFactor(String factor) {
        return factor;
    }

    /**
     * Method for CIS specific Elect calculations. To be overwritten by subclasses if specific calculations are required.
     * Default implementation returns the given String unchanged.
     */
    protected String prepareElectFactor(String factor) {
        return factor;
    }

    private int getElectFactor(String factor) {
        factor = prepareElectFactor(factor); // do CIS specific calculations
        if (isInteger(factor)) {
            return Integer.parseInt(factor);
        } else if (factor.equals("L")) {
            return getLedLines();
        } else if (factor.contains("==")) {
            String[] splitted = factor.split("==");
            if (splitted[0].equals("L") && isInteger(splitted[1]) && getLedLines() == Integer.parseInt(splitted[1])) {
                return 1;
            }
        } else if (factor.contains(">")) {
            String[] splitted = factor.split(">");
            if (splitted[0].equals("L") && isInteger(splitted[1]) && getLedLines() > Integer.parseInt(splitted[1])) {
                return 1;
            }
        } else if (factor.contains("<")) {
            String[] splitted = factor.split("<");
            if (splitted[0].equals("L") && isInteger(splitted[1]) && getLedLines() < Integer.parseInt(splitted[1])) {
                return 1;
            }
        }
        return -1;
    }

    protected int getBoardCount() {
        return getScanWidth() / BASE_LENGTH;
    }

    public String createCalculation() {
        CISCalculation calculation = calculate();

        String printout = getTiViKey() + "\n\t\n";
        StringBuilder electOutput = new StringBuilder(Util.getString("Electronics")).append(":").append("\n\n");
        StringBuilder mechaOutput = new StringBuilder(Util.getString("Mechanics")).append(":").append("\n\n");
        StringBuilder totalOutput = new StringBuilder(Util.getString("Totals")).append(":").append("\n\n");

        electOutput.append(Util.getString("Component")).append("\t")
                .append(Util.getString("Item no.")).append("\t")
                .append(Util.getString("Amount")).append("\t")
                .append(Util.getString("Price/pc (EUR)")).append("\t")
                .append(Util.getString("Weight/pc (kg)")).append("\t")
                .append(Util.getString("Time/pc (h)")).append("\t")
                .append(Util.getString("Power/pc (A)")).append("\n");

        mechaOutput.append(Util.getString("Component")).append("\t")
                .append(Util.getString("Item no.")).append("\t")
                .append(Util.getString("Amount")).append("\t")
                .append(Util.getString("Price/pc (EUR)")).append("\t")
                .append(Util.getString("Weight/pc (kg)")).append("\n");

        calculation.electConfig.forEach((priceRecord, amount) -> electOutput.append(priceRecord.getFerixKey()).append("\t")
                .append(String.format("%05d", priceRecord.getArtNo())).append("\t")
                .append(amount).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPrice() * amount)).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getWeight() * amount)).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getAssemblyTime() * amount)).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPowerConsumption() * amount)).append("\n"));

        electOutput.append("\n\t\n").append(Util.getString("Totals")).append("\t")
                .append(" \t")
                .append("0\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[3])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[1])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[2])).append("\n");

        calculation.mechaConfig.forEach((priceRecord, amount) -> mechaOutput.append(priceRecord.getFerixKey()).append("\t")
                .append(String.format("%05d", priceRecord.getArtNo())).append("\t")
                .append(amount).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPrice() * amount)).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getWeight() * amount)).append("\n"));

        mechaOutput.append("\n\t\n").append(Util.getString("Totals")).append("\t")
                .append(" \t")
                .append("0\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[0])).append("\t")
                .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[3])).append("\n");

        try {
            Map<String, Double> calcMap = Util.getDatabase().map(Stream::of).orElse(Stream.empty())
                    .flatMap(context -> context.selectFrom(CONFIG)
                            .where(CONFIG.CIS_TYPE.eq(getClass().getSimpleName())).stream())
                    .collect(Collectors.toMap(ConfigRecord::getKey, configRecord -> Double.parseDouble(configRecord.getValue())));

            totalOutput.append(Util.getString("calcfor10")).append("\t \t \t \t ").append("\n");
            totalOutput.append(Util.getString("Electronics")).append(":\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0])).append("\t \n");
            calculation.totalPrices[2] = calculation.electSums[0];
            totalOutput.append(Util.getString("Overhead Electronics")).append(" (").append(calcMap.get("A_ELEKTRONIK")).append("%):\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100))).append("\t \n");
            calculation.totalPrices[2] += calculation.electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100);
            totalOutput.append(Util.getString("Testing")).append(":\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[1] * calcMap.get("STUNDENSATZ"))).append("\t \n");
            calculation.totalPrices[2] += calculation.electSums[1] * calcMap.get("STUNDENSATZ");
            if (isGigeInterface()) {
                totalOutput.append(Util.getString("Overhead GigE")).append(" (").append(calcMap.get("Z_GIGE")).append("%):\t \t \t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.electSums[0] * calcMap.get("Z_GIGE") / 100)).append("\t \n");
                calculation.totalPrices[2] += calculation.electSums[0] * (calcMap.get("Z_GIGE") / 100);
            }
            totalOutput.append(Util.getString("Mechanics")).append(":\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[0])).append("\t \n");
            calculation.totalPrices[2] += calculation.mechaSums[0];
            totalOutput.append(Util.getString("Overhead Mechanics")).append(" (").append(calcMap.get("A_MECHANIK")).append("%):\t \t \t")
                    .append(String.format(Util.getLocale(), "%.2f", calculation.mechaSums[0] * (calcMap.get("A_MECHANIK") / 100))).append("\t \n");
            calculation.totalPrices[2] += calculation.mechaSums[0] * (calcMap.get("A_MECHANIK") / 100);
            totalOutput.append(Util.getString("Assembly")).append(":\t \t ")
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
            totalOutput.append(Util.getString("Price/pc")).append(":\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[0])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[1])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[2])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[3])).append("\n");
            totalOutput.append(Util.getString("Surcharge Transport")).append(" (").append(value).append("%):\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[0] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[1] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[2] * value)).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[3] * value)).append("\n");
            surcharge += value;

            if (this instanceof MXCIS) {
                String cat = getTiViKey().split("_")[4];
                value = calcMap.get("Z_" + cat) / 100;
                totalOutput.append(Util.getString("Surcharge")).append(" ").append(cat).append(" (").append(calcMap.get("Z_" + cat)).append("%):\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[0] * value)).append("\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[1] * value)).append("\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[2] * value)).append("\t")
                        .append(String.format(Util.getLocale(), "%.2f", calculation.totalPrices[3] * value)).append("\n");
                surcharge += value;
            } else if (!(this instanceof LDSTD)) {
                value = calcMap.get(getDpiCode()) / 100.0;
                totalOutput.append(Util.getString("Surcharge DPI/Switchable")).append(" (").append(calcMap.get(getDpiCode())).append("%):\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[0] * value)).append("\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[1] * value)).append("\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[2] * value)).append("\t")
                        .append(String.format(Util.getLocale(), format, calculation.totalPrices[3] * value)).append("\n");
                surcharge += value;
            }

            format = "%.2f";
            value = calcMap.get("LIZENZ");
            totalOutput.append(Util.getString("Licence")).append(":\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\t")
                    .append(String.format(Util.getLocale(), format, value)).append("\n");
            addition += value;

            format = "%.2f";
            value = calcMap.get("Z_DISCONT") / 100;
            totalOutput.append(Util.getString("Discount Surcharge")).append(" (").append(value).append("%):\t")
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

            totalOutput.append(Util.getString("Totals")).append(" (EUR):").append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[0])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[1])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[2])).append("\t")
                    .append(String.format(Util.getLocale(), format, calculation.totalPrices[3])).append("\n");
        } catch (NullPointerException | IndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
            throw new CISException(Util.getString("MissingConfigTables"));
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
    protected int calcNumOfPix() {
        int sensorBoardCount = getBoardCount();
        SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
        int numOfPix = (int) (sensorBoard.getChips() * sensorBoardCount * 0.72 * getSelectedResolution().getActualResolution());
        if (isGigeInterface() && getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000 > 80) {
            throw new CISException(Util.getString("GIGEERROR") + (getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000) + " MByte");
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

    public static double decodeQuantity(String s) {
        switch (s) {
            case "2":
                return 1000;
            case "1":
                return 100;
            case "0":
            default:
                return 1;
        }
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
     * calculates the number of pixel and returns it
     */
    public int getNumOfPix() {
        setNumOfPix(calcNumOfPix());
        return numOfPix;
    }

    /**
     * calculates the actual supported scan width that resulting from the pixel count of the cpucLinks.
     * Might be lower than the selected scan width because lvals should be dividable by 16 and therefore some pixels might be lost.
     */
    public double getActualSupportedScanWidth(List<CPUCLink> cpucLinks) {
        long pixelSum = cpucLinks.stream().mapToLong(CPUCLink::getPixelCount).sum();
        return pixelSum * (21 + 1. / 6) * 1200 / selectedResolution.actualResolution / 1000;
    }

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

        public static Optional<LightColor> findByDescription(String description) {
            return Arrays.stream(LightColor.values())
                    .filter(c -> c.getDescription().equals(description))
                    .findFirst();
        }

        @SuppressWarnings("unused")
        public static Optional<LightColor> findByShortHand(String shortHand) {
            return Arrays.stream(LightColor.values())
                    .filter(c -> c.getShortHand().equals(shortHand))
                    .findFirst();
        }

        @SuppressWarnings("unused")
        public static Optional<LightColor> findByCode(char code) {
            return Arrays.stream(LightColor.values())
                    .filter(c -> c.getCode() == code)
                    .findFirst();
        }

        public boolean isShapeFromShading() {
            return this == WHITE_SFS || this == RED_SFS;
        }
    }

    public enum Cooling {
        NONE("NOCO", "None", "none"),
        PAIR("PAIR", "Passive Air", "passair"),
        FAIR("", "Int. Forced Air (Default)", "intforced"),
        EAIR("FAIR", "Ext. Forced Air", "extforced"),
        LICO("LICO", "Liquid Cooling", "lico");

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

        public static Optional<Cooling> findByDescription(String description) {
            return Arrays.stream(Cooling.values())
                    .filter(c -> c.getDescription().equals(description))
                    .findFirst();
        }

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
    protected static class CISCalculation {
        public Double[] electSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        public Double[] mechaSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        public int numFPGA;
        Map<PriceRecord, Integer> electConfig = new HashMap<>();
        Map<PriceRecord, Integer> mechaConfig = new HashMap<>();
        Double[] totalPrices = new Double[]{0.0, 0.0, 0.0, 0.0};
    }
}