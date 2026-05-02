package SiminRLStormScheduler.Calculations;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Utility {

    public static double JainIndex(int m, double[] x) {
        double JainIndex = 0;

        double sum1 = 0, sum2 = 0;
        for (int i = 0; i < m; i++) {
            sum1 += x[i];
        }
        for (int i = 0; i < m; i++) {
            sum2 += x[i] * x[i];
        }
        JainIndex = (sum1 * sum1) / m * sum2;
        return JainIndex;

    }

    public static double NetworkUsageNode(int port, String fileName, int maxRound) {
        File file = new File(fileName);
        Scanner input = null;
        try {
            input = new Scanner(file);
            List<String> list = new ArrayList<String>();

            while (input.hasNextLine()) {
                list.add(input.nextLine());
            }
            System.out.println("first line: " + list.get(0));

            String portStr = String.valueOf(port);
            double sum=0;
            int count = 0;
            String receive = "";
            for (String line : list) {
                String[] lineArrayTotal = line.trim().split(" ");
                List<String> lineArray = new ArrayList<>();
                for(int i =0 ; i< lineArrayTotal.length; i++){
                    if(!lineArrayTotal[i].trim().isEmpty())
                        lineArray.add(lineArrayTotal[i].trim());
                }
                if(lineArray.size() >= 4) {
                    if (/*lineArray.get(1).contains("gnome-terminal") &&*/ lineArray.get(2).trim().equalsIgnoreCase(portStr)
                     || /*lineArray.get(2).contains("gnome-terminal") &&*/ lineArray.get(3).trim().equalsIgnoreCase(portStr)) {
                        receive = lineArray.get(0).trim().substring(0, lineArray.get(0).length() - 1);//1357B
                        if(!receive.isEmpty()) {
                            sum += Integer.valueOf(receive);
                            count++;
                            if(count >= maxRound)
                                break;
                        }
                    }
                }
            }
            System.out.println("count: " + count + " avg: " + sum/count);
            return sum/count;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


}
