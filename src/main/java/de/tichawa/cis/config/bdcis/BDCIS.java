package de.tichawa.cis.config.bdcis;

import de.tichawa.cis.config.*;

import java.util.*;

/**
 * CIS subclass for all BDCIS objects.
 * {@link BDCIS} extends {@link CisWith5Lights} even though BDCIS only supports 2 lights to use the same (and new) user interface as VUCIS
 */
public class BDCIS extends CisWith5Lights {
    /**
     * Available resolutions for BDCIS
     */
    private static final List<Resolution> resolutions;

    static {
        resolutions = Arrays.asList(
                new CIS.Resolution(240, 1200, false, 10, 0.10575),
                new CIS.Resolution(120, 1200, false, 15, 0.2115),
                new CIS.Resolution(60, 1200, false, 20, 0.423),
                new CIS.Resolution(30, 1200, false, 25, 0.847)
        );
    }

    /**
     * Default constructor. Sets the initial values for a new BDCIS
     */
    public BDCIS() {
        this.setSelectedResolution(resolutions.get(0)); // start with the first resolution
        this.setSelectedLineRate((int) getMaxLineRate()); // start with the maximum line rate (for the starting resolution)
        // initialize all BDCIS attributes here if there are any in the future
    }

    /**
     * Copy constructor. Copies all attribute values of the given BDCIS
     *
     * @param bdcis the BDCIS to copy
     */
    public BDCIS(BDCIS bdcis) {
        super(bdcis);
        // copy all BDCIS attributes here if there are any in the future
    }

    /**
     * Returns a list of possible BDCIS resolutions
     */
    public static List<Resolution> getResolutions() {
        return resolutions;
    }

    /**
     * Returns whether the given light color is a valid option for BDCIS
     */
    public static boolean isBDCISLightColor(LightColor lightColor) {
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
            case IRUV:
                return true;
            default:
                return false;
        }
    }

    @Override
    public CIS copy() {
        return new BDCIS(this);
    }

    /**
     * returns a fixed weight string depending on CIS size since calculation is not perfect due to many missing weight values
     */
    @Override
    protected String getWeightString(CISCalculation calculation) {
        return CisWith5Lights.WEIGHTS[getScanWidth() / BASE_LENGTH] + "kg";
    }

    /**
     * Prepares the electronic factor string by replacing
     * - SN with the number of sensors
     *
     * @param factor      the factor string that gets (partly) replaced
     * @param calculation the CIS calculation that might be needed for replacements in subclasses
     * @return the factor string after alteration
     */
    @Override
    protected String prepareElectronicsAmountString(String factor, CISCalculation calculation) {
        return super.prepareElectronicsAmountString(factor, calculation)
                .replaceAll("SN", getNumberOfSensors() + "");
    }

    @Override
    protected double getGeometryFactor(boolean coax) {
        return 0.4; //TODO
    }

    /**
     * Returns the lens code which is "64" as there is only one lens for BDCIS
     */
    @Override
    protected String getLensTypeCode() {
        return "64"; // only one lens option for BDCIS
    }

    /**
     * Returns the maximum scan width with coax. BDCIS does not have coax lighting so the maximum value is returned.
     */
    @Override
    protected int getMaxScanWidthWithCoax() {
        return Integer.MAX_VALUE; // BDCIS has no coax so no restriction here
    }

    /**
     * Returns the number of cpuc links for BDCIS which is always 1
     */
    @Override
    protected int getNumOfCPUCLink() {
        return 1; // always 1 cpuc link for BDCIS
    }

    /**
     * Calculates the number of sensors in this BDCIS.
     * Per each 250mm there are 4 sensors for 240dpi, half of that for 120 dpi, quarter for 60dpi, ...
     */
    private int getNumberOfSensors() {
        return (int) Math.ceil(4 * getSelectedResolution().getActualResolution() / 240. * getScanWidth() / BASE_LENGTH);
    }
}