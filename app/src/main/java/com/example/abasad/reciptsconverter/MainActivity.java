package com.example.abasad.reciptsconverter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    String fromCurr ;
    String toCurr;
    HashMap<String, Double> currenciesMap;
    ArrayList<String> currenciesCodes;
    Boolean hasLastRec;

    Spinner fromSpinner;
    Spinner toSpinner;
    ImageView imageView;
    TextView receiptInfoTxt;
    TextView ReciptTitle;

    // photo utilities
    static final int REQUEST_TAKE_PHOTO = 1;
    String currentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fromSpinner = findViewById(R.id.fromSpinner);
        toSpinner = findViewById(R.id.toSpinner);
        imageView = findViewById(R.id.imageView);
        receiptInfoTxt = findViewById(R.id.receiptInfoTxt);
        receiptInfoTxt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ReciptTitle = findViewById(R.id.ReciptTitle);
        currentPhotoPath = new String();
        // Initiliaze and fill the currencies
        currenciesMap = new HashMap<String,Double>();
        currenciesCodes =new ArrayList();
        fillCurrienciesMap();
        ArrayAdapter<String> currenciesArrayAdapter = new ArrayAdapter<String>(this, R.layout.spinnertext, currenciesCodes );

        fromSpinner.setAdapter(currenciesArrayAdapter);
        toSpinner.setAdapter(currenciesArrayAdapter);



          // Drawable d = Drawable.createFromPath("storage/emulated/0/Android/data/com.example.abasad.reciptsconverter/files/Pictures/receipt2.jpg");
        Drawable d = Drawable.createFromPath(currentPhotoPath);
            imageView.setImageDrawable(d);


    }
    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences settings = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("toCurr",   toCurr);
        editor.putString("fromCurr", fromCurr);
        if(currentPhotoPath != null)
        editor.putString("currentPath",   currentPhotoPath);

        // commit the shared pref
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences settings = getSharedPreferences("myPref",  Context.MODE_PRIVATE);
        fromCurr =settings.getString("fromCurr","USD");
        toCurr = settings.getString("toCurr","CAD");
        fromSpinner.setSelection(((ArrayAdapter)fromSpinner.getAdapter()).getPosition(fromCurr));
        toSpinner.setSelection(((ArrayAdapter)toSpinner.getAdapter()).getPosition(toCurr));

        if(currentPhotoPath.isEmpty())
        currentPhotoPath = settings.getString("currentPath","");
      if  (currentPhotoPath.length() == 0)  hasLastRec = false; else hasLastRec = true;
      if(hasLastRec)
      {ReciptTitle.setText(getString(R.string.LastReceiptTxt));
      processReceiptFromImage();
      }
          else
        ReciptTitle.setText("");


    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
               receiptInfoTxt.setText("Error occurred when creating file\nplease make sure there is\n enough space in your phone.");

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);



            }


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            processReceiptFromImage();
        }


    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        File prevRec = new File(currentPhotoPath);
        if(prevRec.exists()) prevRec.delete();

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private  void processReceiptFromImage()
    {

        Drawable d = Drawable.createFromPath(currentPhotoPath);
        imageView.setImageDrawable(d);


        FirebaseVisionImage image = null;
        try {
            image = FirebaseVisionImage.fromFilePath(this, Uri.fromFile(new File(currentPhotoPath)));
        } catch (IOException e) {
            receiptInfoTxt.setText("Sorry reading this receipt failed,\nplease try again.");
        }


                // the on-device model for text recognition
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        // pass the image to the processImage method
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                Log.d("TEST1TEST", firebaseVisionText.getText() + " R   ");

                                Receipts r = getReceipts(firebaseVisionText.getText());
                               if(r != null)
                               {
                                   double toValue = (r.total
                                           / currenciesMap.get(fromCurr) )
                                           * currenciesMap.get(toCurr);
                                   String receiptReport = String.format("Your visit to %s\ncost you %.2f in %s\nthat is %.2f in %s", r.type, r.total, fromCurr,toValue, toCurr);

                                   receiptInfoTxt.setText(receiptReport);

                               }
                               else
                                   receiptInfoTxt.setText("Seems that the picture you have taken\nis not a receipt.");

                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                        receiptInfoTxt.setText("Sorry reading this receipt failed,\nplease try again.");
                                    }
                                });
    }


    // Currencies Utilities
    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getAssets().open("Currencies.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }

    private void fillCurrienciesMap()
    {
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            JSONObject feedJson = obj.getJSONObject("feed");

            JSONArray entryJsonArr = feedJson.getJSONArray("entry");

            currenciesCodes.add("USD");
            currenciesMap.put("USD", 1.0);
            for (int i = 0; i < entryJsonArr.length(); i++) {
                JSONObject currencyJson = entryJsonArr.getJSONObject(i);


                JSONObject codeJson = currencyJson.getJSONObject("title");

                JSONObject valueJson = currencyJson.getJSONObject("content");
                String CurrCode = codeJson.getString("$t");
                String currValue = valueJson.getString("$t");
                if(currValue.length() < 8)
                    continue;
                double CurrValue = 0;

try { CurrValue = Double.parseDouble( (valueJson.getString("$t")).substring(7)) ;  }
catch (NumberFormatException e) {   continue;}

currenciesMap.put(CurrCode, CurrValue);
currenciesCodes.add(CurrCode);
            } // end of for

        } catch (JSONException e) {

            e.printStackTrace();
        }
    }


    // Buttons Callbacks
    public void CaptureReciptClick(View view)
    {
        String fromCurrTemp = fromSpinner.getSelectedItem().toString();
       String  toCurrTemp = toSpinner.getSelectedItem().toString();
        if(toCurrTemp == fromCurrTemp)
            Toast.makeText(this, getString(R.string.ErrorSameCurr), Toast.LENGTH_LONG).show();
        else
        {
            fromCurr = fromCurrTemp;
            toCurr = toCurrTemp;
            dispatchTakePictureIntent();
        }

    }

    public void onAboutClicked(View view) {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

   private Receipts getReceipts(String text) {

       ArrayList<Double>  originalResult = Utilites.findFloat(text);
        if (originalResult.isEmpty()) return null;
        else {
            Receipts receipts = new Receipts();
            Double totalF =  Collections.max(originalResult);
            receipts.total = totalF;
            receipts.type = Utilites.firstLine(text);
            return receipts;
        }
    }


    }
