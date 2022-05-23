package de.tichawa.cis.config.ldstd;

import de.tichawa.cis.config.*;

import java.util.Optional;

public class LDSTD extends CIS
{

    public LDSTD()
    {
        super();
        this.setSelectedResolution(new Resolution(100,100,false,1.0,1.0));
    }

    @Override
    public double getMaxLineRate()
    {
        return 0;
    }

    @Override
    public String getTiViKey()
    {
        String key = "G_LDSTD";
        key += String.format("_%04d", getScanWidth());

        key += "_";
        if(getLightSources().equals("0D0C"))
        {
            key += "NO";
        }

        if(getPhaseCount() == 4)
        {
            key += "RGB";
        }
        else
        {
            key += getLightColors().stream()
                    .findAny().orElse(LightColor.NONE)
                    .getShortHand();
        }

        if(!getLightSources().endsWith("0C"))
        {
            key += "C";
        }

        key += getMechaVersion();

        if(key.endsWith("_"))
        {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }

    @Override
    public Optional<CameraLink> getCLCalc(int numOfPix)
    {
        return Optional.empty();
    }

    @Override
    public double getGeometry(boolean coax)
    {
        return 1;
    }
}