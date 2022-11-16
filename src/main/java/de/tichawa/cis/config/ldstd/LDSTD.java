package de.tichawa.cis.config.ldstd;

import de.tichawa.cis.config.*;

import java.util.Optional;

public class LDSTD extends CIS {

    public LDSTD() {
        super();
        this.setSelectedResolution(new Resolution(100, 100, false, 1.0, 1.0));
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

        key += getMechaVersion();

        if (key.endsWith("_")) {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }

    @Override
    public Optional<CameraLink> getCLCalc(int numOfPix) {
        return Optional.empty();
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
    public String createPrntOut() {
        String printout = getTiViKey();
        printout += "\n\t\n";
        printout += getString("suitedfor") + getScanWidth() + getString("mm CIS scan width") + "\n";

        LightColor color;

        if (getPhaseCount() == 3) {
            color = LightColor.RGB;
        } else {
            color = getLightColors().stream()
                    .findAny().orElse(LightColor.NONE);
        }
        printout += getString("Color:") + getString(color.getDescription()) + "\n";
        printout += "\n\t\n";
        printout += getString("line width") + ": ~ 1 mm\n";
        printout += getString("case length") + ": ~ " + (getScanWidth() + 100) + " mm\n";
        printout += getString("Aluminium case profile: 53x50mm (HxT) with bondedmxled") + "\n";
        printout += getString("glass pane, see drawing") + "\n";
        printout += getString("shading") + "\n";
        printout += getString("powersource") + "(24 +/- 1) VDC\n";
        printout += getString("Needed power:") + (((electSums[2] == null) ? 0.0 : (Math.round(10.0 * electSums[2]) / 10.0)) + " A").replace(" 0 A", " ???") + " +/- 20%\n";
        printout += getString("weight") + ": ~ " + (Math.round((((electSums[3] == null) ? 0.0 : electSums[3]) + ((mechaSums[3] == null) ? 0.0 : mechaSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";

        return printout;
    }

    /**
     * returns the depth of field for print out: 0.5mm
     */
    @Override
    protected double getDepthOfField() {
        return 0.5;
    }

    /**
     * returns the case profile string for print out: 53x50
     */
    @Override
    protected String getCaseProfile() {
        return getString("Aluminium case profile: 53x50mm (HxT) with bonded");
    }
}