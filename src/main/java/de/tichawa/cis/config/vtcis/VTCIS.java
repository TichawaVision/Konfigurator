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
  public String getCLCalc(int numOfPix)
  {
    int numOfPixNominal;
    int taps;
    int pixPerTap;
    int lval;
    int tapCount;
    StringBuilder printOut = new StringBuilder();

    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
    numOfPixNominal = numOfPix - (getBoardCount() * sensorBoard.getOverlap() / (1200 / getSelectedResolution().getActualResolution()));
    long mhzLineRate = (long) numOfPixNominal * getSelectedLineRate() / 1000000;
    taps = (int) Math.ceil(1.01 * mhzLineRate / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    boolean mediumMode = !getCLMode().equals("Full80");

    printOut.append(getString("datarate")).append(Math.round(getPhaseCount() * numOfPixNominal * getSelectedLineRate() / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append("%%%%%\n");
    printOut.append(getString("numofport")).append(taps * getPhaseCount()).append("\n");
    printOut.append("Pixel Clock: 85 MHz\n");
    printOut.append(getString("nomPix")).append(numOfPixNominal).append("\n");
    printOut.append("LVAL (Modulo 8): ").append(lval).append("\n");
    printOut.append(getString("clMode")).append(mediumMode ? "Base/Medium/Full" : "Full80").append("\n");
    printOut.append(getString("numPhases")).append(getPhaseCount()).append("\n");

    Map<Integer, List<Integer>> mediumMap = new HashMap<>();
    mediumMap.put(1, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 9, 10, 11, 12, 13, 14, 15, 16, 0, 0));
    mediumMap.put(2, Arrays.asList(1, 1, 0, 2, 2, 0, 0, 0, 0, 0, 3, 3, 0, 4, 4, 0, 0, 0, 0, 0));
    mediumMap.put(3, Arrays.asList(1, 1, 1, 2, 2, 2, 0, 0, 0, 0, 3, 3, 3, 4, 4, 4, 0, 0, 0, 0));
    mediumMap.put(4, Arrays.asList(1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0));
    mediumMap.put(5, Arrays.asList(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0));
    mediumMap.put(6, Arrays.asList(1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0));

    Map<Integer, List<Integer>> highMap = new HashMap<>();
    highMap.put(1, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
    highMap.put(2, Arrays.asList(1, 1, 0, 2, 2, 0, 3, 3, 0, 0, 4, 4, 0, 5, 5, 0, 6, 6, 0, 0));
    highMap.put(3, Arrays.asList(1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 4, 4, 4, 5, 5, 5, 6, 6, 6, 0));
    highMap.put(4, mediumMap.get(4));
    highMap.put(5, mediumMap.get(5));
    highMap.put(6, mediumMap.get(6));

    List<Integer> tapConfig = (mediumMode ? mediumMap : highMap).get(getPhaseCount());
    if(taps > tapConfig.stream().mapToInt(x -> x).max().orElse(0))
    {
      throw new CISException("Number of required taps (" + taps * getPhaseCount() + ") is too high. Please reduce the data rate.");
    }
    //Out Of Flash Memory
    printOut.append("Flash Extension: ");
    if(getSelectedResolution().getActualResolution() >= 1200 && (numOfPix - 16 * getScanWidth() / BASE_LENGTH * 6 * 2) * getPhaseCount() * 2 > 327680)
    {
      printOut.append("Required.\n");
    }
    else
    {
      printOut.append("Not required.\n");
    }

    int cableCount = 0;
    for(tapCount = 0; tapCount < tapConfig.size(); tapCount++)
    {
      int currentTap = tapConfig.get(tapCount);
      if(currentTap > 0 && taps >= currentTap)
      {
        if(tapCount % 10 == 0)
        {
          printOut.append("Camera Link ").append((tapCount / 10) + 1).append(":\n");
        }
        printOut.append("   Port ").append(getPortName(tapCount % 10)).append(":   ")
            .append(String.format("%05d", (currentTap - 1) * lval)).append("   - ")
            .append(String.format("%05d", currentTap * lval - 1)).append("\n");

        if(tapCount % 10 == 0 || tapCount % 10 == 3)
        {
          cableCount++;
        }
      }
    }

    printOut.append(getString("configOnRequest"));
    printOut.replace(printOut.indexOf("%%%%%"), printOut.indexOf("%%%%%") + 5, cableCount+ "");
    return printOut.toString();
  }

  @Override
  public double getMaxLineRate()
  {
    AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
    return Math.round(1000 * 1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getPixelPerSensor() + 100) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;

  }

  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.229 : 0.252;
  }
}
