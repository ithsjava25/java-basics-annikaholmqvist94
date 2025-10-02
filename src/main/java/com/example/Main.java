package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;



public class Main {

    private static ElpriserAPI.Prisklass currentZone = null;
    private static LocalDate currentDate = LocalDate.now();
    private static boolean isSorted = false;
    private static Integer chargingHour = null;

    private static final ElpriserAPI elpriserAPI = new ElpriserAPI();

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private record programArguments(ElpriserAPI.Prisklass zone, LocalDate date, boolean sorted, Integer chargingHour,
                                    boolean showHelp) {
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
            } catch (IllegalArgumentException e) {
                System.out.println("fel i argument" + e.getMessage());
                showHelp();
            } catch (IllegalStateException e){
                System.out.println("Programfel: " + e.getMessage());
                showHelp();
            }


        } else {
            System.out.println("skriv help för kommando");
            showHelp();

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
                        }catch (IllegalArgumentException e)
                        {throw new IllegalArgumentException("ogiltig zon. VÄlj SE1, SE2, SE3, SE4.");
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
                        }catch (DateTimeParseException e)
                        {throw new IllegalArgumentException("ogiltigt datumformat, använd YYY-MM-SS.");
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
                            throw new IllegalArgumentException("ogiltig period, välj 2 h, 4h eller 8h. ");
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
            System.out.println("inga priser");
            return;

        }

        List<ElpriserAPI.Elpris> priserForDisplay=new java.util.ArrayList<>();
        for(ElpriserAPI.Elpris elpris : totalPriser){
            if (elpris.timeStart().toLocalDate().isEqual(currentDate)){
                priserForDisplay.add(elpris);
            }
        }

        if (chargingHour != null) {
            int intervallMinuter=beräknaIntervalletIMinuter(totalPriser);

            hittaBilligasteIntervallet(totalPriser,chargingHour,intervallMinuter);

        } else {

            if (isSorted) {
                priserForDisplay.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh));
            }
            visaPriser(priserForDisplay);
            System.out.println("--------------------------------------------------------");
            beräknaMedelpris(priserForDisplay);
            hittaBilligasteOchDyrasteTimme(priserForDisplay);
        }

    }

    private static void showHelp() {
        System.out.println(" Användning (usage): java Main <kommando>");
        System.out.println(" -----------------------------------------------------------------");
        System.out.println(" --zone SE1 | SE2 | SE3 | SE4   (Ange elområde)");
        System.out.println(" --date           (YYYY-MM-DD)");
        System.out.println(" --sorted         (sorterar priserna med dyraste först)");
        System.out.println(" --charging 2h | 4h | 8h   (Beräknar billigaste laddningsfönstret)");
        System.out.println(" --help         (Visar denna infon)");
        System.out.println(" -----------------------------------------------------------------");


    }

    private static String formatOre(double sekPerKWh) {

        double ore = sekPerKWh * 100.0;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.of("sv", "SE"));
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        return df.format(ore);
    }

    private static void visaPriser(List<ElpriserAPI.Elpris> priser) {
        final DateTimeFormatter hourFormatter=DateTimeFormatter.ofPattern("HH");

        if(!isSorted){
            System.out.println("\n----Timpriser------");
        }
        for (ElpriserAPI.Elpris elpris : priser) {
            String startTId = elpris.timeStart().toLocalTime().format(hourFormatter);
            String slutTid = elpris.timeEnd().toLocalTime().format(hourFormatter);
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
        String result = String.format(" Medelpriset för idag är %s öre/kWh", formatOre(medelPris));
        System.out.println(result);
    }

    private static void hittaBilligasteOchDyrasteTimme(List<ElpriserAPI.Elpris> dagensPris) {
        if (dagensPris == null || dagensPris.isEmpty()) return;

        final DateTimeFormatter hourFormatter=DateTimeFormatter.ofPattern("HH");

        ElpriserAPI.Elpris lägsta = dagensPris.getFirst();
        ElpriserAPI.Elpris högsta=dagensPris.getFirst();

        double lägstaPris = lägsta.sekPerKWh();
        LocalDateTime tidLägsta = lägsta.timeStart().toLocalDateTime();

        double högstaPris = högsta.sekPerKWh();
        LocalDateTime tidHögsta = högsta.timeStart().toLocalDateTime();

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



            String intervallLägsta=lägsta.timeStart().toLocalTime().format(hourFormatter) + "-" + lägsta.timeEnd().toLocalTime().format(hourFormatter);
            String intervallHögsta=högsta.timeStart().toLocalTime().format(hourFormatter) + "-" + högsta.timeEnd().toLocalTime().format(hourFormatter);;

            System.out.println(" Lägsta priset idag är: " + formatOre(lägsta.sekPerKWh()) + " öre och det är i intervallet:  "
                + intervallLägsta);
            System.out.println(" Högsta priset idag är: " + formatOre(högsta.sekPerKWh()) + " öre och det är i intervallet:  "
                + intervallHögsta);

        }


    public static int beräknaIntervalletIMinuter(List<ElpriserAPI.Elpris> priser){
        //vi vill göra ett sliding window för att ta reda på intervallet vi ska beräkan billigaste intervallet på.

        if(priser==null || priser.size()< 2) {
            return 60;
        }

        //skillnaden punkt 1,2
        long minutes=java.time.Duration.between
                (priser.getFirst().timeStart(),
                priser.get(1).timeStart()).toMinutes();

        return Math.toIntExact(minutes);
    }

    
    public static void hittaBilligasteIntervallet(List<ElpriserAPI.Elpris> priser, int timmar, int intervallMinuter) {
 //timmar*60 /intervallet är antaletpunkter
        int antalPrispunkter=(timmar*60)/intervallMinuter;

        if (priser.size() < antalPrispunkter) {
            System.out.println("inte tillräckligt många timmar för ett " + timmar + "hs fönster");
            return;
        }

        double lägstaMedel = Double.MAX_VALUE;
        String bästStartTid = null;
        String bästSlutTid = null;

        //testa alla startindex
        for (int i = 0; i <= priser.size() - antalPrispunkter; i++) {
            double sum = 0.0;

            for (int j = 0; j < antalPrispunkter; j++) {
                sum = sum + priser.get(i + j).sekPerKWh();

            }

            double medel = sum / antalPrispunkter;

            if (medel < lägstaMedel) {
                lägstaMedel = medel;
                bästStartTid = priser.get(i).timeStart().toLocalDateTime().format(timeFormatter);
                bästSlutTid = priser.get(i + antalPrispunkter - 1).timeEnd().toLocalDateTime().format(timeFormatter);

            }


        }

            System.out.println("Påbörja laddning kl " + bästStartTid + " för att få det billigaste " + timmar + "h-fönstret");
            String result = String.format(" Medelpris för fönster: %s öre/kWh", formatOre(lägstaMedel));
            System.out.println(result);

            System.out.println("Laddning slutar kl: " + bästSlutTid);



    }
}
















