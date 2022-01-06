package de.tichawa.cis.config.vscis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;

public class VSCIS extends CIS
{

  public VSCIS()
  {
    super();
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VSCIS";
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
      key += getLightColor().getShortHand();
    }

    if(!getLightSources().endsWith("0C"))
    {
      key += "C";
    }

    key += getMechaVersion();

    if(isGigeInterface())
    {
      key += "GT";
    }

    key += getCooling().getCode();

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

    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VS").orElseThrow(() -> new CISException("Unknown sensor chip"));
    numOfPixNominal = numOfPix - ((getScanWidth() / BASE_LENGTH) * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));
    taps = (int) Math.ceil((numOfPix * getSelectedLineRate() / 1000000.0) / 85.0);
    chipsPerTap = (int) Math.ceil((sensorBoard.getChips() * (getScanWidth() / BASE_LENGTH)) / (double) taps);
    ppsbin = sensorChip.getPixelPerSensor() / ((double) getSelectedResolution().getBoardResolution() / (double) getSelectedResolution().getActualResolution());
    pixPerTap = (int) (chipsPerTap * ppsbin);
    portDataRate = pixPerTap * getSelectedLineRate() / 1000000.0;

    while(portDataRate > 85.0)
    {
      taps++;
      chipsPerTap = (int) Math.ceil((sensorBoard.getChips() * (getScanWidth() / BASE_LENGTH)) / (double) taps);
      ppsbin = sensorChip.getPixelPerSensor() / ((double) getSelectedResolution().getBoardResolution() / (double) getSelectedResolution().getActualResolution());
      pixPerTap = (int) (chipsPerTap * ppsbin);
      portDataRate = pixPerTap * getSelectedLineRate() / 1000000.0;
    }
    binning = 1 / (sensorChip.getBinning() * ((double) getSelectedResolution().getBoardResolution() / (double) getSelectedResolution().getActualResolution()));
    lval = (int) (chipsPerTap * (ppsbin - (sensorBoard.getOverlap() * binning) / sensorBoard.getChips()));
    lval -= lval % 8;

    printOut.append(getString("datarate")).append(Math.round(getPhaseCount() * numOfPix * getSelectedLineRate() / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append((taps * getPhaseCount() > 3) ? "2" : "1").append("\n");
    printOut.append(getString("numofport")).append(taps * getPhaseCount()).append("\n");
    printOut.append("Pixel Clock: 85 MHz").append("\n");
    printOut.append("Nominal pixel count: ").append(numOfPixNominal).append("\n");

    switch(getPhaseCount())
    {
      case 3:
      {
        if(taps > 3)
        {
          System.out.println("Please select a lower line rate. Currently required number of taps (" + taps * getPhaseCount() + ") is too high.");
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
          System.out.println("Please select a lower line rate. Currently required number of taps (" + taps * getPhaseCount() + ") is too high.");
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
  public double getMaxLineRate()
  {
    AdcBoardRecord adcBoard = getADC("VARICISC").orElseThrow(() -> new CISException("Unknown ADC board"));
    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VS").orElseThrow(() -> new CISException("Unknown sensor chip"));
    return Math.round(1000 * 1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
  }

  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.105 : 0.128;
  }
}
