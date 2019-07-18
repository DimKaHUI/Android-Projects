package com.dmitrymon.cipherbox;

import java.util.List;

public class ShellSort<T>
{
    public static class ComparatorCollection
    {
        public static class StringComparator extends Comparator<String>
        {
            boolean caseSense = true;
            @Override
            public Result compare(String a, String b)
            {
                String str1 = a;
                String str2 = b;
                if(!caseSense)
                {
                    str1 = str1.toLowerCase();
                    str2 = str2.toLowerCase();
                }
                int cmp = str1.compareTo(str2);
                if(cmp == 0)
                    return Result.EQUAL;
                if(cmp > 0)
                    return Result.GREATER;
                return Result.LOWER;
            }

            public StringComparator(boolean caseSense)
            {
                this.caseSense = caseSense;
            }
        }
    }

    public interface SwapCallback
    {
        void onSwap(int a, int b);
    }

    public abstract static class Comparator<T>
    {
        public enum Result
        {
            LOWER, EQUAL, GREATER
        }

        public abstract Result compare(T a, T b);
    }

    public boolean isSorted(T[] arr)
    {
        for(int i = 0; i < arr.length - 1; i++)
        {
            T a = arr[i];
            T b = arr[i + 1];
            Comparator.Result result = comparator.compare(a, b);
            if(result == Comparator.Result.GREATER)
                return false;
        }
        return true;
    }

    private Comparator<T> comparator;
    private SwapCallback swapCallback;

    ShellSort(Comparator<T> comparator)
    {
        this.comparator = comparator;
    }

    public void setSwapCallback(SwapCallback callback)
    {
        swapCallback = callback;
    }

    public void sort(List<T> arr)
    {
        int increment = arr.size() / 2;
        while(increment >= 1)
        {
            for(int startInd = 0; startInd < increment; startInd++)
            {
                // TODO Call insertionSort(arr, startInd, increment)
                insertionSort(arr, startInd, increment);
            }
            increment /= 2;
        }
    }

    private void insertionSort(List<T> arr, int start, int inc)
    {
        for(int i = start; i< arr.size() - 1; i += inc)
        {
            for (int j = Math.min(i + inc, arr.size() - 1); j - inc >= 0; j -= inc)
            {
                T a = arr.get(j - inc);
                T b = arr.get(j);
                Comparator.Result compareResult = comparator.compare(a, b);
                if(compareResult == Comparator.Result.GREATER)
                {
                    arr.set(j, a);
                    arr.set(j - inc, b);
                    if(swapCallback != null)
                    {
                        swapCallback.onSwap(j, j - inc);
                    }
                }
                else
                    break;
            }
        }
    }
}
