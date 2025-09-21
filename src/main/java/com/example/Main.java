package com.example;

import com.example.api.ElpriserAPI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;



public class Main {
    public static void main(String[] args) {

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        System.out.println("--- Fetching Data ---");
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








    }









}
