package de.tichawa.cis.config.ldstd;

import de.tichawa.cis.config.*;

import java.util.*;

public class LDSTD extends CIS {

    public LDSTD() {
        super();
        this.setSelectedResolution(new Resolution(100, 100, false, 1.0, 1.0));
    }

    protected LDSTD(LDSTD cis) {
        super(cis);
    }

    @Override
    public CIS copy() {
        return new LDSTD(this);
    }

    @Override
    public String createPrntOut() {
        CISCalculation calculation = calculate();

        String printout = getTiViKey();
        printout += "\n\t\n";
        printout += Util.getString("suitedFor") + " " + getScanWidth() + " mm " + Util.getString("cisScanWidth") + "\n";

        LightColor color;

        if (getPhaseCount() == 3) {
            color = LightColor.RGB;
        } else {
            color = getLightColors().stream()
                    .findAny().orElse(LightColor.NONE);
        }
        printout += Util.getString("color") + ": " + Util.getString(color.getDescription()) + "\n";
        printout += "\n\t\n";
        printout += Util.getString("lineWidthLight") + ": ~ 1 mm\n";
        printout += Util.getString("caseLength") + ": ~ " + (getScanWidth() + 100) + " mm\n";
        printout += Util.getString("aluminumCaseLDSTD") + "\n";
        printout += Util.getString("glassPane") + "\n";
        printout += Util.getString("shading") + "\n";
        printout += Util.getString("powerSource") + ": (24 +/- 1) VDC\n";
        printout += Util.getString("maxPower") + ": "
                + (((calculation.electronicSums[2] == null) ? 0.0 : (Math.round(10.0 * calculation.electronicSums[2]) / 10.0)) + " A")
                .replace(" 0 A", " ???") + " +/- 20%\n";
        printout += Util.getString("weight") + ": ~ "
                + (Math.round((((calculation.electronicSums[3] == null) ? 0.0 : calculation.electronicSums[3])
                + ((calculation.mechanicSums[3] == null) ? 0.0 : calculation.mechanicSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";

        return printout;
    }

    @Override
    public List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation) {
        return new LinkedList<>();
    }

    /**
     * returns the case profile string for print out: 53x50
     */
    @Override
    protected String getCaseProfile() {
        return Util.getString("aluminumCaseVSCIS");
    }

    /**
     * returns the depth of field for print out: 0.5mm
     */
    @Override
    protected double getDepthOfField() {
        return 0.5;
    }

    @Override
    public double getGeometryFactor(boolean coax) {
        return 1;
    }

    @Override
    public String getLights() {
        return "";
    }

    @Override
    public double getMaxLineRate() {
        return 0;
    }

    @Override
    public String getTiViKey() {
        String key = "G_LDSTD";
        key += String.format("_%04d", getScanWidth());

        key += "_";
        if (getLightSources().equals("0D0C")) {
            key += "NO";
        }

        if (getPhaseCount() == 3) {
            key += "RGB";
        } else {
            key += getLightColors().stream()
                    .findAny().orElse(LightColor.NONE)
                    .getShortHand();
        }

        if (!getLightSources().endsWith("0C")) {
            key += "C";
        }

        key += getMechanicVersion();

        if (key.endsWith("_")) {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }
}