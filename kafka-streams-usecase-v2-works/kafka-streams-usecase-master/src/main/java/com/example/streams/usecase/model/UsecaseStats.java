package com.example.streams.usecase.model;

import com.example.streams.usecase.model.*;

public class UsecaseStats{


  double temperature;
  double temperatureF;
  int humidityAbs;
  int humidityRel;


  public UsecaseStats add(Usecase ucase){

      this.temperature = ucase.getTemperature();
      this.humidityAbs = ucase.getHumidityAbs();
      // Umrechnung von Celcius in Fahrenheit
        if (this.temperatureF == 0){
          this.temperatureF=this.temperature*(1.8)+ 32;
        }

    return this;

  }

  public UsecaseStats huhu(){
    this.humidityRel = this.humidityAbs + 10;
    return this;
  }


}
