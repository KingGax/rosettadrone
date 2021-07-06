package multidrone.coordinates;

import static multidrone.coordinates.CoordinateTranslator.e_eccentricity_squared;

public class GlobalRefrencePoint {
    public double lat;
    public double lng;
    public double h;
    public double xe;
    public double ye;
    public double ze;

    public GlobalRefrencePoint(double _lat, double _lng, double _h){
        lat = _lat;
        lng = _lng;
        h = _h;
        double lat_r = Math.toRadians(_lat);
        double lng_r = Math.toRadians(_lng);
        float Ne = CoordinateTranslator.getVerticalRadius(lat_r);

        xe =  ((Ne + _h) * Math.cos(lat_r) * Math.cos(lng_r));
        ye =  ((Ne + _h) * Math.cos(lat_r) * Math.sin(lng_r));
        ze =  ((Ne*(1-e_eccentricity_squared) + _h) * Math.sin(lat_r));

    }
}
