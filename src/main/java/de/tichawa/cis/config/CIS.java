package de.tichawa.cis.config;

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

/**
 * Base class for all contact image sensors (CIS) and therefore for the model.
 * Implements the observer pattern via {@link PropertyChangeSupport}. {@link PropertyChangeListener}s can register via {@link #addObserver(PropertyChangeListener)}.
 */
public abstract class CIS {

    /**
     * The base (scan) length of a CIS
     */
    public static final int BASE_LENGTH = 260;
    public static final String PRINTOUT_WARNING = "!!WARNING: ";

    public static final String PROPERTY_BINNING = "binning";
    public static final String PROPERTY_COAX_LIGHT_SOURCES = "coaxLightSources";
    public static final String PROPERTY_COOLING = "cooling";
    public static final String PROPERTY_DIFFUSE_LIGHT_SOURCES = "diffuseLightSources";
    public static final String PROPERTY_EXTERNAL_TRIGGER = "externalTrigger";
    public static final String PROPERTY_GIG_E = "gigE";
    public static final String PROPERTY_LINE_RATE = "lineRate";
    public static final String PROPERTY_PHASE_COUNT = "phaseCount";
    public static final String PROPERTY_RESOLUTION = "resolution";
    public static final String PROPERTY_SCAN_WIDTH = "scanWidth";
    public static final String PROPERTY_TRANSPORT_SPEED = "transportSpeed";

    protected static final int DEFAULT_MOD = 8;
    /**
     * Map of all available adc boards (loaded from the database). Maps the name of the adc board to the adc board.
     */
    private static final Map<String, AdcBoardRecord> ADC_BOARDS;
    /**
     * Mapping of actual resolutions to the maximum rate in half mode. Resolutions that don't support half mode are not in this map.
     */
    private static final HashMap<Integer, Integer> MAX_RATE_FOR_HALF_MODE;
    /**
     * Map of all available sensor boards (loaded from the database). Maps the name of the sensor board to the sensor board.
     */
    private static final Map<String, SensorBoardRecord> SENSOR_BOARDS;
    /**
     * Map of all available sensor chips (loaded from the database). Maps the name of the sensor chip to the sensor chip.
     */
    private static final Map<String, SensorChipRecord> SENSOR_CHIPS;

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

    /**
     * The name (type) of the CIS (e.g. VUCIS, VTCIS, ...)
     */
    public final String cisName;

    /**
     * Observers of the model (= the CIS).
     * Part of the observer pattern (via property change) to communicate changes e.g. to the GUI.
     * Observing classes should implement {@link PropertyChangeListener} and register via {@link #addObserver(PropertyChangeListener)}.
     * All model classes ({@link CIS} and its subclasses) provide global PROPERTY_ parameters to identify the changed values.
     * <p>
     * Only used by VUCIS and BDCIS currently.
     */
    protected final transient PropertyChangeSupport observers = new PropertyChangeSupport(this);

    @Getter
    private final Set<LightColor> lightColors;
    @Getter
    private int binning;
    @Getter
    private int coaxLightSources;
    @Getter
    private Cooling cooling;
    @Getter
    private int diffuseLightSources;
    @Getter
    private boolean externalTrigger;
    @Getter
    private boolean gigEInterface;
    @Getter
    private int phaseCount;
    @Getter
    private int scanWidth;
    @Getter
    private int selectedLineRate;
    @Getter
    private Resolution selectedResolution;
    @Getter
    private int transportSpeed;

    protected CIS() {
        // inits (known at creation)
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
        this.gigEInterface = cis.gigEInterface;
        this.lightColors = new HashSet<>(cis.lightColors);
        this.phaseCount = cis.phaseCount;
        this.scanWidth = cis.scanWidth;
        this.selectedLineRate = cis.selectedLineRate;
        this.selectedResolution = cis.selectedResolution; //resolutions don't change so we can copy reference
        this.transportSpeed = cis.transportSpeed;
    }

    /**
     * Creates the electronic output part of the calculation
     *
     * @param electronicComponents a map that consists of price records of all electronic components with their required amount
     * @param electronicSums       an array of the electronic totals at the following indices:
     *                             0 - price total
     *                             1 - time total
     *                             2 - power total
     *                             3 - weight total
     * @return the electronic calculation as a string represented as a table with lines separated by the newline character and items separated by the tab character
     */
    private static String createElectronicCalculationString(Map<PriceRecord, Integer> electronicComponents, Double[] electronicSums) {
        StringBuilder electronicOutput = new StringBuilder(Util.getString("electronics")).append(":").append("\n\n");

        // "table" head
        electronicOutput.append(Util.getString("component")).append("\t")
                .append(Util.getString("itemNumber")).append("\t")
                .append(Util.getString("amount")).append("\t")
                .append(Util.getString("priceEur")).append("\t")
                .append(Util.getString("weightKg")).append("\t")
                .append(Util.getString("timeH")).append("\t")
                .append(Util.getString("powerA")).append("\n");

        // "table" content
        electronicComponents.forEach((priceRecord, amount) -> electronicOutput.append(priceRecord.getFerixKey()).append("\t") // component
                .append(String.format("%05d", priceRecord.getArtNo())).append("\t")  //item number
                .append(amount).append("\t") // amount
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPrice())).append("\t") // price
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getWeight())).append("\t") // weight
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getAssemblyTime())).append("\t") // time
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPowerConsumption())).append("\n")); //power

        // "table" summary (totals)
        electronicOutput.append("\n\t\n").append(Util.getString("totals")).append("\t") // component
                .append(" \t") // item number
                .append("0\t") // amount
                .append(String.format(Util.getLocale(), "%.2f", electronicSums[0])).append("\t") // price
                .append(String.format(Util.getLocale(), "%.2f", electronicSums[3])).append("\t") // weight
                .append(String.format(Util.getLocale(), "%.2f", electronicSums[1])).append("\t") // time
                .append(String.format(Util.getLocale(), "%.2f", electronicSums[2])).append("\n"); // power

        return electronicOutput.toString();
    }

    /**
     * Creates the mechanic output part of the given calculation
     *
     * @param mechanicComponents a map that consists of price records of all mechanic components with their required amount
     * @param mechanicSums       an array of the mechanic totals at the following indices:
     *                           0 - price total
     *                           3 - weight total
     * @return the mechanic calculation as a string represented as a table with lines separated by the newline character and items separated by the tab character
     */
    private static String createMechanicCalculationString(Map<PriceRecord, Integer> mechanicComponents, Double[] mechanicSums) {
        StringBuilder mechanicOutput = new StringBuilder(Util.getString("mechanics")).append(":").append("\n\n");

        // "table" head
        mechanicOutput.append(Util.getString("component")).append("\t")
                .append(Util.getString("itemNumber")).append("\t")
                .append(Util.getString("amount")).append("\t")
                .append(Util.getString("priceEur")).append("\t")
                .append(Util.getString("weightKg")).append("\n");

        // "table" content
        mechanicComponents.forEach((priceRecord, amount) -> mechanicOutput.append(priceRecord.getFerixKey()).append("\t") // component
                .append(String.format("%05d", priceRecord.getArtNo())).append("\t") // item number
                .append(amount).append("\t") // amount
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getPrice())).append("\t") // price
                .append(String.format(Util.getLocale(), "%.2f", priceRecord.getWeight())).append("\n")); // weight

        // "table" summary (totals)
        mechanicOutput.append("\n\t\n").append(Util.getString("totals")).append("\t") // component
                .append(" \t") // item number
                .append("0\t") // amount
                .append(String.format(Util.getLocale(), "%.2f", mechanicSums[0])).append("\t") // price
                .append(String.format(Util.getLocale(), "%.2f", mechanicSums[3])).append("\n"); // weight

        return mechanicOutput.toString();
    }

    /**
     * Returns the adc board with the given name
     *
     * @param name the name of the adc board
     * @return an {@link Optional} that contains the adc board if there is one with the given name in the {@link #ADC_BOARDS} map (that was previously loaded from the database) or an empty Optional
     */
    public static Optional<AdcBoardRecord> getADC(String name) {
        return Optional.ofNullable(ADC_BOARDS.get(name));
    }

    /**
     * adds the given observer to the list of observers that will get notified by changes to the model (this class)
     */
    public void addObserver(PropertyChangeListener observer) {
        observers.addPropertyChangeListener(observer);
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
        if (this.isGigEInterface() && getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000 > 80) {
            throw new CISException(Util.getString("errorGigE") + ": " + (getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000) + " MByte");
        }
        return numOfPix;
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
                            int amount = (int) (getElectronicsMultiplier(electronicRecord.getMultiplier()) *
                                    // replace codes with numbers and evaluate factor
                                    MathEval.evaluate(prepareElectronicsAmountString(electronicRecord.getAmount(), calculation)));

                            // add to prices
                            calculateAndAddSinglePrice(electronicRecord.getArtNo(), amount, calculation.electronicConfig, calculation.electronicSums, priceRecords);

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
                            calculateAndAddSinglePrice(mechanicRecord.getArtNo(), amount, calculation.mechanicConfig, calculation.mechanicSums, priceRecords);
                        });
            } catch (DataAccessException e) {
                throw new CISException("Error in Mechanics");
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
                            // only one record will have a photo value, so it can be overwritten ?!
                            // but is this actually the case?
                        }
                    });
        }
    }

    protected double calculateExtraSurcharge(Map<String, Double> calcMap) {
        return calcMap.get(getDpiCode()) / 100.0;
    }

    /**
     * Extracts the needed power from the given calculation
     *
     * @param calculation the calculation
     * @return the power needed, rounded to 1 decimal, or 0 if the corresponding field is null
     */
    public double calculateNeededPower(CISCalculation calculation) {
        return (calculation.electronicSums[2] == null) ? 0.0 : (Math.round(10.0 * calculation.electronicSums[2]) / 10.0);
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
            calculateAndAddSinglePrice(mechanicRecord.getNextSizeArtNo(), amount, calculation.mechanicConfig, calculation.mechanicSums, priceRecords);
        }
    }

    /**
     * Method for CIS specific applicability check. To be overwritten by subclasses for specific checks.
     * Default implementation returns false as the given code is unknown and therefore could not be matched.
     */
    protected boolean checkSpecificApplicability(String code) {
        return false;
    }

    /**
     * creates a copy (to be able to handle multiple configurations at once)
     */
    public abstract CIS copy();

    /**
     * Creates a calculation in string format for this CIS in its current state.
     *
     * @return a string representing "tables" with table rows separated by newlines and items separated by tabs.
     * the first "table" is the electronics overview, followed by the mechanics overview, followed by the total summary
     */
    public String createCalculationString() {
        try {
            CISCalculation calculation = calculate();

            // read data from table "config"
            Map<String, Double> calcMap = Util.getDatabase().map(Stream::of).orElse(Stream.empty())
                    .flatMap(context -> context.selectFrom(CONFIG)
                            .where(CONFIG.CIS_TYPE.eq(getClass().getSimpleName())).stream())
                    .collect(Collectors.toMap(ConfigRecord::getKey, configRecord -> Double.parseDouble(configRecord.getValue())));

            // calculate total prices
            // - electronic sum
            calculation.totalPrices[2] = calculation.electronicSums[0];
            // - add electronic extra factor
            calculation.totalPrices[2] += calculation.electronicSums[0] * (calcMap.get("A_ELEKTRONIK") / 100);
            // - add electronic time
            calculation.totalPrices[2] += calculation.electronicSums[1] * calcMap.get("STUNDENSATZ");
            // - add gigE extra
            if (this.isGigEInterface()) {
                calculation.totalPrices[2] += calculation.electronicSums[0] * (calcMap.get("Z_GIGE") / 100);
            }
            // - add mechanic sum
            calculation.totalPrices[2] += calculation.mechanicSums[0];
            // - add mechanic extra factor
            calculation.totalPrices[2] += calculation.mechanicSums[0] * (calcMap.get("A_MECHANIK") / 100);
            // - add mechanic time
            calculation.totalPrices[2] += (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()) * calcMap.get("STUNDENSATZ");

            // calculate base prices based on number of pieces
            calculation.totalPrices[0] = calculation.totalPrices[2] * calcMap.get("F_1") / 100;
            calculation.totalPrices[1] = calculation.totalPrices[2] * calcMap.get("F_5") / 100;
            calculation.totalPrices[2] = calculation.totalPrices[2] * calcMap.get("F_10") / 100;
            calculation.totalPrices[3] = calculation.totalPrices[2] * calcMap.get("F_25") / 100;
            Double[] basePrices = new Double[4];
            System.arraycopy(calculation.totalPrices, 0, basePrices, 0, basePrices.length);

            // calculate total surcharge factor
            double transportFactor = calcMap.get("Z_TRANSPORT") / 100;
            double extraSurcharge = calculateExtraSurcharge(calcMap);
            double discountFactor = calcMap.get("Z_DISCONT") / 100;
            double surcharge = transportFactor + discountFactor + extraSurcharge;
            // calculate new prices (applying surcharge factor)
            calculation.totalPrices[0] *= 1 + surcharge;
            calculation.totalPrices[1] *= 1 + surcharge;
            calculation.totalPrices[2] *= 1 + surcharge;
            calculation.totalPrices[3] *= 1 + surcharge;

            // calculate additional price
            double licence = calcMap.get("LIZENZ");
            // calculate new (and final) prices (add additional price)
            calculation.totalPrices[0] += licence;
            calculation.totalPrices[1] += licence;
            calculation.totalPrices[2] += licence;
            calculation.totalPrices[3] += licence;
            Double[] finalPrices = new Double[4];
            System.arraycopy(calculation.totalPrices, 0, finalPrices, 0, finalPrices.length);

            // create string output
            String printout = getTiViKey() + "\n\t\n";
            printout += createElectronicCalculationString(calculation.electronicConfig, calculation.electronicSums);
            printout += "\n\t\n";
            printout += createMechanicCalculationString(calculation.mechanicConfig, calculation.mechanicSums);
            printout += "\n\t\n";
            printout += createTotalCalculationString(calculation, calcMap, isGigEInterface(), basePrices, transportFactor, licence, discountFactor, finalPrices);
            return printout;
        } catch (NullPointerException | IndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
            throw new CISException(Util.getString("errorMissingConfigTables"));
        }
    }

    /**
     * Creates a string that represents an extra surcharge as part of the total summary.
     * The default implementation adds the switchable dpi surcharge.
     * This can be changed or expanded by subclasses as necessary.
     *
     * @param calcMap    A map that contains information from the config database table and contains extra surcharge values
     * @param basePrices the base prices (before surcharges) of 1, 5, 10, 25 pieces
     * @return a string representing one (or more) extra surcharge "table rows" for the total summary "table" created by {@link #createCalculationString()}
     */
    protected String createExtraSurchargeString(Map<String, Double> calcMap, Double[] basePrices) {
        StringBuilder totalOutput = new StringBuilder();
        String format = "%.2f";
        double switchableDpiSurchargeFactor = calculateExtraSurcharge(calcMap);
        totalOutput.append(Util.getString("switchableDpiSurcharge")).append(" (").append(calcMap.get(getDpiCode())).append("%):\t")
                .append(String.format(Util.getLocale(), format, basePrices[0] * switchableDpiSurchargeFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[1] * switchableDpiSurchargeFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[2] * switchableDpiSurchargeFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[3] * switchableDpiSurchargeFactor)).append("\n");
        return totalOutput.toString();
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
        if (this.isGigEInterface()) {
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
     * Creates a string "table" for the total summary with surcharges and additional costs
     *
     * @param calculation     the calculation object that contains the base sums for electronic and mechanic components
     * @param calcMap         the calculation map that contains the config values from the database which holds the surcharge values
     * @param isGigE          whether there is a gigE surcharge
     * @param basePrices      the base prices before surcharges and additional costs for 1, 5, 10, 25 pieces (as an array)
     * @param transportFactor the transport surcharge factor
     * @param licence         the licence extra costs
     * @param discountFactor  the discount surcharge factor
     * @param finalPrices     the prices after applying surcharges and additional costs
     * @return the "table" string representing the total summary with surcharges and extra costs for 1, 5, 10, 25 pieces.
     * Table rows are separated by newlines, items by tabs
     */
    private String createTotalCalculationString(CISCalculation calculation, Map<String, Double> calcMap, boolean isGigE, Double[] basePrices,
                                                double transportFactor, double licence, double discountFactor, Double[] finalPrices) {
        String format = "%.2f"; // general number output format
        // table header
        StringBuilder totalOutput = new StringBuilder(Util.getString("totals")).append(":").append("\n\n");
        // base calculation for 10 pieces
        // - header
        totalOutput.append(Util.getString("calcFor10")).append("\t \t \t \t ").append("\n");
        // - electronic components sum
        totalOutput.append(Util.getString("electronics")).append(":\t \t \t")
                .append(String.format(Util.getLocale(), format, calculation.electronicSums[0])).append("\t \n");
        // - electronic overhead
        totalOutput.append(Util.getString("electronicsOverhead")).append(" (").append(calcMap.get("A_ELEKTRONIK")).append("%):\t \t \t")
                .append(String.format(Util.getLocale(), format, calculation.electronicSums[0] * (calcMap.get("A_ELEKTRONIK") / 100))).append("\t \n");
        // - electronic testing sum
        totalOutput.append(Util.getString("electronicsTesting")).append(":\t \t \t")
                .append(String.format(Util.getLocale(), format, calculation.electronicSums[1] * calcMap.get("STUNDENSATZ"))).append("\t \n");
        // - gigE surcharge
        if (isGigE) {
            totalOutput.append(Util.getString("gigEOverhead")).append(" (").append(calcMap.get("Z_GIGE")).append("%):\t \t \t")
                    .append(String.format(Util.getLocale(), format, calculation.electronicSums[0] * calcMap.get("Z_GIGE") / 100)).append("\t \n");
        }
        // - mechanic components sum
        totalOutput.append(Util.getString("mechanics")).append(":\t \t \t")
                .append(String.format(Util.getLocale(), format, calculation.mechanicSums[0])).append("\t \n");
        // - mechanic overhead
        totalOutput.append(Util.getString("mechanicsOverhead")).append(" (").append(calcMap.get("A_MECHANIK")).append("%):\t \t \t")
                .append(String.format(Util.getLocale(), format, calculation.mechanicSums[0] * (calcMap.get("A_MECHANIK") / 100))).append("\t \n");
        // - assembly costs
        totalOutput.append(Util.getString("assembly")).append(":\t \t ")
                .append(calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()).append(" h\t")
                .append(String.format(Util.getLocale(), format, (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()) * calcMap.get("STUNDENSATZ"))).append("\t \n");

        // overview by number of pieces
        // - header
        totalOutput.append(" \t(1 ").append(Util.getString("pc")).append(")\t")
                .append("(5 ").append(Util.getString("pcs")).append(")\t")
                .append("(10 ").append(Util.getString("pcs")).append(")\t")
                .append("(25 ").append(Util.getString("pcs")).append(")\n");
        // - base prices
        totalOutput.append(Util.getString("price")).append(":\t")
                .append(String.format(Util.getLocale(), format, basePrices[0])).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[1])).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[2])).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[3])).append("\n");
        // - transport surcharge
        totalOutput.append(Util.getString("transportSurcharge")).append(" (").append(transportFactor).append("%):\t")
                .append(String.format(Util.getLocale(), format, basePrices[0] * transportFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[1] * transportFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[2] * transportFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[3] * transportFactor)).append("\n");
        // - extra surcharge (based on type of CIS)
        totalOutput.append(createExtraSurchargeString(calcMap, basePrices));
        // - licence cost
        totalOutput.append(Util.getString("licence")).append(":\t")
                .append(String.format(Util.getLocale(), format, licence)).append("\t")
                .append(String.format(Util.getLocale(), format, licence)).append("\t")
                .append(String.format(Util.getLocale(), format, licence)).append("\t")
                .append(String.format(Util.getLocale(), format, licence)).append("\n");
        // - discount surcharge
        totalOutput.append(Util.getString("discountSurcharge")).append(" (").append(discountFactor).append("%):\t")
                .append(String.format(Util.getLocale(), format, basePrices[0] * discountFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[1] * discountFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[2] * discountFactor)).append("\t")
                .append(String.format(Util.getLocale(), format, basePrices[3] * discountFactor)).append("\n");
        // - final prices
        totalOutput.append(Util.getString("totals")).append(" (EUR):").append("\t")
                .append(String.format(Util.getLocale(), format, finalPrices[0])).append("\t")
                .append(String.format(Util.getLocale(), format, finalPrices[1])).append("\t")
                .append(String.format(Util.getLocale(), format, finalPrices[2])).append("\t")
                .append(String.format(Util.getLocale(), format, finalPrices[3])).append("\n");
        return totalOutput.toString();
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
     * returns the base case length.
     * Default is the scan width if not overwritten by subclass
     */
    protected int getBaseCaseLength() {
        return getScanWidth();
    }

    /**
     * Returns the board count (that is the scan width divided by the base length)
     */
    protected int getBoardCount() {
        return getScanWidth() / BASE_LENGTH;
    }

    public abstract List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation);

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
     * returns the case profile for print out.
     * Default is unknown aluminum case
     */
    protected String getCaseProfile() {
        return Util.getString("aluminumCaseUnknown");
    }

    /**
     * returns the depth of field for print out.
     * Default is the depth of field from the selected resolution if not overwritten by subclass
     */
    protected double getDepthOfField() {
        return getSelectedResolution().getDepthOfField();
    }

    /**
     * Returns the dpi code that is used in the config database table.
     *
     * @return 'Z_SWI_DPI' if the dpi is switchable or
     * 'Z_[dpi]_DPI' where [dpi] is the currently selected dpi if dpi are not switchable
     */
    private String getDpiCode() {
        if (getSelectedResolution().isSwitchable()) {
            return "Z_SWT_DPI";
        } else {
            return "Z_" + String.format("%04d", getSelectedResolution().getActualResolution()) + "_DPI";
        }
    }

    /**
     * Determines the int value of the given String factor by replacing Variables with the corresponding values.
     * Currently, replaces 'L' with the number of led lines.
     *
     * @param factor the factor String that might contain variables
     * @return the calculated int value corresponding to the given String. If there is a condition, the return value is 1 if the condition is met or -1 otherwise
     */
    private int getElectronicsMultiplier(String factor) {
        factor = prepareElectronicsMultiplier(factor); // do CIS specific calculations
        if (Util.isInteger(factor)) {
            // just a number -> convert to int
            return Integer.parseInt(factor);
        } else if (factor.equals("L")) {
            // just 'L' -> return number of led lines
            return getLedLines();
        } else if (factor.contains("==")) {
            // condition with L==...
            String[] split = factor.split("==");
            if (split[0].equals("L") && Util.isInteger(split[1]) && getLedLines() == Integer.parseInt(split[1])) {
                return 1;
            }
        } else if (factor.contains(">")) {
            // condition with L>...
            String[] split = factor.split(">");
            if (split[0].equals("L") && Util.isInteger(split[1]) && getLedLines() > Integer.parseInt(split[1])) {
                return 1;
            }
        } else if (factor.contains("<")) {
            // condition with L<...
            String[] split = factor.split("<");
            if (split[0].equals("L") && Util.isInteger(split[1]) && getLedLines() < Integer.parseInt(split[1])) {
                return 1;
            }
        }
        // condition isn't met, or we have something else -> return -1
        return -1;
    }

    /**
     * returns a string that gets appended to the end of the camera link section of print out.
     * Default is the empty string unless overwritten by subclass
     */
    protected String getEndOfCameraLinkSection() {
        return "";
    }

    /**
     * returns a string that gets appended to the end of the spec section of print out.
     * Default is the empty string unless overwritten by subclass
     */
    protected String getEndOfSpecs() {
        return "";
    }

    /**
     * returns the extra case length needed in addition to the base case length.
     * Default is 100 if not overwritten by subclass
     */
    protected int getExtraCaseLength() {
        return 100;
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

    protected abstract double getGeometryFactor(boolean coax);

    /**
     * returns the interface printout.
     * Default is GigE or CameraLink max 5m unless overwritten by subclass
     */
    protected String getInterfacePrintout() {
        return "Interface: " + (this.isGigEInterface() ? "GigE" : "CameraLink (max. 5m) " + Util.getString("interfaceSdr"));
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

    public int getLedLines() {
        Matcher m = Pattern.compile("(\\d+)D(\\d+)C").matcher(getLightSources());
        if (m.matches()) {
            return Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2));
        } else {
            throw new IllegalArgumentException(getLightSources() + " is not a valid light source pattern.");
        }
    }

    public String getLightSources() {
        return getDiffuseLightSources() + "D" + getCoaxLightSources() + "C";
    }

    public abstract String getLights();

    public abstract double getMaxLineRate();

    /**
     * returns the line rate factor that gets multiplied with the max line rate.
     * Default is 1 if not overwritten by subclass.
     */
    protected double getMaxLineRateFactor() {
        return 1;
    }

    private Optional<Integer> getMaxRateForHalfMode(Resolution res) {
        if (MAX_RATE_FOR_HALF_MODE.containsKey(res.getActualResolution())) {
            return Optional.of(MAX_RATE_FOR_HALF_MODE.get(res.getActualResolution()));
        } else {
            return Optional.empty();
        }
    }

    public String getMechanicVersion() {
        return "_" + Util.getDatabase().flatMap(context -> context.selectFrom(CONFIG)
                        .where(CONFIG.CIS_TYPE.eq(getClass().getSimpleName()))
                        .and(CONFIG.KEY.eq("VERSION"))
                        .fetchOptional(CONFIG.VALUE))
                .orElse("2.0") + "_";
    }

    /**
     * Determines the mechanics factor by replacing variables in the given factor string with their corresponding numeric values.
     * If the string just represents an integer, the integer value is returned.
     * Otherwise, calls {@link #prepareMechanicsFactor(String)} to allow manipulation (by subclasses), afterwards
     * replaces occurrences of the following variables:
     * - F with the number of FPGAs that was determined in the given calculation,
     * - S with the current scan width divided by the base length (260 -> 1, 520 -> 2, 780 -> 3, ...),
     * - N with the current scan width,
     * - L with the number of LED lines of the current light selection.
     * Afterwards, calls {@link #prepareMechanicsFactor(String)} to allow further manipulation (by subclasses)
     *
     * @param factor      the mechanic factor string that might contain variables
     * @param calculation the latest calculation data that might be needed to replace variables in the factor string
     * @return the integer value determined by evaluating the factor string after replacing variables
     */
    private int getMechanicsFactor(String factor, CISCalculation calculation) {
        if (Util.isInteger(factor)) {
            return Integer.parseInt(factor);
        } else {
            String ev = prepareMechanicsFactor(factor);// do CIS specific calculations
            ev = ev.replaceAll("F", "" + calculation.numFPGA)
                    .replaceAll("S", "" + getScanWidth() / BASE_LENGTH)
                    .replaceAll("N", "" + getScanWidth())
                    .replaceAll(" ", "")
                    .replaceAll("L", "" + getLedLines());
            return (int) MathEval.evaluate(ev);
        }
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
        return 100 * calculation.electronicSums[4]
                * calculation.mechanicSums[4]
                * (coax ? 1 : getLedLines())
                * getGeometryFactor(coax)
                * getSensitivityFactor() / (1.5 * (key.contains("RGB") ? 3 : 1));
    }

    /**
     * returns the camera link modulo for this CIS. The default is 8.
     *
     * @return the camera link modulo
     */
    public int getMod() {
        return DEFAULT_MOD;
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
     * returns the resolution string for print out.
     * Default is the actual resolution unless overwritten by subclass
     */
    protected String getResolutionString() {
        return "~ " + getSelectedResolution().getActualResolution() + "dpi";
    }

    /**
     * returns the scan distance string that is shown in print out.
     * Default is 9-12mm unless overwritten by subclass
     */
    protected String getScanDistanceString() {
        return "9-12mm " + Util.getString("warningExactScanDistance");
    }

    /**
     * returns the sensitivity value that is multiplied for the minimum frequency calculation
     * Default value is 1 unless overwritten by subclass
     */
    protected double getSensitivityFactor() {
        return 1;
    }

    public Optional<SensorBoardRecord> getSensorBoard(String name) {
        return Optional.ofNullable(SENSOR_BOARDS.get(name));
    }

    public Optional<SensorChipRecord> getSensorChip(String name) {
        return Optional.ofNullable(SENSOR_CHIPS.get(name));
    }

    /**
     * returns the beginning of the camera link printout.
     * Default is the empty string unless overwritten by subclass
     */
    protected String getStartOfCLPrintOut() {
        return "";
    }

    public abstract String getTiViKey();

    /**
     * returns the trigger printout.
     * Default is CC1 if not external unless overwritten by subclass (that has no options)
     */
    protected String getTriggerPrintout() {
        return "Trigger: " + (isExternalTrigger() ? "extern (RS422)" : "CC1") + ", ";
    }

    public String getVersion() {
        return ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version");
    }

    public String getVersionHeader() {
        return "Version: " + getVersion() + "; " + SimpleDateFormat.getInstance().format(new Date());
    }

    /**
     * returns the weight printout
     */
    protected String getWeightString(CISCalculation calculation) {
        return (" " + Math.round((((calculation.electronicSums[3] == null) ? 0.0 : calculation.electronicSums[3])
                + ((calculation.mechanicSums[3] == null) ? 0.0 : calculation.mechanicSums[3])) * 10) / 10.0 + "kg")
                .replace(" 0kg", " ???");
    }

    /**
     * returns whether the trigger printout should be in the beginning.
     * Default is true (for subclasses with options) unless overwritten by subclass.
     */
    protected boolean hasEarlyTriggerPrintout() {
        return true;
    }

    /**
     * returns whether the CIS has LEDs or not.
     * Default is true unless overwritten by subclass
     */
    protected boolean hasLEDs() {
        return true;
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
                    proceed = this.isGigEInterface();
                    break;
                case "CL": //CameraLink only
                    proceed = !this.isGigEInterface();
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
     * returns whether this CIS uses a reduced pixel clock.
     * Default is false unless overwritten by subclass.
     *
     * @return false for general CIS
     */
    public boolean isReducedPixelClock() {
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

    /**
     * Prepares the electronic amount factor string so that it can be evaluated by {@link MathEval#evaluate(String)} afterwards.
     * This base implementation removes whitespaces.
     * Subclasses can further alter the factor string
     *
     * @param factor      the factor string that gets (partly) replaced
     * @param calculation the CIS calculation that might be needed for replacements in subclasses
     * @return the factor string after alterations
     */
    protected String prepareElectronicsAmountString(String factor, CISCalculation calculation) {
        return factor.replaceAll("\\s", ""); // remove all whitespace
    }

    /**
     * Method for CIS specific Elect calculations. To be overwritten by subclasses if specific calculations are required.
     * Default implementation returns the given String unchanged.
     */
    protected String prepareElectronicsMultiplier(String factor) {
        return factor;
    }

    /**
     * Method for CIS specific Mecha calculations. To be overwritten by subclasses if specific calculations are required.
     * Default implementation returns the given String unchanged.
     */
    protected String prepareMechanicsFactor(String factor) {
        return factor;
    }

    /**
     * returns whether the CIS requires the next size housing.
     * Default is false unless overwritten by subclass
     */
    public boolean requiresNextSizeHousing() {
        return false;
    }

    public void setBinning(int binning) {
        int oldValue = this.binning;
        this.binning = binning;
        observers.firePropertyChange(PROPERTY_BINNING, oldValue, binning);
    }

    public void setCoaxLightSources(int coaxLightSources) {
        int oldValue = this.coaxLightSources;
        this.coaxLightSources = coaxLightSources;
        observers.firePropertyChange(PROPERTY_COAX_LIGHT_SOURCES, oldValue, coaxLightSources);
    }

    public void setCooling(Cooling cooling) {
        Cooling oldValue = this.cooling;
        this.cooling = cooling;
        observers.firePropertyChange(PROPERTY_COOLING, oldValue, cooling);
    }

    public void setDiffuseLightSources(int diffuseLightSources) {
        int oldValue = this.diffuseLightSources;
        this.diffuseLightSources = diffuseLightSources;
        observers.firePropertyChange(PROPERTY_DIFFUSE_LIGHT_SOURCES, oldValue, diffuseLightSources);
    }

    public void setExternalTrigger(boolean externalTrigger) {
        boolean oldValue = this.externalTrigger;
        this.externalTrigger = externalTrigger;
        observers.firePropertyChange(PROPERTY_EXTERNAL_TRIGGER, oldValue, externalTrigger);
    }

    public void setGigEInterface(boolean gigEInterface) {
        boolean oldValue = this.gigEInterface;
        this.gigEInterface = gigEInterface;
        observers.firePropertyChange(PROPERTY_GIG_E, oldValue, gigEInterface);
    }

    public void setLightColor(LightColor lightColor) {
        getLightColors().clear();
        getLightColors().add(lightColor);
    }

    public void setPhaseCount(int phaseCount) {
        int oldValue = this.phaseCount;
        this.phaseCount = phaseCount;
        observers.firePropertyChange(PROPERTY_PHASE_COUNT, oldValue, phaseCount);
    }

    public void setScanWidth(int scanWidth) {
        int oldValue = this.scanWidth;
        this.scanWidth = scanWidth;
        observers.firePropertyChange(PROPERTY_SCAN_WIDTH, oldValue, scanWidth);
    }

    public void setSelectedLineRate(int selectedLineRate) {
        int oldValue = this.selectedLineRate;
        this.selectedLineRate = selectedLineRate;
        observers.firePropertyChange(PROPERTY_LINE_RATE, oldValue, selectedLineRate);
    }

    public void setSelectedResolution(Resolution resolution) {
        Resolution oldValue = this.selectedResolution;
        this.selectedResolution = resolution;
        observers.firePropertyChange(PROPERTY_RESOLUTION, oldValue, resolution);
    }

    public void setTransportSpeed(int transportSpeed) {
        int oldValue = this.transportSpeed;
        this.transportSpeed = transportSpeed;
        observers.firePropertyChange(PROPERTY_TRANSPORT_SPEED, oldValue, transportSpeed);
    }

    /**
     * returns whether this CIS uses an MDR camera link connection on the CIS side.
     * Default is true unless overwritten by subclass that might use SDR
     */
    public boolean usesMdrCameraLinkOnCisSide() {
        return true;
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

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String getShortHand() {
            return shortHand;
        }
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

        private final char code;
        private final String description;
        private final String shortHand;

        LightColor(String description, String shortHand, char code) {
            this.description = description;
            this.shortHand = shortHand;
            this.code = code;
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

        public static Optional<LightColor> findByShortHand(String shortHand) {
            return Arrays.stream(LightColor.values())
                    .filter(c -> c.getShortHand().equals(shortHand))
                    .findFirst();
        }

        public char getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public String getShortHand() {
            return shortHand;
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
     * class for a CIS calculation. Collects electronics and mechanics items and prices.
     */
    public static class CISCalculation {
        public Map<PriceRecord, Integer> electronicConfig = new HashMap<>();
        public Double[] electronicSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        public Map<PriceRecord, Integer> mechanicConfig = new HashMap<>();
        public Double[] mechanicSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        public int numFPGA;
        Double[] totalPrices = new Double[]{0.0, 0.0, 0.0, 0.0};
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

    @Value
    public static class Resolution {
        int actualResolution;
        int boardResolution;
        double depthOfField;
        double pixelSize;
        boolean switchable;

        public Resolution(int actualResolution, int boardResolution, boolean switchable, double depthOfField, double pixelSize) {
            this.pixelSize = pixelSize;
            this.boardResolution = boardResolution;
            this.switchable = switchable;
            this.depthOfField = depthOfField;
            this.actualResolution = actualResolution;
        }
    }
}