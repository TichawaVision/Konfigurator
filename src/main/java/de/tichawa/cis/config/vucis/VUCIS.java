package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.CIS;
import de.tichawa.cis.config.CISException;
import de.tichawa.cis.config.model.tables.records.AdcBoardRecord;
import de.tichawa.cis.config.model.tables.records.SensorBoardRecord;
import de.tichawa.cis.config.model.tables.records.SensorChipRecord;

import java.util.*;
import java.util.stream.Collectors;

public class VUCIS extends CIS
{

  private LightPreset lightPreset;
  private LightColor leftBrightField;
  private LightColor coaxLight;
  private LightColor rightBrightField;
  private LightColor leftDarkField;
  private LightColor rightDarkField;
  private LensType lensType;

  public enum LightPreset
  {
    MANUAL("Manual"),
    SHAPE_FROM_SHADING("Shape from Shading"),
    CLOUDY_DAY("Cloudy Day");

    private final String description;

    LightPreset(String description)
    {
      this.description = description;
    }

    public String getDescription()
    {
      return this.description;
    }

    public static Optional<LightPreset> findByDescription(String description)
    {
      return Arrays.stream(LightPreset.values())
              .filter(c -> c.getDescription().equals(description))
              .findFirst();
    }
  }

  public enum LensType
  {
    TC48("TC48","10"),
    TC48L("TC48 with long DOF","1L"),
    TC54("TC54","20"),
    TC54L("TC54 with long DOF","2L"),
    TC80("TC80","30"),
    TC80L("TC80 with long DOF","3L"),
    TC99("TC99","40"),
    TC147("TC147","50"),
    OBJ("OL_OBJ_M12x0.5R_25mm_GLAS_0_ß=1:4","64");

    private final String description;
    private final String code;

    public String getDescription() {
      return description;
    }

    public String getCode() {
      return code;
    }

    LensType(String description, String code) {
      this.description = description;
      this.code = code;
    }

    public static Optional<LensType> findByDescription(String description)
    {
      return Arrays.stream(LensType.values())
              .filter(c -> c.getDescription().equals(description))
              .findFirst();
    }

    @SuppressWarnings("unused")
    public static Optional<LensType> findByCode(String code)
    {
      return Arrays.stream(LensType.values())
              .filter(c -> c.getCode().equals(code))
              .findFirst();
    }
  }

  public VUCIS()
  {
    super();

    this.lightPreset = LightPreset.MANUAL;
    this.leftBrightField = LightColor.RED;
    this.coaxLight = LightColor.NONE;
    this.rightBrightField = LightColor.RED;
    this.leftDarkField = LightColor.NONE;
    this.rightDarkField = LightColor.NONE;
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VUCIS";
    key += String.format("_%04d", getScanWidth());
    key += "_S";

    key += "_";
    key += getLightSources();

    key += "_" + getLensType().getCode();

    key += "_C";
    key += getMechaVersion();

    if(key.endsWith("_"))
    {
      key = key.substring(0, key.length() - 1);
    }

    return key;
  }

  @Override
  public Set<LightColor> getLightColors()
  {
    return getLightSources().chars()
            .mapToObj(c -> LightColor.findByCode((char) c))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
  }

  @Override
  public String getLightSources()
  {
    switch(getLightPreset())
    {
      case CLOUDY_DAY: return "DRXRD";
      case SHAPE_FROM_SHADING: return "RRSRR";
      default:
      {
        String key = "";
        key += getLeftDarkField().getCode();
        key += getLeftBrightField().getCode();
        key += getCoaxLight().getCode();
        key += getRightBrightField().getCode();
        key += getRightDarkField().getCode();
        return key;
      }
    }
  }

  @Override
  public int getLedLines()
  {
    return (int) getLightSources().chars()
            .mapToObj(c -> LightColor.findByCode((char) c))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(l -> l != LightColor.NONE)
            .count();
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

    printOut.append(getString("datarate")).append(Math.round(getPhaseCount() * numOfPixNominal * getSelectedLineRate() / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append("%%%%%\n");
    printOut.append(getString("numofport")).append(taps * getPhaseCount()).append("\n");
    printOut.append("Pixel Clock: 85 MHz\n");
    printOut.append(getString("nomPix")).append(numOfPixNominal).append("\n");
    printOut.append("LVAL (Modulo 8): ").append(lval).append("\n");
    printOut.append(getString("numPhases")).append(getPhaseCount()).append("\n");

    Map<Integer, List<Integer>> highMap = new HashMap<>();
    highMap.put(1, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20));
    highMap.put(2, Arrays.asList(1, 1, 0, 2, 2, 0, 3, 3, 0, 0, 4, 4, 0, 5, 5, 0, 6, 6, 0, 0));
    highMap.put(3, Arrays.asList(1, 1, 1, 2, 2, 2, 3, 3, 3, 0, 4, 4, 4, 5, 5, 5, 6, 6, 6, 0));
    highMap.put(4, Arrays.asList(1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 0, 0));
    highMap.put(5, Arrays.asList(1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 0, 0, 0, 0, 0));
    highMap.put(6, Arrays.asList(1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 0, 0, 0, 0));

    List<Integer> tapConfig = highMap.get(getPhaseCount());
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
  public double getGeometry(boolean coax)
  {
    return coax ? 0.229 : 0.252;
  }

  public LightPreset getLightPreset()
  {
    return this.lightPreset;
  }

  public void setLightPreset(LightPreset lightPreset)
  {
    this.lightPreset = lightPreset;
  }

  public LightColor getLeftBrightField() {
    return leftBrightField;
  }

  public void setLeftBrightField(LightColor leftBrightField) {
    this.leftBrightField = leftBrightField;
  }

  public LightColor getCoaxLight() {
    return coaxLight;
  }

  public void setCoaxLight(LightColor coaxLight) {
    this.coaxLight = coaxLight;
  }

  public LightColor getRightBrightField() {
    return rightBrightField;
  }

  public void setRightBrightField(LightColor rightBrightField) {
    this.rightBrightField = rightBrightField;
  }

  public LightColor getLeftDarkField() {
    return leftDarkField;
  }

  public void setLeftDarkField(LightColor leftDarkField) {
    this.leftDarkField = leftDarkField;
  }

  public LightColor getRightDarkField() {
    return rightDarkField;
  }

  public void setRightDarkField(LightColor rightDarkField) {
    this.rightDarkField = rightDarkField;
  }

  public LensType getLensType() {
    return lensType;
  }

  public void setLensType(LensType lensType) {
    this.lensType = lensType;
  }

  @Override
  public double getMaxLineRate()
  {
    AdcBoardRecord adcBoard = getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
    SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
    SensorChipRecord sensorChip = getSensorChip("SMARAGD" + getSelectedResolution().getBoardResolution() + "_VD").orElseThrow(() -> new CISException("Unknown sensor chip"));
    return Math.round(1000 * 1000 * sensorBoard.getLines() / (getColorCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
  }

  public int getColorCount()
  {
    List<LightColor> colors = Arrays.asList(getLeftBrightField(), getCoaxLight(), getRightBrightField(), getLeftDarkField(), getRightDarkField());
    return Math.max((int) colors.stream()
            .distinct()
            .filter(c -> c != LightColor.NONE)
            .count(), 1);
  }
}
