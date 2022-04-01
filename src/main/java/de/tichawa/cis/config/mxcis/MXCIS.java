package de.tichawa.cis.config.mxcis;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.records.*;

import java.util.*;

// Alle MXCIS spezifische Funktionen 
public class MXCIS extends CIS
{
  private static final HashMap<Integer, String> resToSens = new HashMap<>();
  static
  {
    resToSens.put(200, "PI3033");
    resToSens.put(300, "PI3041");
    resToSens.put(400, "PI3042");
    resToSens.put(600, "PI3039");
    resToSens.put(1200, "PI5002_1200");
    resToSens.put(2400, "PI5002_2400");
  }

  private static final HashMap<Integer, Integer> dpiToRate = new HashMap<>();
  static
  {
    dpiToRate.put(25, 27);
    dpiToRate.put(50, 27);
    dpiToRate.put(75, 18);
    dpiToRate.put(100, 27);
    dpiToRate.put(150, 18);
    dpiToRate.put(200, 27);
    dpiToRate.put(300, 18);
    dpiToRate.put(400, 13);
    dpiToRate.put(600, 10);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_MXCIS";
    key += String.format("_%04d", getScanWidth());
    key += String.format("_%04d", getSelectedResolution().getActualResolution());

    if(getPhaseCount() == 4)
    {
      key += "_K4";
    }
    else if((getSelectedResolution().getActualResolution() != 600 && getSelectedLineRate() <= getMaxLineRate() * 0.25) || (getSelectedResolution().getActualResolution() == 600 && getSelectedLineRate() <= getMaxLineRate() * 0.2))
    {
      key += "_K1";
    }
    else if(getSelectedLineRate() <= getMaxLineRate() * 0.5)
    {
      key += "_K2";
    }
    else
    {
      key += "_K3";
    }

    key += "_";
    if(getLightSources().equals("0D0C"))
    {
      key += "NO";
    }

    if(getPhaseCount() == 4)
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
  public double getMaxLineRate()
  {
    SensorChipRecord sensorChip = getSensorChip(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
    SensorBoardRecord sensorBoard = getSensorBoard(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown ADC board"));
    AdcBoardRecord adcBoard = getADC("MODU_ADC(SLOW)").orElseThrow(() -> new CISException("Unknown ADC board."));
    return Math.round(1000 * 1000 * sensorBoard.getLines() / (getPhaseCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
  }

  @Override
  public Optional<CameraLink> getCLCalc(int numOfPix)
  {
    double fpgaDataRate;
    int tapsPerFpga;
    int lval;
    int pixPerFpga;
    int pixPerTap;
    int sensPerFpga;

    if(getSelectedResolution().getActualResolution() > 600)
    {
      setMode(2);
      sensPerFpga = 1;
    }
    else if((getSelectedLineRate() / 1000.0) <= dpiToRate.get(getSelectedResolution().getActualResolution()) && getPhaseCount() != 4)
    {
      //HALF
      setMode(4);
      sensPerFpga = 4;
    }
    else
    {
      //FULL
      setMode(2);
      sensPerFpga = 2;
    }

    SensorChipRecord sensorChip = getSensorChip(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
    SensorBoardRecord sensorBoard = getSensorBoard(getSelectedResolution().getActualResolution()).orElseThrow(() -> new CISException("Unknown ADC board"));
    pixPerFpga = sensPerFpga * sensorBoard.getChips() * sensorChip.getPixelPerSensor() / getBinning();
    fpgaDataRate = pixPerFpga * getSelectedLineRate() / 1000.0;
    tapsPerFpga = (int) Math.ceil(fpgaDataRate / (84 * 1000.0));
    if(getPhaseCount() == 1 && getNumFPGA() > 1 && tapsPerFpga % 2 == 1)
    {
      tapsPerFpga++;
    }

    pixPerTap = (int) ((double) pixPerFpga / (double) tapsPerFpga);
    lval = pixPerTap;

    int conCount = (int) Math.ceil(numOfPix / (lval * (getPhaseCount() == 1 ? 2.0 : 1.0)));
    int portCount =  (int) Math.ceil(numOfPix / (lval * (getPhaseCount() == 1 ? 1.0 : 3.0)));

    long dataRate = (long) portCount * Math.min(lval, numOfPix) * getSelectedLineRate();
    CameraLink cameraLink = new CameraLink(dataRate, numOfPix,85000000);

    if(getPhaseCount() == 4)
    {
      for(int x = 0; x < tapsPerFpga * getNumFPGA(); x++)
      {
        int endPixel = lval > numOfPix ? numOfPix - 1 : (x + 1) * lval - 1;
        CameraLink.Connection conn = new CameraLink.Connection();
        conn.addPorts(new CameraLink.Port(x * lval, endPixel, "Red"),
                new CameraLink.Port(x * lval, endPixel, "Green"),
                new CameraLink.Port(x * lval, endPixel, "Blue"));

        cameraLink.addConnection(conn);
      }
    }
    else if(getPhaseCount() == 1)
    {
      LinkedList<CameraLink.Connection> connections = new LinkedList<>();
      for(int x = 0; x  * lval < numOfPix; x++)
      {
        if(x % 2 == 0)
        {
          connections.add(new CameraLink.Connection());
        }

        int endPixel = lval > numOfPix ? numOfPix - 1 : (x + 1) * lval - 1;
        connections.getLast().addPorts(new CameraLink.Port(x * lval, endPixel));
      }

      connections.forEach(cameraLink::addConnection);
    }

    return Optional.of(cameraLink);
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

    setBinning(z);

    return getSensorChip(resToSens.get(res * z));
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 0.021 : 0.366;
  }
}
