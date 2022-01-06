package de.tichawa.cis.config;

import java.sql.*;
import java.util.*;
import java.util.stream.*;

public class FerixSynchronizer
{

  private Connection con;
  private CIS cis;

  public FerixSynchronizer()
  {
    try
    {
      con = DriverManager.getConnection("jdbc:postgresql://10.0.0.114:5432/FerixDB", "postgres", "ferix");
    }
    catch(SQLException ex)
    {
      ex.printStackTrace();
    }
  }

  private boolean exists()
  {
    boolean exists = true;
    try(PreparedStatement existsQuery = con.prepareStatement("SELECT * FROM f_art4 WHERE a_bez1 = ?;"))
    {
      existsQuery.setString(1, cis.getTiViKey());
      exists = existsQuery.executeQuery().next();
    }
    catch(SQLException ex)
    {
      ex.printStackTrace();
    }
    return exists;
  }

  private int getNextArtNo()
  {
    int artNo = -1;
    try(PreparedStatement artNoQuery = con.prepareStatement("SELECT a_artikel FROM f_art4;"))
    {
      Set<Integer> artNoMap = new HashSet<>();
      ResultSet artNos = artNoQuery.executeQuery();
      while(artNos.next())
      {
        if(CIS.isInteger(artNos.getString(1)))
        {
          artNoMap.add(Integer.parseInt(artNos.getString(1)));
        }
      }
      artNo = IntStream.iterate(1000, i -> ++i).filter(i -> !artNoMap.contains(i)).findFirst().orElse(-1);
    }
    catch(SQLException ex)
    {
      ex.printStackTrace();
    }
    return artNo;
  }

  public boolean insert()
  {
    boolean success = false;
    if(!exists())
    {
      Integer artNo = getNextArtNo();
      if(artNo > -1)
      {
        try(PreparedStatement insertArt = con.prepareStatement("INSERT INTO f_art4 (a_artikel, a_bez1, a_bez2, a_teileart, a_umfakt, a_lagst, a_cre_dat,a_mod_dat, a_mod_sachb, a_lagerort, a_umfakt_vk)"
                + "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?, ?);");
                PreparedStatement insertBest = con.prepareStatement("INSERT INTO f_best (b_artikel, b_cre_dat, b_mod_dat, b_mod_sachb) VALUES (?, NOW(), NOW(), ?);");
                PreparedStatement insertNotice = con.prepareStatement("INSERT INTO Artikel_nt (ant_schl, ant_text) VALUES (?, ?);");
                PreparedStatement insertLongtext = con.prepareStatement("INSERT INTO f_art4_lt (artlt_schl, artlt_txt) VALUES (?, ?);"))
        {
          String text = cis.createPrntOut();
          insertArt.setString(1, artNo.toString());
          insertArt.setString(2, cis.getTiViKey());
          insertArt.setString(3, text.substring(0, text.indexOf('\n'))); //TODO
          insertArt.setInt(4, 1);
          insertArt.setInt(5, 1);
          insertArt.setString(6, "A");
          insertArt.setString(7, "TiViCC");
          insertArt.setString(8, ".");
          insertArt.setInt(9, 1);
          
          if(insertArt.executeUpdate() > 0)
          {
            insertBest.setString(1, artNo.toString());
            insertBest.setString(2, "TiViCC");
            insertNotice.setString(1, artNo.toString());
            insertNotice.setString(2, "");
            insertLongtext.setString(1, artNo.toString());
            insertLongtext.setString(2, text.substring(text.indexOf('\n') + 1)); //TODO
            
            success = insertBest.executeUpdate() > 0 && insertNotice.executeUpdate() > 0 && insertLongtext.executeUpdate() > 0;
            if(success)System.out.println("Angelegt mit ID " + artNo);
          }
        }
        catch(SQLException ex)
        {
          ex.printStackTrace();
        }
      }
    }
    return success;
  }

  public FerixSynchronizer setCIS(CIS cis)
  {
    this.cis = cis;
    return this;
  }

  public void close()
  {
    if(con != null)
    {
      try
      {
        con.close();
      }
      catch(SQLException ignored)
      {}
    }
  }
}
