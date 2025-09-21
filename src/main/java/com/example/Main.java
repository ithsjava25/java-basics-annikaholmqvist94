package com.example;

import com.example.api.ElpriserAPI;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;



public class Main {
    public static void main(String[] args) {

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        System.out.println("--- Hämtar Data ---");
        LocalDate today = LocalDate.now();

        List<ElpriserAPI.Elpris> dagensPris =
                elpriserAPI.getPriser(today, ElpriserAPI.Prisklass.SE3);

        System.out.println("Dagens priser för; " + ElpriserAPI.Prisklass.SE3);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (int i = 0; i < dagensPris.size(); i++) {
            ElpriserAPI.Elpris elpris = dagensPris.get(i);
            String startTid = elpris.timeStart().toLocalTime().format(timeFormatter);
            String slutTid = elpris.timeEnd().toLocalTime().format(timeFormatter);
            System.out.println(startTid + " - " + slutTid + " | "
                    + elpris.sekPerKWh() + " kr/kWh");
            System.out.println(" ----------- ");


        }

        //För imorgon:
        LocalDate imorgon = today.plusDays(1);
        List<ElpriserAPI.Elpris> morgonDagensPris =
                elpriserAPI.getPriser(imorgon, ElpriserAPI.Prisklass.SE3);

        System.out.println("Morgondagens priser för; " + ElpriserAPI.Prisklass.SE3);

        if (morgonDagensPris == null || morgonDagensPris.isEmpty()) {
            System.out.println(" Inga priser tillgängliga för " + imorgon + " i området: " + ElpriserAPI.Prisklass.SE3);
        } else {
            for (int i = 0; i < morgonDagensPris.size(); i++) {
                ElpriserAPI.Elpris elpris = morgonDagensPris.get(i);
                String startTid = elpris.timeStart().toLocalTime().format(timeFormatter);
                String slutTid = elpris.timeEnd().toLocalTime().format(timeFormatter);
                System.out.println(startTid + " - " + slutTid + " | "
                        + elpris.sekPerKWh() + " kr/kWh");
                System.out.println(" ----------- ");
            }

        }

        // NU ska vi hitta medelpriser för 24H, vi sätter den som en double.

        double sum = 0.0;
        for (ElpriserAPI.Elpris elpris : dagensPris) {
            sum += elpris.sekPerKWh();
        }

        //dividera med antalet
        double medelPris = sum / dagensPris.size();
        System.out.println(" Medelpriset för idag är: " + medelPris + " kr/kWh");

        // hitta billigaste och dyraste timmen

        double lägstaPris = dagensPris.getFirst().sekPerKWh();
        double högstaPris = dagensPris.getFirst().sekPerKWh();

        LocalDateTime tidLägsta = dagensPris.getFirst().timeStart().toLocalDateTime();
        LocalDateTime tidHögsta = dagensPris.getFirst().timeStart().toLocalDateTime();

        for (ElpriserAPI.Elpris elpris : dagensPris) {
            double pris = elpris.sekPerKWh();
            //en till parameter för tiden som behöver kollas
            //om priset är det lägsta och det är tidsmässigt före det lägsta ska det appliceras
            LocalDateTime startTid = elpris.timeStart().toLocalDateTime();

            if (pris < lägstaPris || pris == lägstaPris && startTid.isBefore(tidLägsta)) {
                lägstaPris = pris;
                tidLägsta = startTid;
            }
            if (pris > högstaPris || pris == högstaPris && startTid.isBefore(tidHögsta)) {
                högstaPris = pris;
                tidHögsta = startTid;
            }

        }

        System.out.println(" Lägsta priset idag är: " + lägstaPris + " kr/kWh och det är klockan "
                + tidLägsta.format(timeFormatter));
        System.out.println(" Högsta priset idag är: " + högstaPris + " kr/kWh och det är klockan "
                + tidHögsta.format(timeFormatter));

        //vi behöver kolla om det finns flera timmar som är lika och sätta villkor att det tidigaste ska väljas

        //kolla i loopen med villkor för billigaste tidsintervall för 2,4 och 8 timmar.
        // Använda duration i Localtimedate?
        //bästa starttiden till sluttiden
        LocalDateTime bästAttStarta = null;
        LocalDateTime bästAttSluta = null;
        //vill hitta detta intervallet, duration.between bästAttStarta och bästAttSluta
        double lägstaMedelPris2H = Double.MAX_VALUE; //vi börjar bakifrån med det högsta vi kan hitta.Så fort
        // vi hittat detta ska vi byta det mot nästa som är mindre->

        for (int i = 0; i < dagensPris.size()-1; i++) { //vi ska hitta de två värdena som ger minst medelvärde
            // (första o andra timmen)
            ElpriserAPI.Elpris pris1 = dagensPris.get(i);
            ElpriserAPI.Elpris pris2 = dagensPris.get(i + 1);

            double medelPris2H = (pris1.sekPerKWh() + pris2.sekPerKWh()) / 2.0;


            if (medelPris2H < lägstaMedelPris2H) {
                lägstaMedelPris2H = medelPris2H;
                bästAttStarta = pris1.timeStart().toLocalDateTime();
                bästAttSluta = pris2.timeEnd().toLocalDateTime();
            }


        }
        if(bästAttStarta!=null && bästAttSluta!=null){
            Duration bästaIntervallet= Duration.between(bästAttStarta,bästAttSluta);

            System.out.println("-------------------------------");
            System.out.println("Om du vill ladda så billigt som möjligt under 2 timmar är det kl: ");
            System.out.println(bästAttStarta.toLocalTime().format(timeFormatter) + " till "
                    + bästAttSluta.toLocalTime().format(timeFormatter) + " för "
                    + lägstaMedelPris2H + " kr/kWh");
            System.out.println("-------------------------------");

            //vi behöver hitta det lägsta värdet(sek kr/kwh i loopen
        }


    }

}








