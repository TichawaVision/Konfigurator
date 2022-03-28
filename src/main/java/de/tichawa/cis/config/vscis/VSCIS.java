package de.tichawa.cis.config.vscis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;

import java.util.*;

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
      key += getLightColors().stream()
              .findAny().orElse(LightColor.NONE)
              .getShortHand();
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
  public Optional<CameraLink> getCLCalc(int numOfPix)
  {
    int numOfPixNominal;
    int taps;
    int chipsPerTap;
    long portDataRate;
    int lval;
    int pixPerTap;
    double ppsbin;
    double binning;
    int pixelClock = 85000000;

    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VS").orElseThrow(() -> new CISException("Unknown sensor chip"));
    numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));

    taps = numOfPix * getSelectedLineRate() / pixelClock;
    taps--;

    do
    {
      taps++;
      chipsPerTap = (int) Math.ceil((sensorBoard.getChips() * getBoardCount()) / (double) taps);
      ppsbin = sensorChip.getPixelPerSensor() / ((double) getSelectedResolution().getBoardResolution() / (double) getSelectedResolution().getActualResolution());
      pixPerTap = (int) (chipsPerTap * ppsbin);
      portDataRate = (long) pixPerTap * getSelectedLineRate();
    }
    while(portDataRate > pixelClock);

    binning = 1 / (sensorChip.getBinning() * ((double) getSelectedResolution().getBoardResolution() / (double) getSelectedResolution().getActualResolution()));
    lval = (int) (chipsPerTap * (ppsbin - (sensorBoard.getOverlap() * binning) / sensorBoard.getChips()));
    lval -= lval % 8;

    CameraLink cl = new CameraLink(portDataRate, numOfPixNominal, pixelClock);
    LinkedList<CameraLink.Connection> connections = new LinkedList<>();

    if(getPhaseCount() == 3)
    {
      if(taps > 3)
      {
        System.out.println("Please select a lower line rate. Currently required number of taps (" + taps * getPhaseCount() + ") is too high.");
        return Optional.empty();
      }

      for(int x = 0; x < Math.min(2, taps); x++)
      {
        connections.add(new CameraLink.Connection());
        connections.getLast().addPorts(new CameraLink.Port(x * lval,(x + 1) * lval - 1,"Red"),
            new CameraLink.Port(x * lval,(x + 1) * lval - 1,"Green"),
            new CameraLink.Port(x * lval,(x + 1) * lval - 1,"Blue"));
      }

      if(taps == 3)
      {
        connections.getLast().addPorts(new CameraLink.Port(2 * lval, (2 + 1) * lval - 1, "Red"),
            new CameraLink.Port(2 * lval, (2 + 1) * lval - 1, "Green"),
            new CameraLink.Port(2 * lval, (2 + 1) * lval - 1, "Blue"));
      }
    }
    else if(getPhaseCount() == 1)
    {
      if(taps > 8)
      {
        System.out.println("Please select a lower line rate. Currently required number of taps (" + taps * getPhaseCount() + ") is too high.");
        return Optional.empty();
      }

      connections.add(new CameraLink.Connection());
      for(int x = 0; x < Math.min(3, taps); x++)
      {
        connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
      }

      if(taps > 3)
      {
        connections.add(new CameraLink.Connection());
        for(int x = 3; x < taps; x++)
        {
          connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
        }
      }
    }

    connections.forEach(cl::addConnection);
    return Optional.of(cl);
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
