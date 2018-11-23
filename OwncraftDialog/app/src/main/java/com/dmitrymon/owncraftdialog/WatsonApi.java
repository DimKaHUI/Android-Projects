package com.dmitrymon.owncraftdialog;

public abstract class WatsonApi
{
    public abstract void StartSession();
    public abstract void EndSession();
    public abstract void SendUserInput(String text);
    public abstract String ReadWatsonAnswer();

    public abstract void SetSendUserInputCallback(WatsonCallback callback);
    public abstract void SetStartSessionCallback(WatsonCallback callback);

    public static class WatsonCallback
    {
        public void onSuccess()
        {

        }

        public void onFail()
        {

        }
    }
}
