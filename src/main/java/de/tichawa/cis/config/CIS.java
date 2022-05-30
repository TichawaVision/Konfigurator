package de.tichawa.cis.config;

import de.tichawa.cis.config.ldstd.LDSTD;
import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.cis.config.mxcis.MXCIS;
import de.tichawa.cis.config.vdcis.VDCIS;
import de.tichawa.cis.config.vhcis.VHCIS;
import de.tichawa.cis.config.vscis.VSCIS;
import de.tichawa.cis.config.vtcis.VTCIS;
import de.tichawa.cis.config.vucis.VUCIS;
import de.tichawa.util.MathEval;
import lombok.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;
import java.util.stream.*;

import static de.tichawa.cis.config.model.Tables.*;

// Alle allgemeine CIS Funktionen
public abstract class CIS
{
  public static final int BASE_LENGTH = 260;
  protected static Locale locale = Locale.getDefault();
  private static final Pattern LIGHT_PATTERN = Pattern.compile("(\\d+)D(\\d+)C");

  public final String cisName;

  protected final HashMap<Integer, Integer> maxRateForHalfMode;

  private final Properties dbProperties;
  private final BasicDataSource dataSource;
  private final Map<String, AdcBoardRecord> adcBoards;
  private final Map<String, SensorChipRecord> sensChips;
  private final Map<String, SensorBoardRecord> sensBoards;
  @Getter
  private final Set<LightColor> lightColors;

  protected Map<PriceRecord, Integer> electConfig;
  protected Map<PriceRecord, Integer> mechaConfig;
  protected Double[] electSums;
  protected Double[] mechaSums;
  protected Double[] totalPrices;
  protected int numFPGA;

  @Getter @Setter
  private int mode;
  @Getter @Setter
  private int phaseCount;
  @Getter @Setter
  private int scanWidth;
  @Getter @Setter
  private int selectedLineRate;
  @Getter @Setter
  private boolean gigeInterface;
  @Getter @Setter
  private Resolution selectedResolution;
  @Getter @Setter
  private int numOfPix;
  @Getter @Setter
  private boolean externalTrigger;
  @Getter @Setter
  private Cooling cooling;
  @Getter @Setter
  private int binning;
  @Getter @Setter
  private int transportSpeed;
  @Getter @Setter
  private int diffuseLightSources;
  @Getter @Setter
  private int coaxLightSources;

  public void setLightColor(LightColor lightColor)
  {
    getLightColors().clear();
    getLightColors().add(lightColor);
  }

  public enum LightColor
  {
    NONE("None","NO", 'X'),
    RED("Red","AM",'R'),
    GREEN("Green","GR",'G'),
    BLUE("Blue","BL",'B'),
    YELLOW("Yellow","YE",'Y'),
    WHITE("White","WH",'W'),
    IR("IR","IR",'I'),
    IR950("IR 950nm","JR",'J'),
    UVA("UVA 365nm","UV",'U'),
    VERDE("Verde","VE",'V'),
    RGB("RGB","RGB",'C'),
    IRUV("LEDIRUV","HI",'H'),
    REBZ8("REBZ8LED","REBZ8",'8'),
    REBELMIX("REBELMIX","REBEL",'E');

    private final String description;
    private final String shortHand;
    private final char code;

    public String getDescription() {
      return description;
    }

    public char getCode() {
      return code;
    }

    @SuppressWarnings("unused")
    public String getShortHand()
    {
      return shortHand;
    }

    LightColor(String description, String shortHand, char code) {
      this.description = description;
      this.shortHand = shortHand;
      this.code = code;
    }

    public static Optional<LightColor> findByDescription(String description)
    {
      return Arrays.stream(LightColor.values())
              .filter(c -> c.getDescription().equals(description))
              .findFirst();
    }

    @SuppressWarnings("unused")
    public static Optional<LightColor> findByShortHand(String shortHand)
    {
      return Arrays.stream(LightColor.values())
              .filter(c -> c.getShortHand().equals(shortHand))
              .findFirst();
    }

    @SuppressWarnings("unused")
    public static Optional<LightColor> findByCode(char code)
    {
      return Arrays.stream(LightColor.values())
              .filter(c -> c.getCode() == code)
              .findFirst();
    }
  }

  public enum Cooling
  {
    NONE("NOCO", "None", "none"),
    PAIR("PAIR", "Passive Air", "passair"),
    FAIR("", "Int. Forced Air(Default)", "intforced"),
    EAIR("FAIR", "Ext. Forced Air", "extforced"),
    LICO("LICO", "Liquid Cooling", "lico");

    private final String code;
    private final String description;
    private final String shortHand;

    Cooling(String code, String description, String shortHand)
    {
      this.code = code;
      this.description = description;
      this.shortHand = shortHand;
    }

    public String getCode()
    {
      return code;
    }

    public  String getShortHand(){return shortHand;}

    public String getDescription()
    {
      return description;
    }

    public static Optional<Cooling> findByDescription(String description)
    {
      return Arrays.stream(Cooling.values())
              .filter(c -> c.getDescription().equals(description))
              .findFirst();
    }

    @SuppressWarnings("unused")
    public static Optional<Cooling> findByCode(String code)
    {
      return Arrays.stream(Cooling.values())
              .filter(c -> c.getCode().equals(code))
              .findFirst();
    }
  }

  @Value
  public static class Resolution
  {
    int boardResolution;
    double pixelSize;
    boolean switchable;
    int actualResolution;
    double depthOfField;

    public Resolution(int actualResolution, int boardResolution, boolean switchable, double depthOfField, double pixelSize)
    {
      this.pixelSize = pixelSize;
      this.boardResolution = boardResolution;
      this.switchable = switchable;
      this.depthOfField = depthOfField;
      this.actualResolution = actualResolution;
    }
  }

  public abstract double getMaxLineRate();

  protected CIS()
  {
    this.lightColors = new HashSet<>();
    maxRateForHalfMode = new HashMap<>();
    maxRateForHalfMode.put(25, 27);
    maxRateForHalfMode.put(50, 27);
    maxRateForHalfMode.put(75, 18);
    maxRateForHalfMode.put(100, 27);
    maxRateForHalfMode.put(150, 18);
    maxRateForHalfMode.put(200, 27);
    maxRateForHalfMode.put(300, 18);
    maxRateForHalfMode.put(400, 13);
    maxRateForHalfMode.put(600, 10);

    String fullName = this.getClass().getName();
    cisName = fullName.substring(fullName.lastIndexOf('.') + 1);
    dbProperties = new Properties();
    dataSource = new BasicDataSource();
    try
    {
      dbProperties.loadFromXML(new FileInputStream("U:/SW/PC/Java/Konfigurator/connection.xml"));

      switch(dbProperties.getProperty("dbType"))
      {
        case "SQLite":
          dataSource.setUrl("jdbc:sqlite:" + "U:/SW/PC/Java/Konfigurator" + "/" + dbProperties.getProperty("dbFile"));
          break;
        case "MariaDB":
          dataSource.setUrl("jdbc:mariadb://" + dbProperties.getProperty("dbHost") + ":" + dbProperties.getProperty("dbPort")
              + "/" + dbProperties.getProperty("dbName"));
          dataSource.setUsername(dbProperties.getProperty("dbUser"));
          dataSource.setPassword(dbProperties.getProperty("dbPwd"));
          break;
        default:
          break;
      }
    }
    catch(IOException ignored)
    {}

    adcBoards = getDatabase()
        .map(context -> context.selectFrom(ADC_BOARD).stream())
        .orElse(Stream.empty())
        .collect(Collectors.toMap(AdcBoardRecord::getName, Function.identity()));
    sensChips = getDatabase()
        .map(context -> context.selectFrom(SENSOR_CHIP).stream())
        .orElse(Stream.empty())
        .collect(Collectors.toMap(SensorChipRecord::getName, Function.identity()));
    sensBoards = getDatabase()
        .map(context -> context.selectFrom(SENSOR_BOARD).stream())
        .orElse(Stream.empty())
        .collect(Collectors.toMap(SensorBoardRecord::getName, Function.identity()));
  }

  public abstract Optional<CameraLink> getCLCalc(int numOfPix);

  public abstract String getTiViKey();

  public String getLightSources()
  {
    return getDiffuseLightSources() + "D" + getCoaxLightSources() + "C";
  }

  public int getLedLines()
  {
    Matcher m = LIGHT_PATTERN.matcher(getLightSources());
    if(m.matches())
    {
      return Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2));
    }
    else
    {
      throw new IllegalArgumentException(getLightSources() + " is not a valid light source pattern.");
    }
  }

  public static void setLocale(Locale l)
  {
    locale = l;
  }

  public Optional<AdcBoardRecord> getADC(String name)
  {
    return Optional.ofNullable(adcBoards.get(name));
  }

  public Optional<SensorBoardRecord> getSensorBoard(String name)
  {
    return Optional.ofNullable(sensBoards.get(name));
  }

  public Optional<SensorChipRecord> getSensorChip(String name)
  {
    return Optional.ofNullable(sensChips.get(name));
  }

  public static double round(double value, int digits)
  {
    return Math.round(Math.pow(10.0, digits) * value) / Math.pow(10.0, digits);
  }

  public Optional<DSLContext> getDatabase()
  {
    try
    {
      SQLDialect dialect;
      switch(dbProperties.getProperty("dbType"))
      {
        case "SQLite":
          dialect = SQLDialect.SQLITE;
          break;
        case "MariaDB":
          dialect = SQLDialect.MARIADB;
          break;
        default:
          dialect = SQLDialect.MYSQL;
      }
      DSLContext context = DSL.using(dataSource, dialect);

      return Optional.of(context);
    }
    catch(Exception ex)
    {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public void calculate()
  {
    Map<Integer, PriceRecord> priceRecords = new HashMap<>();
    mechaSums = new Double[]{0.0, 0.0, 0.0, 0.0, 100.0};
    electSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
    totalPrices = new Double[]{0.0, 0.0, 0.0, 0.0};
    mechaConfig = new HashMap<>();
    electConfig = new HashMap<>();

    getDatabase().ifPresent(context ->
    {
      context.selectFrom(PRICE).stream()
          .forEach(priceRecord -> priceRecords.put(priceRecord.getArtNo(), priceRecord));

      //Electronics
      int sensPerFpga;

      if(getSelectedResolution().getActualResolution() > 600)
      {
        sensPerFpga = 1;
      }
      else if((this instanceof VDCIS && getPhaseCount() > 1) || (this instanceof  MXCIS && getPhaseCount() == 4))
      {
        //FULL (RGB)
        sensPerFpga = 2;
      }
      else if(getMaxRateForHalfMode(getSelectedResolution())
              .map(rate -> getSelectedLineRate() / 1000 <= rate)
              .orElse(false))
      {
        //HALF
        sensPerFpga = 4;
      }
      else
      {
        //FULL
        sensPerFpga = 2;
      }

      setMode(sensPerFpga);

      double lengthPerSens = BASE_LENGTH * sensPerFpga;
      numFPGA = (int) Math.ceil(getScanWidth() / lengthPerSens);

      try
      {
        // Einlesen der Elektronik-Tabelle
        context.selectFrom(ELECTRONIC)
            .where(ELECTRONIC.CIS_TYPE.eq(getClass().getSimpleName()))
            .and(ELECTRONIC.CIS_LENGTH.eq(getScanWidth()))
            .stream()
            .filter(electronicRecord -> isApplicable(electronicRecord.getSelectCode()))
            .forEach(electronicRecord ->
            {
              int amount = (int) (getElectFactor(electronicRecord.getMultiplier())
                  * MathEval.evaluate(electronicRecord.getAmount()
                  .replace(" ", "")
                  .replace("L", "" + getLedLines())
                  .replace("F", "" + numFPGA)
                  .replace("S", "" + getBoardCount())
                  .replace("N", "" + getScanWidth())));

              if(amount > 0)
              {
                Optional.ofNullable(priceRecords.get(electronicRecord.getArtNo()))
                    .ifPresent(priceRecord ->
                    {
                      electConfig.put(priceRecord, amount);
                      electSums[0] += priceRecord.getPrice() * amount;
                      electSums[1] += priceRecord.getAssemblyTime() * amount;
                      electSums[2] += priceRecord.getPowerConsumption() * amount;
                      electSums[3] += priceRecord.getWeight() * amount;

                      if(priceRecord.getPhotoValue() != 0)
                      {
                        electSums[4] = priceRecord.getPhotoValue();          //Math.min(priceRecord.getPhotoValue(), electSums[4])
                      }
                    });

                if(electronicRecord.getSelectCode() != null && electronicRecord.getSelectCode().contains("FPGA"))
                {
                  numFPGA += amount;
                }
              }
            });
      }
      catch(DataAccessException e)
      {
        throw new CISException("Error in Electronics");
      }

      //Mechanics
      try
      {
        context.selectFrom(MECHANIC)
            .where(MECHANIC.CIS_TYPE.eq(getClass().getSimpleName()))
            .and(MECHANIC.CIS_LENGTH.eq(getScanWidth()))
            .and(MECHANIC.LIGHTS.eq(getLightSources())
                    .or(MECHANIC.LIGHTS.isNull()))
            .stream()
            .filter(mechanicRecord -> isApplicable(mechanicRecord.getSelectCode()))
            .forEach(mechanicRecord ->
            {
              int amount = getMechaFactor(mechanicRecord.getAmount());

              if(amount > 0)
              {
                Optional.ofNullable(priceRecords.get(mechanicRecord.getArtNo()))
                    .ifPresent(priceRecord ->
                    {
                      mechaConfig.put(priceRecord, amount);
                      mechaSums[0] += priceRecord.getPrice() * amount;
                      mechaSums[1] += priceRecord.getAssemblyTime() * amount;
                      mechaSums[2] += priceRecord.getPowerConsumption() * amount;
                      mechaSums[3] += priceRecord.getWeight() * amount;

                      if(priceRecord.getPhotoValue() != 0)
                      {
                        mechaSums[4] = priceRecord.getPhotoValue();
                      }
                    });
              }
            });
      }
      catch(DataAccessException e)
      {
        throw new CISException("Error in Mechanics");
      }

      if(!(this instanceof LDSTD))
      {
        setNumOfPix(calcNumOfPix());
      }
    });
  }

  public String getVersion()
  {
    return ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version");
  }
  
  public String getMechaVersion()
  {
    return "_" + getDatabase().flatMap(context -> context.selectFrom(CONFIG)
        .where(CONFIG.CIS_TYPE.eq(getClass().getSimpleName()))
        .and(CONFIG.KEY.eq("VERSION"))
        .fetchOptional(CONFIG.VALUE))
        .orElse("2.0") + "_";
  }

  public String getVersionHeader()
  {
    return "Version: " + getVersion() + "; " + SimpleDateFormat.getInstance().format(new Date());
  }

  public Optional<Integer> getMaxRateForHalfMode(Resolution res)
  {
    if(maxRateForHalfMode.containsKey(res.getActualResolution()))
    {
      return Optional.of(maxRateForHalfMode.get(res.getActualResolution()));
    }
    else
    {
      return Optional.empty();
    }
  }

  public String createPrntOut()
  {
    if(this instanceof LDSTD)
    {
      return createBlPrntOut();
    }

    String key = getTiViKey();
    String printout = key;
    printout += "\n\t\n";
    printout += getScanWidth() + " mm, Trigger: " + (!isExternalTrigger()? "CC1" : "extern (RS422)");

    if(key.contains("MXCIS"))
    {
      double factor;
      if(key.contains("K1"))
      {
        if(key.contains("0600"))
        {
          factor = 0.2;
        }
        else
        {
          factor = 0.25;
        }
      }
      else if(key.contains("K2"))
      {
        factor = 0.5;
      }
      else
      {
        factor = 1;
      }
      double roundLineRate = Math.round((getMaxLineRate() * factor / 1000.0) * 100.0) / 100.0;
      printout += ", max. " + roundLineRate + " kHz\n";
    }
    else
    {
      double roundLineRate = Math.round((getMaxLineRate() / 1000.0) * 100.0) / 100.0;
      printout += ", max. " + roundLineRate + " kHz\n";
    }
    printout += getString("Resolution: ");

    if(getSelectedResolution().isSwitchable()
        && (this instanceof VSCIS || this instanceof VTCIS || this instanceof VUCIS || this instanceof VHCIS))
    {
      printout += getString("binning200") + "\n";
    }
    else
    {
      printout += "~ " + getSelectedResolution().getActualResolution() + "dpi\n";
    }

    printout += getString("internal light");

    if(this instanceof VUCIS)
    {
      printout += getLightSources();
    }
    else
    {
      String color;

      if (
              getPhaseCount() == 3 || ((this instanceof VDCIS || this instanceof MXCIS) && getPhaseCount() == 4))
      {
        color = "RGB";
      }
      else
      {
        color = getLightColors().stream()
                .findAny().orElse(LightColor.NONE)
                .getDescription();
      }

      switch(getLightSources())
      {
        case "0D0C":
          printout += color + getString("None");
          break;
        case "1D0C":
          printout += color + getString("onesided");
          break;
        case "2D0C":
          printout += color + getString("twosided");
          break;
        case "1D1C":
          printout += color + getString("onepluscoax");
          break;
        case "2D1C":
          printout += color + getString("twopluscoax");
          break;
        case "0D1C":
          printout += color + getString("coax");
          break;
        default:
          printout += "Unknown";
      }

      if(this instanceof MXCIS)
      {
        printout += getString("schipal");

        if (getSelectedResolution().getActualResolution() > 600)
        {
          printout += getString("staggered");
        }
        else
        {
          printout += getString("inline");
        }
      }
    }

    printout += "\n\n";
    int numOfPix = getNumOfPix();

    printout += getString("sellinerate") + Math.round(getSelectedLineRate() / 100.0) / 10.0 + " kHz\n";

//  Korr CTi. 04.11.2019
    if(this instanceof MXCIS)
    {
       printout += getString("transport speed") + ": " + String.format("%.1f", (getTransportSpeed() / 1000.0))  + " mm/s\n";
    }
    else
    {
      printout += getString("transport speed") + ": " + String.format("%.1f", (getTransportSpeed() / 1000.0) * (1.0 * getSelectedLineRate() / getMaxLineRate())) + " mm/s\n";
    }

    if(this instanceof MXCIS)
    {
      printout += getString("chpltol") + "\n";
      printout += getString("Geocor_opt") + "\n";
    }
    else if(getSelectedResolution().getActualResolution() >= 1200)
    {
      printout += getString("Geometry correction: x and y") + "\n";
    }
    else
    {
      printout += getString("Geometry correction: x") + "\n";
    }

    if(this instanceof MXCIS)
    {
      printout += getString("scan distance") + ": ~ 10 mm " + getString("exactseetypesign") + "\n";
      printout += getString("DepthofField") + ": ~ +/- " + getSelectedResolution().getDepthOfField() + " mm\n" + getString("line width") + ": ~ 1 mm\n";
      printout += getString("case length") + ": ~ " + (getScanWidth() + 288) + " mm\n";
      if(getLedLines() < 2)
      {
        printout += getString("alucase_mxcis") + "\n";
      }
      else
      {
        printout += getString("alucase_mxcis_two") + "\n";
      }
    }
    else if(this instanceof VDCIS)
    {
      printout += getString("scan distance") + ": ~ 55 - 70 mm " + getString("exactresolution") + "\n";
      printout += getString("DepthofField") + ": ~ " + getSelectedResolution().getDepthOfField() + " mm\n" + getString("line width") + ": ~ 1 mm\n";
      printout += getString("case length") + ": ~ " + (getScanWidth() + 100) + " mm\n";
      printout += getString("Aluminium case profile: 80x80mm (HxT) with bonded") + "\n";
    }else if(this instanceof VTCIS) {
      printout += getString("scan distance") + ": 10 mm " + "\n";
      printout += getString("DepthofField") + ": ~ +/- " + getSelectedResolution().getDepthOfField() + " mm\n" + getString("line width") + ": ~ 1mm\n";
      printout += getString("case length") + ": ~ " + (getScanWidth() + 100) + " mm\n";

      if(!getLightSources().endsWith("0C")) {
        printout += getString("Aluminium case profile: 53x50mm (HxT) with bondedcoax") + "\n";
      } else {
        printout += getString("Aluminium case profile: 86x80mm (HxT) with bonded") + "\n";
      }
    }
    else
    {
      printout += getString("scan distance") + ": 9-12 mm " + getString("exactseetypesign") + "\n";
      if(this instanceof VSCIS ||  this instanceof VHCIS)
      {
        printout += getString("DepthofField") + ": ~ +/- "  + getSelectedResolution().getDepthOfField() + " mm\n" + getString("line width") + ": ~ 1mm\n";
      }
      else
      {
        printout += getString("DepthofField") + ": ~ +/- 0.50 mm\n" + getString("line width") + ": ~ 1mm\n";
      }
      printout += getString("case length") + ": ~ " + (getScanWidth() + 100) + " mm\n";
      if(this instanceof VHCIS)
      {
        printout += "Unknown aluminium case profile with bonded\n";
      }
      else if(!getLightSources().endsWith("0C"))
      {
        printout += getString("Aluminium case profile: 53x50mm (HxT) with bondedcoax") + "\n";
      }
      else
      {
        printout += getString("Aluminium case profile: 53x50mm (HxT) with bonded") + "\n";
      }
    }
    printout += getString("glass pane, see drawing") + "\n";
    printout += getString("shading") + "\n";
    printout += getString("powersource") + "(24 +/- 1) VDC\n";
    printout += getString("Needed power:") + (" " + ((electSums[2] == null) ? 0.0 : (Math.round(10.0 * electSums[2]) / 10.0)) + " A").replace(" 0 A", " ???") + " +/- 20%\n";
    printout += getString("FrequencyLimit") + " " + Math.round(1000 * getMinFreq(getTiViKey())) / 1000 + " kHz\n";
    if(this instanceof VTCIS){
      printout += getString("lico") + "\n";
    }
    else if(this instanceof VSCIS){
      printout += getString(getCooling().getShortHand()) + "\n";
    }else{
      printout += getString("intforced") + "\n";
    }
    printout += getString("weight") + ": ~ " + (" " + Math.round((((electSums[3] == null) ? 0.0 : electSums[3]) + ((mechaSums[3] == null) ? 0.0 : mechaSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";
    printout += "Interface: " + (isGigeInterface() ? "GigE" : "CameraLink (max. 5m)") + "\n";

    if(this instanceof VDCIS)
    {
      printout += getString("laser") + "\n";
    }

    if(this instanceof MXCIS)
    {
      printout += getString("clbase");
    }

    if(isGigeInterface())
    {
      printout += "\n\t\n";
      printout += "Pixel Clock: 40MHz\n";
      printout += getString("numofpix") + numOfPix + "\n";
    }
    else
    {
      Optional<CameraLink> clCalc = getCLCalc(numOfPix);
      if(clCalc.isPresent())
      {
        printout += "\n\t\n";
        printout += clCalc.get().toString();
      }
      else
      {
        return null;
      }
      if(this instanceof VTCIS || this instanceof VDCIS){
        printout += getString("configOnRequest");
      }
    }
    return printout;
  }

  private String createBlPrntOut()
  {
    String printout = getTiViKey();
    printout += "\n\t\n";
    printout += getString("suitedfor") + getScanWidth() + getString("mm CIS scan width") + "\n";

    LightColor color;

    if(getPhaseCount() == 3 || ((this instanceof VDCIS || this instanceof MXCIS) && getPhaseCount() == 4))
    {
      color = LightColor.RGB;
    }
    else
    {
      color = getLightColors().stream()
              .findAny().orElse(LightColor.NONE);
    }
    printout += getString("Color:") + color.getDescription() + "\n";
    printout += "\n\t\n";
    printout += getString("line width") + ": ~ 1 mm\n";
    printout += getString("case length") + ": ~ " + (getScanWidth() + 100) + " mm\n";
    printout += getString("Aluminium case profile: 53x50mm (HxT) with bondedmxled") + "\n";
    printout += getString("glass pane, see drawing") + "\n";
    printout += getString("shading") + "\n";
    printout += getString("powersource") + "(24 +/- 1) VDC\n";
    printout += getString("Needed power:") + (((electSums[2] == null) ? 0.0 : (Math.round(10.0 * electSums[2]) / 10.0)) + " A").replace(" 0 A", " ???") + " +/- 20%\n";
    printout += getString("weight") + ": ~ " + (Math.round((((electSums[3] == null) ? 0.0 : electSums[3]) + ((mechaSums[3] == null) ? 0.0 : mechaSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";

    return printout;
  }

  private boolean isApplicable(String selectCode)
  {
    if(selectCode == null)
    {
      return true;
    }

    boolean proceed;
    String key = getTiViKey();
    String[] multiplier = selectCode.split("&");

    boolean invert;
    for(String m : multiplier)
    {
      invert = m.startsWith("!");
      m = (invert ? m.substring(1) : m);

      // Regex pattern, apply to light code
      if(m.startsWith("^") && m.endsWith("$"))
      {
        try
        {
          proceed = getLightSources().matches(m);
        }
        catch(PatternSyntaxException ex)
        {
          //Not a valid expression, ignore and proceed
          proceed = !invert; //!invert ^ invert == true
        }
      }
      else
      {
        switch (m)
        {
          case "": //No code
          case "FPGA": //Notifier for FPGA parts, ignore and proceed
            proceed = !invert; //!invert ^ invert == true
            break;
          case "RGB": //Color coding
            if(this instanceof VUCIS)
            {
              proceed = LightColor.findByShortHand(m)
                      .map(getLightColors()::contains)
                      .orElse(false);
            }
            else
            {
              if(!(this instanceof VDCIS) && !(this instanceof  MXCIS))
              {
                proceed = getPhaseCount() == 3;
              }else
              {
                proceed = getPhaseCount() == 4;
              }
//              if(this instanceof MXCIS && getTiViKey().contains("RGB")) {
//                proceed = getLedLines() >= 3;
//              }
//              else if(getTiViKey().contains("RGB") && this instanceof VTCIS && this instanceof VDCIS && this instanceof VSCIS){
//                proceed = getLedLines() >=3;
//              }else {
//                proceed = getLedLines() >= 3;
//              }
            }
            break;
          case "AM":
          case "BL":
          case "GR":
          case "IR":
          case "JR":
          case "UV":
          case "HI":
          case "YE":
          case "VE":
          case "WH":
          case "REBEL":
          case "REBZ8":
            proceed = LightColor.findByShortHand(m)
                    .map(getLightColors()::contains)
                    .orElse(false);
            break;
          case "MONO": //Monochrome only
            proceed = !getTiViKey().contains("RGB");
            break;
          case "25dpi": //Specific resolution
          case "50dpi":
          case "75dpi":
          case "100dpi":
          case "150dpi":
          case "200dpi":
          case "300dpi":
          case "400dpi":
          case "600dpi":
          case "1200dpi":
          case "2400dpi":
            proceed = m.equals(getSelectedResolution().getBoardResolution() + "dpi");
            break;
          case "GIGE": //GigE only
            proceed = isGigeInterface();
            break;
          case "CL": //CameraLink only
            proceed = !isGigeInterface();
            break;
          case "COAX": //At least one coaxial light
            proceed = key.split("_")[4].endsWith("C") || (this instanceof MXCIS && key.split("_")[5].endsWith("C"));
            break;
          case "DIFF":
            proceed = !(key.split("_")[4].endsWith("C") || (this instanceof MXCIS && key.split("_")[5].endsWith("C"))) //No coaxial light
                    || (key.split("_")[4].startsWith("2") || (this instanceof MXCIS && key.split("_")[5].startsWith("2"))); //Twosided => at least one diffuse (2XX oder 2XXC)
            break;
          case "NOCO": //Specific cooling
          case "FAIR":
          case "PAIR":
          case "LICO":
            proceed = key.contains(m);
            break;
          case "default": //Default cooling
            proceed = !key.contains("NOCO") && !key.contains("FAIR") && !key.contains("PAIR") && !key.contains("LICO");
            break;
          case "NOEXT": //No external trigger
            proceed = !isExternalTrigger();
            break;
          case "EXT": //With external trigger
            proceed = isExternalTrigger();
            break;
          case "L": //MODE: LOW (MXCIS only)
            proceed = this instanceof MXCIS && getMode() == 4;
            break;
          case "H": //Mode: HIGH (MXCIS only)
            proceed = this instanceof MXCIS && getMode() == 2;
            break;
          default: //Unknown modifier
            proceed = invert; //invert ^ invert == false
            break;
        }
      }

      proceed = invert ^ proceed;

      if(!proceed)
      {
        return false;
      }
    }

    return true;
  }

  private int getMechaFactor(String factor)
  {
    if(isInteger(factor))
    {
      return Integer.parseInt(factor);
    }
    else
    {
      String ev = factor.replace("F", "" + numFPGA).replace("S", "" + getScanWidth() / BASE_LENGTH).replace("N", "" + getScanWidth()).replace(" ", "").replace("L", "" + getLedLines());
      return (int) MathEval.evaluate(ev);
    }
  }

  private int getElectFactor(String factor)
  {
    if(isInteger(factor))
    {
      return Integer.parseInt(factor);
    }
    else if(factor.equals("L"))
    {
      return getLedLines();
    }
    else if(factor.contains("=="))
    {
      String[] splitted = factor.split("==");
      if(splitted[0].equals("L") && isInteger(splitted[1]) && getLedLines() == Integer.parseInt(splitted[1]))
      {
        return 1;
      }
    }
    else if(factor.contains(">"))
    {
      String[] splitted = factor.split(">");
      if(splitted[0].equals("L") && isInteger(splitted[1]) && getLedLines() > Integer.parseInt(splitted[1]))
      {
        return 1;
      }
    }
    else if(factor.contains("<"))
    {
      String[] splitted = factor.split("<");
      if(splitted[0].equals("L") && isInteger(splitted[1]) && getLedLines() < Integer.parseInt(splitted[1]))
      {
        return 1;
      }
    }

    return -1;
  }

  public Map<PriceRecord, Integer> getMechaConfig()
  {
    return mechaConfig;
  }

  public Map<PriceRecord, Integer> getElectConfig()
  {
    return electConfig;
  }

  protected int getBoardCount()
  {
    return getScanWidth() / BASE_LENGTH;
  }

  public String createCalculation()
  {
    String printout = getTiViKey() + "\n\t\n";
    StringBuilder electOutput = new StringBuilder(getString("Electronics")).append(":").append("\n\n");
    StringBuilder mechaOutput = new StringBuilder(getString("Mechanics")).append(":").append("\n\n");
    StringBuilder totalOutput = new StringBuilder(getString("Totals")).append(":").append("\n\n");

    electOutput.append(getString("Component")).append("\t")
            .append(getString("Item no.")).append("\t")
            .append(getString("Amount")).append("\t")
            .append(getString("Price/pc (EUR)")).append("\t")
            .append(getString("Weight/pc (kg)")).append("\t")
            .append(getString("Time/pc (h)")).append("\t")
            .append(getString("Power/pc (A)")).append("\n");

    mechaOutput.append(getString("Component")).append("\t")
            .append(getString("Item no.")).append("\t")
            .append(getString("Amount")).append("\t")
            .append(getString("Price/pc (EUR)")).append("\t")
            .append(getString("Weight/pc (kg)")).append("\n");

    getElectConfig().forEach((priceRecord, amount) -> electOutput.append(priceRecord.getFerixKey()).append("\t")
        .append(String.format("%05d", priceRecord.getArtNo())).append("\t")
        .append(amount).append("\t")
        .append(String.format(getLocale(), "%.2f", priceRecord.getPrice() * amount)).append("\t")
        .append(String.format(getLocale(), "%.2f", priceRecord.getWeight() * amount)).append("\t")
        .append(String.format(getLocale(), "%.2f", priceRecord.getAssemblyTime() * amount)).append("\t")
        .append(String.format(getLocale(), "%.2f", priceRecord.getPowerConsumption() * amount)).append("\n"));

    electOutput.append("\n\t\n").append(getString("Totals")).append("\t")
        .append(" \t")
        .append("0\t")
        .append(String.format(getLocale(), "%.2f", electSums[0])).append("\t")
        .append(String.format(getLocale(), "%.2f", electSums[3])).append("\t")
        .append(String.format(getLocale(), "%.2f", electSums[1])).append("\t")
        .append(String.format(getLocale(), "%.2f", electSums[2])).append("\n");

    getMechaConfig().forEach((priceRecord, amount) -> mechaOutput.append(priceRecord.getFerixKey()).append("\t")
        .append(String.format("%05d", priceRecord.getArtNo())).append("\t")
        .append(amount).append("\t")
        .append(String.format(getLocale(), "%.2f", priceRecord.getPrice() * amount)).append("\t")
        .append(String.format(getLocale(), "%.2f", priceRecord.getWeight() * amount)).append("\n"));

    mechaOutput.append("\n\t\n").append(getString("Totals")).append("\t")
            .append(" \t")
            .append("0\t")
            .append(String.format(getLocale(), "%.2f", mechaSums[0])).append("\t")
            .append(String.format(getLocale(), "%.2f", mechaSums[3])).append("\n");

    try
    {
      Map<String, Double> calcMap = getDatabase().map(Stream::of).orElse(Stream.empty())
          .flatMap(context -> context.selectFrom(CONFIG)
              .where(CONFIG.CIS_TYPE.eq(getClass().getSimpleName())).stream())
          .collect(Collectors.toMap(ConfigRecord::getKey, configRecord -> Double.parseDouble(configRecord.getValue())));

      totalOutput.append(getString("calcfor10")).append("\t \t \t \t ").append("\n");
      totalOutput.append(getString("Electronics")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[0])).append("\t \n");
      totalPrices[2] = electSums[0];
      totalOutput.append(getString("Overhead Electronics")).append(" (").append(calcMap.get("A_ELEKTRONIK")).append("%):\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100))).append("\t \n");
      totalPrices[2] += electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100);
      totalOutput.append(getString("Testing")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[1] * calcMap.get("STUNDENSATZ"))).append("\t \n");
      totalPrices[2] += electSums[1] * calcMap.get("STUNDENSATZ");
      if(isGigeInterface())
      {
        totalOutput.append(getString("Overhead GigE")).append(" (").append(calcMap.get("Z_GIGE")).append("%):\t \t \t")
                .append(String.format(getLocale(), "%.2f", electSums[0] * calcMap.get("Z_GIGE") / 100)).append("\t \n");
        totalPrices[2] += electSums[0] * (calcMap.get("Z_GIGE") / 100);
      }
      totalOutput.append(getString("Mechanics")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", mechaSums[0])).append("\t \n");
      totalPrices[2] += mechaSums[0];
      totalOutput.append(getString("Overhead Mechanics")).append(" (").append(calcMap.get("A_MECHANIK")).append("%):\t \t \t")
              .append(String.format(getLocale(), "%.2f", mechaSums[0] * (calcMap.get("A_MECHANIK") / 100))).append("\t \n");
      totalPrices[2] += mechaSums[0] * (calcMap.get("A_MECHANIK") / 100);
      totalOutput.append(getString("Assembly")).append(":\t \t ")
              .append(calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()).append(" h\t")
              .append(String.format(getLocale(), "%.2f", (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()) * calcMap.get("STUNDENSATZ"))).append("\t \n");
      totalPrices[2] += (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * getBoardCount()) * calcMap.get("STUNDENSATZ");

      int addition = 0;
      double surcharge = 0.0;

      totalPrices[0] = totalPrices[2] * calcMap.get("F_1") / 100;
      totalPrices[1] = totalPrices[2] * calcMap.get("F_5") / 100;
      totalPrices[2] = totalPrices[2] * calcMap.get("F_10") / 100;
      totalPrices[3] = totalPrices[2] * calcMap.get("F_25") / 100;
      totalOutput.append(" \t(1 ").append(getString("pc")).append(")\t")
              .append("(5 ").append(getString("pcs")).append(")\t")
              .append("(10 ").append(getString("pcs")).append(")\t")
              .append("(25 ").append(getString("pcs")).append(")\n");

      String format = "%.2f";
      double value = calcMap.get("Z_TRANSPORT") / 100;
      totalOutput.append(getString("Price/pc")).append(":\t")
              .append(String.format(getLocale(), format, totalPrices[0])).append("\t")
              .append(String.format(getLocale(), format, totalPrices[1])).append("\t")
              .append(String.format(getLocale(), format, totalPrices[2])).append("\t")
              .append(String.format(getLocale(), format, totalPrices[3])).append("\n");
      totalOutput.append(getString("Surcharge Transport")).append(" (").append(value).append("%):\t")
              .append(String.format(getLocale(), format, totalPrices[0] * value)).append("\t")
              .append(String.format(getLocale(), format, totalPrices[1] * value)).append("\t")
              .append(String.format(getLocale(), format, totalPrices[2] * value)).append("\t")
              .append(String.format(getLocale(), format, totalPrices[3] * value)).append("\n");
      surcharge += value;

      if(this instanceof MXCIS)
      {
        String cat = getTiViKey().split("_")[4];          /* String cat = getTiViKey().split("_")[3]*/
        value = calcMap.get("Z_" + cat) / 100;
        totalOutput.append(getString("Surcharge")).append(" ").append(cat).append(" (").append(calcMap.get("Z_" + cat)).append("%):\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[0] * value)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[1] * value)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[2] * value)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[3] * value)).append("\n");
      }
      else if(!(this instanceof LDSTD))
      {
        value = calcMap.get(getDpiCode()) / 100.0;
        totalOutput.append(getString("Surcharge DPI/Switchable")).append(" (").append(calcMap.get(getDpiCode())).append("%):\t")
                .append(String.format(getLocale(), format, totalPrices[0] * value)).append("\t")
                .append(String.format(getLocale(), format, totalPrices[1] * value)).append("\t")
                .append(String.format(getLocale(), format, totalPrices[2] * value)).append("\t")
                .append(String.format(getLocale(), format, totalPrices[3] * value)).append("\n");
      }
      surcharge += value;

      format = "%.2f";
      value = calcMap.get("LIZENZ");
      totalOutput.append(getString("Licence")).append(":\t")
              .append(String.format(getLocale(), format, value)).append("\t")
              .append(String.format(getLocale(), format, value)).append("\t")
              .append(String.format(getLocale(), format, value)).append("\t")
              .append(String.format(getLocale(), format, value)).append("\n");
      addition += value;

      format = "%.2f";
      value = calcMap.get("Z_DISCONT") / 100;
      totalOutput.append(getString("Discount Surcharge")).append(" (").append(value).append("%):\t")
              .append(String.format(getLocale(), format, totalPrices[0] * value)).append("\t")
              .append(String.format(getLocale(), format, totalPrices[1] * value)).append("\t")
              .append(String.format(getLocale(), format, totalPrices[2] * value)).append("\t")
              .append(String.format(getLocale(), format, totalPrices[3] * value)).append("\n");
      surcharge += value;

      totalPrices[0] *= 1 + surcharge;
      totalPrices[1] *= 1 + surcharge;
      totalPrices[2] *= 1 + surcharge;
      totalPrices[3] *= 1 + surcharge;

      totalPrices[0] += addition;
      totalPrices[1] += addition;
      totalPrices[2] += addition;
      totalPrices[3] += addition;

      totalOutput.append(getString("Totals")).append(" (EUR):").append("\t")
              .append(String.format(getLocale(), format, totalPrices[0])).append("\t")
              .append(String.format(getLocale(), format, totalPrices[1])).append("\t")
              .append(String.format(getLocale(), format, totalPrices[2])).append("\t")
              .append(String.format(getLocale(), format, totalPrices[3])).append("\n");
    }
    catch(NullPointerException | IndexOutOfBoundsException | NumberFormatException e)
    {
      e.printStackTrace();
      throw new CISException(getString("MissingConfigTables"));
    }

    printout += electOutput.toString();
    printout += "\n\t\n";
    printout += mechaOutput.toString();
    printout += "\n\t\n";
    printout += totalOutput.toString();

    return printout;
  }

  public Locale getLocale()
  {
    return locale;
  }

  private String getDpiCode()
  {
    if(getSelectedResolution().isSwitchable())
    {
      return "Z_SWT_DPI";
    }
    else
    {
      return "Z_" + getTiViKey().split("_")[3] + "_DPI";
    }
  }

  private int calcNumOfPix()
  {
    int numOfPix;
    int sensorBoards = getScanWidth() / BASE_LENGTH;
    int binning = getBinning();
    if(this instanceof MXCIS)
    {
      SensorChipRecord sensorChip = ((MXCIS) this).getSensorChip(getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor chip"));
      SensorBoardRecord sensorBoard = ((MXCIS) this).getSensorBoard(getSelectedResolution().getBoardResolution()).orElseThrow(() -> new CISException("Unknown sensor board"));
      numOfPix = sensorBoard.getChips() * getBoardCount() * sensorChip.getPixelPerSensor() / binning;
    }
    else if(this instanceof VHCIS || this instanceof VTCIS || this instanceof VUCIS)
    {
      SensorBoardRecord sensorBoard = getSensorBoard("SMARDOUB").orElseThrow(() -> new CISException("Unknown sensor board"));
      numOfPix = (int) (sensorBoard.getChips() * sensorBoards * 0.72 * getSelectedResolution().getActualResolution());
    }
    else
    {
      SensorBoardRecord sensorBoard = getSensorBoard("SMARAGD").orElseThrow(() -> new CISException("Unknown sensor board"));
      numOfPix = (int) (sensorBoard.getChips() * sensorBoards * 0.72 * getSelectedResolution().getActualResolution());
    }

    if(getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000 > 80 && isGigeInterface())
    {
      throw new CISException(getString("GIGEERROR") + (getPhaseCount() * numOfPix * getSelectedLineRate() / 1000000) + " MByte");
    }

    return numOfPix;
  }

  public int getNumFPGA()
  {
    return numFPGA;
  }

  public double getMinFreq(String key)
  {
    boolean coax = key.contains("C_");
    return 100 * electSums[4]
            * mechaSums[4]
            * (coax ? 1 : getLedLines())
            * getGeometry(coax)
            * getSensitivity() / (1.5 * (key.contains("RGB") ? 3 : 1));
  }

  public abstract double getGeometry(boolean coax);

  public double getSensitivity()
  {
    if(this instanceof MXCIS)
    {
      return 30;
    }
    else if(this instanceof VTCIS)
    {
      switch(getSelectedResolution().getBoardResolution())
      {
        case 1200:
          return 500;
        case 600:
          return 1000;
        case 300:
          return 1800;
        default:
          throw new UnsupportedOperationException();
      }
    }
    else if(this instanceof VUCIS)
    {
      if(getSelectedResolution().getBoardResolution() == 1000)
      {
        return 500;
      }
      else
      {
        throw new UnsupportedOperationException();
      }
    }
    else if(this instanceof VDCIS)
    {
      switch(getSelectedResolution().getBoardResolution())
      {
        case 1000:
          return 500;
        case 500:
          return 1000;
        case 250:
          return 1800;
        default:
          throw new UnsupportedOperationException();
      }
    }

    return 1;
  }

  public static boolean isInteger(String s)
  {
    return s != null && s.matches("[-+]?\\d+");
  }

  public static boolean isDouble(String s)
  {
    return s != null && s.matches("[-+]?\\d+[.,]?\\d*");
  }

  public static double decodeQuantity(String s)
  {
    switch(s)
    {
      case "2":
        return 1000;
      case "1":
        return 100;
      case "0":
      default:
        return 1;
    }
  }

  public static String getPortName(int x)
  {
    return Character.toString((char) (65 + x));
  }

  // Laden von statischen Texten aus dem Bundle (de/en)
  public String getString(String key)
  {
    return ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", getLocale()).getString(key);
  }
}
