package de.tichawa.cis.config;

import de.tichawa.cis.config.model.tables.records.*;
import de.tichawa.util.MathEval;
import de.tichawa.cis.config.mxcis.MXCIS;
import org.apache.commons.dbcp2.*;
import org.jooq.*;
import org.jooq.exception.*;
import org.jooq.impl.*;
import org.jooq.types.*;

import java.io.*;
import java.io.IOException;
import java.nio.charset.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import static de.tichawa.cis.config.model.Tables.*;

// Alle allgemeine CIS Funktionen
public abstract class CIS
{

  public final String cisName;

  protected HashMap<String, Integer> spec;
  protected HashMap<String, Integer[]> sensChipTab;
  protected HashMap<String, Integer[]> sensBoardTab;
  protected HashMap<String, Integer[]> adcBoardTab;
  protected HashMap<PriceRecord, Integer> electConfig;
  protected HashMap<PriceRecord, Integer> mechaConfig;
  protected Double[] electSums;
  protected Double[] mechaSums;
  protected Double[] totalPrices;
  protected final HashMap<Integer, Integer> maxRateForHalfMode;
  public static final int BASE_LENGTH = 260;
  protected int numFPGA;
  protected static Locale locale = Locale.getDefault();

  protected static final String[] COLOR_CODE = new String[]
  {
    "AM", "GR", "BL", "IR", "YE", "WH"
  };

  public String getPortName(int x)
  {
    return Character.toString((char) (65 + x));
  }

  public CIS()
  {
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

    sensChipTab = readConfigTable("/de/tichawa/cis/config/sensChips.csv");
    sensBoardTab = readConfigTable("/de/tichawa/cis/config/sensBoards.csv");
    adcBoardTab = readConfigTable("/de/tichawa/cis/config/adcBoards.csv");

    spec = new HashMap<>();
  }

  public abstract String getCLCalc(int numOfPix);

  public abstract String getTiViKey();

  public abstract int[] getLightSources();

  public String getBlKey()
  {
    String key = "G_MXLED";
    key += String.format("_%04d", getSpec("sw_cp"));

    switch(getSpec("Internal Light Source"))
    {
      case 0:
        key += "_NO";
        break;
      case 1:
        key += "_" + COLOR_CODE[getSpec("Internal Light Color")];
        break;
      case 2:
        key += "_2" + COLOR_CODE[getSpec("Internal Light Color")];
        break;
      case 3:
        key += "_2" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
        break;
      case 4:
        key += "_" + COLOR_CODE[getSpec("Internal Light Color")] + "C";
        break;
      default:
        key += "_UNKNOWN";
    }

    if(getSpec("Color") == 4)
    {
      key = key.replace(COLOR_CODE[getSpec("Internal Light Color")], "RGB");
    }

    try
    {
      String version = Files.lines(Launcher.tableHome.resolve("mxled/Calculation.csv"))
              .map(line -> line.split("\t"))
              .filter(line -> line[0].equals("VERSION"))
              .map(line -> line[1])
              .findAny().orElse("2.0");

      key += "_" + version + "_";
    }
    catch(IOException e)
    {
      e.printStackTrace();
      key += "_2.0_";
    }

    if(key.endsWith("_"))
    {
      key = key.substring(0, key.length() - 1);
    }

    return key;
  }

  public static void setLocale(Locale l)
  {
    locale = l;
  }

  protected final HashMap<String, Integer[]> readConfigTable(String path)
  {
    HashMap<String, Integer[]> map = new HashMap<>();

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path), StandardCharsets.UTF_8)))
    {
      String line;

      while((line = reader.readLine()) != null)
      {
        String[] data = line.split(",");
        Integer[] list = new Integer[data.length - 1];

        for(int x = 1; x < data.length; x++)
        {
          list[x - 1] = Integer.parseInt(data[x]);
        }

        map.put(data[0], list);
      }
    }
    catch(IOException e)
    {
      throw new CISException("Error in table " + path);
    }

    return map;
  }

  public Integer[] getADC(String name)
  {
    if(adcBoardTab.containsKey(name))
    {
      return adcBoardTab.get(name);
    }

    return null;
  }

  public Integer[] getSensBoard(String name)
  {
    if(sensBoardTab.containsKey(name))
    {
      return sensBoardTab.get(name);
    }

    return null;
  }

  public Integer[] getSensChip(String name)
  {
    if(sensChipTab.containsKey(name))
    {
      return sensChipTab.get(name);
    }

    return null;
  }

  public final void setSpec(String key, int value)
  {
    spec.put(key, value);
  }

  public Integer getSpec(String key)
  {
    if(spec.containsKey(key))
    {
      return spec.get(key);
    }

    return null;
  }

  public static double round(double value, int digits)
  {
    return Math.round(Math.pow(10.0, digits) * value) / Math.pow(10.0, digits);
  }

  public Optional<DSLContext> getDatabase()
  {
    try
    {
      DSLContext context;
      Properties dbProperties = new Properties();
      dbProperties.loadFromXML(new FileInputStream("connection.xml"));
      BasicDataSource dataSource = new BasicDataSource();
      dataSource.setUrl("jdbc:mariadb://" + dbProperties.getProperty("dbHost") + ":" + dbProperties.getProperty("dbPort")
          + "/" + dbProperties.getProperty("dbName"));
      dataSource.setUsername(dbProperties.getProperty("dbUser"));
      dataSource.setPassword(dbProperties.getProperty("dbPwd"));
      context = DSL.using(dataSource, SQLDialect.MARIADB);

      return Optional.of(context);
    }
    catch(IOException ex)
    {
      return Optional.empty();
    }
  }

  public boolean calculate()
  {
    Map<Integer, PriceRecord> priceRecords = new HashMap<>();
    mechaSums = new Double[]{0.0, 0.0, 0.0, 0.0, 100.0};
    electSums = new Double[]{0.0, 0.0, 0.0, 0.0, 1.0};
    totalPrices = new Double[]{0.0, 0.0, 0.0, 0.0};
    mechaConfig = new HashMap<>();
    electConfig = new HashMap<>();

    return getDatabase().map(context ->
    {
      context.selectFrom(PRICE)
          .fetchStream()
          .forEach(priceRecord -> priceRecords.put(priceRecord.getArtNo(), priceRecord));


      //Electronics
      int sensPerFpga;

      if(getSpec("res_cp2") != null && getSpec("res_cp2") > 600)
      {
        sensPerFpga = 1;
      }
      else if((getSpec("VDCIS") != null && getSpec("Color") > 2) || (getSpec("VDCIS") == null && getSpec("Color") > 1))
      {
        //FULL (RGB)
        sensPerFpga = 2;
      }
      else if(getSpec("res_cp2") != null && maxRateForHalfMode.get(getSpec("res_cp2")) != null && (getSpec("Selected line rate") / 1000.0) <= maxRateForHalfMode.get(getSpec("res_cp2")))
      {
        //HALF
        sensPerFpga = 4;
      }
      else
      {
        //FULL
        sensPerFpga = 2;
      }

      setSpec("MODE", sensPerFpga);

      double lengthPerSens = BASE_LENGTH * sensPerFpga;
      numFPGA = (int) Math.ceil(getSpec("sw_cp") / lengthPerSens);

      try
      {
        // Einlesen der Elektronik-Tabelle
        context.selectFrom(ELECTRONIC)
            .where(ELECTRONIC.CIS_TYPE.eq(getClass().getSimpleName()))
            .and(ELECTRONIC.CIS_LENGTH.eq(getSpec("sw_cp")))
            .stream()
            .filter(electronicRecord -> isApplicable(electronicRecord.getSelectCode()))
            .forEach(electronicRecord ->
            {
              int amount = (int) (getElectFactor(electronicRecord.getMultiplier())
                  * MathEval.evaluate(electronicRecord.getAmount()
                  .replace(" ", "")
                  .replace("L", "" + getSpec("LEDLines"))
                  .replace("F", "" + numFPGA)
                  .replace("S", "" + getSpec("sw_cp") / BASE_LENGTH)
                  .replace("N", "" + getSpec("sw_cp"))));

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

                      if(priceRecord.getPhotoValue() != null)
                      {
                        electSums[4] = Math.min(priceRecord.getPhotoValue(), electSums[4]);
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
            .and(MECHANIC.CIS_LENGTH.eq(getSpec("sw_cp")))
            .and(MECHANIC.DIFFUSE_LIGHTS.eq(getLightSources()[0]))
            .and(MECHANIC.COAX_LIGHTS.eq(getLightSources()[1]))
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

                      if(priceRecord.getPhotoValue() != null)
                      {
                        mechaSums[4] *= priceRecord.getPhotoValue();
                      }
                    });
              }
            });
      }
      catch(DataAccessException e)
      {
        throw new CISException("Error in Mechanics");
      }

      if(getSpec("MXLED") == null)
      {
        setSpec("numOfPix", calcNumOfPix());
      }

      return true;
    }).orElse(false);
  }

  public String getVersion()
  {
    return ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version");
  }
  
  public String getMechaVersion()
  {
    try
    {
      return "_" + Files.lines(Launcher.tableHome.resolve(getClass().getSimpleName() + "/Calculation.csv"))
              .map(line -> line.split("\t"))
              .filter(line -> line[0].equals("VERSION"))
              .map(line -> line[1])
              .findAny().orElse("2.0") + "_";
    }
    catch(IOException e)
    {
      return "_2.0_";
    }
  }

  public String getVersionHeader()
  {
    return "Version: " + getVersion() + "; " + SimpleDateFormat.getInstance().format(new Date());
  }

  @SuppressWarnings("fallthrough")
  public String createPrntOut()
  {
    if(getSpec("MXLED") != null)
    {
      return createBlPrntOut();
    }

    String key = getTiViKey();
    String printout = key;
    printout += "\n\t\n";
    printout += getSpec("sw_cp") + " mm, Trigger: " + (getSpec("External Trigger") == 0 ? "CC1" : "extern (RS422)");

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

      printout += ", max. " + getSpec("Maximum line rate") * factor / 1000.0 + " kHz\n";
    }
    else
    {
      printout += ", max. " + getSpec("Maximum line rate") / 1000.0 + " kHz\n";
    }
    printout += getString("Resolution: ");

    if(getSpec("Resolution") == 0
        && (getSpec("VSCIS") != null || getSpec("VTCIS") != null || getSpec("VHCIS") != null))
    {
      printout += getString("binning200") + "\n";
    }
    else
    {
      printout += "~ " + getSpec("res_cp2") + "dpi\n";
    }

    printout += getString("internal light");
    String color = "";

    if(getSpec("Color") == 3 || ((getSpec("VDCIS") != null || getSpec("MXCIS") != null) && getSpec("Color") == 4))
    {
      color = "RGB";
    }
    else
    {
      switch(getSpec("Internal Light Color"))
      {
        case 0:
          color = getString("Amber (Red)");
          break;
        case 1:
          color = getString("Green");
          break;
        case 2:
          color = getString("Blue");
          break;
        case 3:
          color = getString("Infrared");
          break;
        case 4:
          color = getString("Yellow");
          break;
        case 5:
          color = getString("White");
          break;
        default:
          color = "Unknown";
      }
    }

    if(getSpec("MXCIS") != null)
    {
      switch(getSpec("Internal Light Source"))
      {
        case 0:
          printout += getString("None");
          break;
        case 1:
          printout += color + getString("onesided");
          break;
        case 2:
          printout += color + getString("coax");
          break;
        case 3:
          printout += color + getString("twosided");
          break;
        case 4:
          printout += color + getString("onepluscoax");
          break;
        default:
          printout += "Unknown";
      }

      printout += getString("schipal");

      if(getSpec("res_cp2") > 600)
      {
        printout += getString("staggered");
      }
      else
      {
        printout += getString("inline");
      }
    }
    else
    {
      switch(getSpec("Internal Light Source"))
      {
        case 0:
          printout += getString("None");
          break;
        case 1:
          printout += color + getString("onesided");
          break;
        case 2:
          printout += color + getString("twosided");
          break;
        case 3:
          printout += color + getString("twopluscoax");
          break;
        case 4:
          printout += color + getString("coax");
          break;
        default:
          printout += "Unknown";
      }
    }

    printout += "\n\n";
    int numOfPix = getSpec("numOfPix");

    printout += getString("sellinerate") + Math.round(getSpec("Selected line rate") / 100.0) / 10.0 + " kHz\n";
    
//  Korr CTi. 04.11.2019
    if(getSpec("MXCIS") != null)
    {
       printout += getString("transport speed") + ": " + String.format("%.1f", (getSpec("Speedmms") / 1000.0))  + " mm/s\n";   
    }
    else
    {
      printout += getString("transport speed") + ": " + String.format("%.1f", (getSpec("Speedmms") / 1000.0) * (1.0 * getSpec("Selected line rate") / getSpec("Maximum line rate"))) + " mm/s\n";       
    }

    if(getSpec("MXCIS") != null)
    {
      printout += getString("chpltol") + "\n";
      printout += getString("Geocor_opt") + "\n";
    }
    else if(getSpec("Resolution") < 3)
    {
      printout += getString("Geometry correction: x and y") + "\n";
    }
    else
    {
      printout += getString("Geometry correction: x") + "\n";
    }

    if(getSpec("MXCIS") != null)
    {
      Double[] dof = new Double[]
      {
        16.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.5, 1.0, 1.0, 0.5, 0.5
      };
      printout += getString("scan distance") + ": ~ 10 mm " + getString("exactseetypesign") + "\n";
      printout += getString("DepthofField") + ": ~ +/- " + dof[dof.length - (getSpec("Resolution") + 1)] + " mm\n" + getString("line width") + ": ~ 1 mm\n";
      printout += getString("case length") + ": ~ " + (getSpec("sw_cp") + 288) + " mm\n";
      if(getSpec("LEDLines") < 2)
      {
        printout += getString("alucase_mxcis") + "\n";
      }
      else
      {
        printout += getString("alucase_mxcis_two") + "\n";
      }
    }
    else if(getSpec("VDCIS") != null)
    {
      Double[] dof = new Double[]
      {
        10.0, 10.0, 10.0, 10.0, 10.0, 5.0, 2.5, 2.5
      };
      printout += getString("scan distance") + ": ~ 55 - 70 mm " + getString("exactresolution") + "\n";
      printout += getString("DepthofField") + ": ~ " + dof[dof.length - (getSpec("Resolution") + 1)] + " mm\n" + getString("line width") + ": ~ 1 mm\n";
      printout += getString("case length") + ": ~ " + (getSpec("sw_cp") + 100) + " mm\n";
      printout += getString("Aluminium case profile: 80x80mm (HxT) with bonded") + "\n";
    }
    else
    {
      printout += getString("scan distance") + ": 9-12 mm " + getString("exactseetypesign") + "\n";
      if(getSpec("VSCIS") != null || getSpec("VTCIS") != null || getSpec("VHCIS") != null)
      {
        String[] dof = new String[]
        {
          "+ 16.0 / -10.0", "+/- 8.0", "+/- 6.0", "+/- 4.0", "+/- 3.0", "+/- 2.0", "+/- 1.5", "+/- 1.0", "+/- 1.0", "+/- 0.5", "+/- 0.25"
        };
        printout += getString("DepthofField") + ": ~ " + dof[dof.length - (getSpec("Resolution") + 1)] + " mm\n" + getString("line width") + ": ~ 1mm\n";
      }
      else
      {
        printout += getString("DepthofField") + ": ~ +/- 0.50 mm\n" + getString("line width") + ": ~ 1mm\n";
      }
      printout += getString("case length") + ": ~ " + (getSpec("sw_cp") + 100) + " mm\n";
      if(getSpec("Internal Light Source") == 3 || getSpec("Internal Light Source") == 4)
      {
        printout += getString("Aluminium case profile: 53x50mm (HxT) with bondedcoax") + "\n";
      }
      else if(getSpec("VTCIS") != null)
      {
        printout += getString("Aluminium case profile: 86x80mm (HxT) with bonded") + "\n";
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

    switch(getSpec("Cooling"))
    {
      case 1:
        if(getSpec("VTCIS") != null)
        {
          printout += getString("lico") + "\n";
          break;
        }

        printout += getString("intforced") + "\n";
        break;
      case 2:
        printout += getString("extforced") + "\n";
        break;
      case 3:
        printout += getString("passair") + "\n";
        break;
      case 4:
        printout += getString("lico") + "\n";
        break;
      default:
        printout += "Unknown\n";
    }
    printout += getString("weight") + ": ~ " + (" " + Math.round((((electSums[3] == null) ? 0.0 : electSums[3]) + ((mechaSums[3] == null) ? 0.0 : mechaSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";
    printout += "Interface: " + (getSpec("Interface") == 0 ? "CameraLink (max. 5m)" : "GigE") + "\n";

    if(getSpec("VDCIS") != null)
    {
      printout += getString("laser") + "\n";
    }

    if(getSpec("MXCIS") != null)
    {
      printout += getString("clbase");
    }

    if(getSpec("Interface") == 0)
    {
      String clCalc = getCLCalc(numOfPix);
      if(clCalc == null)
      {
        return null;
      }

      printout += "\n\t\n";
      printout += clCalc;
    }
    else
    {
      printout += "\n\t\n";
      printout += "Pixel Clock: 40MHz\n";
      printout += getString("numofpix") + numOfPix + "\n";
    }
    return printout;
  }

  private String createBlPrntOut()
  {
    String printout = getTiViKey();
    printout += "\n\t\n";
    printout += getString("suitedfor") + getSpec("sw_cp") + getString("mm CIS scan width") + "\n";

    String color = "";

    if(getSpec("Color") == 3 || ((getSpec("VDCIS") != null && getSpec("MXCIS") != null) && getSpec("Color") == 4))
    {
      color = "RGB";
    }
    else
    {
      switch(getSpec("Internal Light Color"))
      {
        case 0:
          color = getString("Amber (Red)");
          break;
        case 1:
          color = getString("Green");
          break;
        case 2:
          color = getString("Blue");
          break;
        case 3:
          color = getString("Infrared");
          break;
        case 4:
          color = getString("Yellow");
          break;
        case 5:
          color = getString("White");
          break;
        default:
          color = "Unknown";
      }
    }

    printout += getString("Color:") + color + "\n";
    printout += "\n\t\n";
    printout += getString("line width") + ": ~ 1 mm\n";
    printout += getString("case length") + ": ~ " + (getSpec("sw_cp") + 100) + " mm\n";
    printout += getString("Aluminium case profile: 53x50mm (HxT) with bondedmxled") + "\n";
    printout += getString("glass pane, see drawing") + "\n";
    printout += getString("shading") + "\n";
    printout += getString("powersource") + "(24 +/- 1) VDC\n";
    printout += getString("Needed power:") + (((electSums[2] == null) ? 0.0 : (Math.round(10.0 * electSums[2]) / 10.0)) + " A").replace(" 0 A", " ???") + " +/- 20%\n";
    printout += getString("weight") + ": ~ " + (Math.round((((electSums[3] == null) ? 0.0 : electSums[3]) + ((mechaSums[3] == null) ? 0.0 : mechaSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";

    return printout;
  }

  public Double[] getMechaSums()
  {
    return mechaSums;
  }

  public Double[] getElectSums()
  {
    return electSums;
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

      switch(m)
      {
        case "": //No code
        case "FPGA": //Notifier for FPGA parts, ignore and proceed
          proceed = !invert; //!invert ^ invert == true
          break;
        case "RGB": //Color coding
        case "AM":
        case "BL":
        case "GR":
        case "IR":
        case "YE":
        case "WH":
          proceed = getSpec("Internal Light Source") > 0 && (key.split("_")[4].contains(m) || (getSpec("MXCIS") != null && key.split("_")[5].contains(m)));
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
          proceed = m.equals(getSpec("res_cp") + "dpi");
          break;
        case "GIGE": //GigE only
          proceed = getSpec("Interface") != null && getSpec("Interface") == 1;
          break;
        case "CL": //CameraLink only
          proceed = getSpec("Interface") != null && spec.get("Interface") == 0;
          break;
        case "COAX": //At least one coaxial light
          proceed = key.split("_")[4].endsWith("C") || (getSpec("MXCIS") != null && key.split("_")[5].endsWith("C"));
          break;
        case "DIFF":
          proceed = !(key.split("_")[4].endsWith("C") || (getSpec("MXCIS") != null && key.split("_")[5].endsWith("C"))) //No coaxial light
              || (key.split("_")[4].startsWith("2") || (getSpec("MXCIS") != null && key.split("_")[5].startsWith("2"))); //Twosided => at least one diffuse (2XX oder 2XXC)
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
          proceed = getSpec("External Trigger") == 0;
          break;
        case "EXT": //With external trigger
          proceed = getSpec("External Trigger") == 1;
          break;
        case "L": //MODE: LOW (MXCIS only)
          proceed = getSpec("MXCIS") != null && getSpec("MODE") == 4;
          break;
        case "H": //Mode: HIGH (MXCIS only)
          proceed = getSpec("MXCIS") != null && getSpec("MODE") == 2;
          break;
        default: //Unknown modifier
          proceed = invert; //invert ^ invert == false
          break;
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
      String ev = factor.replace("F", "" + numFPGA).replace("S", "" + getSpec("sw_cp") / BASE_LENGTH).replace("N", "" + getSpec("sw_cp")).replace(" ", "").replace("L", "" + getSpec("LEDLines"));
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
      return getSpec("LEDLines");
    }
    else if(factor.contains("=="))
    {
      String[] splitted = factor.split("==");
      if(splitted[0].equals("L") && isInteger(splitted[1]) && getSpec("LEDLines") == Integer.parseInt(splitted[1]))
      {
        return 1;
      }
    }
    else if(factor.contains(">"))
    {
      String[] splitted = factor.split(">");
      if(splitted[0].equals("L") && isInteger(splitted[1]) && getSpec("LEDLines") > Integer.parseInt(splitted[1]))
      {
        return 1;
      }
    }
    else if(factor.contains("<"))
    {
      String[] splitted = factor.split("<");
      if(splitted[0].equals("L") && isInteger(splitted[1]) && getSpec("LEDLines") < Integer.parseInt(splitted[1]))
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
        .append(String.format("%05d", priceRecord.getArtNo().intValue())).append("\t")
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
      HashMap<String, Integer> calcMap = new HashMap<>();
      Files.lines(Launcher.tableHome.resolve(getClass().getSimpleName() + "/Calculation.csv"))
              .map(line -> line.split("\t"))
              .forEach(line ->
              {
                try
                {
                  calcMap.put(line[0], Integer.parseInt(line[1]));
                }
                catch(NumberFormatException ignored)
                {}
              });

      totalOutput.append(getString("calcfor10")).append("\t \t \t \t ").append("\n");
      totalOutput.append(getString("Electronics")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[0])).append("\t \n");
      totalPrices[2] = electSums[0];
      totalOutput.append(getString("Overhead Electronics")).append(" (").append(calcMap.get("A_ELEKTRONIK")).append("%):\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100.0))).append("\t \n");
      totalPrices[2] += electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100.0);
      totalOutput.append(getString("Testing")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[1] * calcMap.get("STUNDENSATZ"))).append("\t \n");
      totalPrices[2] += electSums[1] * calcMap.get("STUNDENSATZ");
      if(getSpec("Interface") != null && getSpec("Interface") == 1)
      {
        totalOutput.append(getString("Overhead GigE")).append(" (").append(calcMap.get("Z_GIGE")).append("%):\t \t \t")
                .append(String.format(getLocale(), "%.2f", electSums[0] * calcMap.get("Z_GIGE") / 100.0)).append("\t \n");
        totalPrices[2] += electSums[0] * (calcMap.get("Z_GIGE") / 100.0);
      }
      totalOutput.append(getString("Mechanics")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", mechaSums[0])).append("\t \n");
      totalPrices[2] += mechaSums[0];
      totalOutput.append(getString("Overhead Mechanics")).append(" (").append(calcMap.get("A_MECHANIK")).append("%):\t \t \t")
              .append(String.format(getLocale(), "%.2f", mechaSums[0] * (calcMap.get("A_MECHANIK") / 100.0))).append("\t \n");
      totalPrices[2] += mechaSums[0] * (calcMap.get("A_MECHANIK") / 100.0);
      totalOutput.append(getString("Assembly")).append(":\t \t ")
              .append(calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (getSpec("sw_cp") / BASE_LENGTH)).append(" h\t")
              .append(String.format(getLocale(), "%.2f", (double) (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (spec.get("sw_cp") / BASE_LENGTH)) * calcMap.get("STUNDENSATZ"))).append("\t \n");
      totalPrices[2] += (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (spec.get("sw_cp") / BASE_LENGTH)) * calcMap.get("STUNDENSATZ");

      int addition = 0;
      double surcharge = 0.0;

      totalPrices[0] = totalPrices[2] * calcMap.get("F_1") / 100.0;
      totalPrices[1] = totalPrices[2] * calcMap.get("F_5") / 100.0;
      totalPrices[2] = totalPrices[2] * calcMap.get("F_10") / 100.0;
      totalPrices[3] = totalPrices[2] * calcMap.get("F_25") / 100.0;
      totalOutput.append(" \t(1 ").append(getString("pc")).append(")\t")
              .append("(5 ").append(getString("pcs")).append(")\t")
              .append("(10 ").append(getString("pcs")).append(")\t")
              .append("(25 ").append(getString("pcs")).append(")\n");

      String format = "%.2f";
      double value = calcMap.get("Z_TRANSPORT") / 100.0;
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

      if(getSpec("MXLED") == null)
      {
        value = calcMap.get(getDpiCode()) / 100.0;
        totalOutput.append(getString("Surcharge DPI/Switchable")).append(" (").append(calcMap.get(getDpiCode())).append("%):\t")
                .append(String.format(getLocale(), format, totalPrices[0] * value)).append("\t")
                .append(String.format(getLocale(), format, totalPrices[1] * value)).append("\t")
                .append(String.format(getLocale(), format, totalPrices[2] * value)).append("\t")
                .append(String.format(getLocale(), format, totalPrices[3] * value)).append("\n");
        surcharge += value;
      }
      else
      {
        String cat = getTiViKey().split("_")[3];
        value = calcMap.get("Z_" + cat) / 100.0;
        totalOutput.append(getString("Surcharge")).append(" ").append(cat).append(" (").append(calcMap.get("Z_" + cat)).append("%):\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[0] * value)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[1] * value)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[2] * value)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[3] * value)).append("\n");
        surcharge += value;
      }

      format = "%.2f";
      value = calcMap.get("LIZENZ");
      totalOutput.append(getString("Licence")).append(":\t")
              .append(String.format(getLocale(), format, value)).append("\t")
              .append(String.format(getLocale(), format, value)).append("\t")
              .append(String.format(getLocale(), format, value)).append("\t")
              .append(String.format(getLocale(), format, value)).append("\n");
      addition += value;

      format = "%.2f";
      value = calcMap.get("Z_DISCONT") / 100.0;
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
    catch(NullPointerException | IndexOutOfBoundsException | NumberFormatException | IOException e)
    {
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
    String key = getTiViKey();

    if(key.split("_")[3].equals("XXXX"))
    {
      return "Z_SWT_DPI";
    }

    return "Z_" + key.split("_")[3] + "_DPI";
  }

  private int calcNumOfPix()
  {
    int numOfPix;
    int sensBoards = getSpec("sw_cp") / BASE_LENGTH;

    if(getSpec("MXCIS") != null)
    {
      numOfPix = ((MXCIS) this).getBoard(getSpec("res_cp"))[0] * (getSpec("sw_cp") / BASE_LENGTH) * ((MXCIS) this).getChip(getSpec("res_cp2"))[3] / getSpec("Binning");
    }
    else if(getSpec("VHCIS") != null)
    {
      numOfPix = (int) (getSensBoard("SMARDOUB")[0] * sensBoards * 0.72 * getSpec("res_cp2"));
    }
    else if(getSpec("VTCIS") != null)
    {
      numOfPix = (int) (getSensBoard("SMARDOUB")[0] * sensBoards * 0.72 * getSpec("res_cp2"));
    }
    else
    {
      numOfPix = (int) (getSensBoard("SMARAGD")[0] * sensBoards * 0.72 * getSpec("res_cp2"));
    }

    if((getSpec("Color") * numOfPix * getSpec("Selected line rate") / 1000000 > 80 && getSpec("Interface") == 1))
    {
      throw new CISException(getString("GIGEERROR") + (getSpec("Color") * numOfPix * getSpec("Selected line rate") / 1000000) + " MByte");
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
            * (coax ? 1 : getSpec("LEDLines"))
            * getGeometry(coax)
            * getSensitivity() / (1.5 * (key.contains("RGB") ? 3 : 1));
  }

  public abstract double getGeometry(boolean coax);

  public double getSensitivity()
  {
    if(getSpec("MXCIS") != null)
    {
      return 30;
    }
    else if(getSpec("VTCIS") != null)
    {
      switch(getSpec("res_cp"))
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
    else if(getSpec("VDCIS") != null)
    {
      switch(getSpec("res_cp"))
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

  // Laden von statischen Texten aus dem Bundle (de/en)
  public String getString(String key)
  {
    return ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", getLocale()).getString(key);
  }
}
