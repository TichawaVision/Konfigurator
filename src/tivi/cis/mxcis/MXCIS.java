package tivi.cis.mxcis;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import tivi.cis.*;

public class MXCIS extends CIS
{
  private static HashMap<Integer, String> resToSens;

  public MXCIS()
  {
    super();

    resToSens = new HashMap<>();
    resToSens.put(200, "PI3033");
    resToSens.put(300, "PI3041");
    resToSens.put(400, "PI3042");
    resToSens.put(600, "PI3039");
    resToSens.put(1200, "PI5002_1200");
    resToSens.put(2400, "PI5002_2400");

    setSpec("MXCIS", 1);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_MXCIS";
    key += String.format("_%04d", getSpec("sw_cp"));
    key += String.format("_%04d", getSpec("res_cp2"));

    if(getSpec("Color") == 4)
    {
      key += "_K4";
    }
    else if((getSpec("res_cp2") != 600 && getSpec("Selected line rate") <= getSpec("Maximum line rate") * 0.25) || (getSpec("res_cp2") == 600 && getSpec("Selected line rate") <= getSpec("Maximum line rate") * 0.2))
    {
      key += "_K1";
    }
    else if(getSpec("Selected line rate") <= getSpec("Maximum line rate") * 0.5)
    {
      key += "_K2";
    }
    else
    {
      key += "_K3";
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
        key += "_" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
      case 3:
      {
        key += "_2" + COLORCODE[getSpec("Internal Light Color")];
        break;
      }
      case 4:
      {
        key += "_2" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
    }

    if(getSpec("Color") == 4)
    {
      key = key.replace(COLORCODE[getSpec("Internal Light Color")], "RGB");
    }

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Calculation.csv"), Charset.forName("UTF-8"))))
    {
      String line;
      Map<String, String> calcMap = new HashMap<>();

      while((line = reader.readLine()) != null)
      {
        String[] calc = line.split("\t");
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
    HashMap<Integer, Integer> dpiToRate = new HashMap<>();
    dpiToRate.put(25, 27);
    dpiToRate.put(50, 27);
    dpiToRate.put(75, 18);
    dpiToRate.put(100, 27);
    dpiToRate.put(150, 18);
    dpiToRate.put(200, 27);
    dpiToRate.put(300, 18);
    dpiToRate.put(400, 13);
    dpiToRate.put(600, 10);

    double FpgaDataRate;
    int tapsPerFpga;
    int lval;
    int pixPerFpga;
    int pixPerTap;
    int sensPerFpga;
    StringBuilder printOut = new StringBuilder();

    if(getSpec("res_cp2") > 600)
    {
      spec.put("MODE", 2);
      sensPerFpga = 1;
    }
    else if((getSpec("Selected line rate") / 1000.0) <= dpiToRate.get(getSpec("res_cp2")) && getSpec("Color") != 4)
    {
      //HALF
      spec.put("MODE", 4);
      sensPerFpga = 4;
    }
    else
    {
      //FULL
      spec.put("MODE", 2);
      sensPerFpga = 2;
    }

    pixPerFpga = (int) (sensPerFpga * getBoard(getSpec("res_cp"))[0] * getChip(getSpec("res_cp2"))[3] / getSpec("Binning"));
    FpgaDataRate = pixPerFpga * getSpec("Selected line rate") / 1000.0;
    tapsPerFpga = (int) Math.ceil(FpgaDataRate / (84 * 1000.0));
    if(getSpec("Color") == 1 && getNumFPGA() > 1 && tapsPerFpga % 2 == 1)
    {
      tapsPerFpga++;
    }

    pixPerTap = (int) ((double) pixPerFpga / (double) tapsPerFpga);
    lval = pixPerTap;

    int portCount = getSpec("Color") == 1 ? (int) Math.ceil(numOfPix / (lval * 1.0)) : (int) Math.ceil(3 * Math.ceil(numOfPix / (lval * 1.0)));

    printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("datarate")).append(Math.round(portCount * Math.min(lval, numOfPix) * getSpec("Selected line rate") / 100000.0) / 10.0).append(" MByte/s\n");
    printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("numofpix")).append(numOfPix).append("\n");
    
    if(getSpec("Color") == 1)
    {
      printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("numofcons")).append((int) Math.ceil(numOfPix / (lval * 2.0))).append("\n");
    }
    else
    {
      printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("numofcons")).append((int) Math.ceil(numOfPix / (lval * 1.0))).append("\n");
    }
    printOut.append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("numofport")).append(portCount).append("\n");
    printOut.append("Pixel Clock: 85MHz\n\n");

    switch(getSpec("Color"))
    {
      case 4:
      {
        for(int x = 0; x < tapsPerFpga * getNumFPGA(); x++)
        {
          if(lval > numOfPix)
          {
            printOut.append("Camera Link ").append(x + 1).append(":\n");
            printOut.append("\tPort A:\t")
                    .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", numOfPix - 1)).append("\t")
                    .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Red")).append("\n");
            printOut.append("\tPort B:\t")
                    .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", numOfPix - 1)).append("\t")
                    .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Green")).append("\n");
            printOut.append("\tPort C:\t")
                    .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", numOfPix - 1)).append("\t")
                    .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Blue")).append("\n");
          }
          else
          {
            printOut.append("Camera Link ").append(x + 1).append(":\n");
            printOut.append("\tPort A:\t")
                    .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                    .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Red")).append("\n");
            printOut.append("\tPort B:\t")
                    .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                    .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Green")).append("\n");
            printOut.append("\tPort C:\t")
                    .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                    .append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Blue")).append("\n");
          }
        }

        break;
      }
      case 1:
      {
        for(int x = 1; (x - 1) * lval < numOfPix; x++)
        {
          if(x % 2 == 1)
          {
            printOut.append("Camera Link ").append((x + 1) / 2).append(":\n");
          }

          if(lval > numOfPix)
          {
            printOut.append("\tPort ").append(getPortName((x + 1) % 2)).append(":\t")
                    .append(String.format("%05d", (x - 1) * lval)).append("\t - ").append(String.format("%05d", numOfPix - 1)).append("\n");
          }
          else
          {
            printOut.append("\tPort ").append(getPortName((x + 1) % 2)).append(":\t")
                    .append(String.format("%05d", (x - 1) * lval)).append("\t - ").append(String.format("%05d", x * lval - 1)).append("\n");
          }
        }
      }
    }

    return printOut.toString();
  }

  public Integer[] getBoard(int res)
  {
    while(getSensBoard("SENS_" + res) == null)
    {
      res *= 2;
    }

    return getSensBoard("SENS_" + res);
  }

  public Integer[] getChip(int res)
  {
    int z = 1;
    while(resToSens.get(res * z) == null)
    {
      z++;
    }

    setSpec("Binning", z);

    return getSensChip(resToSens.get(res * z));
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.021 : 0.366;
  }
}
