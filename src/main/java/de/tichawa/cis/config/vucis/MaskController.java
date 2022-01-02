package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.CIS;
import de.tichawa.cis.config.CISException;
import de.tichawa.cis.config.model.tables.records.AdcBoardRecord;
import de.tichawa.cis.config.model.tables.records.SensorBoardRecord;
import de.tichawa.cis.config.model.tables.records.SensorChipRecord;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import javafx.scene.effect.Light;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

public class MaskController extends de.tichawa.cis.config.MaskController<VUCIS>
{

  @FXML
  public ChoiceBox<String> LightPreset;
  @FXML
  public ChoiceBox<String> LensType;
  @FXML
  private ChoiceBox<String> BrightFieldLeft;
  @FXML
  private ChoiceBox<String> Coax;
  @FXML
  private ChoiceBox<String> BrightFieldRight;
  @FXML
  private ChoiceBox<String> DarkFieldLeft;
  @FXML
  private ChoiceBox<String> DarkFieldRight;

  public MaskController()
  {
    CIS_DATA = new VUCIS();
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

    CIS_DATA.setSpec("Resolution", 0);
    CIS_DATA.setSpec("res_cp", 1000);
    CIS_DATA.setSpec("res_cp2", 1000);
    CIS_DATA.setSpec("Scan Width", 0);
    CIS_DATA.setSpec("sw_cp", 1200);
    CIS_DATA.setSpec("sw_index", 5);
    CIS_DATA.setSpec("External Trigger", 0);
    CIS_DATA.setSpec("LEDLines", 2);

    LightPreset.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      Optional<VUCIS.LightPreset> lightPreset = VUCIS.LightPreset.findByDescription(newValue);
      lightPreset.ifPresent(CIS_DATA::setLightPreset);

      boolean disable = !CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.MANUAL);
      BrightFieldLeft.setDisable(disable);
      Coax.setDisable(disable);
      BrightFieldRight.setDisable(disable);
      DarkFieldLeft.setDisable(disable);
      DarkFieldRight.setDisable(disable);
    });

    BrightFieldLeft.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setLeftBrightField));
    Coax.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setCoaxLight));
    BrightFieldRight.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setRightBrightField));
    DarkFieldLeft.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setLeftDarkField));
    DarkFieldRight.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setRightDarkField));
    LensType.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LensType.findByDescription(newValue)
            .ifPresent(CIS_DATA::setLensType));

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

      if(res.equals("1000/500/250"))
      {
        CIS_DATA.setSpec("res_cp2", 1000);
      }
      else
      {
        CIS_DATA.setSpec("res_cp2", Integer.parseInt(res));
      }

      AdcBoardRecord adcBoard = CIS_DATA.getADC("VADCFPGA").orElseThrow(() -> new CISException("Unknown ADC board"));
      SensorBoardRecord sensorBoard = CIS_DATA.getSensorBoard("SMARAGD_INLINE").orElseThrow(() -> new CISException("Unknown sensor board"));
      SensorChipRecord sensorChip = CIS_DATA.getSensorChip("SMARAGD" + CIS_DATA.getSpec("res_cp") + "_VD").orElseThrow(() -> new CISException("Unknown sensor chip"));
      double maxLR = Math.round(1000 * sensorBoard.getLines() / (CIS_DATA.getColorCount() * (sensorChip.getDeadPixels() + 3 + sensorChip.getPixelPerSensor()) * 1.0 / Math.min(sensorChip.getClockSpeed(), adcBoard.getClockSpeed()))) / 1000.0;
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

      CIS_DATA.setSpec("Scan Width", ScanWidth.getSelectionModel().getSelectedIndex());
      CIS_DATA.setSpec("sw_cp", sw);
      CIS_DATA.setSpec("sw_index", sw / CIS.BASE_LENGTH - 1);
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

    Interface.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
            CIS_DATA.setSpec("Interface", Interface.getSelectionModel().getSelectedIndex()));
    Cooling.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
            CIS_DATA.setSpec("Cooling", Cooling.getSelectionModel().getSelectedIndex()));

    LightPreset.getSelectionModel().selectFirst();
    BrightFieldLeft.getSelectionModel().select(1);
    Coax.getSelectionModel().selectFirst();
    BrightFieldRight.getSelectionModel().select(1);
    DarkFieldLeft.getSelectionModel().selectFirst();
    DarkFieldRight.getSelectionModel().selectFirst();
    Resolution.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectLast();
    Interface.getSelectionModel().selectFirst();
    Cooling.getSelectionModel().select(1);
    LensType.getSelectionModel().selectFirst();
  }

  @FXML
  public void printTiViKey(ActionEvent a)
  {
    new Alert(Alert.AlertType.INFORMATION, CIS_DATA.getTiViKey()).showAndWait();
  }
}
