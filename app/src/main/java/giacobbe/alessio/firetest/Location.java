package giacobbe.alessio.firetest;

/**
 * Created by assas on 29/10/2016.
 */

public class Location {

    public Double Latitude;
    public Double Longitude;
    public String Title;
    public String UserName;
    public String type;


    public Location(){

    }

    public Location(String titolo, Double lat, Double longi, String user, String tipo){
        Title  = titolo;
        Latitude = lat;
        Longitude = longi;
        UserName = user;
        type = tipo;
    }
}
