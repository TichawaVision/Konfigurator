package de.tichawa.cis.config;

class CISException extends NullPointerException
{
  private final String message;
  
  public CISException(String message)
  {
    this.message = message;
  }
  
  @Override
  public String getMessage()
  {
    return message;
  }
}
