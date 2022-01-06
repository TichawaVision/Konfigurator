package de.tichawa.cis.config.mxled;

import de.tichawa.cis.config.*;

public class MXLED extends CIS
{

  public MXLED()
  {
    super();
  }

  @Override
  public double getMaxLineRate()
  {
    return 0;
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_MXLED";
    key += String.format("_%04d", getScanWidth());
    key += "_K1";

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
      key += getLightColor().getShortHand();
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
  public String getCLCalc(int numOfPix)
  {
    return " ";
  }

  @Override
  public double getGeometry(boolean coax)
  {
    return 1;
  }
}
