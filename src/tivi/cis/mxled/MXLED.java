package tivi.cis.mxled;

import java.io.*;
import java.util.*;
import tivi.cis.*;

public class MXLED extends CIS
{

  public MXLED()
  {
    super();

    setSpec("MXLED", 1);
  }

  public static int getSWIndex(int length)
  {
    ArrayList<Integer> list = new ArrayList<>();
    list.add(260);
    list.add(390);
    list.add(520);
    list.add(650);
    list.add(780);
    list.add(910);
    list.add(1040);
    list.add(1300);
    list.add(1560);
    list.add(1820);
    list.add(2080);
    list.add(2340);
    list.add(2600);
    list.add(2860);
    list.add(3120);
    list.add(3380);
    list.add(3640);
    list.add(3900);
    list.add(4160);

    return list.indexOf(length);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_MXLED";
    key += String.format("_%04d", getSpec("sw_cp"));
    key += "_K1";

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
        key += "_3" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
      case 4:
      {
        key += "_" + COLORCODE[getSpec("Internal Light Color")] + "C";
        break;
      }
    }

    if(getSpec("Color") == 3 || getSpec("Color") == 4)
    {
      key = key.replace(COLORCODE[getSpec("Internal Light Color")], "RGB");
    }

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("Calculation.csv"))))
    {
      String line;
      Map<String, String> calcMap = new HashMap<>();

      while((line = reader.readLine()) != null)
      {
        String[] calc = line.split(";");
        calcMap.put(calc[0], calc[1]);
      }

      key += calcMap.containsKey("VERSION") ? "_" + calcMap.get("VERSION") + "_" : "_2.0_";
    }
    catch(IOException e)
    {
      key += "_2.0_";
    }

    return key;
  }

  @Override
  public String getCLCalc(int numOfPix, Locale LANGUAGE)
  {
    return " ";
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 1 : 1;
  }
}
