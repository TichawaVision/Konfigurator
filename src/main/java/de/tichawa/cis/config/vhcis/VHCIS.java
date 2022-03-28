package de.tichawa.cis.config.vhcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;

import java.util.*;

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
  public Optional<CameraLink> getCLCalc(int numOfPix)
  {
    int numOfPixNominal;
    int taps;
    int pixPerTap;
    int lval;

    SensorBoardRecord sensorBoard = getSensorBoard("SMARDOUB").orElseThrow(() -> new CISException("Unknown sensor board"));
    numOfPixNominal = numOfPix - ((getScanWidth() / BASE_LENGTH) * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));
    taps = (int) Math.ceil(1.01 * (numOfPixNominal * getMaxLineRate() / 1000000) / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    CameraLink cl = new CameraLink((long) getPhaseCount() * numOfPix * getSelectedLineRate(), numOfPixNominal,85000000);
    LinkedList<CameraLink.Connection> connections = new LinkedList<>();
    int cycleLength = 20;
    int x = 0;

    for(int cycle = 0; cycle * cycleLength < taps; cycle++)
    {
      int cycleOffset = cycle * cycleLength;

      connections.add(new CameraLink.Connection());
      for(; x < Math.min(3 + cycleOffset, taps); x++)
      {
        connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
      }

      if(taps > 3 + cycleOffset)
      {
        connections.add(new CameraLink.Connection());
        int tapLimit = taps > 10 + cycleOffset && taps <= 16 + cycleOffset ? 8 : 10;
        for(; x < Math.min(taps, tapLimit + cycleOffset); x++)
        {
          connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
        }
      }

      if(taps > 10 + cycleOffset)
      {
        connections.add(new CameraLink.Connection());
        if(taps <= 16 + cycleOffset)
        {
          for(; x < 11 + cycleOffset; x++)
          {
            connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
          }

          if(taps > 11 + cycleOffset)
          {
            connections.add(new CameraLink.Connection());
            for(; x < taps; x++)
            {
              connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
            }
          }
        }
        else
        {
          for(; x < 13 + cycleOffset; x++)
          {
            connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
          }

          connections.add(new CameraLink.Connection());
          for(; x < Math.min(20 + cycleOffset, taps); x++)
          {
            connections.getLast().addPorts(new CameraLink.Port(x * lval, (x + 1) * lval - 1));
          }
        }
      }
    }

    connections.forEach(cl::addConnection);
    return Optional.of(cl);
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return 1;
  }
}
