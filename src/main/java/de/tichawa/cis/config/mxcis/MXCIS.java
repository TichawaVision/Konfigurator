package de.tichawa.cis.config.mxcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import lombok.*;

import java.util.*;

// Alle MXCIS spezifische Funktionen 
public class MXCIS extends CIS {
    private static final HashMap<Integer, String> resToSens = new HashMap<>();
    private static final HashMap<Integer, Integer> dpiToRate = new HashMap<>();

    @Getter
    @Setter
    private int mode;

    public MXCIS() {
    }

    protected MXCIS(MXCIS cis) {
        super(cis);
        this.mode = cis.mode;
    }

    static {
        resToSens.put(200, "PI3033");
        resToSens.put(300, "PI3041");
        resToSens.put(400, "PI3042");
        resToSens.put(600, "PI3039");
        resToSens.put(1200, "PI5002_1200");
        resToSens.put(2400, "PI5002_2400");

        dpiToRate.put(25, 27);
        dpiToRate.put(50, 27);
        dpiToRate.put(75, 18);
        dpiToRate.put(100, 27);
        dpiToRate.put(150, 18);
        dpiToRate.put(200, 27);
        dpiToRate.put(300, 18);
        dpiToRate.put(400, 13);
        dpiToRate.put(600, 10);
    }

    @Override
    public String getTiViKey() {
        String key = "G_MXCIS";
        key += String.format("_%04d", getScanWidth());
        key += String.format("_%04d", getSelectedResolution().getActualResolution());

        if (getPhaseCount() == 4) {
            key += "_K4";
        } else if ((getSelectedResolution().getActualResolution() != 600 && getSelectedLineRate() <= getMaxLineRate() * 0.25) || (getSelectedResolution().getActualResolution() == 600 && getSelectedLineRate() <= getMaxLineRate() * 0.2)) {
            key += "_K1";
        } else if (getSelectedLineRate() <= getMaxLineRate() * 0.5) {
            key += "_K2";
        } else {
            key += "_K3";
        }

        key += "_";
        switch (getLightSources()) {
            case "0D0C":
                key += "NO";
                break;
            case "2D0C":
            case "1D1C":
                key += getLedLines();
                break;
        }

        if (!getLightSources().equals("0D0C")) {
            if (getPhaseCount() == 4) {
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

        key += getMechanicVersion();

        if (isGigeInterface()) {
            key += "GT";
        }
        if (key.endsWith("_")) {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }

    @Override
    public double getMaxLineRate() {
        SensorChipRecord sensorChip = getSensorChip(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
        SensorBoardRecord sensorBoard = getSensorBoard(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown ADC board"));
        AdcBoardRecord adcBoard = getADC("MODU_ADC(SLOW)").orElseThrow(() -> new CISException("Unknown ADC board."));
        return 1000 * Math.round(1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
    }

    @Override
    public List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation) {
        double fpgaDataRate;
        int tapsPerFpga;
        int lval;
        int pixPerFpga;
        int pixPerTap;
        int sensPerFpga;

        if (getSelectedResolution().getActualResolution() > 600) {
            setMode(2);
            sensPerFpga = 1;
        } else if ((getSelectedLineRate() / 1000.0) <= dpiToRate.get(getSelectedResolution().getActualResolution()) && getPhaseCount() != 4) {
            //HALF
            setMode(4);
            sensPerFpga = 4;
        } else {
            //FULL
            setMode(2);
            sensPerFpga = 2;
        }

        SensorChipRecord sensorChip = getSensorChip(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
        SensorBoardRecord sensorBoard = getSensorBoard(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown ADC board"));
        pixPerFpga = sensPerFpga * sensorBoard.getChips() * sensorChip.getPixelPerSensor() / getBinning();
        fpgaDataRate = pixPerFpga * getSelectedLineRate() / 1000.0;
        tapsPerFpga = (int) Math.ceil(fpgaDataRate / (84 * 1000.0));
        if (getPhaseCount() == 1 && calculation.numFPGA > 1 && tapsPerFpga % 2 == 1) {
            tapsPerFpga++;
        }

        pixPerTap = (int) ((double) pixPerFpga / (double) tapsPerFpga);
        lval = pixPerTap;
        int portCount = getPhaseCount() == 1 ? (int) Math.ceil(numOfPix / (lval * 1.0)) : (int) Math.ceil(3 * Math.ceil(numOfPix / (lval * 1.0)));
        int conCount = (int) Math.ceil(numOfPix / (lval * (getPhaseCount() == 1 ? 2.0 : 1.0)));
//    int portCount =  (int) Math.ceil(numOfPix / (lval * (getPhaseCount() == 1 ? 1.0 : 3.0)));
        long dataRate = (long) portCount * Math.min(lval, numOfPix) * getSelectedLineRate();

        CPUCLink CPUCLink = new CPUCLink(dataRate, numOfPix, 85000000);

        if (getPhaseCount() == 4) {
            for (int x = 0; x < tapsPerFpga * calculation.numFPGA; x++) {
                int endPixel = lval > numOfPix ? numOfPix - 1 : (x + 1) * lval - 1;
                CPUCLink.CameraLink conn = new CPUCLink.CameraLink();
                conn.addPorts(new CPUCLink.Port(x * lval, endPixel, "Red"),
                        new CPUCLink.Port(x * lval, endPixel, "Green"),
                        new CPUCLink.Port(x * lval, endPixel, "Blue"));

                CPUCLink.addCameraLink(conn);
            }
        } else if (getPhaseCount() == 1) {
            LinkedList<CPUCLink.CameraLink> cameraLinks = new LinkedList<>();
            for (int x = 0; x * lval < numOfPix; x++) {
                if (x % 2 == 0) {
                    cameraLinks.add(new CPUCLink.CameraLink());
                }

                int endPixel = lval > numOfPix ? numOfPix - 1 : (x + 1) * lval - 1;
                cameraLinks.getLast().addPorts(new CPUCLink.Port(x * lval, endPixel));
            }

            cameraLinks.forEach(CPUCLink::addCameraLink);
        }
        return Collections.singletonList(CPUCLink);
    }

    public Optional<SensorBoardRecord> getSensorBoard(int res) {
        while (!getSensorBoard("SENS_" + res).isPresent()) {
            res *= 2;
        }

        return getSensorBoard("SENS_" + res);
    }

    public Optional<SensorChipRecord> getSensorChip(int res) {
        int z = 1;
        while (resToSens.get(res * z) == null) {
            z++;
        }

        setBinning(z);

        return getSensorChip(resToSens.get(res * z));
    }

    @Override
    public String getLights() {
        return "";
    }

    @Override
    public double getGeometryFactor(boolean coax) {
        return coax ? 0.021 : 0.366;
    }

    /**
     * calculates the factor that will get multiplied by the max line rate for print output
     */
    @Override
    protected double getMaxLineRateFactor() {
        String key = getTiViKey();
        double factor;
        if (key.contains("K1")) {
            if (key.contains("0600")) {
                factor = 0.2;
            } else {
                factor = 0.25;
            }
        } else if (key.contains("K2")) {
            factor = 0.5;
        } else {
            factor = 1;
        }
        return factor;
    }

    /**
     * returns the internal light string for print out by adding schipal staggered/inline to the general calculation by the base class CIS
     */
    @Override
    protected String getInternalLightsForPrintOut() {
        String printout = super.getInternalLightsForPrintOut();
        printout += Util.getString("schipal");
        printout += getSelectedResolution().getActualResolution() > 600 ? Util.getString("staggered") : Util.getString("inline");
        return printout;
    }

    /**
     * returns the transport speed factor for print out
     */
    @Override
    protected double getTransportSpeedFactor() {
        return 1;
    }

    /**
     * returns the geometry correction string for print out
     */
    @Override
    protected String getGeometryCorrectionString() {
        return Util.getString("chpltol") + "\n" + Util.getString("Geocor_opt");
    }

    /**
     * returns the scan distance string for print out: ~10mm
     */
    @Override
    protected String getScanDistanceString() {
        return "~ 10 mm " + Util.getString("warningExactScanDistance");
    }

    /**
     * returns the extra case length that is added to the scan width for print out
     */
    @Override
    protected int getExtraCaseLength() {
        return 288;
    }

    /**
     * returns the case profile string for print out
     */
    @Override
    protected String getCaseProfile() {
        return getLedLines() < 2 ? Util.getString("alucase_mxcis") : Util.getString("alucase_mxcis_two");
    }

    /**
     * returns a string that is appended at the end of the specs section for print out: base camera link configuration
     */
    @Override
    protected String getEndOfSpecs() {
        return Util.getString("clbase");
    }

    /**
     * returns the sensitivity factor that is used for the minimum frequency calculation
     */
    @Override
    protected double getSensitivityFactor() {
        return 30;
    }

    /**
     * calculates the number of pixels:
     * chips on board * number of boards * pixels per sensor / binning
     */
    @Override
    public int calcNumOfPix() {
        SensorChipRecord sensorChip = getSensorChip(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
        SensorBoardRecord sensorBoard = getSensorBoard(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown sensor board"));
        int numOfPix = sensorBoard.getChips() * getBoardCount() * sensorChip.getPixelPerSensor() / getBinning();
        if (isGigeInterface() && getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000 > 80) {
            throw new CISException(Util.getString("errorGigE") + (getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000) + " MByte");
        }
        return numOfPix;
    }

    @Override
    public CIS copy() {
        return new MXCIS(this);
    }

    /**
     * Calculates the number of sensors per fpga for MXCIS.
     * Also sets the mode accordingly to mimic previous behaviour in {@link CIS#calculate()} that got refactored to this class.
     *
     * @return the number of sensors per fpga:
     * 1 - if the actual resulotion is greater than 600
     * 2 - if the phase count is 4
     * otherwise the result of the default (super) call in {@link CIS#getNumberOfSensorsPerFpga()}
     */
    @Override
    protected int getNumberOfSensorsPerFpga() {
        int numberOfSensorsPerFpga;
        if (getSelectedResolution().getActualResolution() > 600)
            numberOfSensorsPerFpga = 1;
        else if (getPhaseCount() == 4)
            numberOfSensorsPerFpga = 2;
        else
            numberOfSensorsPerFpga = super.getNumberOfSensorsPerFpga();

        setMode(numberOfSensorsPerFpga);
        return numberOfSensorsPerFpga;
    }

    // should return false if code is unknown
    @Override
    protected boolean checkSpecificApplicability(String code) {
        switch (code) {
            case "L": //MODE: LOW (MXCIS only)
                return getMode() == 4;
            case "H": //Mode: HIGH (MXCIS only)
                return getMode() == 2;
        }
        return false;
    }
}
