package tivi.cis;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import tivi.cis.mxcis.*;

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

    sensChipTab = readConfigTable("/tivi/cis/sensChips.csv");
    sensBoardTab = readConfigTable("/tivi/cis/sensBoards.csv");
    adcBoardTab = readConfigTable("/tivi/cis/adcBoards.csv");

    spec = new HashMap<>();
  }

  public abstract String getCLCalc(int numOfPix, Locale LANGUAGE);

  public abstract String getTiViKey();

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

    key += "_2.0";

    return key;
  }

  public int countNLs() throws IOException
  {
    BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Mechanics.csv")));
    String line = reader.readLine();
    String[] row = line.split(";");

    int x = 0;
    for(String field : row)
    {
      if(field.matches("NL\\d+"))
      {
        x++;
      }
    }

    if(getSpec("MXLED") != null)
    {
      x /= 2;
    }

    return x;
  }

  public void setLocale(Locale l)
  {
    LANGUAGE = l;
  }

  protected HashMap<String, Integer[]> readConfigTable(String path)
  {
    HashMap<String, Integer[]> map = new HashMap<>();

    try
    {
      BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)));
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

  public void setSpec(String key, int value)
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
    mechaSums = new Double[4];
    electSums = new Double[4];
    totalPrices = new Double[4];
    mechaConfig = new HashMap<>();
    electConfig = new HashMap<>();
    prices = new HashMap<>();
    IDToKey = new HashMap<>();
    Double[] values;
    String line;

    //Reading price list
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/tivi/cis/Prices.csv"))))
    {
      reader.readLine();

      while((line = reader.readLine()) != null)
      {
        int artnum = Integer.parseInt(line.split(";")[0].replace("X", ""));
        values = new Double[Math.max(line.split(";").length - 2, 4)];
        for(int x = 2; x < Math.max(line.split(";").length, 6); x++)
        {
          try
          {
            values[x - 2] = Double.parseDouble(line.split(";")[x].replace(",", "."));
          }
          catch(NumberFormatException | ArrayIndexOutOfBoundsException ex)
          {
            values[x - 2] = null;
          }
        }
        IDToKey.put(artnum, line.split(";")[1]);
        prices.put(artnum, values);
      }
    }
    catch(IOException e)
    {
      throw new CISException("Error in Prices.csv");
    }

    //Elektronics
    int sensPerFpga;

    if(getSpec("res_cp2") != null && getSpec("res_cp2") > 600)
    {
      sensPerFpga = 1;
    }
    else if(getSpec("Color") > 1)
    {
      //FULL (RGB)
      sensPerFpga = 2;
    }
    else if(getSpec("res_cp2") != null && (getSpec("Selected line rate") / 1000.0) <= maxRateForHalfMode.get(getSpec("res_cp2")))
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

    double lengthPerSens = 260.0 * sensPerFpga;
    numFPGA = (int) Math.ceil(getSpec("sw_cp") / lengthPerSens);

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Electronics.csv"))))
    {
      reader.readLine();

      while((line = reader.readLine()) != null)
      {
        String[] row = line.split(";");
        Pattern p = Pattern.compile("^\\d+dpi$");

        if(row[0].equals(""))
        {
          continue;
        }

        int amount = getAmount(row);

        if(row[3].matches("\\d+"))
        {
          amount *= Integer.parseInt(row[3]);
        }
        else if(row[3].contains("=="))
        {
          if(row[3].split("==")[0].equals("L"))
          {
            if(Integer.parseInt(row[3].split("==")[1]) == getSpec("LEDLines"))
            {
              amount *= 1;
            }
            else
            {
              amount *= 0;
            }
          }
          else
          {
            amount *= -1;
          }
        }
        else if(row[3].contains(">"))
        {
          if(row[3].split(">")[0].equals("L"))
          {
            if(getSpec("LEDLines") > Integer.parseInt(row[3].split(">")[1]))
            {
              amount *= 1;
            }
            else
            {
              amount *= 0;
            }
          }
          else
          {
            amount *= -1;
          }
        }
        else if(row[3].contains("<"))
        {
          if(row[3].split("<")[0].equals("L"))
          {
            if(getSpec("LEDLines") < Integer.parseInt(row[3].split("<")[1]))
            {
              amount *= 1;
            }
            else
            {
              amount *= 0;
            }
          }
          else
          {
            amount *= -1;
          }
        }
        else if(row[3].equals("L"))
        {
          amount *= getSpec("LEDLines");
        }
        else
        {
          amount *= -1;
        }

        amount *= (int) MathEval.evaluate(row[4 + getSpec("sw_index")].replace(" ", "").replace("L", "" + getSpec("LEDLines")).replace("F", "" + numFPGA).replace("S", "" + getSpec("sw_cp") / 260).replace("N", "" + getSpec("sw_cp")));

        if(amount > 0)
        {
          electConfig.put(Integer.parseInt(row[1]), amount);

          if(prices.containsKey(Integer.parseInt(row[1])))
          {
            Double[] pricelist = prices.get(Integer.parseInt(row[1]));
            if(pricelist.length < 4)
            {
              System.out.println("Error, missing values");
              pricelist = new Double[]
              {
                0.0, 0.0, 0.0, 0.0
              };
            }

            for(int x = 0; x < electSums.length; x++)
            {
              if(pricelist[x] != null)
              {
                if(electSums[x] == null)
                {
                  electSums[x] = 0.0;
                }
                electSums[x] += pricelist[x] * amount;
              }
            }
          }
        }

        if(row[2].contains("FPGA"))
        {
          numFPGA += amount;
        }
      }

    }
    catch(IOException e)
    {
      throw new CISException("Error in Electronics.csv");
    }

    //Mechanics
    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Mechanics.csv"))))
    {
      reader.readLine();

      while((line = reader.readLine()) != null)
      {
        String[] row = line.split(";");

        if(row[0].equals(""))
        {
          continue;
        }

        if(!row[3 + getSpec("sw_index")].equals(""))
        {
          try
          {
            if(getSpec("MXLED") == null)
            {
              int amount = getAmount(row);

              if(row[3 + countNLs() + getSpec("Internal Light Source")].matches("\\d+"))
              {
                amount *= Integer.parseInt(row[3 + countNLs() + getSpec("Internal Light Source")]);
              }
              else
              {
                amount *= (int) MathEval.evaluate(row[3 + countNLs() + getSpec("Internal Light Source")].replace("F", "" + numFPGA).replace("S", "" + getSpec("sw_cp") / 260).replace("N", "" + getSpec("sw_cp")).replace(" ", "").replace("L", "" + getSpec("LEDLines")));
              }

              if(amount > 0)
              {
                mechaConfig.put(Integer.parseInt(row[3 + getSpec("sw_index")].replace("X", "")), amount);

                if(prices.containsKey(Integer.parseInt(row[3 + getSpec("sw_index")].replace("X", ""))))
                {
                  Double[] pricelist = prices.get(Integer.parseInt(row[3 + getSpec("sw_index")]));

                  for(int x = 0; x < mechaSums.length; x++)
                  {
                    if(pricelist[x] != null)
                    {
                      if(mechaSums[x] == null)
                      {
                        mechaSums[x] = 0.0;
                      }
                      mechaSums[x] += pricelist[x] * amount;
                    }
                  }
                }
              }
            }
            else
            {
              int amount = getAmount(row);

              if(row[3 + countNLs() + getSpec("sw_index")].matches("\\d+"))
              {
                amount *= Integer.parseInt(row[3 + countNLs() + getSpec("sw_index")]);
              }
              else
              {
                amount *= (int) MathEval.evaluate(row[3 + countNLs() + getSpec("sw_index")].replace("F", "" + numFPGA).replace("S", "" + getSpec("sw_cp") / 260).replace("N", "" + getSpec("sw_cp")).replace(" ", "").replace("L", "" + getSpec("LEDLines")));
              }

              if(amount > 0)
              {
                mechaConfig.put(Integer.parseInt(row[3 + getSpec("sw_index")].replace("X", "")), amount);

                if(prices.containsKey(Integer.parseInt(row[3 + getSpec("sw_index")].replace("X", ""))))
                {
                  Double[] pricelist = prices.get(Integer.parseInt(row[3 + getSpec("sw_index")]));

                  for(int x = 0; x < mechaSums.length; x++)
                  {
                    if(pricelist[x] != null)
                    {
                      if(mechaSums[x] == null)
                      {
                        mechaSums[x] = 0.0;
                      }
                      mechaSums[x] += pricelist[x] * amount;
                    }
                  }
                }
              }
            }
          }
          catch(NumberFormatException | IOException e)
          {
            throw new CISException("Error in Mechanics.csv");
          }
        }
      }
    }
    catch(IOException e)
    {
      throw new CISException("Error in Mechanics.csv");
    }

    if(getSpec("MXLED") == null)
    {
      setSpec("numOfPix", calcNumOfPix());
    }

    return true;
  }

  public String getVersion()
  {
    String vString = ResourceBundle.getBundle("tivi.cis.version").getString("BUILD");
    return vString.replaceAll("(\\d)(\\d)$", ".$1.$2");
  }

  public String getVersionHeader()
  {
    return "Version: " + getVersion() + "; " + SimpleDateFormat.getInstance().format(new Date());
  }

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
        factor = 0.25;
      }
      else if(key.contains("K2"))
      {
        factor = 0.5;
      }
      else
      {
        factor = 1;
      }
      
      printout += ", max. " +  getSpec("Maximum line rate") * factor / 1000.0 + " kHz\n";
    }
    else
    {
      printout += ", max. " +  getSpec("Maximum line rate") / 1000.0 + " kHz\n";
    }
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Resolution: ");

    switch(getSpec("Resolution"))
    {
      case 0:
      {
        if(getSpec("VSCIS") != null || getSpec("VTCIS") != null || getSpec("VHCIS") != null)
        {
          printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("binning200") + "\n";
          break;
        }
      }
      default:
      {
        printout += "~ " + getSpec("res_cp2") + "dpi\n";
      }
    }
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("internal light");
    String color = "";

    if(getSpec("Color") == 3 || (getSpec("MXCIS") != null && getSpec("Color") == 4))
    {
      color = "RGB";
    }
    else
    {
      switch(getSpec("Internal Light Color"))
      {
        case 0:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Amber (Red)");
          break;
        }
        case 1:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Green");
          break;
        }
        case 2:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Blue");
          break;
        }
        case 3:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Infrared");
          break;
        }
        case 4:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Yellow");
          break;
        }
        case 5:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("White");
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
          printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("None");
          break;
        }
        case 1:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("onesided");
          break;
        }
        case 2:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("twosided");
          break;
        }
        case 3:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("onepluscoax");
          break;
        }
        case 4:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("coax");
          break;
        }
      }

      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("schipal");

      if(getSpec("res_cp2") > 600)
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("staggered");
      }
      else
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("inline");
      }
    }
    else
    {
      switch(getSpec("Internal Light Source"))
      {
        case 0:
        {
          printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("None");
          break;
        }
        case 1:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("onesided");
          break;
        }
        case 2:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("twosided");
          break;
        }
        case 3:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("twopluscoax");
          break;
        }
        case 4:
        {
          printout += color + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("coax");
          break;
        }
      }
    }

    printout += "\n\n";
    int numOfPix = getSpec("numOfPix");

    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("sellinerate") + Math.round(getSpec("Selected line rate") / 100.0) / 10.0 + " kHz\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("transport speed") + ": " + String.format("%.1f", getSpec("Speedmms") / 1000.0) + " mm/s\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("datarate") + Math.round(getSpec("Color") * numOfPix * getSpec("Selected line rate") / 100000.0) / 10.0 + " MByte\n";

    if(getSpec("MXCIS") != null)
    {
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("chpltol") + "\n";
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Geocor_opt") + "\n";
    }
    else if(getSpec("Resolution") < 3)
    {
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Geometry correction: x and y") + "\n";
    }
    else
    {
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Geometry correction: x") + "\n";
    }

    if(getSpec("MXCIS") != null)
    {
      Double[] dof = new Double[]
      {
        16.0, 8.0, 6.0, 4.0, 3.0, 2.0, 1.5, 1.0, 1.0, 0.5, 0.5
      };
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("scan distance") + ": ~ 10 mm " + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("exactseetypesign") + "\n";
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("DepthofField") + ": ~ +/- " + dof[dof.length - (getSpec("Resolution") + 1)] + " mm\n" + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("line width") + ": ~ 1 mm\n";
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("case length") + ": ~ " + (getSpec("sw_cp") + 288) + " mm\n";
      if(getSpec("LEDLines") < 2)
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("alucase_mxcis") + "\n";
      }
      else
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("alucase_mxcis_two") + "\n";
      }
    }
    else
    {
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("scan distance") + ": 9-12 mm " + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("exactseetypesign") + "\n";
      if(getSpec("VSCIS") != null || getSpec("VTCIS") != null || getSpec("VHCIS") != null)
      {
        String[] dof = new String[]
        {
          "+ 16.0 / -10.0", "+/- 8.0", "+/- 6.0", "+/- 4.0", "+/- 3.0", "+/- 2.0", "+/- 1.5", "+/- 1.0", "+/- 1.0", "+/- 0.5", "+/- 0.25"
        };
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("DepthofField") + ": ~ " + dof[dof.length - (getSpec("Resolution") + 1)] + " mm\n" + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("line width") + ": ~ 1mm\n";
      }
      else
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("DepthofField") + ": ~ +/- 0.50 mm\n" + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("line width") + ": ~ 1mm\n";
      }
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("case length") + ": ~ " + (getSpec("sw_cp") + 100) + " mm\n";
      if(getSpec("Internal Light Source") == 3 || getSpec("Internal Light Source") == 4)
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Aluminium case profile: 53x50mm (HxT) with bondedcoax") + "\n";
      }
      else if(getSpec("VTCIS") != null)
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Aluminium case profile: 80x80mm (HxT) with bonded") + "\n";
      }
      else
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Aluminium case profile: 53x50mm (HxT) with bonded") + "\n";
      }
    }
    printout += "\t" + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("glass pane, see drawing") + "\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("shading") + "\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("powersource") + "(24 +/- 1) VDC\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Needed power:") + (" " + ((electSums[2] == null) ? 0.0 : (Math.round(10.0 * electSums[2]) / 10.0)) + " A").replace(" 0 A", " ???") + " +/- 20%\n";

    switch(getSpec("Cooling"))
    {
      case 1:
      {
        if(getSpec("VTCIS") != null)
        {
          printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("lico") + "\n";
          break;
        }

        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("intforced") + "\n";
        break;
      }
      case 2:
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("extforced") + "\n";
        break;
      }
      case 3:
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("passair") + "\n";
        break;
      }
      case 4:
      {
        printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("lico") + "\n";
        break;
      }
    }
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("weight") + ": ~ " + (" " + Math.round((((electSums[3] == null) ? 0.0 : electSums[3]) + ((mechaSums[3] == null) ? 0.0 : mechaSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";
    printout += "Interface: " + (getSpec("Interface") == 0 ? "CameraLink (max. 5m)" : "GigE");

    if(getSpec("MXCIS") != null)
    {
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("clbase");
    }

    if(getSpec("Interface") == 0)
    {
      if(getCLCalc(numOfPix, getLocale()) == null)
      {
        return null;
      }

      printout += "\n\t\n";
      printout += getCLCalc(numOfPix, getLocale());
    }
    else
    {
      printout += "\n\t\n";
      printout += "Pixel Clock: 40MHz\n";
      printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("numofpix") + numOfPix + "\n";
    }
    return printout;
  }

  private String createBlPrntOut()
  {
    String printout = getTiViKey();
    printout += "\n\t\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("suitedfor") + getSpec("sw_cp") + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("mm CIS scan width") + "\n";

    String color = "";

    if(getSpec("Color") == 3 || (getSpec("MXCIS") != null && getSpec("Color") == 4))
    {
      color = "RGB";
    }
    else
    {
      switch(getSpec("Internal Light Color"))
      {
        case 0:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Amber (Red)");
          break;
        }
        case 1:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Green");
          break;
        }
        case 2:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Blue");
          break;
        }
        case 3:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Infrared");
          break;
        }
        case 4:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Yellow");
          break;
        }
        case 5:
        {
          color = ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("White");
          break;
        }
      }
    }

    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Color:") + color + "\n";
    printout += "\n\t\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("line width") + ": ~ 1 mm\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("case length") + ": ~ " + (getSpec("sw_cp") + 288) + " mm\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Aluminium case profile: 53x50mm (HxT) with bondedmxled") + "\n";
    printout += "\t" + ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("glass pane, see drawing") + "\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("shading") + "\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("powersource") + "(24 +/- 1) VDC\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Needed power:") + (((electSums[2] == null) ? 0.0 : (Math.round(10.0 * electSums[2]) / 10.0)) + " A").replace(" 0 A", " ???") + " +/- 20%\n";
    printout += ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("weight") + ": ~ " + (Math.round((((electSums[3] == null) ? 0.0 : electSums[3]) + ((mechaSums[3] == null) ? 0.0 : mechaSums[3])) * 10) / 10.0 + " kg").replace(" 0 kg", " ???") + "\n";

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
    StringBuilder electOutput = new StringBuilder(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Electronics")).append(":").append("\n\n");
    StringBuilder mechaOutput = new StringBuilder(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Mechanics")).append(":").append("\n\n");
    StringBuilder totalOutput = new StringBuilder(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Totals")).append(":").append("\n\n");

    electOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Component")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Item no.")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Amount")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Price/pc (EUR)")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Weight/pc (kg)")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Time/pc (h)")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Power/pc (A)")).append("\n");

    mechaOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Component")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Item no.")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Amount")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Price/pc (EUR)")).append("\t")
            .append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Weight/pc (kg)")).append("\n");

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
    electOutput.append("\n\t\n").append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Totals")).append("\t")
            .append(" \t")
            .append("0\t")
            .append(String.format(getLocale(), "%.2f", electSums[0])).append("\t")
            .append(String.format(getLocale(), "%.2f", electSums[3])).append("\t")
            .append(String.format(getLocale(), "%.2f", electSums[1])).append("\t")
            .append(String.format(getLocale(), "%.2f", electSums[2])).append("\n");

    getMechaConfig().entrySet().stream().forEach((Map.Entry<Integer, Integer> e)
            -> 
            {
              int ID = e.getKey();
              String key = IDToKey.get(e.getKey());

              mechaOutput.append(key).append("\t")
                      .append(String.format("%05d", ID)).append("\t")
                      .append(e.getValue()).append("\t")
                      .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[0]))).append("\t")
                      .append(String.format(getLocale(), "%.2f", (prices.get(ID) == null ? 0.0 : prices.get(ID)[3]))).append("\n");
    });
    mechaOutput.append("\n\t\n").append(ResourceBundle.getBundle("tivi.cis.Bundle", LANGUAGE).getString("Totals")).append("\t")
            .append(" \t")
            .append("0\t")
            .append(String.format(getLocale(), "%.2f", mechaSums[0])).append("\t")
            .append(String.format(getLocale(), "%.2f", mechaSums[3])).append("\n");

    HashMap<String, Integer> calcMap = new HashMap<>();

    try
    {
      BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Calculation.csv")));
      String line;

      while((line = reader.readLine()) != null)
      {
        String[] calc = line.split(";");
        try
        {
          calcMap.put(calc[0], Integer.parseInt(calc[1]));
        }
        catch(NumberFormatException e)
        {

        }
      }

      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("calcfor10")).append("\t \t \t \t ").append("\n");
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Electronics")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[0])).append("\t \n");
      totalPrices[2] = electSums[0];
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Overhead Electronics")).append(" (").append(calcMap.get("A_ELEKTRONIK")).append("%):\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100.0))).append("\t \n");
      totalPrices[2] += electSums[0] * (calcMap.get("A_ELEKTRONIK") / 100.0);
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Testing")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", electSums[1] * calcMap.get("STUNDENSATZ"))).append("\t \n");
      totalPrices[2] += electSums[1] * calcMap.get("STUNDENSATZ");
      if(getSpec("Interface") != null && getSpec("Interface") == 1)
      {
        totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Overhead GigE")).append(" (").append(calcMap.get("Z_GIGE")).append("%):\t \t \t")
                .append(String.format(getLocale(), "%.2f", electSums[0] * calcMap.get("Z_GIGE") / 100.0)).append("\t \n");
        totalPrices[2] += electSums[0] * (calcMap.get("Z_GIGE") / 100.0);
      }
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Mechanics")).append(":\t \t \t")
              .append(String.format(getLocale(), "%.2f", mechaSums[0])).append("\t \n");
      totalPrices[2] += mechaSums[0];
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Overhead Mechanics")).append(" (").append(calcMap.get("A_MECHANIK")).append("%):\t \t \t")
              .append(String.format(getLocale(), "%.2f", mechaSums[0] * (calcMap.get("A_MECHANIK") / 100.0))).append("\t \n");
      totalPrices[2] += mechaSums[0] * (calcMap.get("A_MECHANIK") / 100.0);
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Assembly")).append(":\t \t ")
              .append(calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * ((int) getSpec("sw_cp") / 260)).append(" h\t")
              .append(String.format(getLocale(), "%.2f", (double) (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (spec.get("sw_cp") / 260)) * calcMap.get("STUNDENSATZ"))).append("\t \n");
      totalPrices[2] += (calcMap.get("MONTAGE_BASIS") + calcMap.get("MONTAGE_PLUS") * (spec.get("sw_cp") / 260)) * calcMap.get("STUNDENSATZ");

      int surcharge = 0;
      int addition = 0;

      totalPrices[0] = totalPrices[2] * calcMap.get("F_1") / 100.0;
      totalPrices[1] = totalPrices[2] * calcMap.get("F_5") / 100.0;
      totalPrices[2] = totalPrices[2] * calcMap.get("F_10") / 100.0;
      totalPrices[3] = totalPrices[2] * calcMap.get("F_25") / 100.0;
      totalOutput.append(" \t(1 ").append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("pc")).append(")\t")
              .append("(5 ").append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("pcs")).append(")\t")
              .append("(10 ").append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("pcs")).append(")\t")
              .append("(25 ").append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("pcs")).append(")\n");
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Price/pc")).append(":\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[0])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[1])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[2])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[3])).append("\n");
      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Surcharge Transport")).append(" (").append(calcMap.get("Z_TRANSPORT")).append("%):\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[0] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[1] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[2] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[3] * calcMap.get("Z_TRANSPORT") / 100.0)).append("\n");
      surcharge += calcMap.get("Z_TRANSPORT");

      if(getSpec("MXLED") == null)
      {
        String dpiCode = getDpiCode();

        totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Surcharge DPI/Switchable")).append(" (").append(calcMap.get(dpiCode)).append("%):\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[0] * calcMap.get(dpiCode) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[1] * calcMap.get(dpiCode) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[2] * calcMap.get(dpiCode) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[3] * calcMap.get(dpiCode) / 100.0)).append("\n");
        surcharge += calcMap.get(dpiCode);
      }

      if(getSpec("MXCIS") != null)
      {
        String cat = getTiViKey().split("_")[4];

        totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Surcharge")).append(" ").append(cat).append(" (").append(calcMap.get("Z_" + cat)).append("%):\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[0] * calcMap.get("Z_" + cat) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[1] * calcMap.get("Z_" + cat) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[2] * calcMap.get("Z_" + cat) / 100.0)).append("\t")
                .append(String.format(getLocale(), "%.2f", totalPrices[3] * calcMap.get("Z_" + cat) / 100.0)).append("\n");
        surcharge += calcMap.get("Z_" + cat);
      }

      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Licence")).append(":\t")
              .append(String.format(getLocale(), "%.2f", (double) calcMap.get("LIZENZ"))).append("\t")
              .append(String.format(getLocale(), "%.2f", (double) calcMap.get("LIZENZ"))).append("\t")
              .append(String.format(getLocale(), "%.2f", (double) calcMap.get("LIZENZ"))).append("\t")
              .append(String.format(getLocale(), "%.2f", (double) calcMap.get("LIZENZ"))).append("\n");
      addition += calcMap.get("LIZENZ");

      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Discount Surcharge")).append(" (").append(calcMap.get("Z_DISCONT")).append("%):\t")
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

      totalOutput.append(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("Totals")).append(" (EUR):").append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[0])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[1])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[2])).append("\t")
              .append(String.format(getLocale(), "%.2f", totalPrices[3])).append("\n");
    }
    catch(NullPointerException | IndexOutOfBoundsException | NumberFormatException | IOException e)
    {
      throw new CISException(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("A fatal error occurred: Missing configuration tables.Please contact support@tichawa.de for further help."));
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
      numOfPix = (int) (((MXCIS) this).getBoard(getSpec("res_cp"))[0] * (getSpec("sw_cp") / 260.0) * ((MXCIS) this).getChip(getSpec("res_cp2"))[3] / getSpec("Binning"));
    }
    else if(getSpec("VHCIS") != null)
    {
      numOfPix = (int) (getSensBoard("SMARDOUB")[0] * (getSpec("sw_cp") / 260.0) * 0.72 * getSpec("res_cp2"));
    }
    else if(getSpec("VTCIS") != null)
    {
      numOfPix = (int) (getSensBoard("SMARDOUB")[0] * (getSpec("sw_cp") / 260.0) * 0.72 * getSpec("res_cp2"));
    }
    else
    {
      numOfPix = (int) (getSensBoard("SMARAGD")[0] * (getSpec("sw_cp") / 260.0) * 0.72 * getSpec("res_cp2"));
    }

    if((getSpec("Color") * numOfPix * getSpec("Selected line rate") / 1000000 > 80 && getSpec("Interface") == 1))
    {
      throw new CISException(ResourceBundle.getBundle("tivi.cis.Bundle", getLocale()).getString("GIGEERROR") + (getSpec("Color") * numOfPix * getSpec("Selected line rate") / 1000000) + " MByte");
    }

    return numOfPix;
  }

  public int getNumFPGA()
  {
    return numFPGA;
  }
}
