package com.example;

import com.example.api.ElpriserAPI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;



public class Main {
    public static void main(String[] args) {

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        System.out.println("--- Hämtar Data ---");
        LocalDate today=LocalDate.now();

        List<ElpriserAPI.Elpris> dagensPris =
                elpriserAPI.getPriser(today, ElpriserAPI.Prisklass.SE3);

        System.out.println("Dagens priser för; " + ElpriserAPI.Prisklass.SE3);
        DateTimeFormatter timeFormatter=DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < dagensPris.size(); i++) {
            ElpriserAPI.Elpris elpris =dagensPris.get(i);
            String startTid=elpris.timeStart().toLocalTime().format(timeFormatter);
            String slutTid=elpris.timeEnd().toLocalTime().format(timeFormatter);
            System.out.println(startTid + " - " + slutTid + " | "
                    + elpris.sekPerKWh() + " kr/kWh");
            System.out.println(" ----------- ");


        }

        //För imorgon:
        LocalDate imorgon=today.plusDays(1);
        List<ElpriserAPI.Elpris> morgonDagensPris =
                elpriserAPI.getPriser(imorgon, ElpriserAPI.Prisklass.SE3);

        System.out.println("Morgondagens priser för; " + ElpriserAPI.Prisklass.SE3);

        if(morgonDagensPris==null || morgonDagensPris.isEmpty()){
            System.out.println(" Inga priser tillgängliga för " + imorgon + " i området: " + ElpriserAPI.Prisklass.SE3);
        } else {
            for (int i = 0; i < morgonDagensPris.size(); i++) {
                ElpriserAPI.Elpris elpris =morgonDagensPris.get(i);
                String startTid=elpris.timeStart().toLocalTime().format(timeFormatter);
                String slutTid=elpris.timeEnd().toLocalTime().format(timeFormatter);
                System.out.println(startTid + " - " + slutTid + " | "
                        + elpris.sekPerKWh() + " kr/kWh");
                System.out.println(" ----------- ");
        }

        }

        // NU ska vi hitta medelpriser för 24H, vi sätter den som en double.

        double sum=0.0;
        for (ElpriserAPI.Elpris elpris : dagensPris){
            sum += elpris.sekPerKWh();
        }

        //dividera med antalet
        double medelPris= sum/ dagensPris.size();
        System.out.println(" ----------------- ");
        System.out.println(" Medelpriset för idag är: " + medelPris + " kr/kWh");
        
        // hitta billigaste och dyraste timmen

        double lägstaPris=dagensPris.getFirst().sekPerKWh();
        double högstaPris=dagensPris.getFirst().sekPerKWh();
        for (ElpriserAPI.Elpris elpris : dagensPris) {
            double pris=elpris.sekPerKWh();

            if(pris<lägstaPris){
                lägstaPris=pris;
            }
            if(pris>högstaPris){
                högstaPris=pris;
            }
            
        }

        System.out.println(" Lägsta priset idag är: " + lägstaPris + " kr/kWh ");
        System.out.println(" Högsta priset idag är: " + högstaPris + " kr/kWh ");














    }









}
