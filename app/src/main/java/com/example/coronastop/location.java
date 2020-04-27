package com.example.coronastop;

public class location {
    private String name;
    private double x;
    private double y;
    private String time;

    public location(String name, double x, double y, String time) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
