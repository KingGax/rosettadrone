package multidrone.coordinates;

public class GeodeticCoordinate {
    public float lat;
    public float lng;
    public float height;

    public GeodeticCoordinate(){

    }

    public GeodeticCoordinate(float _lat, float _lng, float _height){
        lat = _lat;
        lng = _lng;
        height = _height;
    }
}
