package com.example.abasad.reciptsconverter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class Utilites {

     public static ArrayList<Double> findFloat(String text)
     {
         //get digits from result
         if (text.isEmpty())
             return new ArrayList<Double>();

         ArrayList<Double> matches = new ArrayList<Double>();
         Matcher m =  Pattern.compile("[+-]?([0-9]*[.])?[0-9]+")
                 .matcher(text);
         while (m.find()) {
             String str = m.group();
            if(isFloatAndWhole(str))
             matches.add(Double.parseDouble( str));
         }

        return matches;
     }

    private static Boolean isFloatAndWhole(String str){return str.matches("\\d*\\.\\d*");}

     public static String firstLine(String str)  {
         if (str.isEmpty()) return "";
         return str.split("\n")[0];
     }



    public static Boolean isFloat(String str) {
         try {
             Double d = Double.valueOf(str);
             return d !=  (double)d.intValue();
         } catch ( Exception e) {
             return false;
         }
     }
}
