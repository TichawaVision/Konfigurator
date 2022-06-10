package de.tichawa.cis.config.vdcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class VDCIS extends CIS
{
  @Getter @Setter
  private String cLMode;

  public VDCIS()
  {
    super();
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VDCIS";
    key += String.format("_%04d", getScanWidth());

    if(getSelectedResolution().isSwitchable()) //Switchable
    {
      key += "_XXXX";
    }
    else
    {
      key += String.format("_%04d", getSelectedResolution().getActualResolution());
    }

    key += "_2";

    if(getPhaseCount() == 4)
    {
      key += "RGB";
    }
    else
    {
      key +=  getLightColors().stream()
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

//    key += getCooling().getCode();

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
    int pixPerTap;
    int lval;

    int softwareBinning = (1200 / getSelectedResolution().getActualResolution());
    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
    numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() / softwareBinning);
    long mhzLineRate = (long) numOfPixNominal * getSelectedLineRate() / 1000000;
    taps = (int) Math.ceil(1.01 * mhzLineRate / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    boolean mediumMode = !getCLMode().equals("Full80");
    long datarate = (long) (getPhaseCount() - 1) * numOfPixNominal * getSelectedLineRate();
    LinkedList<CameraLink.Connection> connections = new LinkedList<>();
    int portLimit = mediumMode ? 8 : 10;

    int blockSize;
    if(getPhaseCount() - 1 == 1)
    {
      blockSize = 1;
    }
    else
    {
      blockSize = 3 * (getPhaseCount() / 3);
      if(blockSize < getPhaseCount() - 1)
      {
        blockSize += 3;
      }
    }

    for(int i = 0; i < taps;)
    {
      connections.add(new CameraLink.Connection(0, (char) (CameraLink.Port.DEFAULT_NAME + connections.stream()
              .mapToInt(CameraLink.Connection::getPortCount)
              .sum())));

      while (connections.getLast().getPortCount() <= portLimit - blockSize && i < taps)
      {
        for (int k = 0; k < blockSize; k++)
        {
          if (k < getPhaseCount() - 1)
          {
            connections.getLast().addPorts(new CameraLink.Port(i * lval, (i + 1) * lval - 1));
          } else {
            connections.getLast().addPorts(new CameraLink.Port(0, 0));
          }
        }
        i++;
      }
    }

    String notes = "LVAL(Modulo 8): " + lval + "\n" +
            getString("clMode") + (mediumMode ? "Base/Medium/Full" : "Full80") + "\n" +
            getString("numPhases") + getPhaseCount() + "\n" ;

    CameraLink cameraLink = new CameraLink(datarate, numOfPixNominal, 85000000, notes);
    connections.forEach(cameraLink::addConnection);

    if(taps > (portLimit/blockSize) * 2)
    {
      throw new CISException("Number of required taps (" + taps * getPhaseCount() + ") is too high. Please reduce the data rate.");
    }
    if(getSelectedResolution().getActualResolution() >= 1200 && (numOfPix - 16 * getScanWidth() / BASE_LENGTH * 6 * 2) * getPhaseCount() * 2 > 327680)
    {
      throw new CISException("Out of Flash memory. Please reduce the scan width or resolution.");
    }

    return Optional.of(cameraLink);
  }

  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.229 : 0.252;
  }

  @Override
  public double getMaxLineRate()
  {
    AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VD").orElseThrow(() -> new CISException("Unknown sensor chip"));
    return 1000 * Math.round(1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;

  }
}
