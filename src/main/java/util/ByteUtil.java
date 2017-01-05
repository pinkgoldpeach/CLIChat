package util;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by dev on 12/5/15.
 */
public final class ByteUtil {

    public static Deque<byte[]> getWords(byte[] message) {
        Deque<byte[]> deque = new LinkedList<>();
        Deque<Integer> positions = new LinkedList<>();

        int current_pos = 0;

        for (int i = 0; i < message.length; i++)
            if (message[i] == ' ')
                positions.add(i);

        positions.add(message.length);

        while (!positions.isEmpty()) {
            int pos = positions.poll();
            int wordSize = pos - current_pos;

            byte[] word = new byte[wordSize];

            for (int j = 0; j < wordSize; j++) {
                word[j] = message[current_pos];
                current_pos++;
            }

            current_pos++;
            deque.add(word);
        }

        return deque;
    }

    public static byte[] setWords(Deque<byte[]> wordList) {
        int wholeSize = wordList.size() - 1;
        int currentPos = 0;

        for (byte[] word : wordList) {
            wholeSize += word.length;
        }

        byte[] message = new byte[wholeSize];

        while (!wordList.isEmpty()) {
            byte[] word = wordList.poll();

            for (int i = 0; i < word.length; i++) {
                message[currentPos] = word[i];
                currentPos++;
            }
            if (!wordList.isEmpty()) {
                message[currentPos] = ' ';
                currentPos++;
            }
        }

        return message;
    }

    public static boolean isEqual(byte[] word1, byte[] word2) {
        if (word1.length != word2.length)
            return false;

        for (int i = 0; i < word1.length; i++)
            if (word1[i] != word2[i])
                return false;

        return true;
    }
}
