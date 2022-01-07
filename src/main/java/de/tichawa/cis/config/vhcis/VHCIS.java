package de.tichawa.cis.config.vhcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;

public class VHCIS extends CIS
{

  public VHCIS()
  {
    super();
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VHCIS";
    key += String.format("_%04d", getScanWidth());

    if(getSelectedResolution().isSwitchable())
    {
      key += "_XXXX";
    }
    else
    {
      key += String.format("_%04d", getSelectedResolution().getActualResolution());
    }

    key += "_";
    if(getLightSources().equals("0D0C"))
    {
      key += "NO";
    }

    if(getPhaseCount() == 3)
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
    key += getCooling().getCode();

    if(key.endsWith("_"))
    {
      key = key.substring(0, key.length() - 1);
    }

    return key;
  }

  @Override
  public double getMaxLineRate() throws CISException
  {
    AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
    SensorBoardRecord sensorBoard = getSensorBoard("SMARDOUB").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
    return Math.round(1000 * 1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
  }

  @Override
  public String getCLCalc(int numOfPix)
  {
    int numOfPixNominal;
    int taps;
    int pixPerTap;
    int lval;
    int tcounter = 0;
    StringBuilder printOut = new StringBuilder();

    SensorBoardRecord sensorBoard = getSensorBoard("SMARDOUB").orElseThrow(() -> new CISException("Unknown sensor board"));
    numOfPixNominal = numOfPix - ((getScanWidth() / BASE_LENGTH) * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));
    taps = (int) Math.ceil(1.01 * (numOfPixNominal * getMaxLineRate() / 1000000) / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    printOut.append(getString("datarate")).append(Math.round(getPhaseCount() * numOfPix * getSelectedLineRate() / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append("%%%%%\n");
    printOut.append(getString("numofport")).append(taps * getPhaseCount()).append("\n");
    printOut.append("Pixel Clock: 85 MHz\n");
    printOut.append("Nominal pixel count: ").append(numOfPixNominal).append("\n");
    printOut.append("\n\nCamera Link 1:");

    switch(taps)
    {
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      {
        tcounter++;
        for(int x = 0; x < Math.min(3, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 3)
        {
          tcounter++;
          printOut.append("\nCamera Link 2:");
          for(int x = 3; x < taps; x++)
          {
            printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }
        }
        break;
      }
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
      {

        for(int x = 0; x < 3; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 2:");
        for(int x = 3; x < 8; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 3:");
        for(int x = 8; x < 11; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x))
                  .append(": ").append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 11)
        {
          tcounter++;
          printOut.append("\nCamera Link 4:");
          for(int x = 11; x < taps; x++)
          {
            printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }
        }
        break;
      }
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      case 33:
      case 34:
      case 35:
      case 36:
      case 37:
      case 38:
      case 39:
      case 40:
      {

        for(int x = 0; x < 3; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 2:");
        for(int x = 3; x < 10; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 3:");
        for(int x = 10; x < 13; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 10)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 4:");
        for(int x = 13; x < Math.min(20, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 10)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }
        break;
      }
    }

    if(taps > 20)
    {
      printOut.append("\nCamera Link 5:");
    }

    switch(taps)
    {
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      {
        tcounter++;
        for(int x = 20; x < Math.min(23, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 23)
        {
          tcounter++;
          printOut.append("\nCamera Link 6:");
          for(int x = 23; x < taps; x++)
          {
            printOut.append("\n   Port ").append(getPortName(x - 20))
                    .append(": ").append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }
        }
        break;
      }
      case 31:
      case 32:
      case 33:
      case 34:
      case 35:
      case 36:
      {

        for(int x = 20; x < 23; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 6:");
        for(int x = 23; x < 28; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 3:");
        for(int x = 28; x < 31; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 28)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 31)
        {
          tcounter++;
          printOut.append("\nCamera Link 4:");
          for(int x = 31; x < taps; x++)
          {
            printOut.append("\n   Port ").append(getPortName(x - 28)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }
        }
        break;
      }
      case 37:
      case 38:
      case 39:
      case 40:
      {
        for(int x = 20; x < 23; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 2:");

        tcounter++;
        printOut.append("\nCamera Link 3:");
        for(int x = 30; x < 33; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 30)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        tcounter++;
        printOut.append("\nCamera Link 4:");
        for(int x = 33; x < taps; x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 30)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }
        break;
      }
    }

    printOut.replace(printOut.indexOf("%%%%%"), printOut.indexOf("%%%%%") + 5, tcounter + "");
    return printOut.toString();
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return 1;
  }
}
