package com.dmitrymon.owncraftdialog;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Entities
class EntityCollector
{
    private String name;            // Entity name: sys-person
    private String location;
    private int age = 0;                // Entity name: sys-number
    private boolean pain;           // Entity name: pain
    private boolean smoking;      // Entity name: smoking
    private boolean drinking;
    private boolean hadActivity;

    private boolean nameCollected, ageCollected, painCollected, smokingCollected,
    drinkingCollected, hadActivityCollected, locationCollected;

    public String getUserName()
    {
        return name;
    }

    public String getLocation()
    {
        return location;
    }

    public int getAge()
    {
        return age;
    }
    public boolean getPain()
    {
        return pain;
    }
    public boolean getSmoking()
    {
        return smoking;
    }
    public boolean getDrinking(){return drinking;}
    public boolean getHadActivity(){return hadActivity;}

    public boolean isAlertDataCollected()
    {
        return nameCollected && ageCollected && painCollected && locationCollected;
    }
    public boolean isAuxDataCollected(){return smokingCollected && drinkingCollected && nameCollected;}
    public boolean isHadActivityCollected(){return hadActivityCollected;}


    public void onNewMessage(JSONObject watsonResponse)
    {
        try
        {
            JSONArray entityArray = watsonResponse.getJSONObject("output").getJSONArray("entities");

            for(int i = 0; i < entityArray.length(); i++)
            {
                JSONObject element = entityArray.getJSONObject(i);
                String entity = element.getString("entity");
                String value = element.getString("value");
                switch (entity)
                {
                    case "sys-person":
                        name = value;
                        nameCollected = true;
                        break;
                    case "sys-number":
                        age = Integer.parseInt(value);
                        ageCollected = true;
                        break;
                    case "pain":
                        pain = value.equals("Have pain");
                        painCollected = true;
                        break;
                    case "Smoking":
                        smoking = value.equals("I smoke");
                        smokingCollected = true;
                        break;
                    case "Drinking":
                        drinking = value.equals("Drink");
                        drinkingCollected = true;
                        break;
                    case "HadActivity":
                        hadActivity = value.equals("Activity present");
                        hadActivityCollected = true;
                        break;
                    case "sys-location":
                        locationCollected = true;
                        location = value;
                        break;


                }
            }

        }
        catch (JSONException ex)
        {
            ex.printStackTrace();
        }

    }

    @Override
    public String toString()
    {
        return name + "\n" + age + "\n" + pain + "\n" + location + "\n" + drinking + "\n" + smoking;
    }

    public String getData()
    {
        return isAlertDataCollected() + " " + isAuxDataCollected() + " " + name + " " + age + " " + pain + " " + location + " " + drinking + " " + smoking;
    }
}
