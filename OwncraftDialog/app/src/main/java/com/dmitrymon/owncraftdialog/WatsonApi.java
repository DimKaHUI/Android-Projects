package com.dmitrymon.owncraftdialog;

public abstract class WatsonApi
{
    public abstract void SendUserInput(String text);
    public abstract String ReadWatsonAnswer();
}
