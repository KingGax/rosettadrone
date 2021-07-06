package multidrone.coordinates;

import org.ejml.simple.SimpleMatrix;

public class CoordinateTranslator {

    final static float e_eccentricity = 0.08181919f;
    final static float e_eccentricity_squared = 0.00669437985f;
    final static float r_ea_semi_major_axis = 6_378_137;
    public static NEDCoordinate GeodeticToNED(GeodeticCoordinate coord, GlobalRefrencePoint ref){
        double lat_r = Math.toRadians(coord.lat);
        double lng_r = Math.toRadians(coord.lng);
        float Ne = getVerticalRadius(lat_r);

        double xe =  ((Ne + coord.height) * Math.cos(lat_r) * Math.cos(lng_r));
        double ye =  ((Ne + coord.height) * Math.cos(lat_r) * Math.sin(lng_r));
        double ze =  ((Ne*(1-e_eccentricity_squared) + coord.height) * Math.sin(lat_r));

        double ref_lat_r = Math.toRadians(ref.lat);
        double ref_lng_r = Math.toRadians(ref.lng);

        double[][] rotArray = {
                {-Math.sin(ref_lat_r) * Math.cos(ref_lng_r),-Math.sin(ref_lat_r) * Math.sin(ref_lng_r),Math.cos(ref_lat_r)},
                {-Math.sin(ref_lng_r),Math.cos(ref_lng_r),0},
                {-Math.cos(ref_lat_r) * Math.cos(ref_lng_r),-Math.cos(ref_lat_r) * Math.sin(ref_lng_r),-Math.sin(ref_lat_r)}
        };

        double[][] peArray = {
                {xe - ref.xe},
                {ye - ref.ye},
                {ze - ref.ze}
        };

        SimpleMatrix pe = new SimpleMatrix(peArray);
        SimpleMatrix rot = new SimpleMatrix(rotArray);
        SimpleMatrix nedMatrix = rot.mult(pe);




        return new NEDCoordinate(nedMatrix.get(0),nedMatrix.get(1),nedMatrix.get(2));
    }

    public static BodyCoordinate GeodeticToBody(GeodeticCoordinate coord, GlobalRefrencePoint ref, double yaw){
        NEDCoordinate temp = GeodeticToNED(coord, ref);
        return NEDToBody(temp,yaw);
    }

    public static BodyCoordinate NEDToBody(NEDCoordinate coord, double yaw){

        double negyaw = -yaw;
        double[][] rotArray = {
                {Math.cos(negyaw),-Math.sin(negyaw),0},
                {Math.sin(negyaw),Math.cos(negyaw),0},
                {0,0,1}
        };

        double[][] peArray = {
                {coord.x},
                {coord.y},
                {coord.z}
        };

        SimpleMatrix pe = new SimpleMatrix(peArray);
        SimpleMatrix rot = new SimpleMatrix(rotArray);
        SimpleMatrix nedMatrix = rot.mult(pe);




        return new BodyCoordinate(nedMatrix.get(0),nedMatrix.get(1),nedMatrix.get(2));
    }

    static float getVerticalRadius(double lat){
        return (float) (r_ea_semi_major_axis / (Math.sqrt(1-(e_eccentricity_squared*Math.pow(Math.sin(lat),2)))));
    }


}
