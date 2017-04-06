package tivi.cis.vscis;

import java.io.*;
import java.util.*;
import tivi.cis.*;

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
        key += "_" + COLORCODE[getSpec("Internal Light Color")];
        break;
      }
      case 2:
      {
        key += "_2" + COLORCODE[getSpec("Internal Light Color")];
        break;
      }
      case 3:
      {
        key += "_3" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
      case 4:
      {
        key += "_" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
    }

    if(getSpec("Color") == 3)
    {
      key = key.replace(COLORCODE[getSpec("Internal Light Color")], "RGB");
    }

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Calculation.csv"))))
    {
      String line;
      Map<String, String> calcMap = new HashMap<>();

      while((line = reader.readLine()) != null)
      {
        String[] calc = line.split(";");
        calcMap.put(calc[0], calc[1]);
      }

      key += calcMap.containsKey("VERSION") ? "_" + calcMap.get("VERSION") + "_" : "_2.0_";
    }
    catch(IOException e)
    {
      key += "_2.0_";
    }

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
  public String getCLCalc(int numOfPix, Locale LANGUAGE)
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

    numOfPixNominal = (int) (numOfPix - ((getSpec("sw_cp") / 260) * getSensBoard("SMARAGD")[7] / (1200 / getSpec("res_cp2"))));
    taps = (int) Math.ceil((numOfPix * getSpec("Selected line rate") / 1000000.0) / 85.0);
    chipsPerTap = (int) Math.ceil((getSensBoard("SMARAGD")[0] * (getSpec("sw_cp") / 260)) / (double) taps);
    ppsbin = getSensChip("SMARAGD" + getSpec("res_cp") + "_VS")[3] / ((double) getSpec("res_cp") / (double) getSpec("res_cp2"));
    pixPerTap = (int) (chipsPerTap * ppsbin);
    portDataRate = pixPerTap * getSpec("Selected line rate") / 1000000.0;

    while(portDataRate > 85.0)
    {
      taps++;
      chipsPerTap = (int) Math.ceil((getSensBoard("SMARAGD")[0] * (getSpec("sw_cp") / 260)) / (double) taps);
      ppsbin = getSensChip("SMARAGD" + getSpec("res_cp") + "_VS")[3] / ((double) getSpec("res_cp") / (double) getSpec("res_cp2"));
      pixPerTap = (int) (chipsPerTap * ppsbin);
      portDataRate = pixPerTap * getSpec("Selected line rate") / 1000000.0;
    }
    binning = 1 / (getSensChip("SMARAGD" + getSpec("res_cp") + "_VS")[6] * ((double) getSpec("res_cp") / (double) getSpec("res_cp2")));
    lval = (int) (chipsPerTap * (ppsbin - (getSensBoard("SMARAGD")[7] * binning) / getSensBoard("SMARAGD")[0]));
    lval -= lval % 8;

    printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("datarate")).append(Math.round(getSpec("Color") * numOfPix * getSpec("Selected line rate") / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("numofcons")).append((taps * getSpec("Color") > 3) ? "2" : "1").append("\n");
    printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("numofport")).append(taps * getSpec("Color")).append("\n");
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
          printOut.append("\tPort ").append(getPortName(x * 3)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Red")).append("\n");
          printOut.append("\tPort ").append(getPortName(x * 3 + 1)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Green")).append("\n");
          printOut.append("\tPort ").append(getPortName(x * 3 + 2)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Blue")).append("\n");
        }

        if(taps == 3)
        {
          printOut.append("\tPort ").append(getPortName(3)).append(":\t")
                  .append(String.format("%05d", 2 * lval)).append("\t - ").append(String.format("%05d", 3 * lval - 1)).append("\t")
                  .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Red")).append("\n");
          printOut.append("\tPort ").append(getPortName(4)).append(":\t")
                  .append(String.format("%05d", 2 * lval)).append("\t - ").append(String.format("%05d", 3 * lval - 1)).append("\t")
                  .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Green")).append("\n");
          printOut.append("\tPort ").append(getPortName(5)).append(":\t")
                  .append(String.format("%05d", 2 * lval)).append("\t - ").append(String.format("%05d", 3 * lval - 1)).append("\t")
                  .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Blue")).append("\n");
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
          printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
        }

        if(taps > 3)
        {
          printOut.append("Camera Link 2:\n");
          for(int x = 3; x < taps; x++)
          {
            printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                    .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
          }
        }
        break;
      }
    }

    return printOut.toString();
  }
}
