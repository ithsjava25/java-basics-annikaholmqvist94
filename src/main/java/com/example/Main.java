package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;



public class Main {

    private static ElpriserAPI.Prisklass currentZone = null;
    private static LocalDate currentDate = LocalDate.now();
    private static boolean isSorted = false;
    private static Integer chargingHour = null;

    private static final ElpriserAPI elpriserAPI = new ElpriserAPI();

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private static class programArguments {
        final ElpriserAPI.Prisklass zone;
        final LocalDate date;
        final boolean sorted;
        final Integer chargingHour;
        final boolean showHelp;

        programArguments(ElpriserAPI.Prisklass zone, LocalDate date, boolean sorted, Integer chargingHour, boolean showHelp) {
            this.zone = zone;
            this.date = date;
            this.sorted = sorted;
            this.chargingHour = chargingHour;
            this.showHelp = showHelp;
        }
    }

    public static void main(String[] args) {

        if (args != null && args.length > 0) {


            try {
                programArguments params = parseArguments(args);
                if (params.showHelp) {
                    showHelp();
                    return;

                }

                if (params.zone == null) {
                    throw new IllegalStateException("Elområde måste anges med --zone.");
                }

                currentDate = params.date != null ? params.date : LocalDate.now();
                currentZone = params.zone;
                isSorted = params.sorted;
                chargingHour = params.chargingHour;

                hanteraPrisvisning();
            } catch (Exception e) {
                System.out.println("fel , se hjälp");
                showHelp();
            }


        } else {
            System.out.println("skriv help för kommando");

        }
    }

    private static programArguments parseArguments(String[] args) {
        ElpriserAPI.Prisklass zone = null;
        LocalDate date = null;
        boolean sorted = false;
        Integer chargingHour = null;
        boolean showHelp = false;


        for (int i = 0; i < args.length; i++) {

            String arg = args[i].toLowerCase();
            switch (arg) {
                case "--zone":
                    if (i + 1 < args.length) {
                        try {
                            zone = ElpriserAPI.Prisklass.valueOf(args[i + 1].toUpperCase());
                            i++;
                        }catch (IllegalArgumentException e){throw new IllegalArgumentException("ogiltigt elområde. VÄlj SE1, SE2, SE3, SE4.");
                    }
                    }else {
                        throw new IllegalArgumentException("saknar värde för --zone");
                    }
                    break;

                case "--date":
                    if (i + 1 < args.length) {
                        try {
                            date = LocalDate.parse(args[i + 1]);
                            i++;
                        }catch (DateTimeParseException e){throw new IllegalArgumentException("ogiltigt datumformat, använd YYY-MM-SS.");
                    }
                    }else {
                        throw new IllegalArgumentException("saknar värde för --date");
                    }
                    break;

                case "--sorted":
                    sorted = true;
                    break;

                case "--charging":
                    if (i + 1 < args.length) {
                        String value = args[i + 1].replace("h", "");
                        int hours = Integer.parseInt(value);
                        if (hours == 2 || hours == 4 || hours == 8) {
                            chargingHour = hours;
                        } else {
                            throw new IllegalArgumentException("ogilitg period, välj 2 h, 4h eller 8h. ");
                        }
                        i++;
                    } else {
                        throw new IllegalArgumentException("saknar värde för --charging");
                    }
                    break;

                case "--help":
                    showHelp = true;
                    break;

                default:
                    System.out.println("okänt kommando skriv ´help´ för hjälp");
            }


        }
        return new programArguments(zone, date, sorted, chargingHour, showHelp);

    }

    private static void hanteraPrisvisning() {
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(currentDate, currentZone);
        List<ElpriserAPI.Elpris> totalPriser = dagensPriser;

        if (chargingHour != null && chargingHour > 0) {
            List<ElpriserAPI.Elpris> morgondagensPriser = elpriserAPI.getPriser(currentDate.plusDays(1), currentZone);
            if (morgondagensPriser != null) {
                totalPriser = new java.util.ArrayList<>(dagensPriser);
                totalPriser.addAll(morgondagensPriser);
            }
        }
        if (totalPriser == null || totalPriser.isEmpty()) {
            System.out.println("inga tillgängliga priser för " + currentDate + " i området " + currentZone);
            return;

        }

        List<ElpriserAPI.Elpris> priserForDisplay=new java.util.ArrayList<>();
        for(ElpriserAPI.Elpris elpris : totalPriser){
            if (elpris.timeStart().toLocalDate().isEqual(currentDate)){
                priserForDisplay.add(elpris);
            }
        }

        if (chargingHour != null) {
            hittaBilligasteIntervallet(totalPriser, chargingHour);
        } else {

            if (isSorted) {
                priserForDisplay.sort((p1, p2) -> Double.compare(p2.sekPerKWh(), p1.sekPerKWh()));
            }
            visaPriser(priserForDisplay);
            System.out.println("--------------------------------------------------------");
            beräknaMedelpris(priserForDisplay);
            hittaBilligasteOchDyrasteTimme(priserForDisplay);
        }

    }

    private static void showHelp() {
        System.out.println(" --zone SE1 | SE2 | SE3 | SE4   (Ange elområde)");
        System.out.println(" --date           (YYYY-MM-DD)");
        System.out.println(" --sorted         (sorterar priserna med dyraste först)");
        System.out.println(" --charging 2h | 4h | 8h   (Beräknar billigaste laddningsfönstret)");
        System.out.println(" --help         (Visar denna infon)");
        System.out.println(" -----------------------------------------------------------------");


    }

    private static String formatOre(double sekPerKWh) {

        double ore = sekPerKWh * 100.0;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("sv", "SE"));
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        return df.format(ore);
    }

    private static void visaPriser(List<ElpriserAPI.Elpris> priser) {
        if(!isSorted){
            System.out.println("\n----Timpriser------");
        }
        for (ElpriserAPI.Elpris elpris : priser) {
            String startTId = elpris.timeStart().toLocalTime().format(timeFormatter);
            String slutTid = elpris.timeEnd().toLocalTime().format(timeFormatter);
            String formateratPris = formatOre(elpris.sekPerKWh());
            System.out.println(startTId + "-" + slutTid + " " + formateratPris + " öre");
        }
    }

    private static void beräknaMedelpris(List<ElpriserAPI.Elpris> dagensPris) {
        if (dagensPris == null || dagensPris.isEmpty()) return;
        double sum = 0.0;
        for (ElpriserAPI.Elpris elpris : dagensPris) {
            sum += elpris.sekPerKWh();
        }
        //dividera med antalet
        double medelPris = sum / dagensPris.size();
        String result = String.format(" Medelpriset för idag är %s kr/kWh", formatOre(medelPris));
        System.out.println(result);
    }

    private static void hittaBilligasteOchDyrasteTimme(List<ElpriserAPI.Elpris> dagensPris) {
        if (dagensPris == null || dagensPris.isEmpty()) return;
        ElpriserAPI.Elpris förstaPris = dagensPris.get(0);

        ElpriserAPI.Elpris lägsta=dagensPris.get(0);
        ElpriserAPI.Elpris högsta=dagensPris.get(0);

        double lägstaPris = förstaPris.sekPerKWh();
        LocalDateTime tidLägsta = förstaPris.timeStart().toLocalDateTime();

        double högstaPris = förstaPris.sekPerKWh();
        LocalDateTime tidHögsta = förstaPris.timeStart().toLocalDateTime();

        for (int i = 1; i < dagensPris.size(); i++) {
            ElpriserAPI.Elpris elpris = dagensPris.get(i);
            double pris = elpris.sekPerKWh();
            LocalDateTime startTid = elpris.timeStart().toLocalDateTime();

            if (pris < lägstaPris || pris == lägstaPris && startTid.isBefore(tidLägsta)) {
                lägstaPris = pris;
                tidLägsta = startTid;
                lägsta=elpris;
            }
            if (pris > högstaPris || pris == högstaPris && startTid.isBefore(tidHögsta)) {
                högstaPris = pris;
                tidHögsta = startTid;
                högsta=elpris;
            }
        }


            System.out.println(" Lägsta priset idag är: " + formatOre(lägsta.sekPerKWh()) + " öre och det är klockan "
                + lägsta.timeStart().toLocalTime().format(timeFormatter));
            System.out.println(" Högsta priset idag är: " + formatOre(högsta.sekPerKWh()) + " öre och det är klockan "
                + högsta.timeStart().toLocalTime().format(timeFormatter));

        }

    public static void hittaBilligasteIntervallet(List<ElpriserAPI.Elpris> priser, int timmar) {

        if (priser.size() < timmar) {
            System.out.println("inte tillräckligt många timmar för ett " + timmar + "hs fönster");
            return;
        }

        double lägstaMedel = Double.MAX_VALUE;
        String bästStartTid = null;
        String bästSlutTid = null;

        //testa alla startindex
        for (int i = 0; i < priser.size() - timmar; i++) {
            double sum = 0.0;

            for (int j = 0; j < timmar; j++) {
                sum = sum + priser.get(i + j).sekPerKWh();

            }

            double medel = sum / timmar;

            if (medel < lägstaMedel) {
                lägstaMedel = medel;
                bästStartTid = priser.get(i).timeStart().toLocalDateTime().format(timeFormatter);
                bästSlutTid = priser.get(i + timmar - 1).timeEnd().toLocalDateTime().format(timeFormatter);

            }


        }
        if (bästStartTid != null && bästSlutTid != null) {
            System.out.println("billigaste " + timmar + " h- fönstret: "
                    + bästStartTid + " - " +
                    bästSlutTid);
            String result = String.format(" | Snittpris %.3f kr /kWh", formatOre(lägstaMedel));
            System.out.println(result);
        }


    }
}
















