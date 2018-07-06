package de.tichawa.cis.config;

import de.tichawa.cis.config.mxled.MXLED;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javafx.beans.property.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.print.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.transform.*;
import javafx.stage.*;

import static de.tichawa.cis.config.CIS.isDouble;

public abstract class MaskController implements Initializable
{

  @FXML
  protected ComboBox<String> Color;
  @FXML
  protected ComboBox<String> Resolution;
  @FXML
  protected ComboBox<String> ScanWidth;
  @FXML
  protected Label PixelSize;
  @FXML
  protected Label DefectSize;
  @FXML
  protected Label MaxLineRate;
  @FXML
  protected Label CurrLineRate;
  @FXML
  protected Slider SelLineRate;
  @FXML
  protected Label Speedmms;
  @FXML
  protected Label Speedms;
  @FXML
  protected Label Speedmmin;
  @FXML
  protected Label Speedips;
  @FXML
  protected ComboBox<String> InternalLightSource;
  @FXML
  protected ComboBox<String> InternalLightColor;
  @FXML
  protected ComboBox<String> ExternalLightSource;
  @FXML
  protected ComboBox<String> ExternalLightColor;
  @FXML
  protected ComboBox<String> Interface;
  @FXML
  protected ComboBox<String> Cooling;
  @FXML
  protected CheckBox Trigger;
  @FXML
  protected Label Infotext;
  @FXML
  protected Button Calculation;
  @FXML
  protected Button PartList;
  @FXML
  protected Button DataSheet;
  @FXML
  protected Button OEMMode;
  @FXML
  protected Button Equip;

  protected CIS CIS_DATA;
  protected MXLED MXLED_DATA;

  public MaskController()
  {
  }

  @Override
  public abstract void initialize(URL url, ResourceBundle rb);

  @FXML
  public void handleCalculation(ActionEvent event)
  {
    if(CIS_DATA.getSpec("MXLED") == null && CIS_DATA.getSpec("External Light Source") != null && CIS_DATA.getSpec("External Light Source") > 0)
    {
      try
      {
        MXLED_DATA.calculate();
      }
      catch(CISException e)
      {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        return;
      }

      try
      {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/Calculation.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("MXLED Calculation");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png")));
        stage.centerOnScreen();
        stage.show();
        stage.setX(stage.getX() - 20);
        stage.setY(stage.getY() - 20);

        CalculationController controller = loader.<CalculationController>getController();
        controller.passData(MXLED_DATA);
      }
      catch(IOException e)
      {

      }
    }

    try
    {
      CIS_DATA.calculate();
    }
    catch(CISException e)
    {
      new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
      return;
    }

    try
    {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/Calculation.fxml"));
      Scene scene = new Scene(loader.load());
      Stage stage = new Stage();
      stage.setScene(scene);
      stage.setTitle(CIS_DATA.CIS_NAME + " Calculation");
      stage.getIcons().add(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png")));
      stage.centerOnScreen();
      stage.show();
      CalculationController controller = loader.<CalculationController>getController();
      controller.passData(CIS_DATA);
    }
    catch(IOException e)
    {

    }
  }

  @FXML
  public void handlePartList(ActionEvent event)
  {
    try
    {
      CIS_DATA.calculate();
    }
    catch(CISException e)
    {
      new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
      return;
    }

    FileChooser f = new FileChooser();
    f.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "csv"));
    f.setInitialFileName(CIS_DATA.getTiViKey() + "_partList.csv");
    File file = f.showSaveDialog(null);

    if(file != null)
    {
      try(BufferedWriter writer = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8")))
      {
        writer.write("Quantity,TiViKey");
        writer.newLine();
        writer.write("");
        writer.newLine();
        writer.flush();

        for(Map.Entry<Integer, Integer> entry : CIS_DATA.getElectConfig().entrySet())
        {
          writer.write("\"" + entry.getValue() + "\",\"" + CIS_DATA.getKey(entry.getKey()) + "\",\" \"");
          writer.newLine();
          writer.flush();
        }

        for(Map.Entry<Integer, Integer> entry : CIS_DATA.getMechaConfig().entrySet())
        {
          writer.write("\"" + entry.getValue() + "\",\"" + CIS_DATA.getKey(entry.getKey()) + "\",\" \"");
          writer.newLine();
          writer.flush();
        }

        writer.close();

        new Alert(Alert.AlertType.INFORMATION, ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("File saved.")).show();
      }
      catch(IOException e)
      {
        new Alert(Alert.AlertType.ERROR, ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("A fatal error occurred during the save attempt.Please close the target file and try again.")).show();
      }
    }
  }

  @FXML
  public void handleDataSheet(ActionEvent event)
  {
    if(CIS_DATA.getSpec("MXLED") == null && CIS_DATA.getSpec("External Light Source") != null && CIS_DATA.getSpec("External Light Source") > 0)
    {
      try
      {
        MXLED_DATA.calculate();
      }
      catch(CISException e)
      {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        return;
      }

      try
      {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/DataSheet.fxml"));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("MXLED Datasheet");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png")));
        stage.centerOnScreen();
        stage.show();
        stage.setX(stage.getX() - 20);
        stage.setY(stage.getY() - 20);

        DataSheetController controller = loader.<DataSheetController>getController();
        controller.passData(MXLED_DATA);

        if(((Button) event.getSource()).equals(OEMMode))
        {
          controller.getHeader().setEditable(true);
          controller.getSpecs().setEditable(true);
          controller.getCLConfig().setEditable(true);
          controller.getProfilePic().setImage(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/OEM_Profile.jpg")));
        }
      }
      catch(IOException e)
      {

      }
    }

    try
    {
      CIS_DATA.calculate();
    }
    catch(CISException e)
    {
      new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
      return;
    }

    try
    {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/tichawa/cis/config/DataSheet.fxml"));
      Scene scene = new Scene(loader.load());
      Stage stage = new Stage();
      stage.setScene(scene);
      stage.setTitle(CIS_DATA.CIS_NAME + " Datasheet");
      stage.getIcons().add(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png")));
      stage.centerOnScreen();
      stage.show();

      DataSheetController controller = loader.<DataSheetController>getController();
      controller.passData(CIS_DATA);

      if(((Button) event.getSource()).equals(OEMMode))
      {
        controller.getHeader().setEditable(true);
        controller.getSpecs().setEditable(true);
        controller.getCLConfig().setEditable(true);
        controller.getProfilePic().setImage(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/OEM_Profile.jpg")));
      }
    }
    catch(IOException e)
    {

    }
  }

  @FXML
  public void handleOEMMode(ActionEvent event)
  {
    handleDataSheet(event);
  }

  @FXML
  public void handleEquipment(ActionEvent a)
  {
    try
    {
      Map<String, Double> prices = Files.lines(Launcher.tableHome.resolve("Prices.csv"))
              .skip(1)
              .map(line -> line.split("\t"))
              .filter(line -> isDouble(line[2]))
              .collect(Collectors.toMap(line -> line[0], line -> Double.parseDouble(line[2].replace(',', '.')), (oldVal, newVal) -> oldVal));

      Stage printStage = new Stage();
      printStage.setTitle("Additional equipment list");
      printStage.getIcons().add(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/TiViCC.png")));
      GridPane printPane = new GridPane();
      printPane.getStylesheets().add("/de/tichawa/cis/config/style.css");
      printPane.getStyleClass().add("white");
      ColumnConstraints c = new ColumnConstraints();
      c.setHgrow(Priority.ALWAYS);
      printPane.getColumnConstraints().addAll(c, c, c, c);
      Scene printScene = new Scene(printPane);

      SimpleBooleanProperty pale = new SimpleBooleanProperty(false);
      Files.lines(Launcher.tableHome.resolve("Equip.csv"))
              .skip(1)
              .map(line -> line.split("\t"))
              .filter(line -> line.length > 3 && Arrays.stream(line[2].split("&"))
              .allMatch(pred -> pred.length() == 0
              || (pred.startsWith("!") != CIS_DATA.getTiViKey().contains(pred.replace("!", "")))
              || (CIS_DATA.getSpec("MXLED") == null && CIS_DATA.getSpec("External Light Source") != null && CIS_DATA.getSpec("External Light Source") > 0 && MXLED_DATA.getTiViKey().contains(pred.replace("!", "")))))
              .map(line ->
              {
                Label[] labels = new Label[4];

                for(int i = 0; i < labels.length; i++)
                {
                  labels[i] = new Label();
                  Label l = labels[i];
                  l.setAlignment(Pos.CENTER_LEFT);
                  l.setMaxWidth(1000);
                  l.getStyleClass().add("bolder");
                  if(pale.get())
                  {
                    l.getStyleClass().add("pale-blue");
                  }
                  else
                  {
                    l.getStyleClass().add("white");
                  }
                }

                labels[0].setText(line[1]);
                labels[1].setText(line[0]);
                labels[2].setText(line[3]);
                labels[3].setText("" + Integer.parseInt(line[3]) * prices.get(line[1]));

                pale.set(!pale.get());
                return labels;
              })
              .forEach(labels -> printPane.addRow(printPane.getChildren().size() / 4, labels));

      printStage.setScene(printScene);
      printStage.show();
    }
    catch(IOException ex)
    {
    }

    /*PrinterJob p = PrinterJob.createPrinterJob();
    
    if(p.showPrintDialog(null))
    {
      p.getJobSettings().setPageLayout(p.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.EQUAL_OPPOSITES));
      double scaleX = p.getJobSettings().getPageLayout().getPrintableWidth() / printPane.getWidth();
      double scaleY = p.getJobSettings().getPageLayout().getPrintableHeight() / (getRowCount(printPane) * 20.0);

      printPane.getTransforms().add(new Scale(Math.min(scaleX, scaleY), Math.min(scaleX, scaleY)));
      
      if(p.printPage(printPane))
      {
        printPane.getTransforms().clear();
        new Alert(Alert.AlertType.INFORMATION, ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("printsuccess")).show();
        p.endJob();
      }
      else
      {
        printPane.getTransforms().clear();
        new Alert(Alert.AlertType.ERROR, ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("A fatal error occurred during the printing attempt.Please control your print settings.")).show();
      }
    }
    
    printStage.close();*/
  }
}
