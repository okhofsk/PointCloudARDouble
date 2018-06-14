package com.example.okhof.pointcloudsardouble.rendering;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// --Reader Class--
/**
 * created by Patrick Ley on 26.03.2018.
 * using the name of a file reads the header
 * and then stores the points and color in two
 * float arrays. These can be accessed through
 * the getters
 */

public class PointCloudReader {
    private float[] points;
    private float[] colors;
    private int numLines;
    private String name;

    public PointCloudReader(){
        points = null;
        colors = null;
        numLines = 0;
    }

    // -- Getters --
    public int getNumLine(){return numLines;}

    public float[] getPoints(){return points;}

    public float[] getColor(){return colors;}

    public String getName(){return name;}


    // -reader function-
    public void reads(String fileName, Context mContext)throws IOException {
        name = fileName;
        float factor = 1.0f;
        float translation = 0.0f;
        String cases = fileName.substring(0,4);

        // --Moving content from 0.0 and scaling for experiment--
        /*(content specific)*/
        switch (cases){
            case "amph":
                factor = 0.831f;
                translation = 0.0f;
                break;
            case "bipl":
                factor = 1.155f;
                translation = 0.5f;
                break;
            case "long":
                factor = 1.432f;
                translation = 0.0f;
                break;
            case "loot":
                factor = 1.181f;
                translation = 0.0f;
                break;
            case "mask":
                factor = 0.659f;
                translation = 0.5f;
                break;
            case "sold":
                factor = 1.0f;
                translation = -0.3f;
                break;
            case "stat":
                factor = 1.399f;
                translation = 0.0f;
                break;
            default:
                Log.e("ReaderError", "NofileFoundForScaling");
        }
        Log.e("ListNotification", "Cases: " + cases + " " + Float.toString(factor));


        // --For Colored Point Clouds .ply format--

        // -setup-
        AssetManager am = mContext.getAssets();
        InputStream is = am.open(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        // -read and handle header-
        String temp = null;
        String[] v_array = null;
        boolean bool = true;
        do{
            temp = br.readLine();
            v_array = temp.split(" ");
            if(v_array.length == 3){
                if(v_array[0].equals("element"))
                    if(v_array[1].equals("vertex"))
                        bool = false;
            }
        }while(bool);
        numLines = Integer.parseInt(v_array[2]);
        do{
            temp = br.readLine();
        }while(!temp.equals("end_header"));


        // -read file and store in arrays-
        /* apply scaling and translation here */
        points = new float[numLines * 4];
        colors = new float[numLines * 4];
        int j = 0;
        for (int i = 0; i < numLines; i++) {
            for (String s : br.readLine().split(" ")) {
                if(j == 0)points[4 * i + j] = Float.parseFloat(s)*factor;
                else if(j == 1)points[4 * i + j] = Float.parseFloat(s)*factor+translation;
                else if(j == 2)points[4 * i + j] = Float.parseFloat(s)*factor;
                else colors[4 * i + j - 3] = Float.parseFloat(s)/255.0f;
                j++;
            }
            points[4 * i + 3] = 1.0f;
            colors[4 * i + 3] = 1.0f;
            j = 0;
        }

        br.close();is.close();
    }
}
