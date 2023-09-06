package de.tichawa.cis.config.bdcis;

import de.tichawa.cis.config.*;

import java.util.*;

public class BDCIS extends CIS {
    /**
     * Available resolutions for VUCIS
     */
    private static final List<Resolution> resolutions;

    static {
        resolutions = new LinkedList<>(); //TODO
    }

    public BDCIS() {
        //TODO set initial values to all attributes
    }

    public BDCIS(BDCIS bdcis) {
        super(bdcis);
        //TODO copy all BDCIS attributes
    }

    public static List<Resolution> getResolutions() {
        return resolutions;
    }

    @Override
    public CIS copy() {
        return new BDCIS(this);
    }

    @Override
    public List<CPUCLink> getCLCalc(int numOfPix, CISCalculation calculation) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected double getGeometryFactor(boolean coax) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public String getLights() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public double getMaxLineRate() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public String getTiViKey() {
        return "BDCIS_ ...";
        //TODO
    }
}
