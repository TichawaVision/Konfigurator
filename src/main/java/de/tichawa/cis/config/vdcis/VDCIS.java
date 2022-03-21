package de.tichawa.cis.config.vdcis;

import de.tichawa.cis.config.*;

import java.util.*;

public class VDCIS extends CIS
{

  public VDCIS()
  {
    super();

    setSpec("VDCIS", 1);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VDCIS";
    key += String.format("_%04d", getSpec("sw_cp"));

    if(getSpec("Resolution") == 0) //Switchable
    {
      key += "_XXXX";
    }
    else
    {
      key += String.format("_%04d", getSpec("res_cp2"));
    }

//    switch(getSpec("Internal Light Source"))
//    {
//      case 0:
//      {
//        key += "_NO";
//        break;
//      }
//      case 1:
//      {
//        key += "_" + COLOR_CODE[getSpec("Internal Light Color")];
//        break;
//      }
//      case 2:
//      {
//        key += "_2" + COLOR_CODE[getSpec("Internal Light Color")];
//        break;
//      }
//      case 3:
//      {
//        key += "_3" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
//        break;
//      }
//      case 4:
//      {
//        key += "_" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
//        break;
//      }
//    }

    if(getSpec("Color") == 4)
    {
      key += "_2" + "RGB";
    }else{
      key += "_2" + COLOR_CODE[getSpec("Internal Light Color")];
  }

    key += getMechaVersion();

//    if(getSpec("Interface") == 1)
//    {
//      key += "GT";
//    }
//
//    switch(getSpec("Cooling"))
//    {
//      case 0:
//      {
//        key += "NOCO";
//        break;
//      }
//      case 1:
//      {
//        break;
//      }
//      case 2:
//      {
//        key += "FAIR";
//        break;
//      }
//      case 3:
//      {
//        key += "PAIR";
//        break;
//      }
//      case 4:
//      {
//        key += "LICO";
//        break;
//      }
//    }

    if(key.endsWith("_"))
    {
      key = key.substring(0, key.length() - 1);
    }

    return key;
  }

  @Override
  public String getCLCalc(int numOfPix)
  {
    int numOfPixNominal;
    int taps;
    int pixPerTap;
    int lval;
    int tapCount;
    StringBuilder printOut = new StringBuilder();

    numOfPixNominal = (int) Math.ceil(numOfPix - ((getSpec("sw_cp") / BASE_LENGTH) * getSensBoard("SMARAGD")[7] / (1200 / getSpec("res_cp2"))));
    taps = (int) Math.ceil(1.01 * ((long) numOfPixNominal * getSpec("Selected line rate") / 1000000) / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    boolean mediumMode = getSpec("CLMode") == 1;

    printOut.append(getString("datarate")).append(Math.round((getSpec("Color") - 1) * numOfPixNominal * getSpec("Selected line rate") / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append("%%%%%\n");
    printOut.append(getString("numofport")).append(taps * (getSpec("Color") - 1)).append("\n");
    printOut.append("Pixel Clock: 85 MHz\n");
    printOut.append(getString("nomPix")).append(numOfPixNominal).append("\n");
    printOut.append("LVAL: ").append(lval).append("\n");
    printOut.append(getString("clMode")).append(mediumMode ? "Base/Medium/Full" : "Full80").append("\n");
    printOut.append(getString("numPhases")).append(getSpec("Color")).append("\n");

    Map<Integer, List<Integer>> mediumMap = new HashMap<>();
    mediumMap.put(1, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 9, 10, 11, 12, 13, 14, 15, 16, 0, 0));
    mediumMap.put(2, Arrays.asList(1, 1, 0, 2, 2, 0, 0, 0, 0, 0, 3, 3, 0, 4, 4, 0, 0, 0, 0, 0));
    mediumMap.put(3, Arrays.asList(1, 1, 1, 2, 2, 2, 0, 0, 0, 0, 3, 3, 3, 4, 4, 4, 0, 0, 0, 0));
    mediumMap.put(4, Arrays.asList(1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0));
    mediumMap.put(5, Arrays.asList(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0));
    mediumMap.put(6, Arrays.asList(1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0));

    Map<Integer, List<Integer>> highMap = new HashMap<>();
    highMap.put(1, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 0));
    highMap.put(2, Arrays.asList(1, 1, 0, 2, 2, 0, 3, 3, 0, 0, 4, 4, 0, 5, 5, 0, 6, 6, 0, 0));
    highMap.put(3, Arrays.asList(1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 4, 4, 4, 5, 5, 5, 6, 6, 6, 0));
    highMap.put(4, mediumMap.get(4));
    highMap.put(5, mediumMap.get(5));
    highMap.put(6, mediumMap.get(6));

    List<Integer> tapConfig = (mediumMode ? mediumMap : highMap).get(getSpec("Color") - 1);
    if(taps > tapConfig.stream().mapToInt(x -> x).max().orElse(0))
    {
      throw new CISException("Number of required taps (" + taps * getSpec("Color") + ") is too high. Please reduce the data rate.");
    }
    if(getSpec("res_cp2") >= 1200 && (numOfPix - 16 * getSpec("sw_cp") / BASE_LENGTH * 6 * 2) * getSpec("Color") * 2 > 327680)
    {
      throw new CISException("Out of Flash memory. Please reduce the scan width or resolution.");
    }

    int cableCount = 0;
    for(tapCount = 0; tapCount < tapConfig.size(); tapCount++)
    {
      int currentTap = tapConfig.get(tapCount);
      if(currentTap > 0 && taps >= currentTap)
      {
        if(tapCount % 10 == 0)
        {
          printOut.append("Camera Link ").append((tapCount / 10) + 1).append(":\n");
        }
        printOut.append("   Port ").append(getPortName(tapCount % 10)).append(":   ")
            .append(String.format("%05d", (currentTap - 1) * lval)).append("   - ")
            .append(String.format("%05d", currentTap * lval - 1)).append("\n");

        if(tapCount % 10 == 0 || tapCount % 10 == 3)
        {
          cableCount++;
        }
      }
    }

    printOut.append(getString("configOnRequest"));
    printOut.replace(printOut.indexOf("%%%%%"), printOut.indexOf("%%%%%") + 5, cableCount + "");
    return printOut.toString();
  }

  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.229 : 0.252;
  }
}
