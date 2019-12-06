package com.example.streams.usecase.model;

//Custumdatatype for Triplet
public class CustomPair {
    private double first;
    private double second;
    private double third;


    public CustomPair(double first, double second, double third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public double getFirst() {
        return first;
    }

    public void setFirst(double first) {
        this.first = first;
    }

    public double getSecond() {
        return second;
    }

    public void setSecond(double second) {
        this.second = second;
    }

    public double getThird() { return third; }

    public void setThird(double third) {this.third = third; }
}
