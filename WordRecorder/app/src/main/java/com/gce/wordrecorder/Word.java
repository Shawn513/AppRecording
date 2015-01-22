package com.gce.wordrecorder;

import android.os.Parcel;
import android.os.Parcelable;

public class Word { //implements Parcelable {
    private char word;
    private String vowel;
    private String consonant;

    public Word(char word, String vowel, String consonant) {
        this.word = word;
        this.vowel = vowel;
        this.consonant = consonant;
    }

    public char getWord() {
        return this.word;
    }

    public String getVowel() {
        return this.vowel;
    }

    public String getConsonant() {
        return this.consonant;
    }

    /*@Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        char[] wordArray = {word};
        dest.writeCharArray(wordArray);
        dest.writeString(vowel);
        dest.writeString(consonant);
    }

    public static final Parcelable.Creator<Word> CREATOR = new Parcelable.Creator<Word>() {
        public Word createFromParcel(Parcel in) {
            char word = ' ';
            char[] wordList = {word};
            in.readCharArray(wordList);
            String vowel = in.readString();
            String consonant = in.readString();
            return new Word(wordList[0], vowel, consonant);
        }

        public Word[] newArray(int size) {
            return new Word[size];
        }*/


}
