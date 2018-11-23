package com.dmitrymon.owncraftdialog;

public class NotImplementedException extends Exception
{
    @Override
    public String getMessage()
    {
        return "Feature is not implemented!";
    }
}
