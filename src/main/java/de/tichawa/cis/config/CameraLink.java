package de.tichawa.cis.config;

import lombok.*;
import java.util.*;
import java.util.stream.Collectors;

import static de.tichawa.cis.config.CIS.locale;

@Value
@AllArgsConstructor
public class CameraLink
{
  long dataRate;
  long pixelCount;
  long pixelClock;
  String notes;

  @Getter(AccessLevel.PRIVATE)
  LinkedList<Connection> connections = new LinkedList<>();

  public CameraLink(long dataRate, long pixelCount, long pixelClock)
  {
    this(dataRate, pixelCount, pixelClock, null);
  }

  public void addConnection(Connection connection)
  {
    int nextCLId = getConnections().isEmpty() ? Connection.DEFAULT_ID : getConnections().getLast().getId() + 1;
    getConnections().add(connection.withId(nextCLId));
  }

  public int getConnectionCount()
  {
    int kabelCount;
    if(getPortCount() / getConnections().size() <= 2)
    {
      kabelCount = getConnections().size();
    }
    else if(getPortCount() > 3 && getConnections().size() == 1)
    {
      kabelCount = 2 ;
    }
    else
    {
      kabelCount = getPortCount() / 3;
    }
    return kabelCount;

  }

  public int getPortCount()
  {
    return getConnections().stream()
        .mapToInt(Connection::getPortCount)
        .sum();
  }

  public int getPortNumber(){
    return getConnections().stream()
            .mapToInt(Connection::getPortNumber)
            .sum();
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder(getString("datarate"))
            .append(Math.round(getDataRate() / 100000.0) / 10.0)
            .append(" MByte/s\n");
    output.append(getString("nomPix"))
            .append(getPixelCount())
            .append("\n");
    output.append(getString("numofcons"))
            .append(getConnectionCount())
            .append("\n");
    output.append(getString("numofport"))
            .append(getPortNumber())
            .append("\n");
    output.append("Pixel clock: ")
            .append(getPixelClock() / 1000000)
            .append("MHz\n");
    if(getNotes() != null)
    {
      output.append(getNotes())
              .append("\n");
    }

    output.append(getConnections().stream()
            .map(Connection::toString)
            .collect(Collectors.joining("\n")));
    return output.toString();
  }

  @With
  @Value
  @AllArgsConstructor(access=AccessLevel.PRIVATE)
  public static class Connection
  {
    public static final int DEFAULT_ID = 1;
    public static final int MAX_PORT_COUNT = 10;

    int id;
    char defaultPort;
    @Getter(AccessLevel.PRIVATE)
    LinkedList<Port> ports;

    public Connection() { this(DEFAULT_ID, Port.DEFAULT_NAME);}

    public Connection(int defaultId, char defaultPort) {this(defaultId, defaultPort, new LinkedList<>());}

    public boolean addPorts(Port... ports)
    {
      if(getPorts().size() + ports.length <= MAX_PORT_COUNT)
      {
        for(Port port : ports)
        {
          char nextPortName = getPorts().isEmpty() ? Port.DEFAULT_NAME : (char) (getPorts().getLast().getName() + 1);
          getPorts().add(port.withName(nextPortName));
        }

        return true;
      }
      else
      {
        return false;
      }
    }

    public int getPortCount()
    {
      return getPorts().size();
    }

    public int getPortNumber()
    {
      int num = 0;
      for(int i = 0; i < getPortCount(); ++i)
      {
        if(!(getPorts().get(i).getEndPixel() == 0))
        {
          num++;
        }
      }
      return num;
    }

    @Override
    public String toString()
    {
      return "CameraLink " + getId() + ":\n" + getPorts().stream()
          .map(Port::toString)
          .map(s -> "    " + s)
          .collect(Collectors.joining("\n"));
    }
  }

  @With
  @Value
  @AllArgsConstructor(access=AccessLevel.PRIVATE)
  public static class Port
  {
    public static  char DEFAULT_NAME = 'A';

    char name;
    int startPixel;
    int endPixel;
    String note;

    public Port(int startPixel, int endPixel)
    {
      this(startPixel, endPixel, null);
    }

    public Port(int startPixel, int endPixel, String note)
    {
      this(DEFAULT_NAME, startPixel, endPixel, note);
    }

    public long getPixelCount()
    {
      return getEndPixel() - getStartPixel() + 1;
    }

    @Override
    public String toString()
    {
      String noteString = getNote() == null ? "" : " (" + getNote() + ")";
      if(getEndPixel() == 0)
      {
        return "Port " + getName() + ": " + "          " + " - " + " ";
      }
      return "Port " + getName() + noteString + ": "
                + String.format("%05d", getStartPixel()) + " - " + String.format("%05d", getEndPixel());
    }
  }
  public String getString(String key)
  {
    return ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", getLocale()).getString(key);
  }
  public Locale getLocale()
  {
    return locale;
  }
}
