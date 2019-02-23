package de.tichawa.cis.config;

import de.tichawa.util.MathEval;
import de.tichawa.cis.config.mxcis.MXCIS;
import de.tichawa.util.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

public abstract class CIS
{

  public final String CIS_NAME;

  protected HashMap<String, Integer> spec;
  protected HashMap<String, Integer[]> sensChipTab;
  protected HashMap<String, Integer[]> sensBoardTab;
  protected HashMap<String, Integer[]> adcBoardTab;
  protected HashMap<Integer, Integer> electConfig;
  protected HashMap<Integer, Integer> mechaConfig;
  protected HashMap<Integer, Double[]> prices;
  protected HashMap<Integer, String> IDToKey;
  protected Double[] electSums;
  protected Double[] mechaSums;
  protected Double[] totalPrices;
  protected final HashMap<Integer, Integer> maxRateForHalfMode;
  protected int numFPGA;
  protected static Locale LANGUAGE = Locale.getDefault();

  public static final String[] COLORCODE = new String[]
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
    CIS_NAME = fullName.substring(fullName.lastIndexOf(".") + 1);

    sensChipTab = readConfigTable("/de/tichawa/cis/config/sensChips.csv");
    sensBoardTab = readConfigTable("/de/tichawa/cis/config/sensBoards.csv");
    adcBoardTab = readConfigTable("/de/tichawa/cis/config/adcBoards.csv");

    spec = new HashMap<>();
  }

  public abstract String getCLCalc(int numOfPix);

  public abstract String getTiViKey();

  public double getBaseLength()
  {
    return 260;
  }

  public String getBlKey()
  {
    String key = "G_MXLED";
    key += String.format("_%04d", getSpec("sw_cp"));

    switch(getSpec("Internal Light Source"))
    {
      case 0:
      {
        key += "_NO";
        break;
      }
      case 1:
      {
        key += "_" + COLORCODE[getSpec("Internal Light Color")];
        break;
      }
      case 2:
      {
        key += "_2" + COLORCODE[getSpec("Internal Light Color")];
        break;
      }
      case 3:
      {
        key += "_2" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
      case 4:
      {
        key += "_" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
    }

    if(getSpec("Color") == 4)
    {
      key = key.replace(COLORCODE[getSpec("Internal Light Color")], "RGB");
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

  public int countNLs()
  {
    try
    {
      int x = (int) Files.lines(Launcher.tableHome.resolve(getClass().getSimpleName() + "/Mechanics.csv"))
              .limit(1)
              .map(line -> line.split("\t"))
              .flatMap(line -> Arrays.stream(line))
              .filter(field -> field.startsWith("NL"))
              .count();

      if(getSpec("MXLED") != null)
      {
        x /= 2;
      }
      return x;
    }
    catch(IOException ex)
    {
      return -1;
    }
  }

  public void setLocale(Locale l)
  {
    LANGUAGE = l;
  }

  protected final HashMap<String, Integer[]> readConfigTable(String path)
  {
    HashMap<String, Integer[]> map = new HashMap<>();

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path), Charset.forName("UTF-8"))))
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

  public boolean calculate()
  {
    mechaSums = new Double[5];
    electSums = new Double[5];
    totalPrices = new Double[4];
    mechaConfig = new HashMap<>();
    electConfig = new HashMap<>();
    prices = new HashMap<>();
    IDToKey = new HashMap<>();

    try
    {
      Files.lines(Launcher.tableHome.resolve("Prices.csv"))
              .map(line -> line.split("\t"))
              .filter(line -> isInteger(line[0]))
              .forEach(line ->
              {
                int artnum = Integer.parseInt(line[0].replace("X", ""));
                Double[] values = new Double[Math.max(line.length - 2, 5)];
                for(int x = 2; x < Math.max(line.length, 6); x++)
                {
                  try
                  {
                    values[x - 2] = Double.parseDouble(line[x]);
                  }
                  catch(NumberFormatException | ArrayIndexOutOfBoundsException ex)
                  {
                    values[x - 2] = null;
                  }
                }
                IDToKey.put(artnum, line[1]);
                prices.put(artnum, values);
              });
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
      throw new CISException("Error in Prices.csv");
    }

    //Elektronics
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

    Pattern p = Pattern.compile("^\\d+dpi$");
    double lengthPerSens = getBaseLength() * sensPerFpga;
    numFPGA = (int) Math.ceil(getSpec("sw_cp") / lengthPerSens);

    try
    {
      Files.lines(Launcher.tableHome.resolve(getClass().getSimpleName() + "/Electronics.csv"))
              .skip(1)
              .map(line -> line.split("\t"))
              .filter(line -> line[0].length() > 0)
              .map(line -> new Tuple<>(line, (int) (getAmount(line) * getElectFactor(line[3])
              * MathEval.evaluate(line[4 + getSpec("sw_index")].replace(" ", "").replace("L", "" + getSpec("LEDLines")).replace("F", "" + numFPGA).replace("S", "" + getSpec("sw_cp") / getBaseLength()).replace("N", "" + getSpec("sw_cp"))))))
              .filter(data -> data.getV() > 0)
              .forEach(data ->
              {
                String[] line = data.getU();
                int amount = data.getV();
                electConfig.put(Integer.parseInt(line[1]), amount);

                if(prices.containsKey(Integer.parseInt(line[1])))
                {
                  Double[] pricelist = prices.get(Integer.parseInt(line[1]));
                  if(pricelist.length < 4)
                  {
                    System.out.println("Error, missing values");
                    pricelist = new Double[]
                    {
                      0.0, 0.0, 0.0, 0.0, 1.0
                    };
                  }

                  for(int x = 0; x < electSums.length; x++)
                  {
                    if(pricelist[x] != null && pricelist[x] != 0)
                    {
                      if(x < 4)
                      {
                        if(electSums[x] == null)
                        {
                          electSums[x] = 0.0;
                        }
                        electSums[x] += pricelist[x] * amount;
                      }
                      else
                      {
                        if(electSums[x] == null)
                        {
                          electSums[x] = 100.0;
                        }
                        electSums[x] = Math.min(electSums[x], pricelist[x]);
                      }
                    }
                  }
                }

                if(line[2].contains("FPGA"))
                {
                  numFPGA += amount;
                }
              });
    }
    catch(IOException e)
    {
      throw new CISException("Error in Electronics.csv");
    }

    if(electSums[4] == null)
    {
      electSums[4] = 1.0;
    }

    //Mechanics
    try
    {
      int index = getSpec("MXLED") == null ? getSpec("Internal Light Source") : getSpec("sw_index");
      Files.lines(Launcher.tableHome.resolve(getClass().getSimpleName() + "/Mechanics.csv"))
              .skip(1)
              .map(line -> line.split("\t"))
              .filter(line -> line[0].length() > 0)
              .filter(line -> line[3 + getSpec("sw_index")].length() > 0)
              .map(line -> {
                int amount = getAmount(line);
                int mechaFactor = getMechaFactor(line[3 + countNLs() + index]);
                return new Tuple<>(line, getAmount(line) * getMechaFactor(line[3 + countNLs() + index]));
                        })
              .filter(data -> data.getV() > 0)
              .forEach(data ->
              {
                String[] line = data.getU();
                int amount = data.getV();
                mechaConfig.put(Integer.parseInt(line[3 + getSpec("sw_index")].replace("X", "")), amount);

                if(prices.containsKey(Integer.parseInt(line[3 + getSpec("sw_index")].replace("X", ""))))
                {
                  Double[] pricelist = prices.get(Integer.parseInt(line[3 + getSpec("sw_index")]));

                  for(int x = 0; x < mechaSums.length; x++)
                  {
                    if(pricelist[x] != null && pricelist[x] != 0)
                    {
                      if(x < 4)
                      {
                        if(mechaSums[x] == null)
                        {
                          mechaSums[x] = 0.0;
                        }
                        mechaSums[x] += pricelist[x] * amount;
                      }
                      else
                      {
                        if(mechaSums[x] == null)
                        {
                          mechaSums[x] = 1.0;
                        }
                        mechaSums[x] *= pricelist[x];
                      }
                    }
                  }
                }
              });
    }
    catch(IOException e)
    {
      throw new CISException("Error in Mechanics.csv");
    }

    if(mechaSums[4] == null)
    {
      mechaSums[4] = 1.0;
    }

    if(getSpec("MXLED") == null)
    {
      setSpec("numOfPix", calcNumOfPix());
    }

    return true;
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

    switch(getSpec("Resolution"))
    {
      case 0:
      {
        if(getSpec("VSCIS") != null || getSpec("VTCIS") != null || getSpec("VHCIS") != null)
        {
          printout += getString("binning200") + "\n";
          break;
        }
      }
      default:
      {
        printout += "~ " + getSpec("res_cp2") + "dpi\n";
      }
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
        {
          color = getString("Amber (Red)");
          break;
        }
        case 1:
        {
          color = getString("Green");
          break;
        }
        case 2:
        {
          color = getString("Blue");
          break;
        }
        case 3:
        {
          color = getString("Infrared");
          break;
        }
        case 4:
        {
          color = getString("Yellow");
          break;
        }
        case 5:
        {
          color = getString("White");
          break;
        }
      }
    }

    if(getSpec("MXCIS") != null)
    {
      switch(getSpec("Internal Light Source"))
      {
        case 0:
        {
          printout += getString("None");
          break;
        }
        case 1:
        {
          printout += color + getString("onesided");
          break;
        }
        case 2:
        {
          printout += color + getString("coax");
          break;
        }
        case 3:
        {
          printout += color + getString("twosided");
          break;
        }
        case 4:
        {
          printout += color + getString("onepluscoax");
          break;
        }
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
        {
          printout += getString("None");
          break;
        }
        case 1:
        {
          printout += color + getString("onesided");
          break;
        }
        case 2:
        {
          printout += color + getString("twosided");
          break;
        }
        case 3:
        {
          printout += color + getString("twopluscoax");
          break;
        }
        case 4:
        {
          printout += color + getString("coax");
          break;
        }
      }
    }

    printout += "\n\n";
    int numOfPix = getSpec("numOfPix");

    printout += getString("sellinerate") + Math.round(getSpec("Selected line rate") / 100.0) / 10.0 + " kHz\n";
    printout += getString("transport speed") + ": " + String.format("%.1f", (getSpec("Speedmms") / 1000.0) * (1.0 * getSpec("Selected line rate") / getSpec("Maximum line rate"))) + " mm/s\n";

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
      printout += getString("glass pane, see drawing") + "\n";
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
        printout += getString("Aluminium case profile: 80x80mm (HxT) with bonded") + "\n";
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
    printout += "Grenzfrequenz: " + Math.round(1000 * getMinFreq(getTiViKey())) / 1000 + " kHz\n";

    switch(getSpec("Cooling"))
    {
      case 1:
      {
        if(getSpec("VTCIS") != null)
        {
          printout += getString("lico") + "\n";
          break;
        }

        printout += getString("intforced") + "\n";
        break;
      }
      case 2:
      {
        printout += getString("extforced") + "\n";
        break;
      }
      case 3:
      {
        printout += getString("passair") + "\n";
        break;
      }
      case 4:
      {
        printout += getString("lico") + "\n";
        break;
      }
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
        {
          color = getString("Amber (Red)");
          break;
        }
        case 1:
        {
          color = getString("Green");
          break;
        }
        case 2:
        {
          color = getString("Blue");
          break;
        }
        case 3:
        {
          color = getString("Infrared");
          break;
        }
        case 4:
        {
          color = getString("Yellow");
          break;
        }
        case 5:
        {
          color = getString("White");
          break;
        }
      }
    }

    printout += getString("Color:") + color + "\n";
    printout += "\n\t\n";
    printout += getString("line width") + ": ~ 1 mm\n";
    printout += getString("case length") + ": ~ " + (getSpec("sw_cp") + 288) + " mm\n";
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

  private int getAmount(String[] row)
  {
    boolean proceed;
    String key = getTiViKey();
    String[] multiplier = row[2].split("&");

    boolean invert;
    for(String m : multiplier)
    {
      invert = m.startsWith("!");
      m = (invert ? m.substring(1) : m);

      switch(m)
      {
        case "": //No code
        case "FPGA": //Notifier for FPGA parts, ignore and proceed
        {
          proceed = !invert; //!invert ^ invert == true
          break;
        }
        case "RGB": //Color coding
        case "AM":
        case "BL":
        case "GR":
        case "IR":
        case "YE":
        case "WH":
        {
          proceed = getSpec("Internal Light Source") > 0 && (key.split("_")[4].contains(m) || (getSpec("MXCIS") != null && key.split("_")[5].contains(m)));
          break;
        }
        case "MONO": //Monochrome only
        {
          proceed = !getTiViKey().contains("RGB");
          break;
        }
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
        {
          proceed = m.equals(getSpec("res_cp") + "dpi");
          break;
        }
        case "GIGE": //GigE only
        {
          proceed = getSpec("Interface") != null && getSpec("Interface") == 1;
          break;
        }
        case "CL": //CameraLink only
        {
          proceed = getSpec("Interface") != null && spec.get("Interface") == 0;
          break;
        }
        case "COAX": //At least one coaxial light
        {
          proceed = key.split("_")[4].endsWith("C") || (getSpec("MXCIS") != null && key.split("_")[5].endsWith("C"));
          break;
        }
        case "DIFF":
        {
          proceed = !(key.split("_")[4].endsWith("C") || (getSpec("MXCIS") != null && key.split("_")[5].endsWith("C"))) //No coaxial light
                  || (key.split("_")[4].startsWith("2") || (getSpec("MXCIS") != null && key.split("_")[5].startsWith("2"))); //Twosided => at least one diffuse (2XX oder 2XXC)
          break;
        }
        case "NOCO": //Specific cooling
        case "FAIR":
        case "PAIR":
        case "LICO":
        {
          proceed = key.contains(m);
          break;
        }
        case "default": //Default cooling
        {
          proceed = !key.contains("NOCO") && !key.contains("FAIR") && !key.contains("PAIR") && !key.contains("LICO");
          break;
        }
        case "NOEXT": //No external trigger
        {
          proceed = getSpec("External Trigger") == 0;
          break;
        }
        case "EXT": //With external trigger
        {
          proceed = getSpec("External Trigger") == 1;
          break;
        }
        case "L": //MODE: LOW (MXCIS only)
        {
          proceed = getSpec("MXCIS") != null && getSpec("MODE") == 4;
          break;
        }
        case "H": //Mode: HIGH (MXCIS only)
        {
          proceed = getSpec("MXCIS") != null && getSpec("MODE") == 2;
          break;
        }
        default: //Unknown modifier
        {
          proceed = invert; //invert ^ invert == false
          break;
        }
      }

      proceed = invert ^ proceed;

      if(!proceed)
      {
        return 0;
      }
    }

    return 1;
  }

  private int getMechaFactor(String factor)
  {
    if(isInteger(factor))
    {
      return Integer.parseInt(factor);
    }
    else
    {
      String ev = factor.replace("F", "" + numFPGA).replace("S", "" + getSpec("sw_cp") / getBaseLength()).replace("N", "" + getSpec("sw_cp")).replace(" ", "").replace("L", "" + getSpec("LEDLines"));
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

  public HashMap<Integer, Integer> getMechaConfig()
  {
    return mechaConfig;
  }

  public HashMap<Integer, Integer> getElectConfig()
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

    getElectConfig().entrySet().stream().forEach((Map.Entry<Integer, Integer> e)
            ->
    {
      int ID = e.getKey();
      String key = IDToKey.get(e.getKey());

      electOutput.append(key).append("\t")
              .append(String.format("%05d", ID)).append("\t")
              .append(e.getValue()).append("\t")
              .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[0]))).append("\t")
              .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[3]))).append("\t")
              .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[1]))).append("\t")
              .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[2]))).append("\n");
    });
    electOutput.append("\n\t\n").append(getString("Totals")).append("\t")
            .append(" \t")
            .append("0\t")
            .append(String.format(getLocale(), "%.2f", electSums[0])).append("\t")
            .append(String.format(getLocale(), "%.2f", electSums[3])).append("\t")
            .append(String.format(getLocale(), "%.2f", electSums[1])).append("\t")
            .append(String.format(getLocale(), "%.2f", electSums[2] == null ? 0.0 : electSums[2])).append("\n");

    getMechaConfig().entrySet().stream().forEach((Map.Entry<Integer, Integer> e) ->
    {
      int ID = e.getKey();
      String key = IDToKey.get(e.getKey());

      mechaOutput.append(key).append("\t")
              .append(String.format("%05d", ID)).append("\t")
              .append(e.getValue()).append("\t")
              .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[0]))).append("\t")
              .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[3]))).append("\n");
    });
    mechaOutput.append("\n\t\n").append(getString("Totals")).append("\t")
            .append(" \t")
            .append("0\t")
            .append(String.format(getLocale(), "%.2f", mechaSums[0])).append("\t")
            .append(String.format(getLocale(), "%.2f", mechaSums[3] == null ? 0.0 : mechaSums[3])).append("\n");

    HashMap<String, Integer> calcMap = new HashMap<>();

    try
    {
      Files.lines(Launcher.tableHome.resolve(getClass().getSimpleName() + "/Calculation.csv"))
              .map(line -> line.split("\t"))
              .forEach(line ->
              {
                try
                {
                  calcMap.put(line[0], Integer.parseInt(line[1]));
                }
                catch(NumberFormatException e)
                {
                }
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
              .append(calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (getSpec("sw_cp") / getBaseLength())).append(" h\t")
              .append(String.format(getLocale(), "%.2f", (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (spec.get("sw_cp") / getBaseLength())) * calcMap.get("STUNDENSATZ"))).append("\t \n");
      totalPrices[2] += (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (spec.get("sw_cp") / getBaseLength())) * calcMap.get("STUNDENSATZ");

      int surcharge = 0;
      int addition = 0;

      totalPrices[0] = totalPrices[2] * calcMap.get("F_1") / 100.0;
      totalPrices[1] = totalPrices[2] * calcMap.get("F_5") / 100.0;
      totalPrices[2] = totalPrices[2] * calcMap.get("F_10") / 100.0;
      totalPrices[3] = totalPrices[2] * calcMap.get("F_25") / 100.0;
      totalOutput.append(" \t(1 ").append(getString("pc")).append(")\t")
              .append("(5 ").append(getString("pcs")).append(")\t")
              .append("(10 ").append(getString("pcs")).append(")\t")
              .append("(25 ").append(getString("pcs")).append(")\n");
      totalOutput.append(getString("Price/pc")).append(":\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[0])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[1])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[2])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[3])).append("\n");
      totalOutput.append(getString("Surcharge Transport")).append(" (").append(calcMap.get("Z_TRANSPORT")).append("%):\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[0] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[1] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[2] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[3] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\n");
      surcharge += calcMap.get("Z_TRANSPORT");

      if(getSpec("MXLED") == null)
      {
        String dpiCode = getDpiCode();

        totalOutput.append(getString("Surcharge DPI/Switchable")).append(" (").append(calcMap.get(dpiCode)).append("%):\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[0] * calcMap.get(dpiCode) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[1] * calcMap.get(dpiCode) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[2] * calcMap.get(dpiCode) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[3] * calcMap.get(dpiCode) / 100.0)).append("\n");
        surcharge += calcMap.get(dpiCode);
      }

      if(getSpec("MXCIS") != null)
      {
        String cat = getTiViKey().split("_")[4];

        totalOutput.append(getString("Surcharge")).append(" ").append(cat).append(" (").append(calcMap.get("Z_" + cat)).append("%):\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[0] * calcMap.get("Z_" + cat) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[1] * calcMap.get("Z_" + cat) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[2] * calcMap.get("Z_" + cat) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[3] * calcMap.get("Z_" + cat) / 100.0)).append("\n");
        surcharge += calcMap.get("Z_" + cat);
      }

      totalOutput.append(getString("Licence")).append(":\t")
              .append(String.format(getLocale(), "%d,00", calcMap.get("LIZENZ"))).append("\t")
              .append(String.format(getLocale(), "%d,00", calcMap.get("LIZENZ"))).append("\t")
              .append(String.format(getLocale(), "%d,00", calcMap.get("LIZENZ"))).append("\t")
              .append(String.format(getLocale(), "%d,00", calcMap.get("LIZENZ"))).append("\n");
      addition += calcMap.get("LIZENZ");

      totalOutput.append(getString("Discount Surcharge")).append(" (").append(calcMap.get("Z_DISCONT")).append("%):\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[0] * calcMap.get("Z_DISCONT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[1] * calcMap.get("Z_DISCONT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[2] * calcMap.get("Z_DISCONT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[3] * calcMap.get("Z_DISCONT") / 100.0)).append("\n");
      surcharge += calcMap.get("Z_DISCONT");

      totalPrices[0] *= (surcharge + 100) / 100.0;
      totalPrices[1] *= (surcharge + 100) / 100.0;
      totalPrices[2] *= (surcharge + 100) / 100.0;
      totalPrices[3] *= (surcharge + 100) / 100.0;

      totalPrices[0] += addition;
      totalPrices[1] += addition;
      totalPrices[2] += addition;
      totalPrices[3] += addition;

      totalOutput.append(getString("Totals")).append(" (EUR):").append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[0])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[1])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[2])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[3])).append("\n");
    }
    catch(NullPointerException | IndexOutOfBoundsException | NumberFormatException | IOException e)
    {
      throw new CISException(getString("A fatal error occurred: Missing configuration tables.Please contact support@tichawa.de for further help."));
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
    return LANGUAGE;
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

  public String getKey(int ID)
  {
    return IDToKey.get(ID);
  }

  private int calcNumOfPix()
  {
    int numOfPix;

    if(getSpec("MXCIS") != null)
    {
      numOfPix = (int) (((MXCIS) this).getBoard(getSpec("res_cp"))[0] * (getSpec("sw_cp") / getBaseLength()) * ((MXCIS) this).getChip(getSpec("res_cp2"))[3] / getSpec("Binning"));
    }
    else if(getSpec("VHCIS") != null)
    {
      numOfPix = (int) (getSensBoard("SMARDOUB")[0] * (getSpec("sw_cp") / getBaseLength()) * 0.72 * getSpec("res_cp2"));
    }
    else if(getSpec("VTCIS") != null)
    {
      numOfPix = (int) (getSensBoard("SMARDOUB")[0] * (getSpec("sw_cp") / getBaseLength()) * 0.72 * getSpec("res_cp2"));
    }
    else
    {
      numOfPix = (int) (getSensBoard("SMARAGD")[0] * (getSpec("sw_cp") / getBaseLength()) * 0.72 * getSpec("res_cp2"));
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
    System.out.println(100 + " gamma " + getGeometry(coax) + " n " + (coax ? 1 : getSpec("LEDLines")) + " I " + electSums[4] + " t " + mechaSums[4] + " Sv " + getSensitivity() + " / m " + 1.5);
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

  public String getString(String key)
  {
    return ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", getLocale()).getString(key);
  }
}
