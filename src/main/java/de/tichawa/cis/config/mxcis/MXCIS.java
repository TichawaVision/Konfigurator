package de.tichawa.cis.config.mxcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;

import java.util.*;

// Alle MXCIS spezifische Funktionen 
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
        key += "_" + COLOR_CODE[getSpec("Internal Light Color")];
        break;
      }
      case 2:
      {
        key += "_" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
        break;
      }
      case 3:
      {
        key += "_2" + COLOR_CODE[getSpec("Internal Light Color")];
        break;
      }
      case 4:
      {
        key += "_2" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
        break;
      }
    }

    if(getSpec("Color") == 4)
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

    double fpgaDataRate;
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

    SensorChipRecord sensorChip = getSensorChip(getSpec("res_cp2")).orElseThrow(() -> new CISException("Unknown sensor chip"));
    SensorBoardRecord sensorBoard = getSensorBoard(getSpec("res_cp2")).orElseThrow(() -> new CISException("Unknown ADC board"));
    pixPerFpga = sensPerFpga * sensorBoard.getChips() * sensorChip.getPixelPerSensor() / getSpec("Binning");
    fpgaDataRate = pixPerFpga * getSpec("Selected line rate") / 1000.0;
    tapsPerFpga = (int) Math.ceil(fpgaDataRate / (84 * 1000.0));
    if(getSpec("Color") == 1 && getNumFPGA() > 1 && tapsPerFpga % 2 == 1)
    {
      tapsPerFpga++;
    }

    pixPerTap = (int) ((double) pixPerFpga / (double) tapsPerFpga);
    lval = pixPerTap;

    int portCount = getSpec("Color") == 1 ? (int) Math.ceil(numOfPix / (lval * 1.0)) : (int) Math.ceil(3 * Math.ceil(numOfPix / (lval * 1.0)));

    printOut.append(getString("datarate")).append(Math.round(portCount * Math.min(lval, numOfPix) * getSpec("Selected line rate") / 100000.0) / 10.0).append(" MByte/s\n");
    printOut.append(getString("numofpix")).append(numOfPix).append("\n");
    
    if(getSpec("Color") == 1)
    {
      printOut.append(getString("numofcons")).append((int) Math.ceil(numOfPix / (lval * 2.0))).append("\n");
    }
    else
    {
      printOut.append(getString("numofcons")).append((int) Math.ceil(numOfPix / (lval * 1.0))).append("\n");
    }
    printOut.append(getString("numofport")).append(portCount).append("\n");
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
            printOut.append("   Port A:   ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", numOfPix - 1)).append("   ")
                    .append(getString("Red")).append("\n");
            printOut.append("   Port B:   ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", numOfPix - 1)).append("   ")
                    .append(getString("Green")).append("\n");
            printOut.append("   Port C:   ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", numOfPix - 1)).append("   ")
                    .append(getString("Blue")).append("\n");
          }
          else
          {
            printOut.append("Camera Link ").append(x + 1).append(":\n");
            printOut.append("   Port A:   ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("   ")
                    .append(getString("Red")).append("\n");
            printOut.append("   Port B:   ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("   ")
                    .append(getString("Green")).append("\n");
            printOut.append("   Port C:   ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1)).append("   ")
                    .append(getString("Blue")).append("\n");
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
            printOut.append("   Port ").append(getPortName((x + 1) % 2)).append(":   ")
                    .append(String.format("%05d", (x - 1) * lval)).append("   - ").append(String.format("%05d", numOfPix - 1)).append("\n");
          }
          else
          {
            printOut.append("   Port ").append(getPortName((x + 1) % 2)).append(":   ")
                    .append(String.format("%05d", (x - 1) * lval)).append("   - ").append(String.format("%05d", x * lval - 1)).append("\n");
          }
        }
      }
    }

    return printOut.toString();
  }

  @Override
  public int[] getLightSources()
  {
    switch(getSpec("Internal Light Source"))
    {
      case 0: return new int[]{0, 0};
      case 1: return new int[]{1, 0};
      case 2: return new int[]{0, 1};
      case 3: return new int[]{2, 0};
      case 4: return new int[]{1, 1};
      default: return new int[0];
    }
  }

  public Optional<SensorBoardRecord> getSensorBoard(int res)
  {
    while(!getSensorBoard("SENS_" + res).isPresent())
    {
      res *= 2;
    }

    return getSensorBoard("SENS_" + res);
  }

  public Optional<SensorChipRecord> getSensorChip(int res)
  {
    int z = 1;
    while(resToSens.get(res * z) == null)
    {
      z++;
    }

    setSpec("Binning", z);

    return getSensorChip(resToSens.get(res * z));
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.021 : 0.366;
  }
}
