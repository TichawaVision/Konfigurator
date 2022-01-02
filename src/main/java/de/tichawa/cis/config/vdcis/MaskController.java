package de.tichawa.cis.config.vdcis;

import de.tichawa.cis.config.*;

import java.net.*;
import java.util.*;

import de.tichawa.cis.config.model.tables.records.*;
import javafx.beans.value.*;
import javafx.fxml.*;
import javafx.scene.control.*;

public class MaskController extends de.tichawa.cis.config.MaskController<VDCIS>
{

  @FXML
  ComboBox<String> CameraLinkMode;

  public MaskController()
  {
    CIS_DATA = new VDCIS();
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    ArrayList<Double> pixelSize = new ArrayList<>();
    pixelSize.add(0.0254);
    pixelSize.add(0.0254);
    pixelSize.add(0.0508);
    pixelSize.add(0.1016);
    pixelSize.add(0.2032);
    pixelSize.add(0.254);
    pixelSize.add(0.508);
    pixelSize.add(1.016);

    CIS_DATA.setSpec("Color", 2);
    CIS_DATA.setSpec("Resolution", 0);
    CIS_DATA.setSpec("res_cp", 1000);
    CIS_DATA.setSpec("res_cp2", 1000);
    CIS_DATA.setSpec("Scan Width", 0);
    CIS_DATA.setSpec("sw_cp", 1200);
    CIS_DATA.setSpec("sw_index", 5);
    CIS_DATA.setSpec("External Trigger", 0);
    CIS_DATA.setSpec("LEDLines", 2);

    Color.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      switch(newValue)
      {
        case "One phase (Monochrome)":
        {
          CIS_DATA.setSpec("Color", 2);
          break;
        }
        case "Two phases":
        {
          CIS_DATA.setSpec("Color", 3);
          break;
        }
        case "Three phases (RGB)":
        {
          CIS_DATA.setSpec("Color", 4);
          break;
        }
        case "Four phases":
        {
          CIS_DATA.setSpec("Color", 5);
          break;
        }
        case "Five phases":
        {
          CIS_DATA.setSpec("Color", 6);
          break;
        }
        case "Six phases":
        {
          CIS_DATA.setSpec("Color", 7);
          break;
        }
      }

      InternalLightColor.setDisable(newValue.equals("RGB"));

      AdcBoardRecord adcBoard = CIS_DATA.getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
      SensorBoardRecord sensorBoard = CIS_DATA.getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
      SensorChipRecord sensorChip = CIS_DATA.getSensorChip("SMARAGD" + CIS_DATA.getSpec("res_cp") + "_VD").orElseThrow(() -> new CISException("Unknown sensor chip"));
      double maxLR = Math.round(1000 * sensorBoard.getLines() / (CIS_DATA.getSpec("Color") * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
      MaxLineRate.setText(maxLR + " kHz");
      SelLineRate.setMax(maxLR * 1000);
      SelLineRate.setValue(maxLR * 1000);

      CIS_DATA.setSpec("Maximum line rate", (int) Math.round(maxLR * 1000));
      CIS_DATA.setSpec("Speedmms", (int) (pixelSize.get(CIS_DATA.getSpec("Resolution")) * CIS_DATA.getSpec("Selected line rate")) * 1000);
    });
    Resolution.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Resolution", Resolution.getSelectionModel().getSelectedIndex());

      String res = newValue.substring(0, newValue.lastIndexOf(" "));
      res = res.trim().split(" ")[res.trim().split(" ").length - 1];

      switch(res)
      {
        case "1000/500/250":
        case "1000":
        {
          CIS_DATA.setSpec("res_cp", 1000);
          break;
        }
        case "500":
        case "100":
        {
          CIS_DATA.setSpec("res_cp", 500);
          break;
        }
        case "250":
        case "125":
        case "50":
        case "25":
        {
          CIS_DATA.setSpec("res_cp", 250);
          break;
        }
      }

      if(CIS_DATA.getSpec("Color") >= 4 && CIS_DATA.getSpec("res_cp2") > 600)
      {
        Resolution.getSelectionModel().select(oldValue);
        return;
      }

      if(res.equals("1000/500/250"))
      {
        CIS_DATA.setSpec("res_cp2", 1000);
      }
      else
      {
        CIS_DATA.setSpec("res_cp2", Integer.parseInt(res));
      }

      if(CIS_DATA.getSpec("Color") >= 4 && CIS_DATA.getSpec("res_cp2") > 600)
      {
        Resolution.getSelectionModel().select(oldValue);
        return;
      }

      AdcBoardRecord adcBoard = CIS_DATA.getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
      SensorBoardRecord sensorBoard = CIS_DATA.getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
      SensorChipRecord sensorChip = CIS_DATA.getSensorChip("SMARAGD" + CIS_DATA.getSpec("res_cp") + "_VD").orElseThrow(() -> new CISException("Unknown sensor chip"));
      double maxLR = Math.round(1000 * sensorBoard.getLines() / (CIS_DATA.getSpec("Color") * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
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

      if(sw > 1200)
      {
        ScanWidth.setValue(oldValue);
        return;
      }
      
      CIS_DATA.setSpec("Scan Width", ScanWidth.getSelectionModel().getSelectedIndex());
      CIS_DATA.setSpec("sw_cp", sw);
      CIS_DATA.setSpec("sw_index", (int) (sw / CIS.BASE_LENGTH) - 1);
    });
    SelLineRate.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
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
      if(CIS_DATA.getSpec("Color") == 4 && newValue.equals("None"))
      {
        InternalLightSource.getSelectionModel().select(oldValue);
        return;
      }

      CIS_DATA.setSpec("Internal Light Source", InternalLightSource.getSelectionModel().getSelectedIndex());

      switch(newValue)
      {
        case "Two sided":
        {
          CIS_DATA.setSpec("LEDLines", 2);
          break;
        }
      }
    });
    InternalLightColor.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("Internal Light Color", InternalLightColor.getSelectionModel().getSelectedIndex());
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
    CameraLinkMode.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
      CIS_DATA.setSpec("CLMode", CameraLinkMode.getSelectionModel().getSelectedIndex());
    });

    Color.getSelectionModel().selectFirst();
    Resolution.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectLast();
    InternalLightSource.getSelectionModel().select(2);
    InternalLightColor.getSelectionModel().selectFirst();
    Interface.getSelectionModel().selectFirst();
    Cooling.getSelectionModel().select(1);
    Trigger.setSelected(false);
    CameraLinkMode.getSelectionModel().selectLast();
  }
}
