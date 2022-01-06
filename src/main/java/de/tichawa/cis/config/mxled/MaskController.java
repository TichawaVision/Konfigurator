package de.tichawa.cis.config.mxled;

import java.net.*;
import java.util.*;

import de.tichawa.cis.config.CIS;

public class MaskController extends de.tichawa.cis.config.MaskController<MXLED>
{
  public MaskController()
  {
    CIS_DATA = new MXLED();
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    CIS_DATA.setPhaseCount(1);
    CIS_DATA.setDiffuseLightSources(1);
    CIS_DATA.setCoaxLightSources(0);
    CIS_DATA.setLightColor(CIS.LightColor.RED);
    CIS_DATA.setScanWidth(1040);
    CIS_DATA.setExternalTrigger(false);

    Color.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      if(newValue.equals("RGB") && CIS_DATA.getLedLines() == 0)
      {
        Color.getSelectionModel().select(oldValue);
        return;
      }

      switch(newValue)
      {
        case "Monochrome":
        {
          CIS_DATA.setPhaseCount(1);
          break;
        }
        case "RGB":
        {
          CIS_DATA.setPhaseCount(3);
          break;
        }
      }

      InternalLightColor.setDisable(newValue.equals("RGB") || CIS_DATA.getLedLines() == 0);
      ExternalLightColor.setDisable(newValue.equals("RGB") || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);
    });
    ScanWidth.valueProperty().addListener((observable, oldValue, newValue) ->
            CIS_DATA.setScanWidth(Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim())));
    InternalLightSource.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      if(CIS_DATA.getPhaseCount() == 3 && newValue.equals("None") && MXLED_DATA.getLedLines() == 0)
      {
        InternalLightSource.getSelectionModel().select(oldValue);
        return;
      }

      switch(InternalLightSource.getSelectionModel().getSelectedIndex())
      {
        case 0:
          CIS_DATA.setDiffuseLightSources(0);
          CIS_DATA.setCoaxLightSources(0);
          break;
        case 1:
          CIS_DATA.setDiffuseLightSources(1);
          CIS_DATA.setCoaxLightSources(0);
          break;
        case 2:
          CIS_DATA.setDiffuseLightSources(2);
          CIS_DATA.setCoaxLightSources(0);
          break;
        case 3:
          CIS_DATA.setDiffuseLightSources(2);
          CIS_DATA.setCoaxLightSources(1);
          break;
        case 4:
          CIS_DATA.setDiffuseLightSources(0);
          CIS_DATA.setCoaxLightSources(1);
          break;
      }
      InternalLightColor.setDisable(CIS_DATA.getPhaseCount() == 3 || CIS_DATA.getLedLines() == 0);
    });
    InternalLightColor.valueProperty().addListener((observable, oldValue, newValue) ->
            CIS.LightColor.findByDescription(newValue).ifPresent(CIS_DATA::setLightColor));

    Color.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectFirst();
    InternalLightSource.getSelectionModel().select(1);
    InternalLightColor.getSelectionModel().selectFirst();
  }

  @Override
  public List<CIS.Resolution> setupResolutions()
  {
    return Collections.emptyList();
  }
}
