package de.tichawa.cis.config;

import de.tichawa.util.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
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
      if(new Alert(AlertType.CONFIRMATION, "Please wait until FERIX has finished the export to " + Launcher.ferixHome.resolve("Export/art.csv") + ", then press OK to continue.\nLast update: "
              + DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss").withZone(ZoneId.systemDefault()).format(Files.getLastModifiedTime(Launcher.tableHome.resolve("Back/Prices.csv")).toInstant()),
              ButtonType.OK, ButtonType.CANCEL).showAndWait()
              .orElse(ButtonType.CANCEL) == ButtonType.OK)
      {
        Map<Integer, Tuple<Double, Boolean>> sourcePrices = Files.readAllLines(Launcher.ferixHome.resolve("Export/art.csv")).stream()
                .map(line -> line.split("\t"))
                .filter(line -> CIS.isInteger(line[0]) && CIS.isDouble(line[1]))
                .peek(line -> {
                  if(line.length > 3)
                  {
                    System.out.println(line);
                  }
                })
                .map(line -> new Tuple<>(Integer.parseInt(line[0]), new Tuple<>(Double.parseDouble(line[1]) / CIS.decodeQuantity(line[2]), line.length < 4 || line[3].equals("A"))))
                .filter(t -> t.getV().getU() > 0)
                .collect(Collectors.toMap(Tuple::getU, Tuple::getV, (oldVal, newVal) -> newVal));

        Files.copy(Launcher.tableHome.resolve("Prices.csv"), Launcher.tableHome.resolve("Back/Prices.csv"), StandardCopyOption.REPLACE_EXISTING);
        Files.write(Launcher.tableHome.resolve("Prices.csv"), (Iterable<String>) Files.readAllLines(Launcher.tableHome.resolve("Prices.csv")).stream()
                .map(line -> line.split("\t"))
                .filter(line -> CIS.isInteger(line[0]))
                .map(line -> new Tuple<>(Integer.parseInt(line[0]), line))
                //.filter(t -> sourcePrices.containsKey(t.getU()))
                //.filter(t -> sourcePrices.get(t.getU()) != Double.parseDouble(t.getV()[2]))
                .peek(t ->
                {
                  if(sourcePrices.containsKey(t.getU()))
                  {
                    t.getV()[2] = sourcePrices.get(t.getU()).getU().toString();
                  }
                })
                .map(Tuple::getV)
                .map(line -> String.join("\t", line))::iterator, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Update successful. New price table written to " + Launcher.tableHome.resolve("Prices.csv"));
        alert.showAndWait();

        Set<Integer> components = Files.readAllLines(Launcher.tableHome.resolve("Prices.csv")).stream()
                .map(line -> line.split("\t", 2))
                .map(line -> line[0])
                .filter(CIS::isInteger)
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        System.out.println("Test");
        List<String> inactiveComponents = sourcePrices.entrySet().stream()
                .filter(e -> !e.getValue().getV())
                .map(Map.Entry::getKey)
                .peek(System.out::println)
                .filter(components::contains)
                .peek(System.out::println)
                .map(Object::toString)
                .collect(Collectors.toList());
        if(!inactiveComponents.isEmpty())
        {
          Alert inactiveAlert = new Alert(AlertType.WARNING);
          inactiveAlert.setHeaderText("Some components are inactive now. Please consider replacing the components with these IDs:");
          inactiveAlert.setContentText(String.join("\n", inactiveComponents));
          inactiveAlert.showAndWait();
        }
      }
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
    }
  }
}
