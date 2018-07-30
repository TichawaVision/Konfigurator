package de.tichawa.cis.config.vtcis;

import de.tichawa.cis.config.*;
import java.io.*;
import java.nio.file.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

public class VTCIS extends CIS
{

  public VTCIS()
  {
    super();

    setSpec("VTCIS", 1);
  }

  @Override
  public String getTiViKey()
  {
    String key = "G_VTCIS";
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

    if(getSpec("Color") == 3)
    {
      key = key.replace(COLORCODE[getSpec("Internal Light Color")], "RGB");
    }

    key += getMechaVersion();

    if(getSpec("Interface") == 1)
    {
      key += "GT";
    }

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

    numOfPixNominal = (int) (numOfPix - ((getSpec("sw_cp") / getBaseLength()) * getSensBoard("SMARAGD")[7] / (1200 / getSpec("res_cp2"))));
    taps = (int) Math.ceil(1.01 * ((long) numOfPixNominal * getSpec("Selected line rate") / 1000000) / 85.0);
    pixPerTap = numOfPixNominal / taps;
    lval = pixPerTap - pixPerTap % 8;

    printOut.append(getString("datarate")).append(Math.round(getSpec("Color") * numOfPixNominal * getSpec("Selected line rate") / 100000.0) / 10.0).append(" MByte\n");
    printOut.append(getString("numofcons")).append("%%%%%\n");
    printOut.append(getString("numofport")).append(taps * getSpec("Color")).append("\n");
    printOut.append("Pixel Clock: 85 MHz\n");
    printOut.append("Nominal pixel count: ").append(numOfPixNominal).append("\n");

    switch(getSpec("Color"))
    {
      case 3:
      {
        if(taps > 6)
        {
          new Alert(AlertType.ERROR, "Please select a lower line rate. Currently required number of taps (" + taps * getSpec("Color") + ") is too high.").showAndWait();
          return null;
        }
        int x = 0;
        int y = 0;

        tcounter = 1;
        printOut.append("Camera Link ").append(tcounter).append(":\n");
        printOut.append("\tPort ").append(getPortName(x * 3)).append(":\t")
                .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                .append(getString("Red")).append("\n");
        printOut.append("\tPort ").append(getPortName(x * 3 + 1)).append(":\t")
                .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                .append(getString("Green")).append("\n");
        printOut.append("\tPort ").append(getPortName(x * 3 + 2)).append(":\t")
                .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                .append(getString("Blue")).append("\n");
        x++;
        y++;

        if(taps > 1)
        {
          tcounter = 2;
          printOut.append("Camera Link ").append(tcounter).append(":\n");
          printOut.append("\tPort ").append(getPortName(x * 3)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Red")).append("\n");
          printOut.append("\tPort ").append(getPortName(x * 3 + 1)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Green")).append("\n");
          printOut.append("\tPort ").append(getPortName(x * 3 + 2)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Blue")).append("\n");
          x++;
          y++;
        }
        
        if(taps > 4 || taps == 3)
        {
          printOut.append("\tPort ").append(getPortName(x * 3)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Red")).append("\n");
          printOut.append("\tPort ").append(getPortName(x * 3 + 1)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Green")).append("\n");
          printOut.append("\tPort ").append(getPortName(x * 3 + 2)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Blue")).append("\n");
          x++;
          y++;
        }

        if(taps > 2 && taps != 3)
        {
          y = 0;
          tcounter = 3;
          printOut.append("Camera Link ").append(tcounter).append(":\n");
          printOut.append("\tPort ").append(getPortName(y * 3)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Red")).append("\n");
          printOut.append("\tPort ").append(getPortName(y * 3 + 1)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Green")).append("\n");
          printOut.append("\tPort ").append(getPortName(y * 3 + 2)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Blue")).append("\n");
          x++;
          y++;
        }

        if(taps > 3)
        {
          tcounter = 4;
          printOut.append("Camera Link ").append(tcounter).append(":\n");
          printOut.append("\tPort ").append(getPortName(y * 3)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Red")).append("\n");
          printOut.append("\tPort ").append(getPortName(y * 3 + 1)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Green")).append("\n");
          printOut.append("\tPort ").append(getPortName(y * 3 + 2)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Blue")).append("\n");
          x++;
          y++;
        }

        if(taps > 5)
        {
          printOut.append("\tPort ").append(getPortName(y * 3)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Red")).append("\n");
          printOut.append("\tPort ").append(getPortName(y * 3 + 1)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Green")).append("\n");
          printOut.append("\tPort ").append(getPortName(y * 3 + 2)).append(":\t")
                  .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\t")
                  .append(getString("Blue")).append("\n");
          x++;
          y++;
        }
        break;
      }
      case 1:
      {
        tcounter = 1;
        printOut.append("Camera Link ").append(tcounter).append(":\n");

        if(taps > 20)
        {
          new Alert(AlertType.ERROR, "Please select a lower line rate. Currently required number of taps (" + taps * getSpec("Color") + ") is too high.").showAndWait();
          return null;
        }

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
            for(int x = 0; x < Math.min(3, taps); x++)
            {
              printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                      .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
            }

            if(taps > 3)
            {
              tcounter = 2;
              printOut.append("Camera Link ").append(tcounter).append(":\n");
              for(int x = 3; x < taps; x++)
              {
                printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                        .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
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
              printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                      .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
            }

            if(taps > 3)
            {
              tcounter = 2;
              printOut.append("Camera Link ").append(tcounter).append(":\n");
              for(int x = 3; x < Math.min(8, taps); x++)
              {
                printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                        .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
              }

              if(taps > 8)
              {
                tcounter = 3;
                printOut.append("Camera Link ").append(tcounter).append(":\n");
                for(int x = 8; x < Math.min(11, taps); x++)
                {
                  printOut.append("\tPort ").append(getPortName(x - 8)).append(":\t")
                          .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
                }

                if(taps > 11)
                {
                  tcounter = 4;
                  printOut.append("Camera Link ").append(tcounter).append(":\n");
                  for(int x = 11; x < taps; x++)
                  {
                    printOut.append("\tPort ").append(getPortName(x - 8)).append(":\t")
                            .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
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
          {

            for(int x = 0; x < Math.min(3, taps); x++)
            {
              printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                      .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
            }

            if(taps > 3)
            {
              tcounter = 2;
              printOut.append("Camera Link ").append(tcounter).append(":\n");
              for(int x = 3; x < Math.min(10, taps); x++)
              {
                printOut.append("\tPort ").append(getPortName(x)).append(":\t")
                        .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
              }

              if(taps > 10)
              {
                tcounter = 3;
                printOut.append("Camera Link ").append(tcounter).append(":\n");
                for(int x = 10; x < Math.min(13, taps); x++)
                {
                  printOut.append("\tPort ").append(getPortName(x - 10)).append(":\t")
                          .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
                }

                if(taps > 13)
                {
                  tcounter = 4;
                  printOut.append("Camera Link ").append(tcounter).append(":\n");
                  for(int x = 13; x < taps; x++)
                  {
                    printOut.append("\tPort ").append(getPortName(x - 10)).append(":\t")
                            .append(String.format("%05d", x * lval)).append("\t - ").append(String.format("%05d", (x + 1) * lval - 1)).append("\n");
                  }
                }
              }
            }
            break;
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
    return coax ? 0.229 : 0.252;
  }
}
