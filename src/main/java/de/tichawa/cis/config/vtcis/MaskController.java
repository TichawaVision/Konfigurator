package de.tichawa.cis.config.vtcis;

import de.tichawa.cis.config.mxled.MXLED;
import de.tichawa.cis.config.CIS;
import java.net.*;
import java.util.*;
import javafx.beans.value.*;
import javafx.fxml.*;
import javafx.scene.control.*;

public class MaskController extends de.tichawa.cis.config.MaskController
{
  @FXML
  Label AllowedColors;

  public MaskController()
  {
    CIS_DATA = new VTCIS();
    MXLED_DATA = new MXLED();
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    ArrayList<Double> pixelSize = new ArrayList<>();
    pixelSize.add(0.02115);
    pixelSize.add(0.02115);
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
    CIS_DATA.setSpec("res_cp", 1200);
    CIS_DATA.setSpec("res_cp2", 1200);
    CIS_DATA.setSpec("Scan Width", 0);
    CIS_DATA.setSpec("sw_cp", 1040);
    CIS_DATA.setSpec("sw_index", 3);
    CIS_DATA.setSpec("External Trigger", 0);
    CIS_DATA.setSpec("LEDLines", 1);

    Color.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      if(newValue.equals("RGB") && CIS_DATA.getSpec("Internal Light Source") == 0)
      {
        Color.getSelectionModel().select(oldValue);
        return;
      }

      switch(newValue)
      {
        case "Monochrome":
        {
          CIS_DATA.setSpec("Color", 1);
          break;
        }
        case "RGB":
        {
          CIS_DATA.setSpec("Color", 3);
          break;
        }
      }

      InternalLightColor.setDisable(newValue.equals("RGB") || CIS_DATA.getSpec("LEDLines") == 0);
      ExternalLightColor.setDisable(newValue.equals("RGB") || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);

      double maxLR = Math.round(1000 * CIS_DATA.getSensBoard("SMARAGD")[2] / (CIS_DATA.getSpec("Color") * (CIS_DATA.getSensChip("SMARAGD" + CIS_DATA.getSpec("res_cp"))[3] + 3 + CIS_DATA.getSensChip("SMARAGD" + CIS_DATA.getSpec("res_cp"))[2]) * 1.0 / Math.min(CIS_DATA.getSensChip("SMARAGD" + CIS_DATA.getSpec("res_cp"))[4], CIS_DATA.getADC("VADCFPGA")[2]))) / 1000.0;
      MaxLineRate.setText(maxLR + " kHz");
      SelLineRate.setMax(maxLR * 1000);
      SelLineRate.setValue(maxLR * 1000);

      CIS_DATA.setSpec("Maximum line rate", (int) Math.round(maxLR * 1000));
      CIS_DATA.setSpec("Speedmms", (int) (pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate")) * 1000);

      MXLED_DATA.setSpec("Color", CIS_DATA.getSpec("Color"));
    });
    Resolution.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Resolution", Resolution.getSelectionModel().getSelectedIndex());

      String res = newValue.substring(0, newValue.lastIndexOf(" "));
      res = res.trim().split(" ")[res.trim().split(" ").length - 1];

      switch(res)
      {
        case "1200/600/300":
        case "1200":
        case "400":
        {
          CIS_DATA.setSpec("res_cp", 1200);
          break;
        }
        case "600":
        case "200":
        {
          CIS_DATA.setSpec("res_cp", 600);
          break;
        }
        case "300":
        case "150":
        case "100":
        case "75":
        case "50":
        {
          CIS_DATA.setSpec("res_cp", 300);
          break;
        }
      }

      if(res.equals("1200/600/300"))
      {
        CIS_DATA.setSpec("res_cp2", 1200);
      }
      else
      {
        CIS_DATA.setSpec("res_cp2", Integer.parseInt(res));
      }

      double maxLR = Math.round(1000 * CIS_DATA.getSensBoard("SMARAGD")[2] / (CIS_DATA.getSpec("Color") * (CIS_DATA.getSensChip("SMARAGD" + CIS_DATA.getSpec("res_cp"))[3] + 3 + CIS_DATA.getSensChip("SMARAGD" + CIS_DATA.getSpec("res_cp"))[2]) * 1.0 / Math.min(CIS_DATA.getSensChip("SMARAGD" + CIS_DATA.getSpec("res_cp"))[4], CIS_DATA.getADC("VADCFPGA")[2]))) / 1000.0;
      MaxLineRate.setText(maxLR + " kHz");
      SelLineRate.setMax(maxLR * 1000);
      SelLineRate.setValue(maxLR * 1000);

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

      if(sw > 1040)
      {
        AllowedColors.setText("1");
      }
      else
      {
        AllowedColors.setText("3");
      }

      CIS_DATA.setSpec("Scan Width", ScanWidth.getSelectionModel().getSelectedIndex());
      CIS_DATA.setSpec("sw_cp", sw);
      CIS_DATA.setSpec("sw_index", (int) (sw / CIS_DATA.getBaseLength()) - 1);

      MXLED_DATA.setSpec("Scan Width", CIS_DATA.getSpec("Scan Width"));
      MXLED_DATA.setSpec("sw_cp", CIS_DATA.getSpec("sw_cp"));
      MXLED_DATA.setSpec("sw_index", MXLED.getSWIndex(sw));
    });
    SelLineRate.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            ->
    {
      CIS_DATA.setSpec("Selected line rate", newValue.intValue());

      CurrLineRate.setText(newValue.intValue() / 1000.0 + " kHz");

      Speedmms.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate"), 3) + " mm/s");
      Speedms.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") / 1000, 3) + " m/s");
      Speedmmin.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") * 0.06, 3) + " m/min");
      Speedips.setText(CIS.round(pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate") * 0.03937, 3) + " ips");
    });
    InternalLightSource.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      if(CIS_DATA.getSpec("Color") == 3 && newValue.equals("None") && CIS_DATA.getSpec("External Light Source") == 0)
      {
        InternalLightSource.getSelectionModel().select(oldValue);
        return;
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
        case "Two sided plus Coax":
        {
          CIS_DATA.setSpec("LEDLines", 3);
          break;
        }
      }

      InternalLightColor.setDisable(CIS_DATA.getSpec("Color") == 3 || CIS_DATA.getSpec("LEDLines") == 0);
    });
    InternalLightColor.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Internal Light Color", InternalLightColor.getSelectionModel().getSelectedIndex());
    });
    ExternalLightSource.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      if(CIS_DATA.getSpec("Color") == 3 && CIS_DATA.getSpec("Internal Light Source") == 0 && newValue.equals("None"))
      {
        ExternalLightSource.getSelectionModel().select(oldValue);
        return;
      }

      CIS_DATA.setSpec("External Light Source", ExternalLightSource.getSelectionModel().getSelectedIndex());

      ExternalLightColor.setDisable(CIS_DATA.getSpec("Color") == 3 || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);

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
