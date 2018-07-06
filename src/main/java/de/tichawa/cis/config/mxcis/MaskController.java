package de.tichawa.cis.config.mxcis;

import de.tichawa.cis.config.mxled.MXLED;
import de.tichawa.cis.config.CIS;
import java.net.*;
import java.util.*;
import javafx.beans.value.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.util.*;

public class MaskController extends de.tichawa.cis.config.MaskController
{
  public MaskController()
  {
    CIS_DATA = new MXCIS();
    MXLED_DATA = new MXLED();
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    ArrayList<Double> pixelSize = new ArrayList<>();
    pixelSize.add(0.0423);
    pixelSize.add(0.0635);
    pixelSize.add(0.0847);
    pixelSize.add(0.125);
    pixelSize.add(0.167);
    pixelSize.add(0.25);
    pixelSize.add(0.339);
    pixelSize.add(0.5);
    pixelSize.add(1.0);

    CIS_DATA.setSpec("Color", 1);
    CIS_DATA.setSpec("Internal Light Source", 1);
    CIS_DATA.setSpec("External Light Source", 0);
    CIS_DATA.setSpec("Resolution", 0);
    CIS_DATA.setSpec("res_cp", 600);
    CIS_DATA.setSpec("res_cp2", 600);
    CIS_DATA.setSpec("Scan Width", 0);
    CIS_DATA.setSpec("sw_cp", 520);
    CIS_DATA.setSpec("sw_index", 0);
    CIS_DATA.setSpec("External Trigger", 0);
    CIS_DATA.setSpec("LEDLines", 1);

    Color.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      if(newValue.equals("RGB") && CIS_DATA.getSpec("Internal Light Source") != 2 && CIS_DATA.getSpec("Internal Light Source") != 3)
      {
        new Alert(AlertType.WARNING, "RGB only usable with Two Sided or One Sided plus Coax").show();
        Color.getSelectionModel().select(oldValue);
        return;
      }

      switch(newValue)
      {
        case "Monochrome":
        {
          CIS_DATA.setSpec("Color", 1);
          MXLED_DATA.setSpec("Color", 1);
          break;
        }
        case "RGB":
        {
          CIS_DATA.setSpec("Color", 4);
          MXLED_DATA.setSpec("Color", 3);
          break;
        }
      }

      InternalLightColor.setDisable(newValue.equals("RGB") || CIS_DATA.getSpec("LEDLines") == 0);
      ExternalLightColor.setDisable(newValue.equals("RGB") || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);

      Integer[] board = ((MXCIS) CIS_DATA).getBoard(CIS_DATA.getSpec("res_cp2"));
      Integer[] chip = ((MXCIS) CIS_DATA).getChip(CIS_DATA.getSpec("res_cp2"));
      double maxLR = Math.round(1000.0 * board[2] / (CIS_DATA.getSpec("Color") * (chip[3] + 3 + chip[2]) * 1.0 / Math.min(chip[4], CIS_DATA.getADC("MODU_ADC(SLOW)")[2]))) / 1000.0;
      MaxLineRate.setText(maxLR + " kHz");
      SelLineRate.setMax(maxLR * 1000);
      SelLineRate.setValue(maxLR * 1000);
      SelLineRate.setMajorTickUnit(SelLineRate.getMax() / 20);
      SelLineRate.setMinorTickCount(0);
      SelLineRate.setShowTickMarks(true);
      SelLineRate.setSnapToTicks(true);
      SelLineRate.setShowTickLabels(true);
      SelLineRate.setLabelFormatter(new StringConverter<Double>()
      {
        @Override
        public String toString(Double n)
        {
          if(CIS_DATA.getSpec("Color") == 4)
          {
            if(n == maxLR * 1000)
            {
              return "K4";
            }
            else
            {
              return "";
            }
          }
          else
          {
            if((CIS_DATA.getSpec("res_cp") != 600 && n == maxLR * 1000 / 4) || (CIS_DATA.getSpec("res_cp") == 600 && n == maxLR * 1000 / 5))
            {
              return "K1";
            }
            else if(n == maxLR * 1000 / 2)
            {
              return "K2";
            }
            else if(n == maxLR * 1000)
            {
              return "K3";
            }
            else
            {
              return "";
            }
          }
        }

        @Override
        public Double fromString(String s)
        {
          return 0.0;
        }
      });

      CIS_DATA.setSpec("Maximum line rate", (int) Math.round(maxLR * 1000));
      CIS_DATA.setSpec("Speedmms", (int) (pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate")) * 1000);
    });
    Resolution.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Resolution", Resolution.getSelectionModel().getSelectedIndex());

      String r = newValue.substring(0, newValue.lastIndexOf(" "));
      Integer res = Integer.parseInt(r.trim().split(" ")[r.trim().split(" ").length - 1]);

      switch(res)
      {
        case 400:
        {
          CIS_DATA.setSpec("res_cp", 400);
          break;
        }
        case 600:
        {
          CIS_DATA.setSpec("res_cp", 600);
          break;
        }
        case 200:
        {
          CIS_DATA.setSpec("res_cp", 200);
          break;
        }
        case 300:
        case 150:
        case 100:
        case 75:
        case 50:
        {
          CIS_DATA.setSpec("res_cp", 300);
          break;
        }
      }

      CIS_DATA.setSpec("res_cp2", res);

      Integer[] board = ((MXCIS) CIS_DATA).getBoard(res);
      Integer[] chip = ((MXCIS) CIS_DATA).getChip(res);
      double maxLR = Math.round(1000.0 * board[2] / (CIS_DATA.getSpec("Color") * (chip[3] + 3 + chip[2]) * 1.0 / Math.min(chip[4], CIS_DATA.getADC("MODU_ADC(SLOW)")[2]))) / 1000.0;
      MaxLineRate.setText(maxLR + " kHz");
      SelLineRate.setMax(maxLR * 1000);
      SelLineRate.setValue(maxLR * 1000);
      if(CIS_DATA.getSpec("res_cp2") != 600)
      {
        SelLineRate.setMajorTickUnit(SelLineRate.getMax() / 4);
        SelLineRate.setMinorTickCount(3);
      }
      else
      {
        SelLineRate.setMajorTickUnit(SelLineRate.getMax() / 20);
        SelLineRate.setMinorTickCount(0);
      }
      SelLineRate.setShowTickMarks(true);
      SelLineRate.setSnapToTicks(true);
      SelLineRate.setShowTickLabels(true);
      SelLineRate.setLabelFormatter(new StringConverter<Double>()
      {
        @Override
        public String toString(Double n)
        {
          if(CIS_DATA.getSpec("Color") == 4)
          {
            if(n == maxLR * 1000)
            {
              return "K4";
            }
            else
            {
              return "";
            }
          }
          else
          {
            if(n == maxLR * 1000 / 4 || (CIS_DATA.getSpec("res_cp") == 600 && n == maxLR * 1000 / 5))
            {
              return "K1";
            }
            else if(n == maxLR * 1000 / 2)
            {
              return "K2";
            }
            else if(n == maxLR * 1000)
            {
              return "K3";
            }
            else
            {
              return "";
            }
          }
        }

        @Override
        public Double fromString(String s)
        {
          return 0.0;
        }
      });

      CIS_DATA.setSpec("Maximum line rate", (int) Math.round(maxLR * 1000));
      CIS_DATA.setSpec("Speedmms", (int) (pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate")) * 1000);

      PixelSize.setText(pixelSize.get(CIS_DATA.getSpec("Resolution")) + " mm");
      DefectSize.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * 3, 5) + " mm");
      Speedmms.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate"), 3) + " mm/s");
      Speedms.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") / 1000, 3) + " m/s");
      Speedmmin.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") * 0.06, 3) + " m/min");
      Speedips.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") * 0.03937, 3) + " ips");
    });
    ScanWidth.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim());

      if(InternalLightSource.getSelectionModel().getSelectedItem() != null && InternalLightSource.getSelectionModel().getSelectedItem().contains("Coax") && sw > 1820)
      {
        ScanWidth.getSelectionModel().select(oldValue);
        return;
      }

      CIS_DATA.setSpec("Scan Width", ScanWidth.getSelectionModel().getSelectedIndex());
      CIS_DATA.setSpec("sw_cp", sw);
      CIS_DATA.setSpec("sw_index", (int) (sw / CIS_DATA.getBaseLength()) - 2);

      MXLED_DATA.setSpec("Scan Width", CIS_DATA.getSpec("Scan Width"));
      MXLED_DATA.setSpec("sw_cp", CIS_DATA.getSpec("sw_cp"));
      MXLED_DATA.setSpec("sw_index", MXLED.getSWIndex(sw));
    });
    SelLineRate.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
      if(newValue.intValue() < 100)
      {
        newValue = 100;
      }

      CIS_DATA.setSpec("Selected line rate", newValue.intValue());
      CIS_DATA.setSpec("Speedmms", (int) (pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate")) * 1000);

      CurrLineRate.setText(newValue.intValue() / 1000.0 + " kHz");

      Speedmms.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate"), 3) + " mm/s");
      Speedms.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") / 1000, 3) + " m/s");
      Speedmmin.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") * 0.06, 3) + " m/min");
      Speedips.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") * 0.03937, 3) + " ips");
    });
    InternalLightSource.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      if(newValue.contains("Coax") && CIS_DATA.getSpec("sw_cp") > 1820)
      {
        InternalLightSource.getSelectionModel().select(oldValue);
        return;
      }

      if(CIS_DATA.getSpec("Color") == 4 && !newValue.equals("Coax") && !newValue.equals("Two sided"))
      {
        InternalLightSource.getSelectionModel().select(oldValue);
        return;
      }
      
      if(newValue.contains("Coax"))
      {
        new Alert(AlertType.WARNING, "Coax selected.\nPlease check line rate.").show();
      }

      CIS_DATA.setSpec("Internal Light Source", InternalLightSource.getSelectionModel().getSelectedIndex());

      switch(newValue)
      {
        case "None":
        {
          CIS_DATA.setSpec("LEDLines", 0);
          break;
        }
        case "One sided":
        {
          CIS_DATA.setSpec("LEDLines", 1);
          break;
        }
        case "Two sided":
        {
          CIS_DATA.setSpec("LEDLines", 2);
          break;
        }
        case "Coax":
        {
          CIS_DATA.setSpec("LEDLines", 1);
          break;
        }
        case "One sided plus Coax":
        {
          CIS_DATA.setSpec("LEDLines", 2);
          break;
        }
      }

      InternalLightColor.setDisable(CIS_DATA.getSpec("Color") == 4 || CIS_DATA.getSpec("LEDLines") == 0);
    });
    InternalLightColor.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Internal Light Color", InternalLightColor.getSelectionModel().getSelectedIndex());
    });
    ExternalLightSource.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("External Light Source", ExternalLightSource.getSelectionModel().getSelectedIndex());

      ExternalLightColor.setDisable(CIS_DATA.getSpec("Color") == 4 || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);

      MXLED_DATA.setSpec("Internal Light Source", CIS_DATA.getSpec("External Light Source"));
    });
    ExternalLightColor.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("External Light Color", ExternalLightColor.getSelectionModel().getSelectedIndex());
      MXLED_DATA.setSpec("Internal Light Color", CIS_DATA.getSpec("External Light Color"));
    });
    Interface.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Interface", Interface.getSelectionModel().getSelectedIndex());
    });
    Cooling.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Cooling", Cooling.getSelectionModel().getSelectedIndex());
    });
    Trigger.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
      CIS_DATA.setSpec("External Trigger", newValue ? 1 : 0);
    });

    Color.getSelectionModel().selectFirst();
    Resolution.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectFirst();
    InternalLightSource.getSelectionModel().select(1);
    ExternalLightSource.getSelectionModel().selectFirst();
    InternalLightColor.getSelectionModel().selectFirst();
    ExternalLightColor.getSelectionModel().selectFirst();
    Interface.getSelectionModel().selectFirst();
    Cooling.getSelectionModel().select(1);
    Trigger.setSelected(false);
  }
}
