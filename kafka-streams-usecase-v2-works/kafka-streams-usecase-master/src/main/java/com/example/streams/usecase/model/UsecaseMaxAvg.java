package com.example.streams.usecase.model;

public class UsecaseMaxAvg {

    private double pm2;
    private double maxPm2 = -1;
    private double avgPm2;
    private int countUsecase;
    private double sumPm2;

    public UsecaseMaxAvg max(Usecase ucase){

        this.pm2 = ucase.getPm2();
        this.countUsecase = this.countUsecase+1;
        this.sumPm2 = this.sumPm2 + ucase.getPm2();
        this.maxPm2 = Math.max(this.maxPm2, ucase.getPm2());
        return this;
    }


    public UsecaseMaxAvg computeAvgPrice() {
        this.avgPm2 = this.sumPm2 / this.countUsecase;
        return this;

    }
}
