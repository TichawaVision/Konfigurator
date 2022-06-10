package de.tichawa.cis.config.vtcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.cis.config.vscis.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class VTCIS extends VSCIS
{

  @Getter @Setter
  private String cLMode;

  public VTCIS()
  {
    super();
  }

  @Override
  public Optional<CameraLink> getCLCalc(int numOfPix)
  {
    int numOfPixNominal;
    int taps;
    int pixPerTap;
    int lval;

    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
    numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));
    long mhzLineRate = (long) numOfPixNominal * getSelectedLineRate() / 1000000;
    taps = (int) Math.ceil(1.01 * mhzLineRate / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    boolean mediumMode = !getCLMode().equals("Full80");
    long datarate = (long) getPhaseCount() * numOfPixNominal * getSelectedLineRate();
    LinkedList<CameraLink.Connection> connections = new LinkedList<>();
    int portLimit = mediumMode ? 8 : 10;

    int blockSize;
    if(getPhaseCount() == 1)
    {
      blockSize = 1;
    }
    else
    {
      blockSize = 3 * (getPhaseCount() / 3);
      if(blockSize < getPhaseCount())
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
          if (k < getPhaseCount())
          {
            connections.getLast().addPorts(new CameraLink.Port(i * lval, (i + 1) * lval - 1));
          } else {
            connections.getLast().addPorts(new CameraLink.Port(0, 0));
          }
        }
        i++;
      }
    }
    boolean flashExtension = getSelectedResolution().getActualResolution() >= 1200 && (numOfPix - 16 * getScanWidth() / BASE_LENGTH * 6 * 2) * getPhaseCount() * 2 > 327680;
    String notes = "LVAL(Modulo 8): " + lval + "\n" +
            getString("clMode") + (mediumMode ? "Base/Medium/Full" : "Full80") + "\n" +
            getString("numPhases") + getPhaseCount() + "\n" +
            "Flash Extension: " + (flashExtension ? "Required" : "Not required.") + "\n";

    CameraLink cameraLink = new CameraLink(datarate, numOfPixNominal, 85000000, notes);
    connections.forEach(cameraLink::addConnection);

    if(taps > (portLimit/blockSize) * 2)
    {
      throw new CISException("Number of required taps (" + taps * getPhaseCount() + ") is too high. Please reduce the data rate.");
    }
    return Optional.of(cameraLink);
  }

  @Override
  public double getMaxLineRate()
  {
    AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
    return 1000 * Math.round(1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getPixelPerSensor() + 100) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;

  }

  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.229 : 0.252;
  }
}
