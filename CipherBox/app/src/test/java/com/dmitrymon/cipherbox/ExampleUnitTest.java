package com.dmitrymon.cipherbox;

import org.junit.Test;

import java.util.Base64;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest
{
    Random rnd = new Random();

    @Test
    public void stringDecodingTest()
    {
        String filename = "examplefile.file";
        byte[] encodedBytes = Base64.getEncoder().encode(filename.getBytes());
        String encodedString = new String(encodedBytes);
        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        String decodedString = new String(decodedBytes);
        assertEquals(filename, decodedString);
    }

    private String[] generateStrings(int count, int minLength, int maxLength)
    {
        String[] res = new String[count];
        for(int i = 0; i < count; i++)
        {
            StringBuilder builder = new StringBuilder();
            int len = rnd.nextInt(maxLength - minLength) + minLength;
            for(int j = 0; j < len; j++)
                builder.append(rnd.nextInt(255));
            res[i] = builder.toString();
        }
        return res;
    }

    @Test
    public void stringSortTest()
    {
        String[] strings = generateStrings(20, 5, 9);
        ShellSort.Comparator<String> comparator = new ShellSort.ComparatorCollection.StringComparator();
        ShellSort<String> sorter = new ShellSort<String>(comparator);
        sorter.sort(strings);
        boolean sorted = sorter.isSorted(strings);
        assertEquals(true, sorted);
    }
}