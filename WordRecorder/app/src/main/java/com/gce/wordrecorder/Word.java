package com.gce.wordrecorder;

public class Word {
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
}
