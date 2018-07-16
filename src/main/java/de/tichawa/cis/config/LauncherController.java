package de.tichawa.cis.config;

import de.tichawa.util.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.*;

public class LauncherController implements Initializable
{

  @FXML
  private ComboBox selectCIS;

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {

  }

  @FXML
  private void handleContinue(ActionEvent event)
  {
    try
    {
      Parent root = FXMLLoader.load(getClass().getResource("/de/tichawa/cis/config/" + selectCIS.getSelectionModel().getSelectedItem().toString().toLowerCase() + "/Mask.fxml"));
      Scene scene = new Scene(root);
      Stage stage = new Stage();
      stage.setScene(scene);
      stage.setTitle((String) selectCIS.getSelectionModel().getSelectedItem());
      stage.getIcons().add(new Image(getClass().getResourceAsStream("TiViCC.png")));
      stage.centerOnScreen();
      stage.show();
    }
    catch(IOException e)
    {
    }
  }

  @FXML
  private void handleUpdate(ActionEvent a)
  {
    try
    {
      Desktop.getDesktop().open(Launcher.ferixHome.resolve("Priceexport.lnk").toFile());
      if(new Alert(AlertType.CONFIRMATION, "Please wait until FERIX has finished the export, then press OK to continue.", ButtonType.OK, ButtonType.CANCEL).showAndWait()
              .orElse(ButtonType.CANCEL) == ButtonType.OK)
      {
        Map<Integer, Double> sourcePrices = Files.lines(Launcher.ferixHome.resolve("Export/art.csv"))
                .map(line -> line.split("\t"))
                .filter(line -> CIS.isInteger(line[0]) && CIS.isDouble(line[1]))
                .map(line -> new Tuple<>(Integer.parseInt(line[0]), Double.parseDouble(line[1]) / CIS.decodeQuantity(line[2])))
                .filter(t -> t.getV() > 0)
                .collect(Collectors.toMap(Tuple::getU, Tuple::getV, (oldVal, newVal) -> newVal));

        Files.write(Launcher.tableHome.resolve("Prices.csv"), (Iterable<String>) Files.readAllLines(Launcher.tableHome.resolve("Prices.csv")).stream()
                .map(line -> line.split("\t"))
                .filter(line -> CIS.isInteger(line[0]))
                .map(line -> new Tuple<>(Integer.parseInt(line[0]), line))
                //.filter(t -> sourcePrices.containsKey(t.getU()))
                //.filter(t -> sourcePrices.get(t.getU()) != Double.parseDouble(t.getV()[2]))
                .map(t ->
                {
                  if(sourcePrices.containsKey(t.getU()))
                  {
                    t.getV()[2] = sourcePrices.get(t.getU()).toString();
                  }
                  return t;
                })
                .map(Tuple::getV)
                .map(line -> String.join("\t", line))::iterator, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        new Alert(AlertType.CONFIRMATION, "Update successful. New price table written to " + Launcher.tableHome.resolve("Prices.csv"), ButtonType.OK).showAndWait();
      }
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
    }
  }
}
