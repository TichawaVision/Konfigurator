package de.tichawa.cis.config.vscis;

import de.tichawa.cis.config.*;

public class VSCIS extends CIS
{

  public VSCIS()
  {
    super();

    setSpec("VSCIS", 1);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VSCIS";
    key += String.format("_%04d", getSpec("sw_cp"));

    if(getSpec("Resolution") == 0) //Switchable
    {
      key += "_XXXX";
    }
    else
    {
      key += String.format("_%04d", getSpec("res_cp2"));
    }

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

    if(getSpec("Color") == 3)
    {
      key = key.replace(COLOR_CODE[getSpec("Internal Light Color")], "RGB");
    }

    key += getMechaVersion();

    if(getSpec("Interface") == 1)
    {
      key += "GT";
    }

    switch(getSpec("Cooling"))
    {
      case 0:
      {
        key += "NOCO";
        break;
      }
      case 1:
      {
        break;
      }
      case 2:
      {
        key += "FAIR";
        break;
      }
      case 3:
      {
        key += "PAIR";
        break;
      }
      case 4:
      {
        key += "LICO";
        break;
      }
    }

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
    int chipsPerTap;
    double portDataRate;
    int lval;
    int pixPerTap;
    double ppsbin;
    double binning;
    StringBuilder printOut = new StringBuilder();

    numOfPixNominal = (int) (numOfPix - ((getSpec("sw_cp") / getBaseLength()) * getSensBoard("SMARAGD")[7] / (1200 / getSpec("res_cp2"))));
    taps = (int) Math.ceil((numOfPix * getSpec("Selected line rate") / 1000000.0) / 85.0);
    chipsPerTap = (int) Math.ceil((getSensBoard("SMARAGD")[0] * (getSpec("sw_cp") / getBaseLength())) / (double) taps);
    ppsbin = getSensChip("SMARAGD" + getSpec("res_cp") + "_VS")[3] / ((double) getSpec("res_cp") / (double) getSpec("res_cp2"));
    pixPerTap = (int) (chipsPerTap * ppsbin);
    portDataRate = pixPerTap * getSpec("Selected line rate") / 1000000.0;

    while(portDataRate > 85.0)
    {
      taps++;
      chipsPerTap = (int) Math.ceil((getSensBoard("SMARAGD")[0] * (getSpec("sw_cp") / getBaseLength())) / (double) taps);
      ppsbin = getSensChip("SMARAGD" + getSpec("res_cp") + "_VS")[3] / ((double) getSpec("res_cp") / (double) getSpec("res_cp2"));
      pixPerTap = (int) (chipsPerTap * ppsbin);
      portDataRate = pixPerTap * getSpec("Selected line rate") / 1000000.0;
    }
    binning = 1 / (getSensChip("SMARAGD" + getSpec("res_cp") + "_VS")[6] * ((double) getSpec("res_cp") / (double) getSpec("res_cp2")));
    lval = (int) (chipsPerTap * (ppsbin - (getSensBoard("SMARAGD")[7] * binning) / getSensBoard("SMARAGD")[0]));
    lval -= lval % 8;

    printOut.append(getString("datarate")).append(Math.round(getSpec("Color") * numOfPix * getSpec("Selected line rate") / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append((taps * getSpec("Color") > 3) ? "2" : "1").append("\n");
    printOut.append(getString("numofport")).append(taps * getSpec("Color")).append("\n");
    printOut.append("Pixel Clock: 85 MHz").append("\n");
    printOut.append("Nominal pixel count: ").append(numOfPixNominal).append("\n");

    switch(getSpec("Color"))
    {
      case 3:
      {
        if(taps > 3)
        {
          System.out.println("Please select a lower line rate. Currently required number of taps (" + taps * getSpec("Color") + ") is too high.");
          return null;
        }

        for(int x = 0; x < Math.min(2, taps); x++)
        {
          printOut.append("Camera Link ").append(x + 1).append(":\n");
          printOut.append("   Port ").append(getPortName(x * 3)).append(":   ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("   ")
                  .append(getString("Red")).append("\n");
          printOut.append("   Port ").append(getPortName(x * 3 + 1)).append(":   ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("   ")
                  .append(getString("Green")).append("\n");
          printOut.append("   Port ").append(getPortName(x * 3 + 2)).append(":   ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("   ")
                  .append(getString("Blue")).append("\n");
        }

        if(taps == 3)
        {
          printOut.append("   Port ").append(getPortName(3)).append(":   ")
                  .append(String.format("%05d", 2 * lval)).append("   - ").append(String.format("%05d", 3 * lval - 1)).append("   ")
                  .append(getString("Red")).append("\n");
          printOut.append("   Port ").append(getPortName(4)).append(":   ")
                  .append(String.format("%05d", 2 * lval)).append("   - ").append(String.format("%05d", 3 * lval - 1)).append("   ")
                  .append(getString("Green")).append("\n");
          printOut.append("   Port ").append(getPortName(5)).append(":   ")
                  .append(String.format("%05d", 2 * lval)).append("   - ").append(String.format("%05d", 3 * lval - 1)).append("   ")
                  .append(getString("Blue")).append("\n");
        }

        break;
      }
      case 1:
      {
        if(taps > 8)
        {
          System.out.println("Please select a lower line rate. Currently required number of taps (" + taps * getSpec("Color") + ") is too high.");
          return null;
        }

        printOut.append("Camera Link 1:\n");
        for(int x = 0; x < Math.min(3, taps); x++)
        {
          printOut.append("   Port ").append(getPortName(x)).append(":   ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
        }

        if(taps > 3)
        {
          printOut.append("Camera Link 2:\n");
          for(int x = 3; x < taps; x++)
          {
            printOut.append("   Port ").append(getPortName(x)).append(":   ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
          }
        }
        break;
      }
    }

    return printOut.toString();
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.105 : 0.128;
  }
}
