package de.tichawa.cis.config.vhcis;

import de.tichawa.cis.config.*;
import java.io.*;
import java.nio.file.*;

public class VHCIS extends CIS
{

  public VHCIS()
  {
    super();

    setSpec("VHCIS", 1);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VHCIS";
    key += String.format("_%04d", getSpec("sw_cp"));

    if(getSpec("Resolution") == 0) //Switchable
    {
      key += "_XXXX";
    }
    else
    {
      key += String.format("_%04d", getSpec("res_cp2"));
    }

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
    }

    if(getSpec("Color") == 3)
    {
      key = key.replace(COLORCODE[getSpec("Internal Light Color")], "RGB");
    }

    key += getMechaVersion();

    switch(getSpec("Cooling"))
    {
      case 0:
      {
        key += "NOCO";
        break;
      }
      case 1:
      {
        break;
      }
      case 2:
      {
        key += "FAIR";
        break;
      }
      case 3:
      {
        key += "PAIR";
        break;
      }
      case 4:
      {
        key += "LICO";
        break;
      }
    }

    if(key.endsWith("_"))
    {
      key = key.substring(0, key.length() - 1);
    }

    return key;
  }

  @Override
  public String getCLCalc(int numOfPix)
  {
    int numOfPixNominal;
    int taps;
    int pixPerTap;
    int lval;
    int tcounter = 0;
    StringBuilder printOut = new StringBuilder();

    numOfPixNominal = (int) (numOfPix - ((getSpec("sw_cp") / getBaseLength()) * getSensBoard("SMARDOUB")[7] / (1200 / getSpec("res_cp2"))));
    taps = (int) Math.ceil(1.01 * (numOfPixNominal * getSpec("Maximum line rate") / 1000000) / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    printOut.append(getString("datarate")).append(Math.round(getSpec("Color") * numOfPix * getSpec("Selected line rate") / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append("%%%%%\n");
    printOut.append(getString("numofport")).append(taps * getSpec("Color")).append("\n");
    printOut.append("Pixel Clock: 85 MHz\n");
    printOut.append("Nominal pixel count: ").append(numOfPixNominal).append("\n");
    printOut.append("\n\nCamera Link 1:");

    switch(taps)
    {
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      {
        tcounter++;
        for(int x = 0; x < Math.min(3, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 3)
        {
          tcounter++;
          printOut.append("\nCamera Link 2:");
          for(int x = 3; x < taps; x++)
          {
            printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }
        }
        break;
      }
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
      {

        for(int x = 0; x < Math.min(3, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 3)
        {
          tcounter++;
          printOut.append("\nCamera Link 2:");
          for(int x = 3; x < Math.min(8, taps); x++)
          {
            printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }

          if(taps > 8)
          {
            tcounter++;
            printOut.append("\nCamera Link 3:");
            for(int x = 8; x < Math.min(11, taps); x++)
            {
              printOut.append("\n   Port ").append(getPortName(x))
                      .append(": ").append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
            }

            if(taps > 11)
            {
              tcounter++;
              printOut.append("\nCamera Link 4:");
              for(int x = 11; x < taps; x++)
              {
                printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                        .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
              }
            }
          }
        }
        break;
      }
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      case 33:
      case 34:
      case 35:
      case 36:
      case 37:
      case 38:
      case 39:
      case 40:
      {

        for(int x = 0; x < Math.min(3, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 3)
        {
          tcounter++;
          printOut.append("\nCamera Link 2:");
          for(int x = 3; x < Math.min(10, taps); x++)
          {
            printOut.append("\n   Port ").append(getPortName(x)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }

          if(taps > 10)
          {
            tcounter++;
            printOut.append("\nCamera Link 3:");
            for(int x = 10; x < Math.min(13, taps); x++)
            {
              printOut.append("\n   Port ").append(getPortName(x - 10)).append(": ")
                      .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
            }

            if(taps > 13)
            {
              tcounter++;
              printOut.append("\nCamera Link 4:");
              for(int x = 13; x < Math.min(20, taps); x++)
              {
                printOut.append("\n   Port ").append(getPortName(x - 10)).append(": ")
                        .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
              }
            }
          }
        }
        break;
      }
    }

    if(taps > 20)
    {
      printOut.append("\nCamera Link 5:");
    }

    switch(taps)
    {
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      {
        tcounter++;
        for(int x = 20; x < Math.min(23, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 23)
        {
          tcounter++;
          printOut.append("\nCamera Link 6:");
          for(int x = 23; x < taps; x++)
          {
            printOut.append("\n   Port ").append(getPortName(x - 20))
                    .append(": ").append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }
        }
        break;
      }
      case 31:
      case 32:
      case 33:
      case 34:
      case 35:
      case 36:
      {

        for(int x = 20; x < Math.min(23, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 23)
        {
          tcounter++;
          printOut.append("\nCamera Link 6:");
          for(int x = 23; x < Math.min(28, taps); x++)
          {
            printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }

          if(taps > 28)
          {
            tcounter++;
            printOut.append("\nCamera Link 3:");
            for(int x = 28; x < Math.min(31, taps); x++)
            {
              printOut.append("\n   Port ").append(getPortName(x - 28)).append(": ")
                      .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
            }

            if(taps > 31)
            {
              tcounter++;
              printOut.append("\nCamera Link 4:");
              for(int x = 31; x < taps; x++)
              {
                printOut.append("\n   Port ").append(getPortName(x - 28)).append(": ")
                        .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
              }
            }
          }
        }
        break;
      }
      case 37:
      case 38:
      case 39:
      case 40:
      {

        for(int x = 20; x < Math.min(23, taps); x++)
        {
          printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                  .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
        }

        if(taps > 23)
        {
          tcounter++;
          printOut.append("\nCamera Link 2:");
          for(int x = 23; x < Math.min(10, taps); x++)
          {
            printOut.append("\n   Port ").append(getPortName(x - 20)).append(": ")
                    .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
          }

          if(taps > 30)
          {
            tcounter++;
            printOut.append("\nCamera Link 3:");
            for(int x = 30; x < Math.min(33, taps); x++)
            {
              printOut.append("\n   Port ").append(getPortName(x - 30)).append(": ")
                      .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
            }

            if(taps > 33)
            {
              tcounter++;
              printOut.append("\nCamera Link 4:");
              for(int x = 33; x < taps; x++)
              {
                printOut.append("\n   Port ").append(getPortName(x - 30)).append(": ")
                        .append(String.format("%05d", x * lval)).append("   - ").append(String.format("%05d", (x + 1) * lval - 1));
              }
            }
          }
        }
        break;
      }
    }

    printOut = printOut.replace(printOut.indexOf("%%%%%"), printOut.indexOf("%%%%%") + 5, tcounter + "");
    return printOut.toString();
  }
  
  @Override
  public double getGeometry(boolean coax)
  {
    return coax ? 1 : 1;
  }
}
