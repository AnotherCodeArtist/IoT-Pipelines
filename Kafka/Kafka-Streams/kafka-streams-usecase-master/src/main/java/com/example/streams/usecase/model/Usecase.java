package com.example.streams.usecase.model;

//POJO
public class Usecase{

	private String sensorId;
	private int lon;
	private int lat;
	private double temperature;
	private int humidity;
	private double pm2;
	private String measurement;

	public Usecase() {

	}

	public Usecase(String sensorId, int lon, int lat, double temperature, int humidity, double pm2, String measurement) {
		this.sensorId = sensorId;
		this.lon = lon;
		this.lat = lat;
		this.temperature = temperature;
		this.humidity = humidity;
		this.pm2 = pm2;
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

	public int getHumidity() {
		return humidity;
	}

	public void setHumidity(int humidityAbs) {
		this.humidity = humidity;
	}

	public double getPm2() {
		return pm2;
	}

	public void setPm2(double pm2) {
		this.pm2 = pm2;
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
				", humidity=" + humidity +
				", pm2=" + pm2 +
				", measurement='" + measurement + '\'' +
				'}';
	}
}
