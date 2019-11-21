package com.example.streams.usecase.model;



public class Usecase{

	private String sensorId;
	private int lon;
	private int lat;
	private double temperature;
	private int humidityAbs;
	private double pm2;
	private double pm10;
	private String measurement;

	public Usecase() {

	}

	public Usecase(String sensorId, int lon, int lat, double temperature, int humidityAbs, double pm2, double pm10, String measurement) {
		this.sensorId = sensorId;
		this.lon = lon;
		this.lat = lat;
		this.temperature = temperature;
		this.humidityAbs = humidityAbs;
		this.pm2 = pm2;
		this.pm10 = pm10;
		this.measurement = measurement;
	}

	public String getSensorId() {
		return sensorId;
	}

	public void setSensorId(String sensorId) {
		this.sensorId = sensorId;
	}

	public int getLon() {
		return lon;
	}

	public void setLon(int lon) {
		this.lon = lon;
	}

	public int getLat() {
		return lat;
	}

	public void setLat(int lat) {
		this.lat = lat;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public int getHumidityAbs() {
		return humidityAbs;
	}

	public void setHumidityAbs(int humidityAbs) {
		this.humidityAbs = humidityAbs;
	}

	public double getPm2() {
		return pm2;
	}

	public void setPm2(double pm2) {
		this.pm2 = pm2;
	}

	public double getPm10() {
		return pm10;
	}

	public void setPm10(double pm10) {
		this.pm10 = pm10;
	}

	public String getMeasurement() {
		return measurement;
	}

	public void setMeasurement(String measurement) {
		this.measurement = measurement;
	}

	@Override
	public String toString() {
		return "Usecase{" +
				"sensorId='" + sensorId + '\'' +
				", lon=" + lon +
				", lat=" + lat +
				", temperature=" + temperature +
				", humidityAbs=" + humidityAbs +
				", pm2=" + pm2 +
				", pm10=" + pm10 +
				", measurement='" + measurement + '\'' +
				'}';
	}
}
