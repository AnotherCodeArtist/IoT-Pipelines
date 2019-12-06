package com.example.streams.usecase.model;

import org.javatuples.Pair;

//POJO
public class UsecaseTrend {

    private String sensorId;
    private String measurement;
    private boolean tooHigh = false;
    private int tooHighCount= 0;
    private String area;
    private double avgPm2;
    private int countUsecase;
    private CustomPair pair = new CustomPair(-1, 0,-1000);
    private double sumPm2;
    private double maxPm2 = -1;
    private double temperatureCMax = -1;
    private double temperatureFMax= -1;

    private int humidityMax = -1;
    
    public UsecaseTrend calc(Usecase uca) {

             boolean tooLow = false;
            this.measurement= uca.getMeasurement();
            this.sensorId = uca.getSensorId();

            //Maximums in one window
            this.temperatureCMax = Math.max(this.temperatureCMax, uca.getTemperature());
            this.humidityMax = Math.max(this.humidityMax, uca.getHumidity());
            this.maxPm2 = Math.max(this.maxPm2, uca.getPm2());
            this.temperatureFMax = Math.max(this.temperatureFMax, uca.getTemperature() *(1.8)+32);


            //Needed for the average and informative data
            this.countUsecase = this.countUsecase + 1;
            this.sumPm2 = this.sumPm2 + uca.getPm2();


            //calculating the area with lon and lat
            Pair<Integer, Integer> areas = Pair.with((uca.getLon() / 25), (uca.getLat() / 25));
            this.area = "Area " + areas.toString();

        if (pair.getFirst() <= uca.getPm2()) {

            if (pair.getFirst() == -1) {
                pair.setSecond(uca.getPm2());
                pair.setFirst(0);
            } else {
                pair.setFirst((uca.getPm2() - pair.getSecond()) + pair.getFirst());
                pair.setSecond(uca.getPm2());

                if (pair.getFirst() >= 1) {
                    tooHigh = true;
                    tooHighCount += 1;
                }
                return this;
            }

            return this;
        }
        // this is against the fluctuations
        if (pair.getThird() >= uca.getPm2()) {

            if (pair.getThird() == -1000) {
                pair.setSecond(uca.getPm2());
                pair.setThird(0);
            } else {
                pair.setThird((uca.getPm2() - pair.getSecond()) + pair.getThird());
                pair.setSecond(uca.getPm2());

                if (pair.getThird() >= 1) {
                    tooLow = true;
                }
                if (tooHigh && tooLow){
                    tooHigh = false;
                }
                return this;
            }

            return this;
        }
        return this;
    }


    // calculates the average
    public UsecaseTrend computeA(){
        this.avgPm2 = this.sumPm2 / this.countUsecase;
        return this;

    }

    public boolean isTooHigh() {
        return tooHigh;
    }


    public String getArea() {
        return area;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public void setTooHigh(boolean tooHigh) {
        this.tooHigh = tooHigh;
    }

    public int getTooHighCount() {
        return tooHighCount;
    }

    public void setTooHighCount(int tooHighCount) {
        this.tooHighCount = tooHighCount;
    }

    public double getMaxPm2() {
        return maxPm2;
    }

    public void setMaxPm2(double maxPm2) {
        this.maxPm2 = maxPm2;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public double getAvgPm2() {
        return avgPm2;
    }

    public void setAvgPm2(double avgPm2) {
        this.avgPm2 = avgPm2;
    }

    public int getCountUsecase() {
        return countUsecase;
    }

    public void setCountUsecase(int countUsecase) {
        this.countUsecase = countUsecase;
    }

    public double getSumPm2() {
        return sumPm2;
    }

    public void setSumPm2(double sumPm2) {
        this.sumPm2 = sumPm2;
    }

    public double getTemperatureCMax() {
        return temperatureCMax;
    }

    public void setTemperatureCMax(double temperatureCMax) {
        this.temperatureCMax = temperatureCMax;
    }


    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }


    public double getTemperatureFMax() {
        return temperatureFMax;
    }

    public void setTemperatureFMax(double temperatureFMax) {
        this.temperatureFMax = temperatureFMax;
    }

    public int getHumidityMax() {
        return humidityMax;
    }

    public void setHumidityMax(int humidityMax) {
        this.humidityMax = humidityMax;
    }
}
