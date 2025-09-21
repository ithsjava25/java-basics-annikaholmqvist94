package com.example;

import java.time.LocalDate;
import java.util.Objects;

public class Elpris {
    private LocalDate datum;
    private double pris;
    private String område;

    public Elpris(LocalDate datum, double pris, String område){
        this.datum=datum;
        this.pris=pris;
        this.område=område;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    public double getPris() {
        return pris;
    }

    public void setPris(double pris) {
        this.pris = pris;
    }

    public String getOmråde() {
        return område;
    }

    public void setOmråde(String område) {
        this.område = område;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Elpris elpris)) return false;
        return Double.compare(pris, elpris.pris) == 0
                && Objects.equals(datum, elpris.datum)
                && Objects.equals(område, elpris.område);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datum, pris, område);
    }

    @Override
    public String toString() {
        return "Elpris{" +
                "datum=" + datum +
                ", pris=" + pris +
                ", område='" + område + '\'' +
                '}';
    }
}
