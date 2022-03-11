package de.tichawa.cis.config;

import lombok.*;

import java.util.*;
import java.util.stream.*;

@Value
public class CameraLink
{
  long dataRate;
  long pixelCount;
  long pixelClock;

  @Getter(AccessLevel.PRIVATE)
  LinkedList<Connection> connections = new LinkedList<>();

  public void addConnection(Connection connection)
  {
    int nextCLId = getConnections().isEmpty() ? Connection.DEFAULT_ID : getConnections().getLast().getId();
    getConnections().add(connection.withId(nextCLId));
  }

  public int getConnectionCount()
  {
    return getConnections().size();
  }

  public int getPortCount()
  {
    return getConnections().stream()
        .mapToInt(Connection::getPortCount)
        .sum();
  }

  //TODO: Load texts from bundle
  @Override
  public String toString()
  {
    StringBuilder output = new StringBuilder("Data rate: ")
        .append(Math.round(getDataRate() / 100000.0) / 10.0)
        .append(" MByte/s\n");
    output.append("Number of pixels: ")
        .append(getPixelCount())
        .append("\n");
    output.append("Number of connections: ")
        .append(getConnectionCount())
        .append("\n");
    output.append("Number of ports: ")
        .append(getPortCount())
        .append("\n");
    output.append("Pixel clock: ")
        .append(getPixelClock() / 1000000)
        .append("MHz\n");

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
    public static final int MAX_PORT_COUNT = 8;

    int id;

    public Connection()
    {
      this(DEFAULT_ID);
    }

    @Getter(AccessLevel.PRIVATE)
    LinkedList<Port> ports = new LinkedList<>();

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
    public static final char DEFAULT_NAME = 'A';

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
      return "Port " + getName() + noteString + ": "
          + String.format("%05d", getStartPixel()) + " - " + String.format("%05d", getEndPixel());
    }
  }
}
