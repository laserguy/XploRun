package com.xplorun.model;

import org.osmdroid.util.GeoPoint;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.converter.PropertyConverter;

@Entity
public class RunStat {
    @Id
    public long id;
    public int counter;
    public long startTime;
    public long endTime;
    public double distance;
    public double avgSpeed;
    public double topSpeed;
    public double calories;
    public double elevation;
    public String environment;
    public double urbanDistance, natureDistance = 0;

    @Convert(converter = GeoPointConverter.class, dbType = String.class)
    public GeoPoint startPoint;
    @Convert(converter = GeoPointConverter.class, dbType = String.class)
    public GeoPoint endPoint;

    public RunStat() {
    }

    public int duration() {
        return (int) (endTime - startTime);
    }

    public int duration(long endTime) {
        return (int) (endTime - startTime);
    }

    public double averageSpeed() {
        return avgSpeed / counter;
    }

    static class GeoPointConverter implements PropertyConverter<GeoPoint, String> {
        @Override
        public GeoPoint convertToEntityProperty(String databaseValue) {
            return GeoPoint.fromDoubleString(databaseValue, ',');
        }

        @Override
        public String convertToDatabaseValue(GeoPoint entityProperty) {
            return entityProperty.toDoubleString();
        }
    }
}
