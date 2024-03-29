package de.tichawa.cis.config.ldstd;

import de.tichawa.cis.config.*;

import java.util.*;

public class LDSTD extends CIS
{

  public LDSTD()
  {
    super();

    setSpec("LDSTD", 1);
  }

  public static int getSWIndex(int length)
  {
    ArrayList<Integer> list = new ArrayList<>();
    list.add(260);
    list.add(390);
    list.add(520);
    list.add(650);
    list.add(780);
    list.add(910);
    list.add(1040);
    list.add(1300);
    list.add(1560);
    list.add(1820);
    list.add(2080);
    list.add(2340);
    list.add(2600);
    list.add(2860);
    list.add(3120);
    list.add(3380);
    list.add(3640);
    list.add(3900);
    list.add(4160);

    return list.indexOf(length);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_LDSTD";
    key += String.format("_%04d", getSpec("sw_cp"));

    switch(getSpec("Internal Light Source"))
    {
      case 0:
      {
        key += "_NO";
        break;
      }
      case 1:
      {
        key += "_" + COLOR_CODE[getSpec("Internal Light Color")];
        break;
      }
      case 2:
      {
        key += "_2" + COLOR_CODE[getSpec("Internal Light Color")];
        break;
      }
      case 3:
      {
        key += "_3" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
        break;
      }
      case 4:
      {
        key += "_" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
        break;
      }
    }

    if(getSpec("Color") == 3 || getSpec("Color") == 4)
    {
      key = key.replace(COLOR_CODE[getSpec("Internal Light Color")], "RGB");
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
    return coax ? 1 : 1;
  }
}
